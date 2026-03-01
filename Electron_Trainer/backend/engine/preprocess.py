from pathlib import Path
from typing import Iterable, List, Optional

import librosa
import numpy as np
import soundfile as sf
from scipy.signal import butter, filtfilt

from .config import TrainingOptions, ProgressCallback


def _bandpass(signal: np.ndarray, sr: int, low: float = 60.0, high: float = 8000.0) -> np.ndarray:
    nyq = 0.5 * sr
    low_norm, high_norm = low / nyq, high / nyq
    b, a = butter(4, [low_norm, high_norm], btype="band")
    return filtfilt(b, a, signal)


def preprocess_audios(
    files: Iterable[Path],
    out_dir: Path,
    opts: TrainingOptions,
    progress: Optional[ProgressCallback] = None,
) -> List[Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    processed: List[Path] = []
    files = list(files)
    total = len(files)

    for idx, path in enumerate(files, 1):
        if progress:
            progress("preprocess", idx / total, f"处理 {path.name}")
        audio, sr = librosa.load(path, sr=opts.sample_rate, mono=True)
        if opts.quality.upper() == "B" and opts.denoise:
            # 轻量带通 + 去噪预加重，避免过度破坏音色
            audio = _bandpass(audio, opts.sample_rate)
            audio = librosa.effects.preemphasis(audio)
        target = out_dir / f"{path.stem}_proc.wav"
        sf.write(target, audio, opts.sample_rate)
        processed.append(target)
    if progress:
        progress("preprocess", 1.0, f"完成预处理，共 {len(processed)} 个文件")
    return processed

