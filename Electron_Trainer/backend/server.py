import json
import os
import sys
import threading
import traceback
from pathlib import Path
from typing import Any, Dict, Optional

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))

from engine.config import DistillOptions, DistillTextSource, ProjectPaths, TrainingOptions, VoxCpmDistillOptions  # type: ignore
from engine.gsv_distill import scan_gsv_models, validate_gsv_root  # type: ignore
from engine.project_state import load_project_config, read_metadata_entries  # type: ignore
from engine.runtime_manager import (  # type: ignore
    describe_piper_runtime,
    describe_piper_cuda_runtime,
    describe_download_sources,
    describe_trainer_resources,
    describe_voxcpm_models,
    describe_voxcpm_runtime,
    download_voxcpm_models,
    install_piper_runtime,
    install_piper_cuda_runtime,
    install_trainer_resources,
    install_voxcpm_runtime,
    save_download_sources,
)
from engine.resource_paths import resolve_resources_root  # type: ignore


def _send(obj: Dict[str, Any]) -> None:
    sys.stdout.write(json.dumps(obj, ensure_ascii=False) + "\n")
    sys.stdout.flush()


def _path_or_none(value: Any) -> Path | None:
    if not value:
        return None
    try:
        return Path(value)
    except Exception:
        return None


def _resources_root() -> Optional[Path]:
    resolved = resolve_resources_root()
    if resolved is not None:
        return resolved
    env_resources = os.environ.get("KGTTS_RESOURCES")
    if env_resources:
        return Path(env_resources)
    env_base = os.environ.get("KGTTS_BASE_DIR")
    if env_base:
        base = Path(env_base)
        candidate = base / "resources_pack"
        if candidate.exists():
            return candidate
    return None


def _build_opts(payload: Dict[str, Any]) -> TrainingOptions:
    opts = TrainingOptions()
    fields = set(TrainingOptions.__annotations__.keys())
    for key, val in payload.items():
        if key not in fields:
            continue
        setattr(opts, key, val)

    # Path fields
    opts.asr_model_zip = _path_or_none(payload.get("asr_model_zip"))
    opts.piper_base_checkpoint = _path_or_none(payload.get("piper_base_checkpoint"))
    opts.phonemizer_dict = _path_or_none(payload.get("phonemizer_dict"))
    opts.piper_config = _path_or_none(payload.get("piper_config"))
    opts.voicepack_avatar = _path_or_none(payload.get("voicepack_avatar"))

    # Provide default phonemizer dict from resources pack if present
    resources_root = _resources_root()
    if resources_root and opts.phonemizer_dict is None:
        candidate = resources_root / "data" / "phonemizer_zh.dict"
        if candidate.exists():
            opts.phonemizer_dict = candidate

    return opts


def _build_distill_opts(payload: Dict[str, Any]) -> DistillOptions:
    gsv_root = _path_or_none(payload.get("gsv_root"))
    if gsv_root is None:
        raise RuntimeError("请先选择 GPT-SoVITS / GSVI 整合包根目录。")

    text_sources = []
    for item in payload.get("text_sources") or []:
        if not isinstance(item, dict):
            continue
        source_path = _path_or_none(item.get("path"))
        kind = str(item.get("kind") or "").strip()
        if not source_path or not kind:
            continue
        text_sources.append(DistillTextSource(kind=kind, path=source_path))

    return DistillOptions(
        gsv_root=gsv_root,
        version=str(payload.get("version") or "").strip(),
        speaker=str(payload.get("speaker") or "").strip(),
        prompt_lang=str(payload.get("prompt_lang") or "").strip(),
        emotion=str(payload.get("emotion") or "").strip(),
        device=str(payload.get("device") or "cuda").strip().lower() or "cuda",
        text_lang=str(payload.get("text_lang") or "中文").strip() or "中文",
        text_split_method=str(payload.get("text_split_method") or "按标点符号切").strip() or "按标点符号切",
        speed_factor=float(payload.get("speed_factor") or 1.0),
        temperature=float(payload.get("temperature") or 1.0),
        batch_size=max(1, int(payload.get("batch_size") or 1)),
        seed=int(payload.get("seed") or -1),
        top_k=int(payload.get("top_k") or 10),
        top_p=float(payload.get("top_p") or 1.0),
        batch_threshold=float(payload.get("batch_threshold") or 0.75),
        split_bucket=bool(payload.get("split_bucket", True)),
        fragment_interval=float(payload.get("fragment_interval") or 0.3),
        parallel_infer=bool(payload.get("parallel_infer", True)),
        repetition_penalty=float(payload.get("repetition_penalty") or 1.35),
        sample_steps=int(payload.get("sample_steps") or 16),
        if_sr=bool(payload.get("if_sr", False)),
        parallel_workers=max(1, int(payload.get("parallel_workers") or 1)),
        text_sources=text_sources,
    )


