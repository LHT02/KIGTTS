from __future__ import annotations

import json
import shutil
from dataclasses import fields
from pathlib import Path
from typing import Any, Iterable, Optional

from .config import DistillOptions, DistillTextSource, ProjectPaths, TrainingOptions, VoxCpmDistillOptions
from .text_normalization import DEFAULT_SENTENCE_PERIOD, normalize_training_texts


PROJECT_CONFIG_NAME = "kigtts_project.json"
PROJECT_CONFIG_VERSION = 2


def project_config_path(paths_or_root: ProjectPaths | Path) -> Path:
    if isinstance(paths_or_root, ProjectPaths):
        return paths_or_root.work_dir / PROJECT_CONFIG_NAME
    return paths_or_root / "work" / PROJECT_CONFIG_NAME


def _path_to_str(value: Any) -> Any:
    if isinstance(value, Path):
        return str(value)
    return value


def _serialize_dataclass(obj: Any) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for field in fields(obj):
        value = getattr(obj, field.name)
        if isinstance(value, list):
            result[field.name] = [_serialize_dataclass(item) if hasattr(item, "__dataclass_fields__") else _path_to_str(item) for item in value]
        else:
            result[field.name] = _path_to_str(value)
    return result


def read_metadata_entries(metadata_csv: Path) -> list[tuple[Path, str]]:
    if not metadata_csv.exists():
        raise RuntimeError("这个项目缺少训练文本记录，无法继续训练。")
    entries: list[tuple[Path, str]] = []
    with metadata_csv.open("r", encoding="utf-8") as handle:
        for raw in handle:
            line = raw.strip()
            if not line or "|" not in line:
                continue
            audio_raw, text = line.split("|", 1)
            text = text.strip()
            if not text:
                continue
            audio_path = Path(audio_raw)
            if not audio_path.is_absolute():
                audio_path = metadata_csv.parent / audio_path
            entries.append((audio_path, text))
    if not entries:
        raise RuntimeError("这个项目没有可用的训练文本，无法继续训练。")
    return entries


def write_metadata_entries(entries: Iterable[tuple[Path, str]], metadata_csv: Path) -> None:
    metadata_csv.parent.mkdir(parents=True, exist_ok=True)
    with metadata_csv.open("w", encoding="utf-8", newline="") as handle:
        for audio_path, text in entries:
            handle.write(f"{audio_path}|{text}\n")


def _metadata_texts(paths: ProjectPaths) -> list[str]:
    if not paths.training_manifest.exists():
        return []
    return [text for _audio, text in read_metadata_entries(paths.training_manifest)]


def read_saved_texts_jsonl(path: Path) -> list[str]:
    if not path.exists():
        return []
    texts: list[str] = []
    with path.open("r", encoding="utf-8") as handle:
        for line_no, raw in enumerate(handle, start=1):
            stripped = raw.strip()
            if not stripped:
                continue
            try:
                record = json.loads(stripped)
            except json.JSONDecodeError as exc:
                raise RuntimeError(f"项目文本记录第 {line_no} 行格式不正确，无法继续训练。") from exc
            text = str(record.get("text") or "").strip()
            if text:
                texts.append(text)
    return texts


def save_project_config(
    paths: ProjectPaths,
    mode: str,
    opts: TrainingOptions,
    distill_opts: Optional[DistillOptions] = None,
    voxcpm_opts: Optional[VoxCpmDistillOptions] = None,
    metadata_texts: Optional[list[str]] = None,
) -> Path:
    paths.work_dir.mkdir(parents=True, exist_ok=True)
    raw_metadata_texts = metadata_texts if metadata_texts is not None else _metadata_texts(paths)
    if getattr(opts, "normalize_text_append_period", True):
        saved_metadata_texts = normalize_training_texts(
            raw_metadata_texts,
            getattr(opts, "text_normalization_period", DEFAULT_SENTENCE_PERIOD) or DEFAULT_SENTENCE_PERIOD,
        )
    else:
        saved_metadata_texts = [str(item).strip() for item in raw_metadata_texts if str(item).strip()]
    data: dict[str, Any] = {
        "version": PROJECT_CONFIG_VERSION,
        "mode": mode,
        "training_options": _serialize_dataclass(opts),
        "input_audio": [str(path) for path in paths.input_audio],
        "metadata_texts": saved_metadata_texts,
        "text_normalization": {
            "append_sentence_period": bool(getattr(opts, "normalize_text_append_period", True)),
            "sentence_period": str(getattr(opts, "text_normalization_period", DEFAULT_SENTENCE_PERIOD) or DEFAULT_SENTENCE_PERIOD),
        },
    }
    if distill_opts is not None:
        data["distill_options"] = _serialize_dataclass(distill_opts)
    if voxcpm_opts is not None:
        data["voxcpm_options"] = _serialize_dataclass(voxcpm_opts)
    path = project_config_path(paths)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    return path


