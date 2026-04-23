from __future__ import annotations

import json
import os
import subprocess
from pathlib import Path
from typing import Any, Callable, Dict, Optional

import librosa
import soundfile as sf

from . import asr
from .config import ProjectPaths, TrainingOptions, VoxCpmDistillOptions
from .gsv_distill import collect_distill_texts
from .runtime_manager import describe_voxcpm_models, get_voxcpm_python_path


ProgressCallback = Callable[[str, float, str], None]


def _helper_env(python_path: Path) -> dict[str, str]:
    root = python_path.parent
    path_entries = [
        str(root),
        str(root / "Scripts"),
        str(root / "Library" / "bin"),
        str(root / "Library" / "usr" / "bin"),
        str(root / "Library" / "mingw-w64" / "bin"),
        os.environ.get("PATH", ""),
    ]
    env = os.environ.copy()
    env["PATH"] = os.pathsep.join(entry for entry in path_entries if entry)
    env["PYTHONIOENCODING"] = "utf-8"
    env["PYTHONUTF8"] = "1"
    return env


def _normal_mode(mode: str) -> str:
    normalized = (mode or "description").strip().lower()
    if normalized not in {"description", "controlled_clone", "high_fidelity"}:
        raise RuntimeError(f"不支持的 VoxCPM2 声音生成模式: {mode}")
    return normalized


def _helper_request_payload(
    paths: ProjectPaths,
    opts: VoxCpmDistillOptions,
    texts: list[str],
    model_status: Dict[str, Any],
    prompt_text: str = "",
) -> Dict[str, Any]:
    corpus_dir = paths.work_dir / "voxcpm_corpus"
    wav_dir = corpus_dir / "wavs"
    reference_audio = opts.reference_audio.expanduser().resolve() if opts.reference_audio else None
    return {
        "model_dir": model_status["main_model_dir"],
        "denoiser_dir": model_status["denoiser_model_dir"],
        "texts": texts,
        "wav_dir": str(wav_dir),
        "metadata_path": str(paths.training_manifest),
        "device": "cuda" if str(opts.device).lower() in {"cuda", "gpu"} else "cpu",
        "allow_cpu_fallback": bool(opts.allow_cpu_fallback),
        "voice_mode": _normal_mode(opts.voice_mode),
        "voice_description": opts.voice_description.strip(),
        "reference_audio": str(reference_audio) if reference_audio else "",
        "prompt_text": prompt_text.strip(),
        "cfg_value": float(opts.cfg_value),
        "inference_timesteps": max(1, int(opts.inference_timesteps)),
        "min_len": max(1, int(opts.min_len)),
        "max_len": max(1, int(opts.max_len)),
        "normalize": bool(opts.normalize),
        "denoise": bool(opts.denoise),
        "retry_badcase": bool(opts.retry_badcase),
        "retry_badcase_max_times": max(0, int(opts.retry_badcase_max_times)),
        "retry_badcase_ratio_threshold": float(opts.retry_badcase_ratio_threshold),
    }


def _run_helper(
    python_path: Path,
    request_path: Path,
    log_path: Path,
    progress: Optional[ProgressCallback] = None,
    progress_stage: str = "synth",
) -> Dict[str, Any]:
    helper_script = Path(__file__).resolve().parents[1] / "tools" / "voxcpm_distill_helper.py"
    result: Dict[str, Any] = {"code": 0, "message": "", "generated": 0}
    env = _helper_env(python_path)
    with log_path.open("a", encoding="utf-8") as log_file:
        proc = subprocess.Popen(
            [str(python_path), "-u", str(helper_script), "--request", str(request_path)],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            stdin=subprocess.DEVNULL,
            text=False,
            cwd=str(request_path.parent),
            bufsize=0,
            close_fds=True,
            env=env,
        )
        log_file.write(f"[spawn] pid={proc.pid}\n")
        log_file.flush()

        if proc.stdout is not None:
            for raw_line in proc.stdout:
                try:
                    line = raw_line.decode("utf-8").rstrip("\n")
                except UnicodeDecodeError:
                    line = raw_line.decode("utf-8", errors="replace").rstrip("\n")
                log_file.write(line + "\n")
                log_file.flush()
                try:
                    event = json.loads(line)
                except json.JSONDecodeError:
                    continue
                if event.get("type") == "progress" and progress:
                    progress(progress_stage, float(event.get("value") or 0.0), str(event.get("message") or "VoxCPM2 合成中"))
                elif event.get("type") == "warning" and progress:
                    progress(progress_stage, float(event.get("value") or 0.0), str(event.get("message") or "VoxCPM2 提示"))
                elif event.get("type") == "done":
                    result["generated"] = int(event.get("generated") or 0)
                elif event.get("type") == "error":
                    result["message"] = str(event.get("message") or "VoxCPM2 合成失败")
                    result["traceback"] = str(event.get("traceback") or "")
        proc.wait()
        result["code"] = int(proc.returncode or 0)
    return result


