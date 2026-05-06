from __future__ import annotations

import shutil
import zipfile
from pathlib import Path
from typing import Iterable, List, Optional, Tuple

from .config import TrainingOptions, ProgressCallback, SegmentMetadata


class OfflineASR:
    """Thin wrapper around sherpa-onnx offline recognizer."""

    def __init__(self, model_zip: Path, device: str = "cpu") -> None:
        import sherpa_onnx
        self.tmp_dir: Optional[Path] = None

        if hasattr(sherpa_onnx.OfflineRecognizer, "from_zip"):
            self.recognizer = sherpa_onnx.OfflineRecognizer.from_zip(
                model_zip=str(model_zip),
                num_threads=4,
                sample_rate=16000,
                feature_dim=80,
                decoding_method="modified_beam_search",
                max_active_paths=4,
                provider=device,
            )
            return

        if hasattr(sherpa_onnx, "OfflineZipModel"):
            model = sherpa_onnx.OfflineZipModel(model_zip)
            self.recognizer = sherpa_onnx.OfflineRecognizer(model)
            return

        # Fallback: unpack zip and build config manually for older sherpa-onnx builds.
        self.tmp_dir = model_zip.with_suffix("")
        if not self.tmp_dir.exists():
            with zipfile.ZipFile(model_zip, "r") as zf:
                self.tmp_dir.mkdir(parents=True, exist_ok=True)
                zf.extractall(self.tmp_dir)

        def _rglob(pattern: str) -> List[Path]:
            if not self.tmp_dir:
                return []
            return list(self.tmp_dir.rglob(pattern))

        def _pick_model(candidates: List[Path]) -> Optional[Path]:
            if not candidates:
                return None
            def score(p: Path) -> int:
                name = p.as_posix().lower()
                s = 0
                if "sensevoice" in name or "sense_voice" in name:
                    s += 10
                if "vad" in name or "punct" in name:
                    s -= 5
                return s
            return max(candidates, key=score)

        onnx_candidates = _rglob("*.onnx")
        model_path = _pick_model(onnx_candidates)
        tokens_candidates = [p for p in _rglob("tokens.txt")] or _rglob("*.txt")
        tokens_path = tokens_candidates[0] if tokens_candidates else None
        if not model_path or not tokens_path:
            raise RuntimeError("无法在 ASR zip 中找到 onnx 或 tokens 文件。")

        # Try available factory methods for newer sherpa-onnx builds
        offline_recognizer = getattr(sherpa_onnx, "OfflineRecognizer", None)
        if offline_recognizer:
            try_methods = []
            if hasattr(offline_recognizer, "from_sense_voice"):
                try_methods.append(("from_sense_voice", dict(
                    model=str(model_path),
                    tokens=str(tokens_path),
                    num_threads=4,
                    sample_rate=16000,
                    feature_dim=80,
                    decoding_method="greedy_search",
                    provider=device,
                    language="zh",
                    use_itn=True,
                )))
            if hasattr(offline_recognizer, "from_paraformer"):
                try_methods.append(("from_paraformer", dict(
                    paraformer=str(model_path),
                    tokens=str(tokens_path),
                    num_threads=4,
                    sample_rate=16000,
                    feature_dim=80,
                    decoding_method="greedy_search",
                    provider=device,
                )))
            if hasattr(offline_recognizer, "from_wenet_ctc"):
                try_methods.append(("from_wenet_ctc", dict(
                    model=str(model_path),
                    tokens=str(tokens_path),
                    num_threads=4,
                    sample_rate=16000,
                    feature_dim=80,
                    decoding_method="greedy_search",
                    provider=device,
                )))
            if hasattr(offline_recognizer, "from_nemo_ctc"):
                try_methods.append(("from_nemo_ctc", dict(
                    model=str(model_path),
                    tokens=str(tokens_path),
                    num_threads=4,
                    sample_rate=16000,
                    feature_dim=80,
                    decoding_method="greedy_search",
                    provider=device,
                )))
            if hasattr(offline_recognizer, "from_telespeech_ctc"):
                try_methods.append(("from_telespeech_ctc", dict(
                    model=str(model_path),
                    tokens=str(tokens_path),
                    num_threads=4,
                    sample_rate=16000,
                    feature_dim=80,
                    decoding_method="greedy_search",
                    provider=device,
                )))
            for name, kwargs in try_methods:
                try:
                    self.recognizer = getattr(offline_recognizer, name)(**kwargs)
                    return
                except Exception:
                    pass

        if hasattr(sherpa_onnx.OfflineRecognizer, "from_sense_voice"):
            self.recognizer = sherpa_onnx.OfflineRecognizer.from_sense_voice(
                model=str(model_path),
                tokens=str(tokens_path),
                num_threads=4,
                sample_rate=16000,
                feature_dim=80,
                decoding_method="greedy_search",
                provider=device,
                language="zh",
                use_itn=True,
            )
            return

        feat_cfg_cls = getattr(sherpa_onnx, "FeatureConfig", None) or getattr(sherpa_onnx, "FeatureExtractorConfig", None)
        if not feat_cfg_cls:
            raise RuntimeError("语音识别组件版本不兼容，请重新安装训练资源包后再试。")
        feat_cfg = feat_cfg_cls()
        if hasattr(feat_cfg, "sample_rate"):
            feat_cfg.sample_rate = 16000
        elif hasattr(feat_cfg, "sampling_rate"):
            feat_cfg.sampling_rate = 16000
        if hasattr(feat_cfg, "feature_dim"):
            feat_cfg.feature_dim = 80

        sense_cfg = sherpa_onnx.OfflineSenseVoiceModelConfig()
        sense_cfg.model = str(model_path)
        if hasattr(sense_cfg, "language"):
            sense_cfg.language = "zh"
        elif hasattr(sense_cfg, "lang"):
            sense_cfg.lang = "zh"
        if hasattr(sense_cfg, "use_inverse_text_normalization"):
            sense_cfg.use_inverse_text_normalization = True
        elif hasattr(sense_cfg, "use_itn"):
            sense_cfg.use_itn = True

        model_cfg = sherpa_onnx.OfflineModelConfig()
        model_cfg.sense_voice = sense_cfg
        model_cfg.tokens = str(tokens_path)
        model_cfg.num_threads = 4
        model_cfg.provider = device

        recog_cfg = sherpa_onnx.OfflineRecognizerConfig()
        recog_cfg.feat_config = feat_cfg
        recog_cfg.model_config = model_cfg
        recog_cfg.decoding_method = "greedy_search"
        recog_cfg.max_active_paths = 4
        self.recognizer = sherpa_onnx.OfflineRecognizer(recog_cfg)

    def transcribe(self, audio: np.ndarray, sr: int) -> Tuple[str, float]:
        import numpy as np

        stream = self.recognizer.create_stream()
        stream.accept_waveform(sr, audio.astype(np.float32))
        self.recognizer.decode_stream(stream)
        result = getattr(stream, "result", None)
        text = getattr(result, "text", "").strip()
        tokens = getattr(result, "tokens", [])
        # Use a simple heuristic score when confidence is unavailable.
        score = getattr(result, "scores", [1.0])[0] if hasattr(result, "scores") else max(0.3, min(1.0, len(tokens) / max(len(text), 1)))
        return text, float(score)


def transcribe_segments(
    segments: Iterable[Path],
    opts: TrainingOptions,
    progress: Optional[ProgressCallback] = None,
) -> List[SegmentMetadata]:
    import librosa
    import soundfile as sf

    if not opts.asr_model_zip:
        raise ValueError("asr_model_zip 未设置，无法转写。")
    engine = OfflineASR(opts.asr_model_zip, device=opts.device)
    segments = list(segments)
    results: List[SegmentMetadata] = []
    for idx, path in enumerate(segments, 1):
        audio, sr = sf.read(path)
        mono = librosa.to_mono(audio.T if audio.ndim > 1 else audio)
        text, score = engine.transcribe(mono, sr)
        results.append(SegmentMetadata(path, text, score))
        if progress:
            progress("asr", idx / len(segments), f"{path.name}: {text[:32]}")
    if progress:
        progress("asr", 1.0, f"转写完成，共 {len(results)} 段")
    return results
