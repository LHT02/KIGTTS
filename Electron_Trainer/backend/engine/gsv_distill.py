import csv
import json
import os
import re
import subprocess
from pathlib import Path
from typing import Any, Callable, Dict, Optional

from .config import DistillOptions, DistillTextSource, ProjectPaths


ProgressCallback = Callable[[str, float, str], None]
_EMOTION_PATTERN = re.compile(r"^【(?P<emotion>[^】]+)】(?P<prompt>.+)\.wav$", re.IGNORECASE)


def _runtime_python(gsv_root: Path) -> Path:
    if os.name == "nt":
        return gsv_root / "runtime" / "python.exe"
    return gsv_root / "runtime" / "bin" / "python3"


def validate_gsv_root(root: Path) -> Dict[str, Any]:
    gsv_root = root.expanduser().resolve()
    runtime_python = _runtime_python(gsv_root)
    models_dir = gsv_root / "models"
    infer_config = gsv_root / "GPT_SoVITS" / "configs" / "tts_infer.yaml"

    problems: list[str] = []
    if not gsv_root.exists():
        problems.append("目录不存在")
    if not runtime_python.exists():
        problems.append(f"缺少运行时 Python: {runtime_python}")
    if not models_dir.exists():
        problems.append(f"缺少模型目录: {models_dir}")
    if not infer_config.exists():
        problems.append(f"缺少推理配置: {infer_config}")

    return {
        "ok": not problems,
        "root": str(gsv_root),
        "runtime_python": str(runtime_python),
        "models_dir": str(models_dir),
        "config_path": str(infer_config),
        "message": "校验通过" if not problems else "；".join(problems),
    }


def scan_gsv_models(root: Path) -> Dict[str, Any]:
    validation = validate_gsv_root(root)
    if not validation["ok"]:
        raise RuntimeError(str(validation["message"]))

    models_dir = Path(validation["models_dir"])
    versions: Dict[str, Any] = {}
    for version_dir in sorted(models_dir.iterdir(), key=lambda item: item.name.lower()):
        if not version_dir.is_dir():
            continue
        speakers: Dict[str, Any] = {}
        for speaker_dir in sorted(version_dir.iterdir(), key=lambda item: item.name.lower()):
            if not speaker_dir.is_dir():
                continue
            reference_root = speaker_dir / "reference_audios"
            if not reference_root.exists():
                continue
            languages: Dict[str, Any] = {}
            for lang_dir in sorted(reference_root.iterdir(), key=lambda item: item.name.lower()):
                emotion_dir = lang_dir / "emotions"
                if not emotion_dir.exists():
                    continue
                emotions = []
                for wav_file in sorted(emotion_dir.glob("*.wav"), key=lambda item: item.name.lower()):
                    match = _EMOTION_PATTERN.match(wav_file.name)
                    if not match:
                        continue
                    emotions.append(
                        {
                            "name": match.group("emotion"),
                            "prompt_text": match.group("prompt"),
                            "ref_audio_path": str(wav_file.resolve()),
                        }
                    )
                if emotions:
                    languages[lang_dir.name] = {"emotions": emotions}
            if languages:
                speakers[speaker_dir.name] = {"languages": languages}
        if speakers:
            versions[version_dir.name] = {"speakers": speakers}

    return {
        "root": validation["root"],
        "versions": versions,
    }