def _build_voxcpm_opts(payload: Dict[str, Any]) -> VoxCpmDistillOptions:
    text_sources = []
    for item in payload.get("text_sources") or []:
        if not isinstance(item, dict):
            continue
        source_path = _path_or_none(item.get("path"))
        kind = str(item.get("kind") or "").strip()
        if not source_path or not kind:
            continue
        text_sources.append(DistillTextSource(kind=kind, path=source_path))

    return VoxCpmDistillOptions(
        device=str(payload.get("device") or "cuda").strip().lower() or "cuda",
        allow_cpu_fallback=bool(payload.get("allow_cpu_fallback", True)),
        voice_mode=str(payload.get("voice_mode") or "description").strip().lower() or "description",
        voice_description=str(payload.get("voice_description") or "").strip(),
        reference_audio=_path_or_none(payload.get("reference_audio")),
        voice_reference_text=str(payload.get("voice_reference_text") or "").strip() or VoxCpmDistillOptions().voice_reference_text,
        prompt_text=str(payload.get("prompt_text") or "").strip(),
        cfg_value=float(payload.get("cfg_value") or 2.0),
        inference_timesteps=max(1, int(payload.get("inference_timesteps") or 10)),
        min_len=max(1, int(payload.get("min_len") or 2)),
        max_len=max(1, int(payload.get("max_len") or 4096)),
        normalize=bool(payload.get("normalize", False)),
        denoise=bool(payload.get("denoise", False)),
        retry_badcase=bool(payload.get("retry_badcase", True)),
        retry_badcase_max_times=max(0, int(payload.get("retry_badcase_max_times") or 3)),
        retry_badcase_ratio_threshold=float(payload.get("retry_badcase_ratio_threshold") or 6.0),
        parallel_workers=max(1, int(payload.get("parallel_workers") or 1)),
        text_sources=text_sources,
    )


def _pick_first(paths: list[Path]) -> Optional[Path]:
    return paths[0] if paths else None


def _find_default_paths() -> Dict[str, str]:
    defaults: Dict[str, str] = {}
    resources_root = _resources_root()
    if not resources_root or not resources_root.exists():
        return defaults

    model_root = resources_root / "Model"
    if not model_root.exists():
        model_root = resources_root

    asr_candidates = list(model_root.rglob("*.zip"))
    if asr_candidates:
        asr_candidates.sort(key=lambda p: (0 if "sosv" in p.name.lower() else 1, p.name.lower()))
        defaults["asr_model_zip"] = str(asr_candidates[0])

    config_candidates = list((model_root / "piper_checkpoints").rglob("config.json"))
    if config_candidates:
        config_candidates.sort(key=lambda p: (0 if "zh" in str(p).lower() else 1, str(p).lower()))
        defaults["piper_config"] = str(config_candidates[0])
        cfg_dir = config_candidates[0].parent
        ckpt_candidates = list(cfg_dir.glob("*.ckpt"))
        if ckpt_candidates:
            ckpt_candidates.sort(key=lambda p: p.stat().st_mtime, reverse=True)
            defaults["piper_base_checkpoint"] = str(ckpt_candidates[0])

    if "piper_base_checkpoint" not in defaults:
        ckpt_candidates = list((model_root / "piper_checkpoints").rglob("*.ckpt"))
        ckpt_candidates += list((resources_root / "CKPT").rglob("*.ckpt"))
        ckpt_candidates += list((resources_root.parent / "CKPT").rglob("*.ckpt"))
        if ckpt_candidates:
            ckpt_candidates.sort(key=lambda p: p.stat().st_mtime, reverse=True)
            defaults["piper_base_checkpoint"] = str(ckpt_candidates[0])

    defaults["resources_root"] = str(resources_root)
    return defaults


