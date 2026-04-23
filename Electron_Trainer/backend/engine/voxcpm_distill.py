from __future__ import annotations

import json
import os
import subprocess
from pathlib import Path
from typing import Any, Callable, Dict, Optional

from .config import ProjectPaths, VoxCpmDistillOptions
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


def _helper_request_payload(
    paths: ProjectPaths,
    opts: VoxCpmDistillOptions,
    texts: list[str],
    model_status: Dict[str, Any],
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
        "voice_description": opts.voice_description.strip(),
        "reference_audio": str(reference_audio) if reference_audio else "",
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
                    progress("synth", float(event.get("value") or 0.0), str(event.get("message") or "VoxCPM2 合成中"))
                elif event.get("type") == "warning" and progress:
                    progress("synth", float(event.get("value") or 0.0), str(event.get("message") or "VoxCPM2 提示"))
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


def build_voxcpm_corpus(
    paths: ProjectPaths,
    opts: VoxCpmDistillOptions,
    progress: Optional[ProgressCallback] = None,
) -> int:
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

    request_payload = _helper_request_payload(paths, opts, texts, model_status)
    _write_request(request_path, request_payload)
    if progress:
        progress(
            "synth",
            0.0,
            f"启动 VoxCPM2 合成（device={request_payload['device']}, denoise={request_payload['denoise']}）",
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
