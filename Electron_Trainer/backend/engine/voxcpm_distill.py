from __future__ import annotations

import json
import os
import subprocess
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Any, Callable, Dict, Optional

from .audio_validation import is_audio_file_usable, split_usable_audio_entries
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
    voice_mode = _normal_mode(opts.voice_mode)
    reference_audio = opts.reference_audio.expanduser().resolve() if opts.reference_audio and voice_mode != "description" else None
    return {
        "model_dir": model_status["main_model_dir"],
        "denoiser_dir": model_status["denoiser_model_dir"],
        "texts": texts,
        "wav_dir": str(wav_dir),
        "metadata_path": str(paths.training_manifest),
        "device": "cuda" if str(opts.device).lower() in {"cuda", "gpu"} else "cpu",
        "allow_cpu_fallback": bool(opts.allow_cpu_fallback),
        "voice_mode": voice_mode,
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


def _default_wav_path(paths: ProjectPaths, corpus_name: str, index: int) -> Path:
    return paths.work_dir / corpus_name / "wavs" / f"{index:05d}.wav"


def _write_metadata(paths: ProjectPaths, entries: list[tuple[Path, str]]) -> None:
    lines = [f"{audio_path}|{text}" for audio_path, text in entries]
    paths.training_manifest.parent.mkdir(parents=True, exist_ok=True)
    paths.training_manifest.write_text("\n".join(lines) + ("\n" if lines else ""), encoding="utf-8")


def _ready_entries(entries: list[tuple[Path, str]]) -> list[tuple[Path, str]]:
    return [(audio_path, text) for audio_path, text in entries if is_audio_file_usable(audio_path)]


def _require_generated_audio(entries: list[tuple[Path, str]], *, label: str) -> list[tuple[Path, str]]:
    ready = _ready_entries(entries)
    if len(ready) == len(entries):
        return ready
    missing = len(entries) - len(ready)
    sample = next((str(audio_path) for audio_path, _text in entries if not is_audio_file_usable(audio_path)), "")
    if not ready:
        raise RuntimeError(f"{label} 未生成可用音频，无法继续训练。示例缺失或损坏文件: {sample}")
    raise RuntimeError(f"{label} 有 {missing} 条音频缺失、为空或损坏，无法继续训练。示例文件: {sample}")


def _split_entries(entries: list[tuple[Path, str]], worker_count: int) -> list[list[tuple[Path, str]]]:
    if not entries:
        return []
    actual_workers = max(1, min(worker_count, len(entries)))
    buckets: list[list[tuple[Path, str]]] = [[] for _ in range(actual_workers)]
    for index, entry in enumerate(entries):
        buckets[index % actual_workers].append(entry)
    return [bucket for bucket in buckets if bucket]


def _run_generation_entries(
    python_path: Path,
    model_status: Dict[str, Any],
    prompt_text: str,
    paths: ProjectPaths,
    opts: VoxCpmDistillOptions,
    entries: list[tuple[Path, str]],
    *,
    progress: Optional[ProgressCallback],
    progress_stage: str,
    log_stem: str,
) -> Dict[str, Any]:
    if not entries:
        return {"code": 0, "generated": 0, "message": ""}
    worker_groups = _split_entries(entries, max(1, int(opts.parallel_workers or 1)))
    if len(worker_groups) <= 1:
        request_path = paths.work_dir / "voxcpm_corpus" / f"{log_stem}_request.json"
        log_path = paths.work_dir / f"{log_stem}.log"
        payload = _helper_request_payload(paths, opts, [text for _audio_path, text in entries], model_status, prompt_text)
        payload["output_paths"] = [str(audio_path) for audio_path, _text in entries]
        payload["metadata_path"] = str(paths.work_dir / "voxcpm_corpus" / f"{log_stem}_metadata.csv")
        _write_request(request_path, payload)
        return _run_helper(python_path, request_path, log_path, progress, progress_stage=progress_stage)

    worker_progress = [0.0 for _ in worker_groups]
    progress_lock = threading.Lock()
    last_message = ""

    def make_progress_callback(worker_index: int) -> ProgressCallback:
        def _cb(stage: str, value: float, message: str) -> None:
            nonlocal last_message
            with progress_lock:
                worker_progress[worker_index] = max(0.0, min(1.0, value))
                last_message = message
                average = sum(worker_progress) / max(1, len(worker_progress))
            if progress:
                progress(stage, average, f"并行合成 {worker_index + 1}/{len(worker_groups)}：{message}")

        return _cb

    def run_worker(worker_index: int, worker_entries: list[tuple[Path, str]]) -> Dict[str, Any]:
        request_path = paths.work_dir / "voxcpm_corpus" / f"{log_stem}_worker_{worker_index + 1}.json"
        log_path = paths.work_dir / f"{log_stem}_worker_{worker_index + 1}.log"
        payload = _helper_request_payload(paths, opts, [text for _audio_path, text in worker_entries], model_status, prompt_text)
        payload["output_paths"] = [str(audio_path) for audio_path, _text in worker_entries]
        payload["metadata_path"] = str(paths.work_dir / "voxcpm_corpus" / f"{log_stem}_worker_{worker_index + 1}_metadata.csv")
        _write_request(request_path, payload)
        return _run_helper(
            python_path,
            request_path,
            log_path,
            make_progress_callback(worker_index),
            progress_stage=progress_stage,
        )

    results: list[Dict[str, Any]] = []
    with ThreadPoolExecutor(max_workers=len(worker_groups)) as executor:
        future_map = {
            executor.submit(run_worker, worker_index, worker_entries): worker_index
            for worker_index, worker_entries in enumerate(worker_groups)
        }
        for future in as_completed(future_map):
            results.append(future.result())

    first_error = next((item for item in results if int(item.get("code") or 0) != 0), None)
    if first_error:
        return {
            "code": int(first_error.get("code") or 1),
            "generated": sum(int(item.get("generated") or 0) for item in results),
            "message": str(first_error.get("message") or last_message or "VoxCPM2 合成失败"),
        }
    return {
        "code": 0,
        "generated": sum(int(item.get("generated") or 0) for item in results),
        "message": "",
    }


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
        raise RuntimeError("找不到语音识别模型，请重新安装训练资源包或重新选择模型。")
    if progress:
        progress(progress_stage, 0.0, "高保真克隆：正在转写参考音频...")
    import librosa
    import soundfile as sf

    from . import asr

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
    texts: Optional[list[str]] = None,
) -> int:
    python_path, model_status, prompt_text = _ensure_common(opts, training_opts, progress, "synth")

    texts = list(texts or collect_distill_texts(opts, paths.work_dir, progress))  # type: ignore[arg-type]
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
    wav_dir = corpus_dir / "wavs"
    wav_dir.mkdir(parents=True, exist_ok=True)

    if progress:
        progress("collect", 1.0, f"文本收集完成，共 {len(texts)} 条")

    entries = [(_default_wav_path(paths, "voxcpm_corpus", index), text) for index, text in enumerate(texts, start=1)]
    ready_entries, pending_entries = split_usable_audio_entries(entries)
    if not pending_entries:
        _write_metadata(paths, ready_entries)
        if progress:
            progress("synth", 1.0, f"检测到已有可用 VoxCPM2 语料 {len(ready_entries)} 条，直接进入训练")
        return len(ready_entries)
    if ready_entries and progress:
        progress(
            "synth",
            len(ready_entries) / max(1, len(entries)),
            f"检测到已有可用音频 {len(ready_entries)} 条，将补生成 {len(pending_entries)} 条缺失或损坏音频",
        )
    if progress:
        progress(
            "synth",
            0.0,
            f"启动 VoxCPM2 合成（mode={_normal_mode(opts.voice_mode)}, device={'cuda' if str(opts.device).lower() in {'cuda', 'gpu'} else 'cpu'}, denoise={bool(opts.denoise)}, workers={max(1, int(opts.parallel_workers or 1))}, pending={len(pending_entries)}）",
        )

    result = _run_generation_entries(
        python_path,
        model_status,
        prompt_text,
        paths,
        opts,
        pending_entries,
        progress=progress,
        progress_stage="synth",
        log_stem="voxcpm_distill",
    )
    if result["code"] != 0 and bool(opts.denoise):
        _ready_entries_now, pending_entries = split_usable_audio_entries(entries)
        if not pending_entries:
            ready_entries = _require_generated_audio(entries, label="VoxCPM2 蒸馏")
            _write_metadata(paths, ready_entries)
            if progress:
                progress("synth", 1.0, f"VoxCPM2 蒸馏语料生成完成，共 {len(ready_entries)} 条")
            return len(ready_entries)
        retry_opts = VoxCpmDistillOptions(**{**opts.__dict__, "denoise": False})
        if progress:
            progress(
                "synth",
                0.0,
                "VoxCPM2 denoiser 初始化失败，已自动关闭 denoiser 重试。",
            )
        with log_path.open("a", encoding="utf-8") as log_file:
            log_file.write("[retry] denoiser failed, retry with denoise=False\n")
        result = _run_generation_entries(
            python_path,
            model_status,
            prompt_text,
            paths,
            retry_opts,
            pending_entries,
            progress=progress,
            progress_stage="synth",
            log_stem="voxcpm_distill_retry",
        )
    if result["code"] != 0:
        raise RuntimeError(str(result.get("message") or f"VoxCPM2 合成失败，详见 {log_path}"))

    ready_entries = _require_generated_audio(entries, label="VoxCPM2 蒸馏")
    _write_metadata(paths, ready_entries)
    if progress:
        progress("synth", 1.0, f"VoxCPM2 蒸馏语料生成完成，共 {len(ready_entries)} 条")
    return len(ready_entries)