_active_lock = threading.Lock()
_active = False


def _set_active(value: bool) -> None:
    global _active
    with _active_lock:
        _active = value


def _is_active() -> bool:
    with _active_lock:
        return _active


def _handle_start(req_id: str, payload: Dict[str, Any]) -> None:
    if _is_active():
        _send({"type": "error", "id": req_id, "message": "当前已有任务在运行，请等待完成后再试。"})
        return

    input_audio = payload.get("input_audio") or []
    output_dir = payload.get("output_dir")
    if not output_dir:
        _send({"type": "error", "id": req_id, "message": "请先选择语音包输出目录。"})
        return

    paths = ProjectPaths(
        project_root=Path(output_dir),
        input_audio=[Path(p) for p in input_audio],
    )
    opts = _build_opts(payload.get("opts") or {})

    def progress(stage: str, value: float, message: str) -> None:
        _send({
            "type": "progress",
            "id": req_id,
            "stage": stage,
            "value": value,
            "message": message,
        })

    def run() -> None:
        _set_active(True)
        try:
            from engine.pipeline import run_pipeline  # type: ignore

            result = run_pipeline(paths, opts, progress)
            _send({
                "type": "done",
                "id": req_id,
                "payload": {
                    "manifest_path": str(result.manifest_path),
                    "voicepack_path": str(result.voicepack_path),
                    "preview_path": str(result.preview_path) if result.preview_path else None,
                    "training_log": str(result.training_log) if result.training_log else None,
                },
            })
        except Exception as exc:
            _send({
                "type": "error",
                "id": req_id,
                "message": str(exc),
                "traceback": traceback.format_exc(),
            })
        finally:
            _set_active(False)

    threading.Thread(target=run, daemon=True).start()
    _send({"type": "response", "id": req_id, "payload": {"started": True}})


def _handle_start_distill(req_id: str, payload: Dict[str, Any]) -> None:
    if _is_active():
        _send({"type": "error", "id": req_id, "message": "当前已有任务在运行，请等待完成后再试。"})
        return

    output_dir = payload.get("output_dir")
    if not output_dir:
        _send({"type": "error", "id": req_id, "message": "请先选择语音包输出目录。"})
        return

    try:
        opts = _build_opts(payload.get("opts") or {})
        distill_opts = _build_distill_opts(payload.get("distill") or {})
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc)})
        return

    paths = ProjectPaths(project_root=Path(output_dir))

    def progress(stage: str, value: float, message: str) -> None:
        _send(
            {
                "type": "progress",
                "id": req_id,
                "stage": stage,
                "value": value,
                "message": message,
            }
        )

    def run() -> None:
        _set_active(True)
        try:
            from engine.pipeline import run_distill_pipeline  # type: ignore

            result = run_distill_pipeline(paths, opts, distill_opts, progress)
            _send(
                {
                    "type": "done",
                    "id": req_id,
                    "payload": {
                        "manifest_path": str(result.manifest_path),
                        "voicepack_path": str(result.voicepack_path),
                        "preview_path": str(result.preview_path) if result.preview_path else None,
                        "training_log": str(result.training_log) if result.training_log else None,
                    },
                }
            )
        except Exception as exc:
            _send(
                {
                    "type": "error",
                    "id": req_id,
                    "message": str(exc),
                    "traceback": traceback.format_exc(),
                }
            )
        finally:
            _set_active(False)

    threading.Thread(target=run, daemon=True).start()
    _send({"type": "response", "id": req_id, "payload": {"started": True}})


