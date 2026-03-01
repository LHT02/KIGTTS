import shutil
import subprocess
from pathlib import Path
from typing import Optional

from . import preprocess, vad, asr, training, packager
from .config import ProjectPaths, TrainingOptions, PipelineResult, ProgressCallback
from .utils import find_executable


def _ensure_dirs(paths: ProjectPaths) -> None:
    paths.work_dir.mkdir(parents=True, exist_ok=True)
    paths.segments_dir.mkdir(parents=True, exist_ok=True)
    paths.export_dir.mkdir(parents=True, exist_ok=True)


def synth_preview(model_dir: Path, opts: TrainingOptions) -> Optional[Path]:
    """Generate preview.wav using piper CLI if available."""
    piper_bin = find_executable(["piper"])
    preview_path = model_dir / "preview.wav"
    if not piper_bin:
        return None
    cmd = [
        piper_bin,
        "--model", str(model_dir / "model.onnx"),
        "--config", str(model_dir / "model.onnx.json"),
        "--output_file", str(preview_path),
        "--text", opts.text_sample,
    ]
    proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    if proc.returncode != 0:
        log = model_dir / "preview.log"
        log.write_text(proc.stdout, encoding="utf-8")
        return None
    return preview_path


def run_pipeline(
    paths: ProjectPaths,
    opts: TrainingOptions,
    progress: Optional[ProgressCallback] = None,
) -> PipelineResult:
    _ensure_dirs(paths)

    processed = preprocess.preprocess_audios(
        paths.input_audio, paths.work_dir / "processed", opts, progress
    )
    segments = vad.vad_split(processed, paths.segments_dir, opts, progress)
    transcripts = asr.transcribe_segments(segments, opts, progress)

    training.write_metadata(transcripts, paths.training_manifest)
    training.write_preview_text(opts.text_sample, paths.work_dir / "preview.txt")

    ckpt = training.run_piper_training(paths.training_manifest, paths.work_dir, opts, progress)
    model_dir = paths.work_dir / "onnx"
    model_dir.mkdir(exist_ok=True, parents=True)
    export_path = training.export_onnx(ckpt, model_dir, opts, progress)
    preview_path = synth_preview(model_dir, opts)

    voicepack_zip = packager.package_voicepack(
        model_dir=model_dir,
        out_zip=paths.voicepack_path,
        opts=opts,
        preview=preview_path,
        phonemizer_dict=opts.phonemizer_dict,
    )
    return PipelineResult(
        manifest_path=model_dir / "manifest.json",
        voicepack_path=voicepack_zip,
        preview_path=preview_path,
        training_log=paths.work_dir / "training.log",
    )

