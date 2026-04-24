import argparse
import gc
import json
import os
import sys
import traceback
from pathlib import Path
from typing import Any


def _emit(payload: dict[str, Any]) -> None:
    sys.stdout.write(json.dumps(payload, ensure_ascii=False) + "\n")
    sys.stdout.flush()


def _looks_like_oom(message: str) -> bool:
    text = message.lower()
    patterns = (
        "out of memory",
        "cuda out of memory",
        "cublas_status_alloc_failed",
        "cuda error: out of memory",
    )
    return any(pattern in text for pattern in patterns)


def _configure_stdio() -> None:
    for stream_name in ("stdout", "stderr"):
        stream = getattr(sys, stream_name, None)
        reconfigure = getattr(stream, "reconfigure", None)
        if callable(reconfigure):
            reconfigure(encoding="utf-8", errors="replace")


def _prepare_runtime(gsv_root: Path, device: str):
    os.chdir(gsv_root)
    sys.path.insert(0, str(gsv_root))
    sys.path.insert(0, str(gsv_root / "GPT_SoVITS"))

    import torch  # type: ignore
    from tools import my_infer  # type: ignore

    if device == "cuda":
        if not torch.cuda.is_available():
            raise RuntimeError("GPT-SoVITS 运行时未检测到 CUDA")
        my_infer.infer_device = "cuda"
    else:
        my_infer.infer_device = "cpu"
    my_infer.force_gpu_infer = False
    my_infer.force_half_infer = False
    my_infer.is_half = bool(device == "cuda" and getattr(my_infer, "is_half", False))

    return torch, my_infer


def main() -> int:
    _configure_stdio()
    parser = argparse.ArgumentParser(description="Generate GPT-SoVITS distillation corpus")
    parser.add_argument("--request", required=True, help="Path to distillation request JSON")
    args = parser.parse_args()

    request_path = Path(args.request).resolve()
    payload = json.loads(request_path.read_text(encoding="utf-8"))

    gsv_root = Path(payload["gsv_root"]).resolve()
    wav_dir = Path(payload["wav_dir"]).resolve()
    metadata_path = Path(payload["metadata_path"]).resolve()
    config_path = str(payload.get("config_path") or (gsv_root / "GPT_SoVITS" / "configs" / "tts_infer.yaml"))
    texts = [str(item).strip() for item in payload.get("texts") or [] if str(item).strip()]
    output_paths = [str(item).strip() for item in payload.get("output_paths") or [] if str(item).strip()]
    if not texts:
        raise RuntimeError("没有可用于蒸馏的文本")

    torch, my_infer = _prepare_runtime(gsv_root, str(payload.get("device") or "cpu").lower())

    try:
        wav_dir.mkdir(parents=True, exist_ok=True)
        for stale in wav_dir.glob("*.wav"):
            stale.unlink(missing_ok=True)
        metadata_path.parent.mkdir(parents=True, exist_ok=True)

        my_infer.pre_infer(config_path, str(gsv_root / "custom_refs"))
        my_infer.load_model(str(payload["speaker"]), str(payload["version"]))

        emo, prompt_text = my_infer.get_ref_audio(
            str(payload["speaker"]),
            str(payload["prompt_lang"]),
            str(payload["emotion"]),
            str(payload["version"]),
        )
        if not emo or not prompt_text:
            raise RuntimeError("未找到对应的参考音频或提示文本")

        ref_audio = (
            gsv_root
            / "models"
            / str(payload["version"])
            / str(payload["speaker"])
            / "reference_audios"
            / str(payload["prompt_lang"])
            / "emotions"
            / f"【{emo}】{prompt_text}.wav"
        )
        if not ref_audio.exists():
            raise RuntimeError(f"参考音频不存在: {ref_audio}")

        metadata_lines: list[str] = []
        total = len(texts)
        for index, text in enumerate(texts, start=1):
            seed = int(payload.get("seed", -1))
            if seed < 0:
                seed = my_infer.random_seed()
            audio = my_infer.tts_infer(
                text,
                str(payload.get("text_lang") or "中文"),
                str(ref_audio),
                prompt_text,
                str(payload["prompt_lang"]),
                int(payload.get("top_k", 10)),
                float(payload.get("top_p", 1.0)),
                float(payload.get("temperature", 1.0)),
                str(payload.get("text_split_method") or "按标点符号切"),
                int(payload.get("batch_size", 1)),
                float(payload.get("batch_threshold", 0.75)),
                bool(payload.get("split_bucket", True)),
                float(payload.get("speed_factor", 1.0)),
                float(payload.get("fragment_interval", 0.3)),
                seed,
                "wav",
                bool(payload.get("parallel_infer", True)),
                float(payload.get("repetition_penalty", 1.35)),
                int(payload.get("sample_steps", 16)),
                bool(payload.get("if_sr", False)),
            )
            wav_path = Path(output_paths[index - 1]).resolve() if index <= len(output_paths) else wav_dir / f"{index:05d}.wav"
            wav_path.parent.mkdir(parents=True, exist_ok=True)
            wav_path.write_bytes(audio)
            metadata_lines.append(f"{wav_path.as_posix()}|{text}")
            _emit(
                {
                    "type": "progress",
                    "stage": "distill",
                    "value": index / total,
                    "message": f"蒸馏合成 {index}/{total}",
                }
            )

        metadata_path.write_text("\n".join(metadata_lines) + "\n", encoding="utf-8")
        _emit(
            {
                "type": "done",
                "generated": total,
                "metadata_path": str(metadata_path),
            }
        )
        return 0
    except Exception as exc:
        message = f"{exc}\n{traceback.format_exc()}"
        _emit(
            {
                "type": "error",
                "message": str(exc),
                "traceback": traceback.format_exc(),
                "oom": _looks_like_oom(message),
            }
        )
        return 2
    finally:
        try:
            if torch.cuda.is_available():
                torch.cuda.empty_cache()
        except Exception:
            pass
        gc.collect()


if __name__ == "__main__":
    raise SystemExit(main())