def _handle_start_voxcpm_distill(req_id: str, payload: Dict[str, Any]) -> None:
    if _is_active():
        _send({"type": "error", "id": req_id, "message": "当前已有任务在运行，请等待完成后再试。"})
        return

    output_dir = payload.get("output_dir")
    if not output_dir:
        _send({"type": "error", "id": req_id, "message": "请先选择语音包输出目录。"})
        return

    try:
        opts = _build_opts(payload.get("opts") or {})
        voxcpm_opts = _build_voxcpm_opts(payload.get("voxcpm") or {})
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc)})
        return

    paths = ProjectPaths(project_root=Path(output_dir))

    def progress(stage: str, value: float, message: str) -> None:
        _send(
            {
                "type": "progress",
                "id": req_id,
                "stage": stage,
                "value": value,
                "message": message,
            }
        )

    def run() -> None:
        _set_active(True)
        try:
            from engine.pipeline import run_voxcpm_distill_pipeline  # type: ignore

            result = run_voxcpm_distill_pipeline(paths, opts, voxcpm_opts, progress)
            _send(
                {
                    "type": "done",
                    "id": req_id,
                    "payload": {
                        "manifest_path": str(result.manifest_path),
                        "voicepack_path": str(result.voicepack_path),
                        "preview_path": str(result.preview_path) if result.preview_path else None,
                        "training_log": str(result.training_log) if result.training_log else None,
                    },
                }
            )
        except Exception as exc:
            _send(
                {
                    "type": "error",
                    "id": req_id,
                    "message": str(exc),
                    "traceback": traceback.format_exc(),
                }
            )
        finally:
            _set_active(False)

    threading.Thread(target=run, daemon=True).start()
    _send({"type": "response", "id": req_id, "payload": {"started": True}})


def _handle_start_resume_project(req_id: str, payload: Dict[str, Any]) -> None:
    if _is_active():
        _send({"type": "error", "id": req_id, "message": "当前已有任务在运行，请等待完成后再试。"})
        return

    project_dir = _path_or_none(payload.get("project_dir"))
    if project_dir is None:
        _send({"type": "error", "id": req_id, "message": "请先选择要继续训练的旧项目目录。"})
        return

    paths = ProjectPaths(project_root=project_dir)

    def progress(stage: str, value: float, message: str) -> None:
        _send({"type": "progress", "id": req_id, "stage": stage, "value": value, "message": message})

    def run() -> None:
        _set_active(True)
        try:
            from engine.pipeline import run_resume_project_pipeline  # type: ignore

            result = run_resume_project_pipeline(paths, progress)
            _send(
                {
                    "type": "done",
                    "id": req_id,
                    "payload": {
                        "manifest_path": str(result.manifest_path),
                        "voicepack_path": str(result.voicepack_path),
                        "preview_path": str(result.preview_path) if result.preview_path else None,
                        "training_log": str(result.training_log) if result.training_log else None,
                    },
                }
            )
        except Exception as exc:
            _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
        finally:
            _set_active(False)

    threading.Thread(target=run, daemon=True).start()
    _send({"type": "response", "id": req_id, "payload": {"started": True}})


