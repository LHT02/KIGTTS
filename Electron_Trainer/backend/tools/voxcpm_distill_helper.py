from __future__ import annotations

import argparse
import json
import os
import re
import sys
import traceback
from pathlib import Path
from typing import Any


def _emit(payload: dict[str, Any]) -> None:
    print(json.dumps(payload, ensure_ascii=False), flush=True)


def _configure_stdio() -> None:
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass


def _detect_device(requested: str, allow_cpu_fallback: bool) -> str:
    import torch

    requested = (requested or "cuda").lower()
    if requested in {"cuda", "gpu"}:
        if torch.cuda.is_available():
            return "cuda"
        if allow_cpu_fallback:
            _emit({"type": "warning", "value": 0.0, "message": "CUDA 不可用，已回退 CPU。VoxCPM2 CPU 推理会非常慢。"})
            return "cpu"
        raise RuntimeError("请求使用 CUDA，但当前运行时未检测到可用 CUDA。")
    return "cpu"


def _build_text(text: str, voice_description: str) -> str:
    text = text.strip()
    voice_description = re.sub(r"[()（）]", "", voice_description.strip()).strip()
    if not voice_description:
        return text
    return f"({voice_description}){text}"


def _normalize_wav(wav: Any) -> Any:
    try:
        import numpy as np

        arr = np.asarray(wav)
        if arr.ndim > 1:
            arr = arr.squeeze()
        return arr.astype("float32", copy=False)
    except Exception:
        return wav


def main() -> int:
    _configure_stdio()
    parser = argparse.ArgumentParser()
    parser.add_argument("--request", required=True)
    args = parser.parse_args()

    request_path = Path(args.request)
    req = json.loads(request_path.read_text(encoding="utf-8"))
    texts = [str(item).strip() for item in req.get("texts") or [] if str(item).strip()]
    wav_dir = Path(req["wav_dir"])
    metadata_path = Path(req["metadata_path"])
    output_paths = [str(item).strip() for item in req.get("output_paths") or [] if str(item).strip()]
    model_dir = Path(req["model_dir"])
    denoiser_dir = Path(req["denoiser_dir"])
    denoise = bool(req.get("denoise", True))
    reference_audio = str(req.get("reference_audio") or "").strip() or None
    voice_description = str(req.get("voice_description") or "").strip()
    voice_mode = str(req.get("voice_mode") or "description").strip().lower()
    prompt_text = str(req.get("prompt_text") or "").strip()
    requested_device = str(req.get("device") or "cuda").lower()
    if requested_device == "cpu":
        os.environ["CUDA_VISIBLE_DEVICES"] = ""

    try:
        import soundfile as sf
        from voxcpm import VoxCPM

        if not texts:
            raise RuntimeError("VoxCPM2 合成文本为空")
        if not model_dir.exists():
            raise RuntimeError(f"VoxCPM2 主模型目录不存在: {model_dir}")
        load_denoiser = denoise and bool(reference_audio)
        if denoise and not reference_audio:
            _emit({"type": "warning", "value": 0.0, "message": "未提供参考音频，本次跳过 denoiser 加载。"})
        if load_denoiser and not denoiser_dir.exists():
            raise RuntimeError(f"VoxCPM2 denoiser 目录不存在: {denoiser_dir}")
        if reference_audio and not Path(reference_audio).exists():
            raise RuntimeError(f"参考音频不存在: {reference_audio}")
        if voice_mode == "high_fidelity" and not reference_audio:
            raise RuntimeError("高保真克隆需要参考音频")
        if voice_mode == "high_fidelity" and not prompt_text:
            raise RuntimeError("高保真克隆需要参考音频转写文本")

        device = _detect_device(str(req.get("device") or "cuda"), bool(req.get("allow_cpu_fallback", True)))
        optimize = device.startswith("cuda")
        _emit({"type": "progress", "value": 0.0, "message": f"加载 VoxCPM2 模型（device={device}）"})
        model = VoxCPM(
            voxcpm_model_path=str(model_dir),
            zipenhancer_model_path=str(denoiser_dir) if load_denoiser else None,
            enable_denoiser=load_denoiser,
            optimize=optimize,
        )

        sample_rate = int(getattr(getattr(model, "tts_model", None), "sample_rate", 48000) or 48000)
        wav_dir.mkdir(parents=True, exist_ok=True)
        metadata_path.parent.mkdir(parents=True, exist_ok=True)
        generated = 0
        with metadata_path.open("w", encoding="utf-8", newline="") as metadata:
            for index, text in enumerate(texts, start=1):
                value = (index - 1) / max(1, len(texts))
                _emit({"type": "progress", "value": value, "message": f"VoxCPM2 合成 {index}/{len(texts)}"})
                generate_kwargs = {
                    "text": _build_text(text, "" if voice_mode == "high_fidelity" else voice_description),
                    "reference_wav_path": reference_audio,
                    "cfg_value": float(req.get("cfg_value", 2.0)),
                    "inference_timesteps": max(1, int(req.get("inference_timesteps", 10))),
                    "min_len": max(1, int(req.get("min_len", 2))),
                    "max_len": max(1, int(req.get("max_len", 4096))),
                    "normalize": bool(req.get("normalize", False)),
                    "denoise": denoise,
                    "retry_badcase": bool(req.get("retry_badcase", True)),
                    "retry_badcase_max_times": max(0, int(req.get("retry_badcase_max_times", 3))),
                    "retry_badcase_ratio_threshold": float(req.get("retry_badcase_ratio_threshold", 6.0)),
                }
                if voice_mode == "high_fidelity":
                    generate_kwargs["prompt_wav_path"] = reference_audio
                    generate_kwargs["prompt_text"] = prompt_text
                wav = model.generate(**generate_kwargs)
                wav = _normalize_wav(wav)
                wav_path = Path(output_paths[index - 1]).resolve() if index <= len(output_paths) else wav_dir / f"{index:05d}.wav"
                wav_path.parent.mkdir(parents=True, exist_ok=True)
                sf.write(str(wav_path), wav, sample_rate)
                metadata.write(f"{wav_path}|{text}\n")
                generated += 1

        _emit({"type": "done", "generated": generated})
        return 0
    except Exception as exc:
        _emit(
            {
                "type": "error",
                "message": str(exc),
                "traceback": traceback.format_exc(),
            }
        )
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
