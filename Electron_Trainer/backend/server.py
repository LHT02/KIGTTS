import json
import os
import sys
import threading
import traceback
from pathlib import Path
from typing import Any, Dict, Optional

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))

from engine import DistillOptions, DistillTextSource, ProjectPaths, TrainingOptions, run_distill_pipeline, run_pipeline  # type: ignore
from engine.gsv_distill import scan_gsv_models, validate_gsv_root  # type: ignore
from engine.voice_preview import synthesize_voicepack  # type: ignore


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
    env_resources = os.environ.get("KGTTS_RESOURCES")
    if env_resources:
        return Path(env_resources)
    env_base = os.environ.get("KGTTS_BASE_DIR")
    if env_base:
        base = Path(env_base)
        candidate = base / "resources_pack"
        if candidate.exists():
            return candidate
        if base.exists():
            return base
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
        raise RuntimeError("缺少 GPT-SoVITS 根目录")

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
        _send({"type": "error", "id": req_id, "message": "已有任务在运行"})
        return

    input_audio = payload.get("input_audio") or []
    output_dir = payload.get("output_dir")
    if not output_dir:
        _send({"type": "error", "id": req_id, "message": "缺少输出目录"})
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
        _send({"type": "error", "id": req_id, "message": "已有任务在运行"})
        return

    output_dir = payload.get("output_dir")
    if not output_dir:
        _send({"type": "error", "id": req_id, "message": "缺少输出目录"})
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


def _handle_preview(req_id: str, payload: Dict[str, Any]) -> None:
    if _is_active():
        _send({"type": "error", "id": req_id, "message": "已有任务在运行"})
        return

    voicepack_path_raw = str(payload.get("voicepack_path") or "").strip()
    text = str(payload.get("text") or "").strip()
    if not voicepack_path_raw:
        _send({"type": "error", "id": req_id, "message": "缺少语音包路径"})
        return
    if not text:
        _send({"type": "error", "id": req_id, "message": "缺少试听文本"})
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
    if msg_type == "preview_voicepack":
        _handle_preview(req_id, payload)
        return
    if msg_type == "get_defaults":
        _send({"type": "defaults", "id": req_id, "payload": _find_default_paths()})
        return
    if msg_type == "validate_gsv_root":
        root = _path_or_none(payload.get("gsv_root"))
        if root is None:
            _send({"type": "response", "id": req_id, "payload": {"ok": False, "message": "缺少 GPT-SoVITS 根目录"}})
            return
        _send({"type": "response", "id": req_id, "payload": validate_gsv_root(root)})
        return
    if msg_type == "scan_gsv_models":
        root = _path_or_none(payload.get("gsv_root"))
        if root is None:
            _send({"type": "error", "id": req_id, "message": "缺少 GPT-SoVITS 根目录"})
            return
        try:
            catalog = scan_gsv_models(root)
        except Exception as exc:
            _send({"type": "error", "id": req_id, "message": str(exc), "traceback": traceback.format_exc()})
            return
        _send({"type": "response", "id": req_id, "payload": catalog})
        return

    _send({"type": "error", "id": req_id, "message": f"未知命令: {msg_type}"})


for line in sys.stdin:
    line = line.strip()
    if not line:
        continue
    try:
        req = json.loads(line)
    except Exception as exc:
        _send({"type": "error", "id": "", "message": f"JSON 解析失败: {exc}"})
        continue
    _handle_request(req)
