import json
import os
import subprocess
import sys
import tempfile
import unicodedata
import wave
import zipfile
from pathlib import Path
from typing import Callable, Dict, List, Optional, Tuple

from .resource_paths import resolve_resources_root
from .runtime_manager import resolve_cuda_python, resolve_piper_runtime_python

PREVIEW_TAIL_PAD_SECONDS = 0.25


def _base_dir() -> Path:
    env_base = os.environ.get("KGTTS_BASE_DIR")
    if env_base:
        return Path(env_base)
    if getattr(sys, "frozen", False):
        return Path(sys.executable).resolve().parent
    return Path(__file__).resolve().parents[2]


def _progress(cb: Optional[Callable[[str, float, str], None]], value: float, msg: str) -> None:
    if cb:
        cb("preview", value, msg)


def _load_voicepack_base(voicepack_path: Path, temp_dir: Path) -> Tuple[Path, dict]:
    if voicepack_path.is_dir():
        manifest_path = voicepack_path / "manifest.json"
        if not manifest_path.exists():
            archive_candidates = [
                voicepack_path / "voicepack.kigvpk",
                voicepack_path / "voicepack.zip",
            ]
            zip_candidate = next((candidate for candidate in archive_candidates if candidate.exists()), None)
            if zip_candidate is not None:
                return _load_voicepack_base(zip_candidate, temp_dir)
            raise FileNotFoundError("这个语音包目录不完整，无法试听。")
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        return voicepack_path, manifest
    if not zipfile.is_zipfile(voicepack_path):
        raise RuntimeError("这个文件不是有效的语音包，无法试听。")
    with zipfile.ZipFile(voicepack_path, "r") as zf:
        zf.extractall(temp_dir)
    manifest_path = temp_dir / "manifest.json"
    if not manifest_path.exists():
        raise FileNotFoundError("这个语音包文件不完整，无法试听。")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    return temp_dir, manifest


def _resolve_voicepack_files(base_dir: Path, manifest: dict) -> Tuple[Path, Path, Path]:
    files = manifest.get("files") or {}
    model_rel = files.get("model")
    config_rel = files.get("config")
    dict_rel = files.get("phonemizer")
    if not model_rel or not config_rel or not dict_rel:
        raise RuntimeError("语音包缺少必要的模型或配置文件，无法试听。")
    model_path = base_dir / model_rel
    config_path = base_dir / config_rel
    dict_path = base_dir / dict_rel
    for path in (model_path, config_path, dict_path):
        if not path.exists():
            raise FileNotFoundError("语音包缺少必要文件，无法试听。")
    return model_path, config_path, dict_path


def _load_phonemizer_dict(dict_path: Path) -> Dict[str, List[str]]:
    mapping: Dict[str, List[str]] = {}
    for line in dict_path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split()
        if len(parts) >= 2:
            mapping[parts[0]] = parts[1:]
    return mapping


def _find_espeak_ng() -> Tuple[Path, Path]:
    candidates: List[Path] = []
    env_path = os.environ.get("ESPEAK_NG_PATH")
    if env_path:
        candidates.append(Path(env_path))
    resources_root = resolve_resources_root()
    if resources_root is not None:
        candidates.append(resources_root / "tools" / "espeak-ng" / "eSpeak NG" / "espeak-ng.exe")
        candidates.append(resources_root / "tools" / "espeak-ng" / "espeak-ng.exe")
    base = _base_dir()
    candidates.append(base / "tools" / "espeak-ng" / "eSpeak NG" / "espeak-ng.exe")
    candidates.append(base / "tools" / "espeak-ng" / "espeak-ng.exe")
    candidates.append(base / "resources_pack" / "tools" / "espeak-ng" / "eSpeak NG" / "espeak-ng.exe")
    candidates.append(base / "resources_pack" / "tools" / "espeak-ng" / "espeak-ng.exe")
    for exe in candidates:
        if exe.exists():
            data_dir = exe.parent / "espeak-ng-data"
            if data_dir.exists():
                return exe, data_dir
    raise RuntimeError("缺少发音组件，请先安装训练资源包后再试听。")


def _strip_language_flags(text: str) -> str:
    out: List[str] = []
    in_flag = False
    for ch in text:
        if in_flag:
            if ch == ")":
                in_flag = False
            continue
        if ch == "(":
            in_flag = True
            continue
        if ch in "\r\n":
            continue
        out.append(ch)
    return "".join(out)