def _write_request(path: Path, payload: Dict[str, Any]) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def _validate_voice_mode(opts: VoxCpmDistillOptions) -> None:
    mode = _normal_mode(opts.voice_mode)
    if mode == "description" and not opts.voice_description.strip():
        raise RuntimeError("声音设定模式需要填写音色描述")
    if mode in {"controlled_clone", "high_fidelity"} and not opts.reference_audio:
        raise RuntimeError("当前 VoxCPM2 模式需要参考音频")


def _transcribe_reference_audio(
    reference_audio: Path,
    training_opts: Optional[TrainingOptions],
    progress: Optional[ProgressCallback] = None,
    progress_stage: str = "synth",
) -> str:
    if training_opts is None or not training_opts.asr_model_zip:
        raise RuntimeError("高保真克隆需要参考音频转写文本；请填写参考文本，或先配置 ASR 模型。")
    if not training_opts.asr_model_zip.exists():
        raise RuntimeError(f"ASR 模型不存在: {training_opts.asr_model_zip}")
    if progress:
        progress(progress_stage, 0.0, "高保真克隆：正在转写参考音频...")
    engine = asr.OfflineASR(training_opts.asr_model_zip, device="cpu")
    audio, sr = sf.read(reference_audio)
    mono = librosa.to_mono(audio.T if getattr(audio, "ndim", 1) > 1 else audio)
    text, _score = engine.transcribe(mono, sr)
    text = text.strip()
    if not text:
        raise RuntimeError("参考音频 ASR 未识别到有效文本，请手动填写参考文本。")
    if progress:
        progress(progress_stage, 0.0, f"参考音频转写完成：{text[:48]}")
    return text


def _resolve_prompt_text(
    opts: VoxCpmDistillOptions,
    training_opts: Optional[TrainingOptions],
    progress: Optional[ProgressCallback] = None,
    progress_stage: str = "synth",
) -> str:
    if _normal_mode(opts.voice_mode) != "high_fidelity":
        return ""
    if not opts.reference_audio:
        raise RuntimeError("高保真克隆需要参考音频")
    prompt_text = (opts.prompt_text or "").strip()
    if prompt_text:
        return prompt_text
    return _transcribe_reference_audio(opts.reference_audio.expanduser().resolve(), training_opts, progress, progress_stage)


def _ensure_common(
    opts: VoxCpmDistillOptions,
    training_opts: Optional[TrainingOptions],
    progress: Optional[ProgressCallback],
    progress_stage: str = "synth",
) -> tuple[Path, Dict[str, Any], str]:
    python_path = get_voxcpm_python_path()
    if python_path is None:
        raise RuntimeError("VoxCPM2 运行时未安装，请先安装运行时。")

    model_status = describe_voxcpm_models()
    if not model_status.get("main_available"):
        raise RuntimeError("VoxCPM2 主模型未下载，请先下载模型。")
    if opts.denoise and not model_status.get("denoiser_available"):
        raise RuntimeError("当前启用了 denoiser，但 denoiser 模型未下载。")
    if opts.reference_audio and not opts.reference_audio.expanduser().exists():
        raise RuntimeError(f"参考音频不存在: {opts.reference_audio}")
    _validate_voice_mode(opts)
    prompt_text = _resolve_prompt_text(opts, training_opts, progress, progress_stage)
    return python_path, model_status, prompt_text


def build_voxcpm_corpus(
    paths: ProjectPaths,
    opts: VoxCpmDistillOptions,
    training_opts: Optional[TrainingOptions] = None,
    progress: Optional[ProgressCallback] = None,
) -> int:
    python_path, model_status, prompt_text = _ensure_common(opts, training_opts, progress, "synth")

    texts = collect_distill_texts(opts, paths.work_dir, progress)  # type: ignore[arg-type]
    if not texts:
        raise RuntimeError("VoxCPM2 蒸馏文本为空")

    corpus_dir = paths.work_dir / "voxcpm_corpus"
    corpus_dir.mkdir(parents=True, exist_ok=True)
    texts_path = corpus_dir / "texts.jsonl"
    texts_path.write_text(
        "\n".join(json.dumps({"text": text}, ensure_ascii=False) for text in texts) + ("\n" if texts else ""),
        encoding="utf-8",
    )
    request_path = corpus_dir / "request.json"
    log_path = paths.work_dir / "voxcpm_distill.log"
    if log_path.exists():
        log_path.unlink()

    if progress:
        progress("collect", 1.0, f"文本收集完成，共 {len(texts)} 条")

    request_payload = _helper_request_payload(paths, opts, texts, model_status, prompt_text)
    _write_request(request_path, request_payload)
    if progress:
        progress(
            "synth",
            0.0,
            f"启动 VoxCPM2 合成（mode={request_payload['voice_mode']}, device={request_payload['device']}, denoise={request_payload['denoise']}）",
        )

    result = _run_helper(python_path, request_path, log_path, progress)
    if result["code"] != 0 and request_payload.get("denoise"):
        request_payload["denoise"] = False
        _write_request(request_path, request_payload)
        if progress:
            progress(
                "synth",
                0.0,
                "VoxCPM2 denoiser 初始化失败，已自动关闭 denoiser 重试。",
            )
        with log_path.open("a", encoding="utf-8") as log_file:
            log_file.write("[retry] denoiser failed, retry with denoise=False\n")
        result = _run_helper(python_path, request_path, log_path, progress)
    if result["code"] != 0:
        raise RuntimeError(str(result.get("message") or f"VoxCPM2 合成失败，详见 {log_path}"))

    if progress:
        progress("synth", 1.0, f"VoxCPM2 蒸馏语料生成完成，共 {result['generated']} 条")
    return len(texts)