def _handle_inspect_training_project(req_id: str, payload: Dict[str, Any]) -> None:
    project_dir = _path_or_none(payload.get("project_dir"))
    if project_dir is None:
        _send({"type": "response", "id": req_id, "payload": {"ok": False, "message": "请先选择要检查的旧项目目录。"}})
        return
    try:
        config = load_project_config(project_dir)
        paths = ProjectPaths(project_root=project_dir)
        mode = str(config.get("mode") or "").strip()
        raw_expected_texts = config.get("metadata_texts") or []
        if not isinstance(raw_expected_texts, list):
            raw_expected_texts = []
        expected_texts = [str(item) for item in raw_expected_texts]
        raw_input_audio = config.get("input_audio") or []
        if not isinstance(raw_input_audio, list):
            raw_input_audio = []
        input_audio = [Path(str(item)) for item in raw_input_audio if str(item).strip()]
        input_audio_existing = [path for path in input_audio if path.exists()]
        metadata_error = ""
        try:
            entries = read_metadata_entries(paths.training_manifest)
            missing = [str(audio_path) for audio_path, _text in entries if not audio_path.exists()]
            metadata_texts = [text for _audio_path, text in entries]
            metadata_message = f"语料 {len(entries)} 条，缺失音频 {len(missing)} 条"
        except Exception as metadata_exc:
            entries = []
            missing = []
            metadata_texts = []
            metadata_error = str(metadata_exc)
            metadata_message = f"训练文本记录不完整：{metadata_exc}"
            if mode != "piper" or not config.get("input_audio"):
                raise
        metadata_inconsistent = bool(expected_texts and metadata_texts and expected_texts != metadata_texts)
        existing_count = len(entries) - len(missing)
        direct_train_ready = bool(entries) and not missing and not metadata_inconsistent
        needs_material_rebuild = not direct_train_ready
        can_rebuild_material = False
        material_status = ""
        training_opts = config.get("training_options") or {}
        if not isinstance(training_opts, dict):
            training_opts = {}
        config_summary = f"device={training_opts.get('device', 'cpu')} / batch={training_opts.get('batch_size', '默认')}"

        if mode == "piper":
            can_rebuild_material = bool(input_audio_existing)
            if direct_train_ready:
                material_status = "训练素材完整，可直接进入训练，不会重新处理原始录音。"
            elif can_rebuild_material:
                material_status = "训练素材不完整，将使用项目保存的原始录音重新准备。"
            else:
                material_status = "训练素材不完整，且项目内没有可用原始音频，无法自动重建。"
        elif mode == "gsv_distill":
            distill_opts = config.get("distill_options") or {}
            if not isinstance(distill_opts, dict):
                distill_opts = {}
            config_summary = (
                f"{distill_opts.get('version', '未知版本')} / {distill_opts.get('speaker', '未知说话人')} / "
                f"{distill_opts.get('prompt_lang', '未知语言')} / {distill_opts.get('emotion', '未知情感')}"
            )
            can_rebuild_material = bool(entries)
            if direct_train_ready:
                material_status = "训练素材完整，可直接进入训练。"
            elif existing_count:
                material_status = "部分合成音频缺失；开始训练时会尝试按项目配置补生成，失败时移除缺失文本后继续。"
            else:
                material_status = "合成音频完全缺失；开始训练时必须能访问原 GPT-SoVITS/GSVI 模型配置才能补生成。"
        elif mode == "voxcpm_distill":
            voxcpm_opts = config.get("voxcpm_options") or {}
            if not isinstance(voxcpm_opts, dict):
                voxcpm_opts = {}
            config_summary = (
                f"{voxcpm_opts.get('voice_mode', 'description')} / device={voxcpm_opts.get('device', 'cuda')} / "
                f"denoise={'on' if voxcpm_opts.get('denoise') else 'off'}"
            )
            can_rebuild_material = bool(entries)
            if direct_train_ready:
                material_status = "训练素材完整，可直接进入训练。"
            else:
                material_status = "部分或全部合成音频缺失；开始训练时会按项目内 VoxCPM2 配置继续生成缺失音频。"
        else:
            material_status = "项目模式未知，无法判断素材恢复策略。"

        known_modes = {"piper", "gsv_distill", "voxcpm_distill"}
        mode_labels = {
            "piper": "Piper 标准",
            "gsv_distill": "GPT-SoVITS 蒸馏",
            "voxcpm_distill": "VoxCPM2 蒸馏",
        }
        ok = mode in known_modes and (direct_train_ready or can_rebuild_material)
        completion_state = "ready" if direct_train_ready else "unfinished"
        _send(
            {
                "type": "response",
                "id": req_id,
                "payload": {
                    "ok": ok,
                    "has_project": True,
                    "message": f"项目{'可用' if ok else '不可用'}：{mode_labels.get(mode, '未知模式')}，{metadata_message}",
                    "mode": mode,
                    "project_dir": str(project_dir),
                    "metadata_count": len(entries),
                    "existing_count": existing_count,
                    "missing_count": len(missing),
                    "metadata_inconsistent": metadata_inconsistent,
                    "metadata_error": metadata_error,
                    "expected_text_count": len(expected_texts),
                    "input_audio_count": len(input_audio),
                    "input_audio_available_count": len(input_audio_existing),
                    "input_audio_missing_count": len(input_audio) - len(input_audio_existing),
                    "direct_train_ready": direct_train_ready,
                    "needs_material_rebuild": needs_material_rebuild,
                    "can_rebuild_material": can_rebuild_material,
                    "material_status": material_status,
                    "config_summary": config_summary,
                    "config_path": str(paths.work_dir / "kigtts_project.json"),
                    "completion_state": completion_state,
                },
            }
        )
    except Exception as exc:
        _send(
            {
                "type": "response",
                "id": req_id,
                "payload": {
                    "ok": False,
                    "has_project": False,
                    "message": str(exc),
                    "project_dir": str(project_dir),
                },
            }
        )


