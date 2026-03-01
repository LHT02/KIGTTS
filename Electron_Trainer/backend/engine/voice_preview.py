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
            zip_candidate = voicepack_path / "voicepack.zip"
            if zip_candidate.exists():
                return _load_voicepack_base(zip_candidate, temp_dir)
            raise FileNotFoundError("语音包目录缺少 manifest.json")
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        return voicepack_path, manifest
    if not zipfile.is_zipfile(voicepack_path):
        raise RuntimeError("语音包不是有效的 zip 文件")
    with zipfile.ZipFile(voicepack_path, "r") as zf:
        zf.extractall(temp_dir)
    manifest_path = temp_dir / "manifest.json"
    if not manifest_path.exists():
        raise FileNotFoundError("语音包缺少 manifest.json")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    return temp_dir, manifest


def _resolve_voicepack_files(base_dir: Path, manifest: dict) -> Tuple[Path, Path, Path]:
    files = manifest.get("files") or {}
    model_rel = files.get("model")
    config_rel = files.get("config")
    dict_rel = files.get("phonemizer")
    if not model_rel or not config_rel or not dict_rel:
        raise RuntimeError("语音包 manifest 缺少 files/model/config/phonemizer")
    model_path = base_dir / model_rel
    config_path = base_dir / config_rel
    dict_path = base_dir / dict_rel
    for path in (model_path, config_path, dict_path):
        if not path.exists():
            raise FileNotFoundError(f"缺少语音包文件: {path}")
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
    raise RuntimeError("未找到 espeak-ng，请先集成 espeak-ng")


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
        raise RuntimeError(f"espeak-ng 失败: {err}")
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


def synthesize_voicepack(
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
        raise RuntimeError("缺少 onnxruntime/numpy，无法测试语音包") from exc

    with tempfile.TemporaryDirectory() as tmp_dir:
        base_dir, manifest = _load_voicepack_base(voicepack_path, Path(tmp_dir))
        _progress(progress, 0.15, "解析语音包配置")
        model_path, config_path, dict_path = _resolve_voicepack_files(base_dir, manifest)
        cfg = json.loads(config_path.read_text(encoding="utf-8"))
        phoneme_type = str(cfg.get("phoneme_type", "text")).lower()
        id_map = cfg.get("phoneme_id_map") or {}
        if not id_map:
            raise RuntimeError("语音包缺少 phoneme_id_map，无法合成")
        if phoneme_type == "espeak":
            ids = _text_to_espeak_ids(text, cfg)
        elif phoneme_type == "text":
            phoneme_map = _load_phonemizer_dict(dict_path)
            ids = _text_to_phoneme_ids(text, phoneme_map, id_map)
        else:
            raise RuntimeError(f"不支持的 phoneme_type: {phoneme_type}")
        if not ids:
            raise RuntimeError("测试文本无法转换为 phoneme ids")

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
        audio_i16 = (audio * 32767.0).astype(np.int16)
        sample_rate = int(
            manifest.get("sample_rate")
            or cfg.get("audio", {}).get("sample_rate")
            or cfg.get("sample_rate")
            or 22050
        )
        out_path.parent.mkdir(parents=True, exist_ok=True)
        _progress(progress, 0.85, "写入音频文件")
        with wave.open(str(out_path), "wb") as wf:
            wf.setnchannels(1)
            wf.setsampwidth(2)
            wf.setframerate(sample_rate)
            wf.writeframes(audio_i16.tobytes())

    _progress(progress, 1.0, "试听生成完成")
    return out_path