def _phonemize_espeak(text: str, voice: str) -> List[str]:
    exe, data_dir = _find_espeak_ng()
    env = os.environ.copy()
    env["ESPEAK_DATA_PATH"] = str(data_dir)
    cmd = [
        str(exe),
        "-q",
        "--ipa",
        "-b",
        "1",
        "-v",
        voice,
        "--path",
        str(data_dir.parent),
        "--stdin",
    ]
    proc = subprocess.run(
        cmd,
        input=text,
        text=True,
        encoding="utf-8",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        env=env,
    )
    if proc.returncode != 0:
        err = (proc.stderr or proc.stdout).strip()
        raise RuntimeError(f"发音组件处理失败：{err}")
    out = proc.stdout.strip()
    if not out:
        return []
    out = _strip_language_flags(out)
    out = unicodedata.normalize("NFD", out)
    return [ch for ch in out if ch not in "\r\n"]


def _pick_trailing_punct(text: str) -> Optional[str]:
    mapping = {
        "，": ",",
        "。": ".",
        "！": "!",
        "？": "?",
        "：": ":",
        "；": ";",
        "、": ",",
    }
    for ch in reversed(text):
        if ch.isspace():
            continue
        if ch in mapping:
            return mapping[ch]
        if ch in ",.!?;:":
            return ch
        break
    return None


def _text_to_espeak_ids(text: str, cfg: dict) -> List[int]:
    raw_id_map = cfg.get("phoneme_id_map") or {}
    if not raw_id_map:
        return []
    id_map: Dict[str, List[int]] = {}
    for key, value in raw_id_map.items():
        if isinstance(value, list):
            id_map[key] = [int(v) for v in value]
        else:
            id_map[key] = [int(value)]
    voice = ""
    espeak_cfg = cfg.get("espeak") or {}
    if isinstance(espeak_cfg, dict):
        voice = str(espeak_cfg.get("voice") or "")
    if not voice:
        lang_cfg = cfg.get("language") or {}
        if isinstance(lang_cfg, dict):
            voice = str(lang_cfg.get("code") or "")
    if not voice:
        voice = "en"
    phoneme_map_raw = cfg.get("phoneme_map") or {}
    phoneme_map: Dict[str, List[str]] = {}
    for key, value in phoneme_map_raw.items():
        if isinstance(value, list):
            phoneme_map[key] = [str(v) for v in value]
        else:
            phoneme_map[key] = [str(value)]
    phonemes = _phonemize_espeak(text, voice)
    if phoneme_map:
        mapped: List[str] = []
        for ph in phonemes:
            repl = phoneme_map.get(ph)
            if repl:
                mapped.extend(repl)
            else:
                mapped.append(ph)
        phonemes = mapped
    tail = _pick_trailing_punct(text)
    if tail and tail in id_map:
        phonemes.append(tail)
    pad = id_map.get("_", [0])
    ids: List[int] = []
    bos = id_map.get("^")
    eos = id_map.get("$")
    if bos:
        ids.extend(bos)
        ids.extend(pad)
    for ph in phonemes:
        mapped_ids = id_map.get(ph)
        if not mapped_ids:
            continue
        ids.extend(mapped_ids)
        ids.extend(pad)
    if eos:
        ids.extend(eos)
    return ids


def _text_to_phoneme_ids(text: str, mapping: Dict[str, List[str]], id_map: dict) -> List[int]:
    ids: List[int] = []
    fallback = int(id_map.get("_", 0))
    for ch in text:
        if ch.isspace():
            continue
        phones = mapping.get(ch)
        if phones:
            for ph in phones:
                ids.append(int(id_map.get(ph, fallback)))
        else:
            ids.append(int(id_map.get(ch, fallback)))
    return ids


def _pick_input(names: List[str], keys: List[str], fallback_first: bool = False) -> Optional[str]:
    for key in keys:
        for name in names:
            if key in name.lower():
                return name
    if fallback_first and names:
        return names[0]
    return None


def _create_ort_session(ort, model_path: Path):
    """Create ONNX Runtime session with explicit providers for ORT>=1.9."""
    try:
        available = list(ort.get_available_providers() or [])
    except Exception:
        available = []

    # Keep preview inference predictable on desktop builds.
    if "CPUExecutionProvider" in available:
        providers = ["CPUExecutionProvider"]
    elif available:
        providers = available
    else:
        providers = ["CPUExecutionProvider"]

    return ort.InferenceSession(str(model_path), providers=providers)