def generate_voxcpm_entries(
    paths: ProjectPaths,
    opts: VoxCpmDistillOptions,
    training_opts: Optional[TrainingOptions],
    entries: list[tuple[Path, str]],
    progress: Optional[ProgressCallback] = None,
) -> int:
    if not entries:
        return 0
    python_path, model_status, prompt_text = _ensure_common(opts, training_opts, progress, "synth")
    resume_dir = paths.work_dir / "voxcpm_corpus"
    resume_dir.mkdir(parents=True, exist_ok=True)
    request_path = resume_dir / "resume_request.json"
    log_path = paths.work_dir / "voxcpm_resume.log"
    payload = _helper_request_payload(paths, opts, [text for _path, text in entries], model_status, prompt_text)
    payload["output_paths"] = [str(audio_path) for audio_path, _text in entries]
    payload["metadata_path"] = str(resume_dir / "resume_metadata.csv")
    _write_request(request_path, payload)
    if progress:
        progress("synth", 0.0, f"旧项目缺失 {len(entries)} 条 VoxCPM2 音频，开始补生成...")
    result = _run_helper(python_path, request_path, log_path, progress)
    if result["code"] != 0 and payload.get("denoise"):
        payload["denoise"] = False
        _write_request(request_path, payload)
        if progress:
            progress("synth", 0.0, "VoxCPM2 denoiser 初始化失败，已自动关闭 denoiser 重试补生成。")
        result = _run_helper(python_path, request_path, log_path, progress)
    if result["code"] != 0:
        raise RuntimeError(str(result.get("message") or f"VoxCPM2 补生成失败，详见 {log_path}"))
    if progress:
        progress("synth", 1.0, f"VoxCPM2 缺失音频补生成完成，共 {result['generated']} 条")
    return int(result.get("generated") or 0)


def synthesize_voxcpm_preview(
    preview_root: Path,
    opts: VoxCpmDistillOptions,
    text: str,
    training_opts: Optional[TrainingOptions] = None,
    progress: Optional[ProgressCallback] = None,
) -> Path:
    text = text.strip()
    if not text:
        raise RuntimeError("缺少试听文本")
    python_path, model_status, prompt_text = _ensure_common(opts, training_opts, progress, "preview")

    paths = ProjectPaths(project_root=preview_root)
    preview_dir = paths.work_dir / "voxcpm_preview"
    preview_dir.mkdir(parents=True, exist_ok=True)
    request_path = preview_dir / "request.json"
    log_path = preview_dir / "preview.log"
    if log_path.exists():
        log_path.unlink()

    payload = _helper_request_payload(paths, opts, [text], model_status, prompt_text)
    payload["wav_dir"] = str(preview_dir / "wavs")
    payload["metadata_path"] = str(preview_dir / "metadata.csv")
    _write_request(request_path, payload)
    if progress:
        progress("preview", 0.0, "启动 VoxCPM2 试听合成...")
    result = _run_helper(python_path, request_path, log_path, progress, progress_stage="preview")
    if result["code"] != 0 and payload.get("denoise"):
        payload["denoise"] = False
        _write_request(request_path, payload)
        if progress:
            progress("preview", 0.0, "VoxCPM2 denoiser 初始化失败，已自动关闭 denoiser 重试试听。")
        result = _run_helper(python_path, request_path, log_path, progress, progress_stage="preview")
    if result["code"] != 0:
        raise RuntimeError(str(result.get("message") or f"VoxCPM2 试听失败，详见 {log_path}"))
    audio_path = Path(payload["wav_dir"]) / "00001.wav"
    if not audio_path.exists():
        raise RuntimeError(f"VoxCPM2 试听未生成音频: {audio_path}")
    if progress:
        progress("preview", 1.0, "VoxCPM2 试听生成完成")
    return audio_path