def _handle_preview(req_id: str, payload: Dict[str, Any]) -> None:
    if _is_active():
        _send({"type": "error", "id": req_id, "message": "当前已有任务在运行，请等待完成后再试。"})
        return

    voicepack_path_raw = str(payload.get("voicepack_path") or "").strip()
    text = str(payload.get("text") or "").strip()
    if not voicepack_path_raw:
        _send({"type": "error", "id": req_id, "message": "请先选择要试听的语音包。"})
        return
    if not text:
        _send({"type": "error", "id": req_id, "message": "请先输入试听文本。"})
        return

    voicepack_path = Path(voicepack_path_raw)
    output_root = _path_or_none(payload.get("output_dir")) or Path.cwd()
    preview_dir = output_root / "preview"
    preview_path = preview_dir / "voicepack_preview.wav"

    def progress(stage: str, value: float, message: str) -> None:
        _send({
            "type": "progress",
            "id": req_id,
            "stage": stage,
            "value": value,
            "message": message,
        })

    def run() -> None:
        _set_active(True)
        try:
            from engine.voice_preview import synthesize_voicepack  # type: ignore

            result = synthesize_voicepack(voicepack_path, text, preview_path, progress)
            _send({
                "type": "preview_done",
                "id": req_id,
                "payload": {
                    "audio_path": str(result),
                },
            })
        except Exception as exc:
            _send({
                "type": "error",
                "id": req_id,
                "message": str(exc),
                "traceback": traceback.format_exc(),
            })
        finally:
            _set_active(False)

    threading.Thread(target=run, daemon=True).start()
    _send({"type": "response", "id": req_id, "payload": {"started": True}})


def _handle_gsv_distill_preview(req_id: str, payload: Dict[str, Any]) -> None:
    if _is_active():
        _send({"type": "error", "id": req_id, "message": "当前已有任务在运行，请等待完成后再试。"})
        return

    text = str(payload.get("text") or "").strip()
    if not text:
        _send({"type": "error", "id": req_id, "message": "请先输入试听文本。"})
        return
    try:
        distill_opts = _build_distill_opts(payload.get("distill") or {})
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc)})
        return

    output_root = _path_or_none(payload.get("output_dir")) or Path.cwd()
    preview_root = output_root / "preview" / "gsv_distill"

    def progress(stage: str, value: float, message: str) -> None:
        _send({"type": "progress", "id": req_id, "stage": stage, "value": value, "message": message})

    def run() -> None:
        _set_active(True)
        try:
            from engine.gsv_distill import synthesize_gsv_preview  # type: ignore

            result = synthesize_gsv_preview(preview_root, distill_opts, text, progress)
            _send({"type": "preview_done", "id": req_id, "payload": {"audio_path": str(result)}})
        except Exception as exc:
            _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
        finally:
            _set_active(False)

    threading.Thread(target=run, daemon=True).start()
    _send({"type": "response", "id": req_id, "payload": {"started": True}})


def _handle_voxcpm_distill_preview(req_id: str, payload: Dict[str, Any]) -> None:
    if _is_active():
        _send({"type": "error", "id": req_id, "message": "当前已有任务在运行，请等待完成后再试。"})
        return

    text = str(payload.get("text") or "").strip()
    if not text:
        _send({"type": "error", "id": req_id, "message": "请先输入试听文本。"})
        return
    try:
        opts = _build_opts(payload.get("opts") or {})
        voxcpm_opts = _build_voxcpm_opts(payload.get("voxcpm") or {})
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc)})
        return

    output_root = _path_or_none(payload.get("output_dir")) or Path.cwd()
    preview_root = output_root / "preview" / "voxcpm_distill"

    def progress(stage: str, value: float, message: str) -> None:
        _send({"type": "progress", "id": req_id, "stage": stage, "value": value, "message": message})

    def run() -> None:
        _set_active(True)
        try:
            from engine.voxcpm_distill import synthesize_voxcpm_preview  # type: ignore

            result = synthesize_voxcpm_preview(preview_root, voxcpm_opts, text, opts, progress)
            _send({"type": "preview_done", "id": req_id, "payload": {"audio_path": str(result)}})
        except Exception as exc:
            _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
        finally:
            _set_active(False)

    threading.Thread(target=run, daemon=True).start()
    _send({"type": "response", "id": req_id, "payload": {"started": True}})


def _handle_get_piper_cuda_runtime(req_id: str) -> None:
    try:
        payload = describe_piper_cuda_runtime()
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
        return
    _send({"type": "response", "id": req_id, "payload": payload})