def _preview_runtime_env(python_path: Path) -> dict:
    env = os.environ.copy()
    runtime_root = python_path.parent
    env["PYTHONHOME"] = str(runtime_root)
    env["PYTHONUNBUFFERED"] = "1"
    env["PYTHONIOENCODING"] = "utf-8"
    env["PYTHONUTF8"] = "1"
    path_parts = [
        str(runtime_root),
        str(runtime_root / "Scripts"),
        str(runtime_root / "Library" / "bin"),
        str(runtime_root / "Library" / "usr" / "bin"),
        str(runtime_root / "Library" / "mingw-w64" / "bin"),
    ]
    env["PATH"] = os.pathsep.join(path_parts + [env.get("PATH", "")])
    return env


def _preview_runtime_candidates() -> List[Path]:
    candidates: List[Path] = []
    for candidate in (resolve_piper_runtime_python(), resolve_cuda_python()):
        if candidate and candidate.exists() and candidate not in candidates:
            candidates.append(candidate)
    return candidates


def _has_inprocess_preview_deps() -> bool:
    try:
        import numpy  # noqa: F401
        import onnxruntime  # noqa: F401
    except Exception:
        return False
    return True


def _run_preview_in_runtime(
    python_path: Path,
    voicepack_path: Path,
    text: str,
    out_path: Path,
    progress: Optional[Callable[[str, float, str], None]],
) -> Path:
    backend_root = Path(__file__).resolve().parents[1]
    helper_code = (
        "import json,sys,traceback\n"
        "from pathlib import Path\n"
        f"sys.path.insert(0, {str(backend_root)!r})\n"
        "from engine.voice_preview import _synthesize_voicepack_inprocess\n"
        "payload=json.loads(Path(sys.argv[1]).read_text(encoding='utf-8'))\n"
        "def progress(stage,value,message):\n"
        "    print(json.dumps({'type':'progress','stage':stage,'value':value,'message':message}, ensure_ascii=False), flush=True)\n"
        "try:\n"
        "    result=_synthesize_voicepack_inprocess(Path(payload['voicepack_path']), payload['text'], Path(payload['out_path']), progress)\n"
        "    print(json.dumps({'type':'done','audio_path':str(result)}, ensure_ascii=False), flush=True)\n"
        "except Exception as exc:\n"
        "    print(json.dumps({'type':'error','message':str(exc),'traceback':traceback.format_exc()}, ensure_ascii=False), flush=True)\n"
        "    raise\n"
    )
    with tempfile.TemporaryDirectory() as tmp_dir:
        request_path = Path(tmp_dir) / "voice_preview_request.json"
        request_path.write_text(
            json.dumps(
                {
                    "voicepack_path": str(voicepack_path),
                    "text": text,
                    "out_path": str(out_path),
                },
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )
        proc = subprocess.Popen(
            [str(python_path), "-c", helper_code, str(request_path)],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            stdin=subprocess.DEVNULL,
            text=True,
            encoding="utf-8",
            errors="replace",
            env=_preview_runtime_env(python_path),
            cwd=str(python_path.parent),
            bufsize=1,
            close_fds=True,
        )
        output_tail: List[str] = []
        result_path: Optional[Path] = None
        if proc.stdout is not None:
            for line in proc.stdout:
                raw = line.strip()
                if raw:
                    output_tail.append(raw)
                    output_tail = output_tail[-20:]
                try:
                    event = json.loads(raw)
                except Exception:
                    continue
                event_type = event.get("type")
                if event_type == "progress":
                    message = str(event.get("message") or "")
                    try:
                        value = float(event.get("value") or 0.0)
                    except Exception:
                        value = 0.0
                    if progress and message:
                        progress(str(event.get("stage") or "preview"), value, message)
                elif event_type == "done":
                    audio_path = str(event.get("audio_path") or "")
                    if audio_path:
                        result_path = Path(audio_path)
        code = proc.wait()
        if code == 0 and result_path and result_path.exists():
            return result_path
        tail = "\n".join(output_tail[-8:])
        raise RuntimeError(tail or f"试听运行时退出，code={code}")


def _synthesize_voicepack_inprocess(
    voicepack_path: Path,
    text: str,
    out_path: Path,
    progress: Optional[Callable[[str, float, str], None]] = None,
) -> Path:
    _progress(progress, 0.02, "加载语音包")
    try:
        import numpy as np
        import onnxruntime as ort
    except Exception as exc:  # noqa: BLE001
        raise RuntimeError("试听组件未安装完整，请重新安装 Piper 基础运行时后再试。") from exc

    with tempfile.TemporaryDirectory() as tmp_dir:
        base_dir, manifest = _load_voicepack_base(voicepack_path, Path(tmp_dir))
        _progress(progress, 0.15, "解析语音包配置")
        model_path, config_path, dict_path = _resolve_voicepack_files(base_dir, manifest)
        cfg = json.loads(config_path.read_text(encoding="utf-8"))
        phoneme_type = str(cfg.get("phoneme_type", "text")).lower()
        id_map = cfg.get("phoneme_id_map") or {}
        if not id_map:
            raise RuntimeError("语音包缺少发音映射信息，无法试听。")
        if phoneme_type == "espeak":
            ids = _text_to_espeak_ids(text, cfg)
        elif phoneme_type == "text":
            phoneme_map = _load_phonemizer_dict(dict_path)
            ids = _text_to_phoneme_ids(text, phoneme_map, id_map)
        else:
            raise RuntimeError("当前语音包格式暂不支持试听。")
        if not ids:
            raise RuntimeError("试听文本无法转换为可朗读内容，请换一段文本再试。")

        infer_cfg = cfg.get("inference") or {}
        noise_scale = float(infer_cfg.get("noise_scale", 0.667))
        length_scale = float(infer_cfg.get("length_scale", 1.0))
        noise_w = float(infer_cfg.get("noise_w", infer_cfg.get("noise_scale_w", 0.8)))

        _progress(progress, 0.35, "加载 ONNX 模型")
        sess = _create_ort_session(ort, model_path)
        input_names = [i.name for i in sess.get_inputs()]
        input_name = _pick_input(input_names, ["input", "text", "phoneme"], fallback_first=True)
        length_name = _pick_input(input_names, ["length", "len"])
        if not input_name or not length_name:
            raise RuntimeError("语音包模型输入不完整，无法合成")
        scale_name = _pick_input(input_names, ["scale"])
        sid_name = _pick_input(input_names, ["sid", "speaker"])

        inputs = {
            input_name: np.array([ids], dtype=np.int64),
            length_name: np.array([len(ids)], dtype=np.int64),
        }
        if scale_name:
            inputs[scale_name] = np.array([noise_scale, length_scale, noise_w], dtype=np.float32)
        if sid_name:
            inputs[sid_name] = np.array([0], dtype=np.int64)

        _progress(progress, 0.55, "模型推理中")
        audio = sess.run(None, inputs)[0]
        audio = np.squeeze(audio)
        audio = np.clip(audio, -1.0, 1.0)
        sample_rate = int(
            manifest.get("sample_rate")
            or cfg.get("audio", {}).get("sample_rate")
            or cfg.get("sample_rate")
            or 22050
        )
        tail_pad = np.zeros(max(1, int(sample_rate * PREVIEW_TAIL_PAD_SECONDS)), dtype=audio.dtype)
        audio = np.concatenate([audio, tail_pad])
        audio_i16 = (audio * 32767.0).astype(np.int16)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        _progress(progress, 0.85, "写入音频文件")
        with wave.open(str(out_path), "wb") as wf:
            wf.setnchannels(1)
            wf.setsampwidth(2)
            wf.setframerate(sample_rate)
            wf.writeframes(audio_i16.tobytes())

    _progress(progress, 1.0, "试听生成完成")
    return out_path


def synthesize_voicepack(
    voicepack_path: Path,
    text: str,
    out_path: Path,
    progress: Optional[Callable[[str, float, str], None]] = None,
) -> Path:
    if _has_inprocess_preview_deps():
        return _synthesize_voicepack_inprocess(voicepack_path, text, out_path, progress)

    runtime_errors: List[str] = []
    for python_path in _preview_runtime_candidates():
        try:
            _progress(progress, 0.03, f"使用 Piper 运行时试听: {python_path}")
            return _run_preview_in_runtime(python_path, voicepack_path, text, out_path, progress)
        except Exception as exc:  # noqa: BLE001
            runtime_errors.append(f"{python_path}: {exc}")

    detail = "\n".join(runtime_errors[-2:])
    if detail:
        detail = f"\n最近错误：\n{detail}"
    raise RuntimeError(
        "试听组件未安装完整。请在“设置”里安装或重新解压 Piper 基础运行时；"
        "如果只安装了 CUDA 运行时，也可以重新解压 Piper CUDA 运行时后再试。"
        + detail
    )
