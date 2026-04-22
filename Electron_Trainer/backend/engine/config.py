from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable, List, Optional


ProgressCallback = Callable[[str, float, str], None]


@dataclass
class TrainingOptions:
    """User-configurable options for a training run."""

    quality: str = "A"  # A or B
    sample_rate: int = 22050
    vad_frame_ms: int = 30
    vad_aggressiveness: int = 2
    min_seg_ms: int = 800
    max_seg_ms: int = 12000
    energy_threshold: float = -35.0
    # B 档可选轻降噪
    denoise: bool = False
    # sherpa-onnx model paths
    asr_model_zip: Optional[Path] = None
    # base model for piper training
    piper_base_checkpoint: Optional[Path] = None
    language: str = "zh"
    speaker_id: int = 0
    epochs: int = 8
    batch_size: int = 24
    learning_rate: float = 2e-4
    device: str = "cpu"
    export_fp16: bool = False
    phonemizer_dict: Optional[Path] = None
    use_espeak: bool = False
    piper_config: Optional[Path] = None
    text_sample: str = "你好，这是音色预览。"
    voicepack_name: str = "未命名"
    voicepack_remark: str = ""
    voicepack_avatar: Optional[Path] = None


@dataclass
class DistillTextSource:
    kind: str
    path: Path


@dataclass
class DistillOptions:
    gsv_root: Path
    version: str
    speaker: str
    prompt_lang: str
    emotion: str
    device: str = "cuda"
    text_lang: str = "中文"
    text_split_method: str = "按标点符号切"
    speed_factor: float = 1.0
    temperature: float = 1.0
    batch_size: int = 1
    seed: int = -1
    top_k: int = 10
    top_p: float = 1.0
    batch_threshold: float = 0.75
    split_bucket: bool = True
    fragment_interval: float = 0.3
    parallel_infer: bool = True
    repetition_penalty: float = 1.35
    sample_steps: int = 16
    if_sr: bool = False
    text_sources: List[DistillTextSource] = field(default_factory=list)


@dataclass
class ProjectPaths:
    project_root: Path
    input_audio: List[Path] = field(default_factory=list)
    work_dir: Path = field(init=False)
    segments_dir: Path = field(init=False)
    transcripts_path: Path = field(init=False)
    training_manifest: Path = field(init=False)
    export_dir: Path = field(init=False)
    voicepack_path: Path = field(init=False)

    def __post_init__(self) -> None:
        self.work_dir = self.project_root / "work"
        self.segments_dir = self.work_dir / "segments"
        self.transcripts_path = self.work_dir / "transcripts.jsonl"
        self.training_manifest = self.work_dir / "metadata.csv"
        self.export_dir = self.project_root / "export"
        self.voicepack_path = self.export_dir / "voicepack.kigvpk"


@dataclass
class SegmentMetadata:
    audio_path: Path
    text: str
    score: float


@dataclass
class PipelineResult:
    manifest_path: Path
    voicepack_path: Path
    preview_path: Optional[Path]
    training_log: Optional[Path]