def _handle_get_piper_runtime(req_id: str) -> None:
    try:
        payload = describe_piper_runtime()
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
        return
    _send({"type": "response", "id": req_id, "payload": payload})


def _handle_install_piper_runtime(req_id: str, payload: Dict[str, Any]) -> None:
    if _is_active():
        _send({"type": "error", "id": req_id, "message": "当前已有任务在运行，请等待完成后再试。"})
        return

    force = bool(payload.get("force", False))
    local_archive_path = _path_or_none(payload.get("local_archive_path"))

    def progress(stage: str, value: float, message: str) -> None:
        _send({"type": "progress", "id": req_id, "stage": stage, "value": value, "message": message})

    _set_active(True)
    try:
        result = install_piper_runtime(progress=progress, force=force, local_archive_path=local_archive_path)
        _send({"type": "response", "id": req_id, "payload": result})
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
    finally:
        _set_active(False)


def _handle_install_piper_cuda_runtime(req_id: str, payload: Dict[str, Any]) -> None:
    if _is_active():
        _send({"type": "error", "id": req_id, "message": "当前已有任务在运行，请等待完成后再试。"})
        return

    force = bool(payload.get("force", False))
    local_archive_path = _path_or_none(payload.get("local_archive_path"))

    def progress(stage: str, value: float, message: str) -> None:
        _send(
            {
                "type": "progress",
                "id": req_id,
                "stage": stage,
                "value": value,
                "message": message,
            }
        )

    _set_active(True)
    try:
        result = install_piper_cuda_runtime(progress=progress, force=force, local_archive_path=local_archive_path)
        _send({"type": "response", "id": req_id, "payload": result})
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
    finally:
        _set_active(False)


def _handle_get_voxcpm_runtime(req_id: str) -> None:
    try:
        payload = describe_voxcpm_runtime()
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
        return
    _send({"type": "response", "id": req_id, "payload": payload})


def _handle_install_voxcpm_runtime(req_id: str, payload: Dict[str, Any]) -> None:
    if _is_active():
        _send({"type": "error", "id": req_id, "message": "当前已有任务在运行，请等待完成后再试。"})
        return

    force = bool(payload.get("force", False))
    local_archive_path = _path_or_none(payload.get("local_archive_path"))

    def progress(stage: str, value: float, message: str) -> None:
        _send({"type": "progress", "id": req_id, "stage": stage, "value": value, "message": message})

    _set_active(True)
    try:
        result = install_voxcpm_runtime(progress=progress, force=force, local_archive_path=local_archive_path)
        _send({"type": "response", "id": req_id, "payload": result})
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
    finally:
        _set_active(False)


def _handle_get_trainer_resources(req_id: str) -> None:
    try:
        payload = describe_trainer_resources()
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
        return
    _send({"type": "response", "id": req_id, "payload": payload})


def _handle_get_download_sources(req_id: str) -> None:
    try:
        payload = describe_download_sources()
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
        return
    _send({"type": "response", "id": req_id, "payload": payload})


def _handle_save_download_sources(req_id: str, payload: Dict[str, Any]) -> None:
    try:
        result = save_download_sources(payload)
        _send({"type": "response", "id": req_id, "payload": result})
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})


def _handle_install_trainer_resources(req_id: str, payload: Dict[str, Any]) -> None:
    if _is_active():
        _send({"type": "error", "id": req_id, "message": "当前已有任务在运行，请等待完成后再试。"})
        return

    force = bool(payload.get("force", False))
    local_archive_path = _path_or_none(payload.get("local_archive_path"))

    def progress(stage: str, value: float, message: str) -> None:
        _send({"type": "progress", "id": req_id, "stage": stage, "value": value, "message": message})

    _set_active(True)
    try:
        result = install_trainer_resources(progress=progress, force=force, local_archive_path=local_archive_path)
        _send({"type": "response", "id": req_id, "payload": result})
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
    finally:
        _set_active(False)


def _handle_get_voxcpm_models(req_id: str) -> None:
    try:
        payload = describe_voxcpm_models()
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
        return
    _send({"type": "response", "id": req_id, "payload": payload})


