from pathlib import Path
from typing import Iterable, List, Optional

import librosa
import numpy as np
import soundfile as sf
try:
    import webrtcvad
except Exception:  # pragma: no cover - optional dependency
    webrtcvad = None

from .config import TrainingOptions, ProgressCallback


def _frame_generator(signal: np.ndarray, frame_length: int) -> List[np.ndarray]:
    total = len(signal)
    frames = []
    for start in range(0, total, frame_length):
        end = min(start + frame_length, total)
        frames.append(signal[start:end])
    return frames


def _rms_db(signal: np.ndarray) -> float:
    rms = np.sqrt(np.mean(np.square(signal))) + 1e-12
    return 20 * np.log10(rms)


def vad_split(
    files: Iterable[Path],
    out_dir: Path,
    opts: TrainingOptions,
    progress: Optional[ProgressCallback] = None,
) -> List[Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    segments: List[Path] = []
    files = list(files)
    use_webrtc = webrtcvad is not None
    if use_webrtc:
        vad = webrtcvad.Vad()
        vad.set_mode(min(max(opts.vad_aggressiveness, 0), 3))
        frame_length = int(0.001 * opts.vad_frame_ms * 16000)
    else:
        if progress:
            progress("vad", 0.0, "未检测到 webrtcvad，使用简单切分模式")

    for idx, path in enumerate(files, 1):
        audio, sr = sf.read(path)
        # resample to 16k for VAD
        mono = librosa.to_mono(audio.T if audio.ndim > 1 else audio)
        if use_webrtc:
            mono_16k = librosa.resample(mono, orig_sr=sr, target_sr=16000)
            frames = _frame_generator(mono_16k, frame_length)
            voiced_flags = [
                len(f) == frame_length and vad.is_speech((f * 32768).astype(np.int16).tobytes(), 16000)
                for f in frames
            ]

            current: List[np.ndarray] = []
            for i, flag in enumerate(voiced_flags):
                if flag:
                    current.append(frames[i])
                elif current:
                    seg = np.concatenate(current)
                    dur_ms = len(seg) / 16000 * 1000
                    if opts.min_seg_ms <= dur_ms <= opts.max_seg_ms and _rms_db(seg) > opts.energy_threshold:
                        # upsample back to target sample rate
                        seg_full_sr = librosa.resample(seg, orig_sr=16000, target_sr=opts.sample_rate)
                        target = out_dir / f"{path.stem}_seg{len(segments):04d}.wav"
                        sf.write(target, seg_full_sr, opts.sample_rate)
                        segments.append(target)
                    current = []
        else:
            # Simple fallback: resample to target sample rate and chunk by max_seg_ms
            mono_sr = librosa.resample(mono, orig_sr=sr, target_sr=opts.sample_rate) if sr != opts.sample_rate else mono
            max_samples = int(max(opts.max_seg_ms, 1.0) / 1000 * opts.sample_rate)
            min_samples = int(max(opts.min_seg_ms, 0.0) / 1000 * opts.sample_rate)
            if max_samples <= 0:
                max_samples = len(mono_sr)
            for start in range(0, len(mono_sr), max_samples):
                seg = mono_sr[start:start + max_samples]
                if len(seg) < max(1, min_samples):
                    continue
                if _rms_db(seg) <= opts.energy_threshold:
                    continue
                target = out_dir / f"{path.stem}_seg{len(segments):04d}.wav"
                sf.write(target, seg, opts.sample_rate)
                segments.append(target)
        if progress:
            progress("vad", idx / len(files), f"{path.name} → {len(segments)} 段")
    if progress:
        progress("vad", 1.0, f"完成切分，共 {len(segments)} 段")
    return segments
