import json
import shutil
import zipfile
from pathlib import Path
from typing import Optional

from .config import TrainingOptions


def build_manifest(
    model_dir: Path,
    opts: TrainingOptions,
    phonemizer_dict: Path,
    preview: Optional[Path] = None,
) -> dict:
    manifest = {
        "id": f"voice-{opts.language}-{opts.quality.lower()}",
        "name": f"Custom voice ({opts.quality})",
        "lang": opts.language,
        "engine": "piper-onnx",
        "sample_rate": opts.sample_rate,
        "quant": "fp16" if opts.export_fp16 else "fp32",
        "files": {
            "model": "tts/model.onnx",
            "config": "tts/model.onnx.json",
            "phonemizer": "tts/phonemizer.dict",
        },
    }
    if preview:
        manifest["preview"] = "preview.wav"
    return manifest

def build_voice_meta(opts: TrainingOptions, avatar_name: Optional[str]) -> dict:
    meta = {
        "name": opts.voicepack_name or "未命名",
        "remark": opts.voicepack_remark or "",
    }
    if avatar_name:
        meta["avatar"] = avatar_name
    return meta


def package_voicepack(
    model_dir: Path,
    out_zip: Path,
    opts: TrainingOptions,
    preview: Optional[Path] = None,
    phonemizer_dict: Optional[Path] = None,
) -> Path:
    out_zip.parent.mkdir(parents=True, exist_ok=True)
    phonemizer = phonemizer_dict
    if phonemizer is None:
        phonemizer = Path(__file__).resolve().parent.parent / "data" / "phonemizer_zh.dict"
    if not phonemizer.exists():
        raise FileNotFoundError(f"缺少 phonemizer 字典：{phonemizer}")

    manifest = build_manifest(model_dir, opts, phonemizer, preview)
    manifest_path = model_dir / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")

    avatar_name = None
    if opts.voicepack_avatar and opts.voicepack_avatar.exists():
        avatar_name = "avatar.png"
        avatar_path = model_dir / avatar_name
        shutil.copy2(opts.voicepack_avatar, avatar_path)
    meta = build_voice_meta(opts, avatar_name)
    meta_path = model_dir / "voicepack.json"
    meta_path.write_text(json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8")

    with zipfile.ZipFile(out_zip, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.write(manifest_path, "manifest.json")
        zf.write(model_dir / "model.onnx", "tts/model.onnx")
        zf.write(model_dir / "model.onnx.json", "tts/model.onnx.json")
        zf.write(phonemizer, "tts/phonemizer.dict")
        zf.write(meta_path, "voicepack.json")
        if avatar_name:
            zf.write(model_dir / avatar_name, avatar_name)
        if preview and preview.exists():
            zf.write(preview, "preview.wav")
    return out_zip
