from __future__ import annotations

import wave
from pathlib import Path


def is_audio_file_usable(path: Path) -> bool:
    """Return True only when an existing generated audio file is safe to train with."""
    try:
        if not path.exists() or not path.is_file() or path.stat().st_size <= 44:
            return False
        if path.suffix.lower() == ".wav":
            return is_wav_file_usable(path)
        return path.stat().st_size > 0
    except OSError:
        return False


def is_wav_file_usable(path: Path) -> bool:
    try:
        with path.open("rb") as handle:
            header = handle.read(12)
        if len(header) < 12 or header[:4] != b"RIFF" or header[8:12] != b"WAVE":
            return False
        with wave.open(str(path), "rb") as wav_file:
            channels = wav_file.getnchannels()
            sample_width = wav_file.getsampwidth()
            frame_rate = wav_file.getframerate()
            frame_count = wav_file.getnframes()
            if channels <= 0 or sample_width <= 0 or frame_rate <= 0 or frame_count <= 0:
                return False
            data = wav_file.readframes(frame_count)
            return len(data) >= frame_count * channels * sample_width
    except (OSError, EOFError, wave.Error):
        return False


def split_usable_audio_entries(entries: list[tuple[Path, str]]) -> tuple[list[tuple[Path, str]], list[tuple[Path, str]]]:
    ready: list[tuple[Path, str]] = []
    pending: list[tuple[Path, str]] = []
    for audio_path, text in entries:
        if is_audio_file_usable(audio_path):
            ready.append((audio_path, text))
        else:
            pending.append((audio_path, text))
    return ready, pending