def generate_voxcpm_entries(
    paths: ProjectPaths,
    opts: VoxCpmDistillOptions,
    training_opts: Optional[TrainingOptions],
    entries: list[tuple[Path, str]],
    progress: Optional[ProgressCallback] = None,
) -> int:
    if not entries:
        return 0
    _ready, entries = split_usable_audio_entries(entries)
    if not entries:
        return 0
    python_path, model_status, prompt_text = _ensure_common(opts, training_opts, progress, "synth")
    resume_dir = paths.work_dir / "voxcpm_corpus"
    resume_dir.mkdir(parents=True, exist_ok=True)
    log_path = paths.work_dir / "voxcpm_resume.log"
    if progress:
        progress("synth", 0.0, f"旧项目有 {len(entries)} 条 VoxCPM2 音频缺失或损坏，开始补生成...")
    result = _run_generation_entries(
        python_path,
        model_status,
        prompt_text,
        paths,
        opts,
        entries,
        progress=progress,
        progress_stage="synth",
        log_stem="voxcpm_resume",
    )
    if result["code"] != 0 and bool(opts.denoise):
        retry_opts = VoxCpmDistillOptions(**{**opts.__dict__, "denoise": False})
        if progress:
            progress("synth", 0.0, "VoxCPM2 denoiser 初始化失败，已自动关闭 denoiser 重试补生成。")
        result = _run_generation_entries(
            python_path,
            model_status,
            prompt_text,
            paths,
            retry_opts,
            entries,
            progress=progress,
            progress_stage="synth",
            log_stem="voxcpm_resume_retry",
        )
    if result["code"] != 0:
        raise RuntimeError(str(result.get("message") or f"VoxCPM2 补生成失败，详见 {log_path}"))
    _require_generated_audio(entries, label="VoxCPM2 补生成")
    if progress:
        progress("synth", 1.0, f"VoxCPM2 不可用音频补生成完成，共 {result['generated']} 条")
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