def _read_text_file(path: Path) -> list[str]:
    return [line.strip() for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def _read_csv_texts(path: Path) -> list[str]:
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        if "text" not in (reader.fieldnames or []):
            raise RuntimeError(f"CSV 缺少 text 列: {path}")
        return [str(row.get("text") or "").strip() for row in reader if str(row.get("text") or "").strip()]


def _read_jsonl_texts(path: Path) -> list[str]:
    texts: list[str] = []
    with path.open("r", encoding="utf-8") as handle:
        for line_no, line in enumerate(handle, start=1):
            stripped = line.strip()
            if not stripped:
                continue
            try:
                record = json.loads(stripped)
            except json.JSONDecodeError as exc:
                raise RuntimeError(f"JSONL 解析失败: {path}:{line_no}: {exc}") from exc
            text = str(record.get("text") or "").strip()
            if text:
                texts.append(text)
    return texts


def _read_project_texts(path: Path) -> list[str]:
    metadata = path / "work" / "metadata.csv"
    if not metadata.exists():
        raise RuntimeError(f"旧项目缺少 metadata.csv: {metadata}")
    texts: list[str] = []
    with metadata.open("r", encoding="utf-8") as handle:
        for raw in handle:
            line = raw.strip()
            if not line or "|" not in line:
                continue
            _, text = line.split("|", 1)
            text = text.strip()
            if text:
                texts.append(text)
    return texts


def collect_distill_texts(
    distill: DistillOptions,
    work_dir: Path,
    progress: Optional[ProgressCallback] = None,
) -> list[str]:
    if not distill.text_sources:
        raise RuntimeError("未提供蒸馏文本来源")

    collected: list[str] = []
    total_sources = len(distill.text_sources)
    for index, source in enumerate(distill.text_sources, start=1):
        source_path = source.path.expanduser().resolve()
        if source.kind == "text_file":
            suffix = source_path.suffix.lower()
            if suffix == ".txt":
                texts = _read_text_file(source_path)
            elif suffix == ".csv":
                texts = _read_csv_texts(source_path)
            elif suffix == ".jsonl":
                texts = _read_jsonl_texts(source_path)
            else:
                raise RuntimeError(f"不支持的文本文件格式: {source_path.name}")
        elif source.kind == "project_dir":
            texts = _read_project_texts(source_path)
        else:
            raise RuntimeError(f"不支持的文本来源类型: {source.kind}")

        collected.extend([text for text in texts if text.strip()])
        if progress:
            progress(
                "collect",
                index / total_sources,
                f"已收集 {index}/{total_sources} 个来源，共 {len(collected)} 条文本",
            )

    corpus_dir = work_dir / "distill_corpus"
    corpus_dir.mkdir(parents=True, exist_ok=True)
    texts_path = corpus_dir / "texts.jsonl"
    texts_path.write_text(
        "\n".join(json.dumps({"text": text}, ensure_ascii=False) for text in collected) + ("\n" if collected else ""),
        encoding="utf-8",
    )
    return collected


def _ensure_selection(catalog: Dict[str, Any], distill: DistillOptions) -> None:
    versions = catalog.get("versions") or {}
    version = versions.get(distill.version)
    if not version:
        raise RuntimeError(f"未找到 GPT-SoVITS 版本: {distill.version}")
    speaker = (version.get("speakers") or {}).get(distill.speaker)
    if not speaker:
        raise RuntimeError(f"未找到说话人模型: {distill.speaker}")
    language = (speaker.get("languages") or {}).get(distill.prompt_lang)
    if not language:
        raise RuntimeError(f"未找到参考语言: {distill.prompt_lang}")
    emotions = language.get("emotions") or []
    if not any(item.get("name") == distill.emotion for item in emotions):
        raise RuntimeError(f"未找到情感参考: {distill.emotion}")


def _helper_request_payload(
    validation: Dict[str, Any],
    distill: DistillOptions,
    texts: list[str],
    paths: ProjectPaths,
    device: str,
    batch_size: int,
    parallel_infer: bool,
) -> Dict[str, Any]:
    corpus_dir = paths.work_dir / "distill_corpus"
    wav_dir = corpus_dir / "wavs"
    return {
        "gsv_root": validation["root"],
        "config_path": validation["config_path"],
        "version": distill.version,
        "speaker": distill.speaker,
        "prompt_lang": distill.prompt_lang,
        "emotion": distill.emotion,
        "texts": texts,
        "wav_dir": str(wav_dir),
        "metadata_path": str(paths.training_manifest),
        "device": device,
        "batch_size": batch_size,
        "text_lang": distill.text_lang,
        "text_split_method": distill.text_split_method,
        "speed_factor": distill.speed_factor,
        "temperature": distill.temperature,
        "seed": distill.seed,
        "top_k": distill.top_k,
        "top_p": distill.top_p,
        "batch_threshold": distill.batch_threshold,
        "split_bucket": distill.split_bucket,
        "fragment_interval": distill.fragment_interval,
        "parallel_infer": parallel_infer,
        "repetition_penalty": distill.repetition_penalty,
        "sample_steps": distill.sample_steps,
        "if_sr": distill.if_sr,
    }


def _run_helper(
    validation: Dict[str, Any],
    request_path: Path,
    log_path: Path,
    progress: Optional[ProgressCallback] = None,
    progress_stage: str = "distill",
) -> Dict[str, Any]:
    helper_script = Path(__file__).resolve().parents[1] / "tools" / "gsv_distill_helper.py"
    runtime_python = str(validation["runtime_python"])
    gsv_root = str(validation["root"])

    result: Dict[str, Any] = {"code": 0, "oom": False, "message": "", "generated": 0}
    env = {
        **os.environ,
        "PYTHONIOENCODING": "utf-8",
        "PYTHONUTF8": "1",
    }
    with log_path.open("a", encoding="utf-8") as log_file:
        proc = subprocess.Popen(
            [runtime_python, "-u", str(helper_script), "--request", str(request_path)],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            stdin=subprocess.DEVNULL,
            text=False,
            cwd=gsv_root,
            bufsize=0,
            close_fds=True,
            env=env,
        )
        log_file.write(f"[spawn] pid={proc.pid}\n")
        log_file.flush()

        if proc.stdout is not None:
            for raw_line in proc.stdout:
                if isinstance(raw_line, str):
                    line = raw_line.rstrip("\n")
                else:
                    try:
                        line = raw_line.decode("utf-8").rstrip("\n")
                    except UnicodeDecodeError:
                        try:
                            line = raw_line.decode("gbk").rstrip("\n")
                        except UnicodeDecodeError:
                            line = raw_line.decode("utf-8", errors="replace").rstrip("\n")
                log_file.write(line + "\n")
                log_file.flush()
                try:
                    event = json.loads(line)
                except json.JSONDecodeError:
                    continue
                if event.get("type") == "progress" and progress:
                    progress(progress_stage, float(event.get("value") or 0.0), str(event.get("message") or "蒸馏中"))
                elif event.get("type") == "done":
                    result["generated"] = int(event.get("generated") or 0)
                elif event.get("type") == "error":
                    result["oom"] = bool(event.get("oom"))
                    result["message"] = str(event.get("message") or "蒸馏失败")
                    result["traceback"] = str(event.get("traceback") or "")
        proc.wait()
        result["code"] = int(proc.returncode or 0)
    return result


def build_distill_corpus(
    paths: ProjectPaths,
    distill: DistillOptions,
    progress: Optional[ProgressCallback] = None,
) -> int:
    validation = validate_gsv_root(distill.gsv_root)
    if not validation["ok"]:
        raise RuntimeError(str(validation["message"]))

    catalog = scan_gsv_models(distill.gsv_root)
    _ensure_selection(catalog, distill)

    texts = collect_distill_texts(distill, paths.work_dir, progress)
    if not texts:
        raise RuntimeError("蒸馏文本为空")

    distill_dir = paths.work_dir / "distill_corpus"
    distill_dir.mkdir(parents=True, exist_ok=True)
    request_path = distill_dir / "request.json"
    log_path = paths.work_dir / "distill.log"
    if log_path.exists():
        log_path.unlink()

    device = "cuda" if str(distill.device).lower() in {"cuda", "gpu"} else "cpu"
    batch_size = max(1, int(distill.batch_size))
    parallel_infer = bool(distill.parallel_infer)

    if progress:
        progress("collect", 1.0, f"文本收集完成，共 {len(texts)} 条")

    attempts = 0
    while True:
        attempts += 1
        request_payload = _helper_request_payload(
            validation,
            distill,
            texts,
            paths,
            device=device,
            batch_size=batch_size,
            parallel_infer=parallel_infer,
        )
        request_path.write_text(json.dumps(request_payload, ensure_ascii=False, indent=2), encoding="utf-8")
        if progress:
            progress(
                "distill",
                0.0,
                f"启动 GPT-SoVITS 蒸馏，第 {attempts} 次尝试（device={device}, batch={batch_size}, parallel={parallel_infer}）",
            )

        result = _run_helper(validation, request_path, log_path, progress)
        if result["code"] == 0:
            if progress:
                progress("distill", 1.0, f"蒸馏语料生成完成，共 {result['generated']} 条")
            return len(texts)

        if not result.get("oom") or device != "cuda":
            raise RuntimeError(str(result.get("message") or f"蒸馏失败，详见 {log_path}"))

        retry_message = ""
        if parallel_infer:
            parallel_infer = False
            retry_message = "检测到显存不足，自动关闭并行推理后重试。"
        elif batch_size > 1:
            batch_size = max(1, batch_size // 2)
            retry_message = f"检测到显存不足，自动降低蒸馏 batch_size 到 {batch_size} 后重试。"
        else:
            device = "cpu"
            retry_message = "检测到显存不足，自动切换到 CPU 蒸馏。"

        with log_path.open("a", encoding="utf-8") as log_file:
            log_file.write(retry_message + "\n")
        if progress:
            progress("distill", 0.0, retry_message)


def generate_distill_entries(
    paths: ProjectPaths,
    distill: DistillOptions,
    entries: list[tuple[Path, str]],
    progress: Optional[ProgressCallback] = None,
) -> int:
    if not entries:
        return 0
    validation = validate_gsv_root(distill.gsv_root)
    if not validation["ok"]:
        raise RuntimeError(str(validation["message"]))
    catalog = scan_gsv_models(distill.gsv_root)
    _ensure_selection(catalog, distill)

    distill_dir = paths.work_dir / "distill_corpus"
    distill_dir.mkdir(parents=True, exist_ok=True)
    request_path = distill_dir / "resume_request.json"
    log_path = paths.work_dir / "distill_resume.log"
    texts = [text for _path, text in entries]
    output_paths = [str(audio_path) for audio_path, _text in entries]
    payload = _helper_request_payload(
        validation,
        distill,
        texts,
        paths,
        device="cuda" if str(distill.device).lower() in {"cuda", "gpu"} else "cpu",
        batch_size=max(1, int(distill.batch_size)),
        parallel_infer=bool(distill.parallel_infer),
    )
    payload["output_paths"] = output_paths
    payload["metadata_path"] = str(distill_dir / "resume_metadata.csv")
    request_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    if progress:
        progress("distill", 0.0, f"旧项目缺失 {len(entries)} 条 GPT-SoVITS 音频，开始补生成...")
    result = _run_helper(validation, request_path, log_path, progress)
    if result["code"] != 0:
        raise RuntimeError(str(result.get("message") or f"GPT-SoVITS 补生成失败，详见 {log_path}"))
    if progress:
        progress("distill", 1.0, f"GPT-SoVITS 缺失音频补生成完成，共 {result['generated']} 条")
    return int(result.get("generated") or 0)


def synthesize_gsv_preview(
    preview_root: Path,
    distill: DistillOptions,
    text: str,
    progress: Optional[ProgressCallback] = None,
) -> Path:
    text = text.strip()
    if not text:
        raise RuntimeError("缺少试听文本")

    validation = validate_gsv_root(distill.gsv_root)
    if not validation["ok"]:
        raise RuntimeError(str(validation["message"]))
    catalog = scan_gsv_models(distill.gsv_root)
    _ensure_selection(catalog, distill)

    paths = ProjectPaths(project_root=preview_root)
    preview_dir = paths.work_dir / "gsv_preview"
    wav_dir = preview_dir / "wavs"
    preview_dir.mkdir(parents=True, exist_ok=True)
    request_path = preview_dir / "request.json"
    log_path = preview_dir / "preview.log"
    if log_path.exists():
        log_path.unlink()

    payload = _helper_request_payload(
        validation,
        distill,
        [text],
        paths,
        device="cuda" if str(distill.device).lower() in {"cuda", "gpu"} else "cpu",
        batch_size=max(1, int(distill.batch_size)),
        parallel_infer=bool(distill.parallel_infer),
    )
    payload["wav_dir"] = str(wav_dir)
    payload["metadata_path"] = str(preview_dir / "metadata.csv")
    request_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    if progress:
        progress("preview", 0.0, "启动 GPT-SoVITS 试听合成...")
    result = _run_helper(validation, request_path, log_path, progress, progress_stage="preview")
    if result["code"] != 0:
        raise RuntimeError(str(result.get("message") or f"GPT-SoVITS 试听失败，详见 {log_path}"))
    audio_path = wav_dir / "00001.wav"
    if not audio_path.exists():
        raise RuntimeError(f"GPT-SoVITS 试听未生成音频: {audio_path}")
    if progress:
        progress("preview", 1.0, "GPT-SoVITS 试听生成完成")
    return audio_path