def _handle_download_voxcpm_models(req_id: str, payload: Dict[str, Any]) -> None:
    if _is_active():
        _send({"type": "error", "id": req_id, "message": "当前已有任务在运行，请等待完成后再试。"})
        return

    include_denoiser = bool(payload.get("include_denoiser", True))
    force = bool(payload.get("force", False))

    def progress(stage: str, value: float, message: str) -> None:
        _send({"type": "progress", "id": req_id, "stage": stage, "value": value, "message": message})

    _set_active(True)
    try:
        result = download_voxcpm_models(progress=progress, include_denoiser=include_denoiser, force=force)
        _send({"type": "response", "id": req_id, "payload": result})
    except Exception as exc:
        _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
    finally:
        _set_active(False)


def _handle_request(req: Dict[str, Any]) -> None:
    req_id = str(req.get("id") or "")
    msg_type = req.get("type")
    payload = req.get("payload") or {}

    if msg_type == "ping":
        _send({"type": "response", "id": req_id, "payload": {"ok": True}})
        return
    if msg_type == "start_pipeline":
        _handle_start(req_id, payload)
        return
    if msg_type == "start_distill_pipeline":
        _handle_start_distill(req_id, payload)
        return
    if msg_type == "start_voxcpm_distill_pipeline":
        _handle_start_voxcpm_distill(req_id, payload)
        return
    if msg_type == "start_resume_project_pipeline":
        _handle_start_resume_project(req_id, payload)
        return
    if msg_type == "inspect_training_project":
        _handle_inspect_training_project(req_id, payload)
        return
    if msg_type == "preview_voicepack":
        _handle_preview(req_id, payload)
        return
    if msg_type == "preview_gsv_distill":
        _handle_gsv_distill_preview(req_id, payload)
        return
    if msg_type == "preview_voxcpm_distill":
        _handle_voxcpm_distill_preview(req_id, payload)
        return
    if msg_type == "get_defaults":
        _send({"type": "defaults", "id": req_id, "payload": _find_default_paths()})
        return
    if msg_type == "validate_gsv_root":
        root = _path_or_none(payload.get("gsv_root"))
        if root is None:
            _send({"type": "response", "id": req_id, "payload": {"ok": False, "message": "请先选择 GPT-SoVITS / GSVI 整合包根目录。"}})
            return
        _send({"type": "response", "id": req_id, "payload": validate_gsv_root(root)})
        return
    if msg_type == "scan_gsv_models":
        root = _path_or_none(payload.get("gsv_root"))
        if root is None:
            _send({"type": "error", "id": req_id, "message": "请先选择 GPT-SoVITS / GSVI 整合包根目录。"})
            return
        try:
            catalog = scan_gsv_models(root)
        except Exception as exc:
            _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
            return
        _send({"type": "response", "id": req_id, "payload": catalog})
        return
    if msg_type == "get_piper_runtime_status":
        _handle_get_piper_runtime(req_id)
        return
    if msg_type == "install_piper_runtime":
        _handle_install_piper_runtime(req_id, payload)
        return
    if msg_type == "get_piper_cuda_runtime_status":
        _handle_get_piper_cuda_runtime(req_id)
        return
    if msg_type == "install_piper_cuda_runtime":
        _handle_install_piper_cuda_runtime(req_id, payload)
        return
    if msg_type == "get_voxcpm_runtime_status":
        _handle_get_voxcpm_runtime(req_id)
        return
    if msg_type == "install_voxcpm_runtime":
        _handle_install_voxcpm_runtime(req_id, payload)
        return
    if msg_type == "get_trainer_resources_status":
        _handle_get_trainer_resources(req_id)
        return
    if msg_type == "install_trainer_resources":
        _handle_install_trainer_resources(req_id, payload)
        return
    if msg_type == "get_voxcpm_model_status":
        _handle_get_voxcpm_models(req_id)
        return
    if msg_type == "download_voxcpm_models":
        _handle_download_voxcpm_models(req_id, payload)
        return
    if msg_type == "get_download_source_config":
        _handle_get_download_sources(req_id)
        return
    if msg_type == "save_download_source_config":
        _handle_save_download_sources(req_id, payload)
        return

    _send({"type": "error", "id": req_id, "message": "当前版本暂不支持这个操作，请升级软件后再试。"})


for line in sys.stdin:
    line = line.strip()
    if not line:
        continue
    try:
        req = json.loads(line)
    except Exception as exc:
        _send({"type": "error", "id": "", "message": "后台服务收到的请求格式不正确，请重启软件后再试。"})
        continue
    _handle_request(req)