def archive_voicepack_avatar(paths: ProjectPaths, opts: TrainingOptions) -> None:
    if not opts.voicepack_avatar:
        return
    source = opts.voicepack_avatar.expanduser().resolve()
    if not source.exists():
        return
    assets_dir = paths.work_dir / "assets"
    assets_dir.mkdir(parents=True, exist_ok=True)
    suffix = source.suffix or ".png"
    target = assets_dir / f"voicepack_avatar{suffix}"
    try:
        if source == target.resolve():
            opts.voicepack_avatar = target
            return
    except Exception:
        pass
    shutil.copy2(source, target)
    opts.voicepack_avatar = target


def load_project_config(project_root: Path) -> dict[str, Any]:
    path = project_config_path(project_root)
    if not path.exists():
        raise RuntimeError("这个项目缺少训练设置，无法继续训练。")
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise RuntimeError("项目训练设置无法读取，文件可能已损坏。") from exc
    if not isinstance(data, dict):
        raise RuntimeError("项目训练设置格式不正确，无法继续训练。")
    return data


def _path_or_none(value: Any) -> Optional[Path]:
    if value in (None, ""):
        return None
    return Path(str(value))


def training_options_from_dict(data: dict[str, Any]) -> TrainingOptions:
    opts = TrainingOptions()
    valid = {field.name for field in fields(TrainingOptions)}
    for key, value in data.items():
        if key in valid:
            setattr(opts, key, value)
    opts.asr_model_zip = _path_or_none(data.get("asr_model_zip"))
    opts.piper_base_checkpoint = _path_or_none(data.get("piper_base_checkpoint"))
    opts.phonemizer_dict = _path_or_none(data.get("phonemizer_dict"))
    opts.piper_config = _path_or_none(data.get("piper_config"))
    opts.voicepack_avatar = _path_or_none(data.get("voicepack_avatar"))
    opts.normalize_text_append_period = bool(data.get("normalize_text_append_period", True))
    opts.text_normalization_period = str(data.get("text_normalization_period") or DEFAULT_SENTENCE_PERIOD).strip() or DEFAULT_SENTENCE_PERIOD
    return opts


def _text_sources_from_list(items: Any) -> list[DistillTextSource]:
    sources: list[DistillTextSource] = []
    for item in items or []:
        if not isinstance(item, dict):
            continue
        kind = str(item.get("kind") or "").strip()
        path = _path_or_none(item.get("path"))
        if kind and path:
            sources.append(DistillTextSource(kind=kind, path=path))
    return sources


def distill_options_from_dict(data: dict[str, Any]) -> DistillOptions:
    gsv_root = _path_or_none(data.get("gsv_root"))
    if gsv_root is None:
        raise RuntimeError("这个 GPT-SoVITS 项目缺少整合包路径，请重新选择根目录后再试。")
    return DistillOptions(
        gsv_root=gsv_root,
        version=str(data.get("version") or "").strip(),
        speaker=str(data.get("speaker") or "").strip(),
        prompt_lang=str(data.get("prompt_lang") or "").strip(),
        emotion=str(data.get("emotion") or "").strip(),
        device=str(data.get("device") or "cuda").strip().lower() or "cuda",
        text_lang=str(data.get("text_lang") or "中文").strip() or "中文",
        text_split_method=str(data.get("text_split_method") or "按标点符号切").strip() or "按标点符号切",
        speed_factor=float(data.get("speed_factor") or 1.0),
        temperature=float(data.get("temperature") or 1.0),
        batch_size=max(1, int(data.get("batch_size") or 1)),
        seed=int(data.get("seed") or -1),
        top_k=int(data.get("top_k") or 10),
        top_p=float(data.get("top_p") or 1.0),
        batch_threshold=float(data.get("batch_threshold") or 0.75),
        split_bucket=bool(data.get("split_bucket", True)),
        fragment_interval=float(data.get("fragment_interval") or 0.3),
        parallel_infer=bool(data.get("parallel_infer", True)),
        repetition_penalty=float(data.get("repetition_penalty") or 1.35),
        sample_steps=int(data.get("sample_steps") or 16),
        if_sr=bool(data.get("if_sr", False)),
        parallel_workers=max(1, int(data.get("parallel_workers") or 1)),
        text_sources=_text_sources_from_list(data.get("text_sources")),
    )


def voxcpm_options_from_dict(data: dict[str, Any]) -> VoxCpmDistillOptions:
    return VoxCpmDistillOptions(
        device=str(data.get("device") or "cuda").strip().lower() or "cuda",
        allow_cpu_fallback=bool(data.get("allow_cpu_fallback", True)),
        voice_mode=str(data.get("voice_mode") or "description").strip().lower() or "description",
        voice_description=str(data.get("voice_description") or "").strip(),
        reference_audio=_path_or_none(data.get("reference_audio")),
        voice_reference_text=str(data.get("voice_reference_text") or "").strip() or VoxCpmDistillOptions().voice_reference_text,
        prompt_text=str(data.get("prompt_text") or "").strip(),
        cfg_value=float(data.get("cfg_value") or 2.0),
        inference_timesteps=max(1, int(data.get("inference_timesteps") or 10)),
        min_len=max(1, int(data.get("min_len") or 2)),
        max_len=max(1, int(data.get("max_len") or 4096)),
        normalize=bool(data.get("normalize", False)),
        denoise=bool(data.get("denoise", False)),
        retry_badcase=bool(data.get("retry_badcase", True)),
        retry_badcase_max_times=max(0, int(data.get("retry_badcase_max_times") or 3)),
        retry_badcase_ratio_threshold=float(data.get("retry_badcase_ratio_threshold") or 6.0),
        parallel_workers=max(1, int(data.get("parallel_workers") or 1)),
        text_sources=_text_sources_from_list(data.get("text_sources")),
    )


def archive_voxcpm_reference(paths: ProjectPaths, opts: VoxCpmDistillOptions) -> None:
    if not opts.reference_audio:
        return
    source = opts.reference_audio.expanduser().resolve()
    if not source.exists():
        raise RuntimeError(f"参考音频不存在: {source}")
    ref_dir = paths.work_dir / "references"
    ref_dir.mkdir(parents=True, exist_ok=True)
    suffix = source.suffix or ".wav"
    target = ref_dir / f"voxcpm_reference{suffix}"
    try:
        if source == target.resolve():
            opts.reference_audio = target
            return
    except Exception:
        pass
    shutil.copy2(source, target)
    opts.reference_audio = target


def archive_input_audio(paths: ProjectPaths) -> None:
    if not paths.input_audio:
        return
    input_dir = paths.work_dir / "input_audio"
    input_dir.mkdir(parents=True, exist_ok=True)
    archived: list[Path] = []
    used_names: set[str] = set()
    for index, source in enumerate(paths.input_audio, start=1):
        source_path = source.expanduser().resolve()
        if not source_path.exists():
            raise RuntimeError(f"原始音频不存在: {source_path}")
        name = source_path.name
        if name in used_names:
            name = f"{index:04d}_{name}"
        used_names.add(name)
        target = input_dir / name
        try:
            if source_path == target.resolve():
                archived.append(target)
                continue
        except Exception:
            pass
        shutil.copy2(source_path, target)
        archived.append(target)
    paths.input_audio = archived
