import contextlib
import json
import os
import queue
import re
import runpy
import shutil
import subprocess
import threading
import time
import urllib.parse
import sys
import traceback
from pathlib import Path
from typing import Iterable, List, Optional

from .config import SegmentMetadata, TrainingOptions, ProgressCallback
from .resource_paths import resolve_resources_root
from .runtime_manager import resolve_cuda_python, resolve_piper_runtime_python
from .utils import find_executable


def _base_dir() -> Path:
    env_base = os.environ.get("KGTTS_BASE_DIR")
    if env_base:
        return Path(env_base)
    env_resources = os.environ.get("KGTTS_RESOURCES")
    if env_resources:
        return Path(env_resources)
    if getattr(sys, "frozen", False):
        return Path(sys.executable).resolve().parent
    return Path(__file__).resolve().parents[1]


def _resources_dir() -> Path:
    resolved = resolve_resources_root()
    if resolved is not None:
        return resolved
    base = _base_dir()
    candidate = base / "resources_pack"
    if candidate.exists():
        return candidate
    return base


def _find_piper_python(*, prefer_cuda: bool = False) -> Optional[Path]:
    if prefer_cuda:
        cuda_python = resolve_cuda_python()
        if cuda_python and cuda_python.exists():
            return cuda_python
    runtime_python = resolve_piper_runtime_python()
    if runtime_python and runtime_python.exists():
        return runtime_python
    base = _base_dir()
    candidates = [
        base / "piper_env" / "python.exe",
        base / "piper_runtime" / "python.exe",
        base / "python" / "python.exe",
    ]
    for path in candidates:
        if path.exists():
            return path
    return None


def _piper_env(piper_python: Path) -> dict:
    env = os.environ.copy()
    piper_root = piper_python.parent
    env["PYTHONHOME"] = str(piper_root)
    env["PYTHONPATH"] = ""
    path_entries = [
        str(piper_root),
        str(piper_root / "Scripts"),
        str(piper_root / "Library" / "bin"),
        str(piper_root / "Library" / "usr" / "bin"),
        str(piper_root / "Library" / "mingw-w64" / "bin"),
        env.get("PATH", ""),
    ]
    env["PATH"] = os.pathsep.join(entry for entry in path_entries if entry)
    return env


def _same_python_runtime(left: Path, right: Path) -> bool:
    try:
        return left.resolve() == right.resolve()
    except Exception:
        return str(left).lower() == str(right).lower()


def _tail_log(path: Path, *, max_lines: int = 24, max_chars: int = 4000) -> str:
    try:
        text = path.read_text(encoding="utf-8", errors="replace")
    except Exception:
        return ""
    lines = [line.rstrip() for line in text.splitlines() if line.strip()]
    if not lines:
        return ""
    tail = "\n".join(lines[-max_lines:])
    if len(tail) > max_chars:
        tail = tail[-max_chars:]
    return tail


def _cuda_available(piper_python: Path) -> Optional[bool]:
    env = _piper_env(piper_python)
    env.setdefault("PYTORCH_NVML_BASED_CUDA_CHECK", "1")
    cmd = [
        str(piper_python),
        "-c",
        "import torch; print('1' if torch.cuda.is_available() else '0')",
    ]
    timeout_sec = int(os.environ.get("KGTTS_CUDA_CHECK_TIMEOUT_SEC", "15"))
    try:
        proc = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            env=env,
            cwd=str(piper_python.parent),
            timeout=timeout_sec,
        )
    except subprocess.TimeoutExpired:
        return None
    except Exception:
        return None
    if proc.returncode != 0:
        return None
    output = proc.stdout.strip()
    if "1" in output:
        return True
    if "0" in output:
        return False
    return None


def _default_phonemizer_dict() -> Path:
    base = _resources_dir()
    direct = base / "data" / "phonemizer_zh.dict"
    if direct.exists():
        return direct
    if getattr(sys, "frozen", False):
        meipass = getattr(sys, "_MEIPASS", "")
        if meipass:
            alt = Path(meipass) / "data" / "phonemizer_zh.dict"
            if alt.exists():
                return alt
        internal = base / "_internal" / "data" / "phonemizer_zh.dict"
        if internal.exists():
            return internal
    return direct


def _latest_checkpoint(root: Path) -> Optional[Path]:
    if not root.exists():
        return None
    checkpoints = list(root.rglob("*.ckpt"))
    if not checkpoints:
        return None
    return max(checkpoints, key=lambda p: p.stat().st_mtime)


def _config_num_symbols(config_path: Path) -> Optional[int]:
    if not config_path.exists():
        return None
    try:
        config = json.loads(config_path.read_text(encoding="utf-8"))
    except Exception:
        return None
    num_symbols = config.get("num_symbols")
    if isinstance(num_symbols, int) and num_symbols > 0:
        return num_symbols
    phoneme_id_map = config.get("phoneme_id_map") or {}
    if isinstance(phoneme_id_map, dict) and phoneme_id_map:
        try:
            values: list[int] = []
            for raw in phoneme_id_map.values():
                if isinstance(raw, list):
                    values.extend(int(v) for v in raw)
                else:
                    values.append(int(raw))
            if values:
                return max(values) + 1
            return None
        except Exception:
            return None
    return None


def _find_ckpt_files(root: Path) -> List[Path]:
    if not root.exists():
        return []
    out: list[Path] = []
    for pattern in ("*.ckpt", "*.pt", "*.pth"):
        out.extend(root.rglob(pattern))
    return out


def _find_default_base_ckpt(piper_config: Optional[Path]) -> Optional[Path]:
    candidates: list[Path] = []
    if piper_config and piper_config.exists():
        candidates.extend(_find_ckpt_files(piper_config.parent))

    resources = _resources_dir()
    roots = [
        resources / "Model" / "piper_checkpoints",
        resources / "CKPT",
        resources.parent / "CKPT",
    ]
    for root in roots:
        candidates.extend(_find_ckpt_files(root))

    if not candidates:
        return None
    return max(candidates, key=lambda p: p.stat().st_mtime)


def _ckpt_num_symbols(piper_python: Path, ckpt_path: Path) -> Optional[int]:
    env = _piper_env(piper_python)
    code = (
        "import pathlib; "
        "pathlib.PosixPath = pathlib.WindowsPath; "
        "import torch; "
        f"ckpt=torch.load(r'''{ckpt_path}''', map_location='cpu'); "
        "hp=ckpt.get('hyper_parameters', {}); "
        "print(hp.get('num_symbols',''))"
    )
    try:
        proc = subprocess.run(
            [str(piper_python), "-c", code],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            env=env,
            cwd=str(piper_python.parent),
            timeout=int(os.environ.get("KGTTS_CKPT_INSPECT_TIMEOUT_SEC", "20")),
        )
    except subprocess.TimeoutExpired:
        return None
    except Exception:
        return None
    if proc.returncode != 0:
        return None
    out = proc.stdout.strip()
    try:
        return int(out)
    except Exception:
        return None


def _ckpt_dataset_dir(piper_python: Path, ckpt_path: Path) -> Optional[str]:
    env = _piper_env(piper_python)
    code = (
        "import pathlib; "
        "pathlib.PosixPath = pathlib.WindowsPath; "
        "import torch; "
        f"ckpt=torch.load(r'''{ckpt_path}''', map_location='cpu'); "
        "hp=ckpt.get('hyper_parameters', {}); "
        "print(hp.get('dataset_dir',''))"
    )
    try:
        proc = subprocess.run(
            [str(piper_python), "-c", code],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            env=env,
            cwd=str(piper_python.parent),
            timeout=int(os.environ.get("KGTTS_CKPT_INSPECT_TIMEOUT_SEC", "20")),
        )
    except subprocess.TimeoutExpired:
        return None
    except Exception:
        return None
    if proc.returncode != 0:
        return None
    out = proc.stdout.strip()
    return out or None


def _find_piper_config(base_ckpt: Path, piper_python: Path) -> Optional[Path]:
    base = _resources_dir()
    cfg_root = base / "Model" / "piper_checkpoints"
    if not cfg_root.exists():
        return None
    candidates = sorted(cfg_root.rglob("config.json"))
    if not candidates:
        return None
    dataset_dir = _ckpt_dataset_dir(piper_python, base_ckpt)
    if dataset_dir:
        parts = [p for p in dataset_dir.replace("\\", "/").split("/") if p]
        tail = parts[-3:] if len(parts) >= 3 else parts
        if tail:
            for cand in candidates:
                cand_str = cand.as_posix().lower()
                if all(
                    token.lower().replace("-", "_") in cand_str
                    or token.lower() in cand_str
                    for token in tail
                ):
                    return cand
    return candidates[0]


def _ckpt_epoch(piper_python: Path, ckpt_path: Path) -> Optional[int]:
    # Fast path: parse epoch from filename to avoid loading large checkpoints.
    try:
        stem = urllib.parse.unquote(ckpt_path.stem)
        m = re.search(r"epoch[=_-]?(\d+)", stem, flags=re.IGNORECASE)
        if m:
            return int(m.group(1))
    except Exception:
        pass

    env = _piper_env(piper_python)
    code = (
        "import pathlib; "
        "pathlib.PosixPath = pathlib.WindowsPath; "
        "import torch; "
        f"ckpt=torch.load(r'''{ckpt_path}''', map_location='cpu'); "
        "print(ckpt.get('epoch',''))"
    )
    try:
        proc = subprocess.run(
            [str(piper_python), "-c", code],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            env=env,
            cwd=str(piper_python.parent),
            timeout=int(os.environ.get("KGTTS_CKPT_INSPECT_TIMEOUT_SEC", "20")),
        )
    except subprocess.TimeoutExpired:
        return None
    except Exception:
        return None
    if proc.returncode != 0:
        return None
    out = proc.stdout.strip()
    try:
        return int(out)
    except Exception:
        return None


def _portable_ckpt_for_windows(
    piper_python: Path,
    ckpt_path: Path,
    work_dir: Path,
) -> Path:
    """Convert POSIX-pickled checkpoint to a Windows-loadable copy when needed."""
    if os.name != "nt":
        return ckpt_path
    if not ckpt_path.exists():
        return ckpt_path
    cache_dir = work_dir / ".kgtts_ckpt_cache"
    cache_dir.mkdir(parents=True, exist_ok=True)
    target = cache_dir / f"{ckpt_path.stem}.win.ckpt"
    try:
        if target.exists() and target.stat().st_mtime >= ckpt_path.stat().st_mtime:
            return target
    except Exception:
        pass

    env = _piper_env(piper_python)
    script = (
        "import pathlib, torch, sys; "
        "src=sys.argv[1]; dst=sys.argv[2]; "
        "pathlib.PosixPath = pathlib.WindowsPath; "
        "ckpt=torch.load(src, map_location='cpu'); "
        "torch.save(ckpt, dst); "
        "print(dst)"
    )
    try:
        proc = subprocess.run(
            [str(piper_python), "-c", script, str(ckpt_path), str(target)],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            env=env,
            cwd=str(piper_python.parent),
            timeout=int(os.environ.get("KGTTS_CKPT_CONVERT_TIMEOUT_SEC", "300")),
        )
        if proc.returncode == 0 and target.exists():
            return target
    except Exception:
        pass
    return ckpt_path


def _prep_num_speakers(prep_dir: Path) -> Optional[int]:
    config_path = prep_dir / "config.json"
    if not config_path.exists():
        return None
    try:
        config = json.loads(config_path.read_text(encoding="utf-8"))
    except Exception:
        return None
    num_speakers = config.get("num_speakers")
    try:
        num_speakers = int(num_speakers)
    except Exception:
        return None
    return num_speakers if num_speakers > 0 else None


def write_metadata(manifest: Iterable[SegmentMetadata], csv_path: Path) -> None:
    csv_path.parent.mkdir(parents=True, exist_ok=True)
    with csv_path.open("w", encoding="utf-8") as f:
        for seg in manifest:
            line = f"{seg.audio_path.as_posix()}|{seg.text}\n"
            f.write(line)


def write_preview_text(sample_text: str, out_path: Path) -> None:
    out_path.write_text(sample_text, encoding="utf-8")


def run_piper_training(
    metadata_csv: Path,
    work_dir: Path,
    opts: TrainingOptions,
    progress: Optional[ProgressCallback] = None,
) -> Path:
    work_dir.mkdir(parents=True, exist_ok=True)
    log_path = work_dir / "training.log"
    prefer_cuda = opts.device.lower() in {"cuda", "gpu"}
    if progress:
        progress("train", 0.0, "准备 Piper 训练运行时...")
    try:
        with log_path.open("w", encoding="utf-8") as log_file:
            log_file.write("== Piper Runtime ==\n")
            log_file.write(f"requested_device={opts.device}\n")
            log_file.flush()
    except Exception:
        pass
    piper_python = _find_piper_python(prefer_cuda=prefer_cuda)
    if piper_python:
        if progress:
            progress("train", 0.0, f"已选择 Piper 运行时: {piper_python}")
        try:
            with log_path.open("a", encoding="utf-8") as log_file:
                log_file.write(f"piper_python={piper_python}\n")
                log_file.flush()
        except Exception:
            pass
        prep_script = _resources_dir() / "tools" / "piper_prep.py"
        if not prep_script.exists():
            raise RuntimeError("训练组件不完整，请重新安装 Piper 基础运行时后再试。")
        use_espeak = bool(opts.use_espeak)
        piper_config = opts.piper_config
        if use_espeak and piper_config is None and opts.piper_base_checkpoint:
            piper_config = _find_piper_config(opts.piper_base_checkpoint, piper_python)
        if use_espeak and piper_config is None:
            raise RuntimeError("当前基线缺少发音配置，请重新选择基线或安装训练资源包后再试。")
        phonemizer = None
        if not use_espeak:
            phonemizer = opts.phonemizer_dict or _default_phonemizer_dict()
            if not phonemizer.exists():
                raise RuntimeError("缺少发音字典，请安装训练资源包后再试。")

        prep_dir = work_dir / "piper_preprocess"
        prep_cmd = [
            str(piper_python),
            str(prep_script),
            "--metadata",
            str(metadata_csv),
            "--out-dir",
            str(prep_dir),
            "--sample-rate",
            str(opts.sample_rate),
            "--language",
            opts.language,
            "--quality",
            opts.quality.lower(),
            "--speaker-id",
            str(opts.speaker_id),
        ]
        if use_espeak:
            prep_cmd += [
                "--phoneme-type",
                "espeak",
                "--piper-config",
                str(piper_config),
            ]
        else:
            prep_cmd += ["--phoneme-type", "text", "--dict", str(phonemizer)]

        base_ckpt = opts.piper_base_checkpoint
        if base_ckpt and not base_ckpt.exists():
            base_ckpt = None
        if base_ckpt is None and use_espeak:
            auto_ckpt = _find_default_base_ckpt(piper_config)
            if auto_ckpt:
                base_ckpt = auto_ckpt
                if progress:
                    progress("train", 0.0, f"自动使用 Piper 基线: {auto_ckpt.name}")
                try:
                    with log_path.open("a", encoding="utf-8") as log_file:
                        log_file.write(f"[auto] baseline_ckpt={auto_ckpt}\n")
                        log_file.flush()
                except Exception:
                    pass
        env = _piper_env(piper_python)
        if base_ckpt and progress:
            progress("train", 0.0, f"启用基线兼容加载: {base_ckpt.name}")
        if base_ckpt:
            try:
                with log_path.open("a", encoding="utf-8") as log_file:
                    log_file.write("[auto] baseline_posixpath_patch=1\n")
                    log_file.flush()
            except Exception:
                pass
        def run_blocking(
            cmd: list[str],
            stage: str,
            base_progress: float,
            label: str,
            creationflags: int = 0,
        ) -> int:
            if progress:
                progress(stage, base_progress, label)
            env_block = env.copy()
            env_block["PYTHONUNBUFFERED"] = "1"
            env_block["PYTHONIOENCODING"] = "utf-8"
            env_block["PYTHONUTF8"] = "1"
            with log_path.open("a", encoding="utf-8") as log_file:
                proc = subprocess.Popen(
                    cmd,
                    stdout=log_file,
                    stderr=subprocess.STDOUT,
                    stdin=subprocess.DEVNULL,
                    text=True,
                    env=env_block,
                    cwd=str(piper_python.parent),
                    creationflags=creationflags,
                    close_fds=True,
                )
                log_file.write(f"[spawn] pid={proc.pid}\n")
                log_file.flush()
                last_keepalive = 0.0
                while True:
                    code = proc.poll()
                    if code is not None:
                        if code != 0:
                            log_file.write(f"\n[exit] code={code}\n")
                            log_file.flush()
                        return code
                    now = time.monotonic()
                    if progress and (now - last_keepalive >= 2.0):
                        progress(stage, base_progress, label)
                        last_keepalive = now
                    time.sleep(0.2)

        def _new_console_flags() -> int:
            if os.name != "nt":
                return 0
            flags = 0
            flags |= getattr(subprocess, "CREATE_NEW_CONSOLE", 0)
            flags |= getattr(subprocess, "CREATE_NEW_PROCESS_GROUP", 0)
            # Let long-running training process break away from parent job when possible.
            flags |= getattr(subprocess, "CREATE_BREAKAWAY_FROM_JOB", 0)
            return flags

        def run_inprocess_prep(
            script_path: Path,
            args: list[str],
            stage: str,
            base_progress: float,
            label: str,
        ) -> int:
            if progress:
                progress(stage, base_progress, label)
            argv_backup = sys.argv[:]
            sys.argv = [str(script_path)] + args
            with log_path.open("a", encoding="utf-8") as log_file:
                log_file.write("[prep] mode=inprocess\n")
                log_file.flush()
                try:
                    with contextlib.redirect_stdout(log_file), contextlib.redirect_stderr(log_file):
                        runpy.run_path(str(script_path), run_name="__main__")
                    return 0
                except SystemExit as exc:
                    code = exc.code if isinstance(exc.code, int) else 1
                    if code:
                        log_file.write(f"\n[exit] code={code}\n")
                        log_file.flush()
                    return int(code or 0)
                except Exception:
                    log_file.write("\n[prep] exception:\n")
                    traceback.print_exc(file=log_file)
                    log_file.flush()
                    return 1
                finally:
                    sys.argv = argv_backup

        def run_with_stream(cmd: list[str], stage: str, base_progress: float, label: str) -> int:
            if progress:
                progress(stage, base_progress, label)
            env_stream = env.copy()
            env_stream["PYTHONUNBUFFERED"] = "1"
            env_stream["PYTHONIOENCODING"] = "utf-8"
            env_stream["PYTHONUTF8"] = "1"

            def format_stream_message_for_ui(raw_line: str) -> Optional[str]:
                msg = raw_line.strip()
                if not msg:
                    return None

                if msg.startswith("Traceback (most recent call last):"):
                    return None
                if re.match(r'^\s*File ".*", line \d+, in .+$', msg):
                    return None

                prep_entries_match = re.match(r"^\[prep\]\s+entries=(\d+)", msg)
                if prep_entries_match:
                    return f"Piper 预处理开始，样本数 {prep_entries_match.group(1)}"

                prep_progress_match = re.match(r"^\[prep\]\s+processed\s+(\d+)/(\d+)", msg)
                if prep_progress_match:
                    return f"Piper 预处理进度: {prep_progress_match.group(1)}/{prep_progress_match.group(2)}"

                prep_missing_match = re.match(r"^\[prep\]\s+(missing_audio|failed_audio)=(\d+)", msg)
                if prep_missing_match:
                    label_text = "缺失音频" if prep_missing_match.group(1) == "missing_audio" else "处理失败音频"
                    return f"Piper 预处理提示: {label_text} {prep_missing_match.group(2)} 条"

                if msg.startswith("DEBUG:piper_train:Namespace("):
                    accelerator_match = re.search(r"accelerator='([^']+)'", msg)
                    batch_match = re.search(r"batch_size=(\d+)", msg)
                    accelerator_name = accelerator_match.group(1) if accelerator_match else "unknown"
                    batch_value = batch_match.group(1) if batch_match else "?"
                    return f"Piper 训练参数已加载（device={accelerator_name}, batch={batch_value}）"

                if msg.startswith("DEBUG:piper_train:Checkpoints will be saved every "):
                    epoch_match = re.search(r"every (\d+) epoch", msg)
                    every_epoch = epoch_match.group(1) if epoch_match else "?"
                    return f"检查点保存间隔: 每 {every_epoch} 个 epoch"

                if msg.startswith("DEBUG:vits.dataset:Loading dataset:"):
                    dataset_path = msg.split("DEBUG:vits.dataset:Loading dataset:", 1)[1].strip()
                    return f"加载数据集: {Path(dataset_path).name}"

                if msg.startswith("DEBUG:fsspec.local:open file:"):
                    return None

                if msg.startswith("GPU available:"):
                    gpu_match = re.search(r"GPU available:\s*(True|False), used:\s*(True|False)", msg)
                    if gpu_match:
                        available = "可用" if gpu_match.group(1) == "True" else "不可用"
                        used = "使用中" if gpu_match.group(2) == "True" else "未使用"
                        return f"GPU 状态: {available}，当前{used}"
                    return "GPU 状态已更新"

                if msg.startswith(("TPU available:", "IPU available:", "HPU available:")):
                    return None

                if msg.startswith("Missing logger folder:"):
                    return "初始化训练日志目录"

                if msg.startswith("Restoring states from the checkpoint path at "):
                    ckpt_path = msg.split(" at ", 1)[1].strip()
                    return f"正在恢复基线检查点: {Path(ckpt_path).name}"

                if msg.startswith("Restored all states from the checkpoint file at "):
                    ckpt_path = msg.split(" at ", 1)[1].strip()
                    return f"已恢复基线检查点: {Path(ckpt_path).name}"

                warning_match = re.match(
                    r"^[A-Za-z]:\\.*?\.py:\d+:\s*([A-Za-z]+Warning|LightningDeprecationWarning|PossibleUserWarning):\s*(.+)$",
                    msg,
                )
                if warning_match:
                    warning_type = warning_match.group(1)
                    warning_body = warning_match.group(2).strip()
                    warning_body_lower = warning_body.lower()
                    if "does not have many workers" in warning_body_lower:
                        return "提示: train_dataloader 的 num_workers 较少，可能影响训练速度。"
                    if "total length of `dataloader` across ranks is zero" in warning_body_lower:
                        return "提示: 某个 DataLoader 长度为 0，请确认这是你的预期。"
                    if "callbacks used to create the checkpoint need to be provided" in warning_body_lower:
                        return "提示: 恢复检查点时未附带原始回调配置，后续检查点保存策略可能与基线不同。"
                    if "resume_from_checkpoint" in warning_body_lower:
                        return "兼容性提示: 当前 Piper 训练仍在使用旧版 resume_from_checkpoint 接口。"
                    prefix = "兼容性提示" if "Deprecation" in warning_type else "提示"
                    return f"{prefix}: {warning_body}"

                return msg

            with log_path.open("a", encoding="utf-8") as log_file:
                proc = subprocess.Popen(
                    cmd,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    stdin=subprocess.DEVNULL,
                    text=True,
                    encoding="utf-8",
                    errors="replace",
                    env=env_stream,
                    cwd=str(piper_python.parent),
                    bufsize=1,
                    close_fds=True,
                )

                line_queue: queue.Queue[Optional[str]] = queue.Queue()

                def reader() -> None:
                    try:
                        if proc.stdout is None:
                            return
                        for line in proc.stdout:
                            line_queue.put(line)
                    finally:
                        line_queue.put(None)

                threading.Thread(target=reader, daemon=True).start()

                last_emit = 0.0
                last_ui_msg: Optional[str] = None
                while True:
                    try:
                        line = line_queue.get(timeout=0.5)
                    except queue.Empty:
                        if proc.poll() is not None:
                            break
                        now = time.monotonic()
                        if progress and now - last_emit > 5.0:
                            progress(stage, base_progress, label)
                            last_emit = now
                        continue

                    if line is None:
                        break
                    log_file.write(line)
                    log_file.flush()
                    now = time.monotonic()
                    if progress and now - last_emit > 1.0:
                        ui_msg = format_stream_message_for_ui(line)
                        if ui_msg and ui_msg != last_ui_msg:
                            progress(stage, base_progress, ui_msg[:120])
                            last_ui_msg = ui_msg
                            last_emit = now

                try:
                    proc.wait(timeout=1)
                except Exception:
                    pass
                code = proc.returncode or 0
                if code != 0:
                    log_file.write(f"\n[exit] code={code}\n")
                    log_file.flush()
                return code

        def _read_log_lower() -> str:
            if not log_path.exists():
                return ""
            try:
                return log_path.read_text(encoding="utf-8", errors="ignore").lower()
            except Exception:
                return ""

        def _log_contains_any(patterns: tuple[str, ...]) -> bool:
            content = _read_log_lower()
            if not content:
                return False
            return any(pattern in content for pattern in patterns)

        def _log_contains_oom() -> bool:
            return _log_contains_any(
                (
                    "out of memory",
                    "cuda out of memory",
                    "cublas_status_alloc_failed",
                    "cuda error: out of memory",
                )
            )

        def _log_contains_gpu_backend_unavailable() -> bool:
            return _log_contains_any(
                (
                    "no supported gpu backend found",
                    "misconfigurationexception: no supported gpu backend found",
                    "trainer was configured to use `gpu` accelerator, but no gpu",
                    "gpuaccelerator can not run",
                    "cuda is not available",
                )
            )

        def _log_contains_baseline_resume_failure() -> bool:
            return _log_contains_any(
                (
                    "error(s) in loading state_dict",
                    "missing key(s) in state_dict",
                    "unexpected key(s) in state_dict",
                    "size mismatch for",
                    "invalid load key",
                    "pytorchstreamreader failed",
                    "failed finding central directory",
                    "storage has wrong size",
                    "cannot instantiate 'windowspath' on your system",
                    "cannot instantiate 'posixpath' on your system",
                )
            )

        accelerator = "gpu" if opts.device.lower() in {"cuda", "gpu"} else "cpu"
        if progress:
            progress("train", 0.0, "检查 Piper 训练环境...")
        if accelerator == "gpu":
            cuda_probe = _cuda_available(piper_python)
            if cuda_probe is False:
                warn = "未检测到 CUDA，自动切换为 CPU 训练。"
                if progress:
                    progress("train", 0.0, warn)
                try:
                    with log_path.open("a", encoding="utf-8") as log_file:
                        log_file.write("[warn] " + warn + "\n")
                        log_file.flush()
                except Exception:
                    pass
                accelerator = "cpu"
            elif cuda_probe is None:
                warn = "CUDA 检测超时或失败，继续尝试 GPU 训练。"
                if progress:
                    progress("train", 0.0, warn)
                try:
                    with log_path.open("a", encoding="utf-8") as log_file:
                        log_file.write("[warn] " + warn + "\n")
                        log_file.flush()
                except Exception:
                    pass

        if progress:
            progress("train", 0.0, "启动 Piper 预处理")
        with log_path.open("w", encoding="utf-8") as log_file:
            log_file.write("== Piper Runtime ==\n")
            log_file.write(f"piper_python={piper_python}\n")
            log_file.write(f"requested_device={opts.device}\n")
            log_file.write("== Piper Preprocess ==\n")
            log_file.write("CMD: " + " ".join(prep_cmd) + "\n")
            log_file.flush()
        # In-process prep is only safe when the backend itself is running inside
        # the Piper runtime. The packaged app now uses a minimal bootstrap
        # Python, so KGTTS_RESOURCES alone must not force runpy here.
        prep_inproc_flag = str(os.environ.get("KGTTS_PREP_INPROC") or "").strip().lower()
        use_inproc = prep_inproc_flag in {"1", "true", "yes"} or _same_python_runtime(Path(sys.executable), piper_python)
        if use_inproc:
            prep_args = prep_cmd[2:]  # drop python + script
            code = run_inprocess_prep(prep_script, prep_args, "train", 0.05, "Piper 预处理中...")
        else:
            code = run_with_stream(prep_cmd, "train", 0.05, "Piper 预处理中...")
        if code != 0:
            tail = _tail_log(log_path)
            if progress and tail:
                progress("train", 0.05, f"Piper 预处理失败：{tail.splitlines()[-1]}")
            detail = f"\n\n最近日志：\n{tail}" if tail else ""
            raise RuntimeError(f"Piper 预处理失败，详见 {log_path}{detail}")
        prep_dataset = prep_dir / "dataset.jsonl"
        if not prep_dataset.exists():
            raise RuntimeError(f"Piper 预处理未生成 dataset.jsonl：{prep_dataset}")
        try:
            with prep_dataset.open("r", encoding="utf-8") as f:
                num_lines = sum(1 for _ in f)
        except Exception:
            num_lines = 0
        if progress:
            progress("train", 0.1, f"Piper 预处理完成，样本数 {num_lines}")
        if base_ckpt and progress:
            progress("train", 0.13, f"读取基线信息: {base_ckpt.name}")
        if (base_ckpt is None) and (num_lines > 0) and (num_lines < 80):
            warn = (
                f"样本数仅 {num_lines} 且未使用基线 ckpt，"
                "导出音色可能出现嘟声/杂音，建议选择基线或增加样本量。"
            )
            if progress:
                progress("train", 0.12, warn)
            try:
                with log_path.open("a", encoding="utf-8") as log_file:
                    log_file.write("[warn] " + warn + "\n")
                    log_file.flush()
            except Exception:
                pass

        if base_ckpt:
            cfg_symbols = _config_num_symbols(prep_dir / "config.json")
            ckpt_symbols = _ckpt_num_symbols(piper_python, base_ckpt)
            if cfg_symbols and ckpt_symbols and cfg_symbols != ckpt_symbols:
                msg = (
                    "基线 ckpt 与当前字典不兼容（num_symbols"
                    f" ckpt={ckpt_symbols} != data={cfg_symbols}），"
                    "将忽略基线并从头训练。"
                )
                if progress:
                    progress("train", 0.0, msg)
                try:
                    with log_path.open("a", encoding="utf-8") as log_file:
                        log_file.write(msg + "\n")
                        log_file.flush()
                except Exception:
                    pass
                base_ckpt = None
            elif (not use_espeak) and (ckpt_symbols is None):
                # Some baseline checkpoints created on POSIX cannot be safely
                # inspected/resumed on Windows (e.g. PosixPath in pickle state).
                msg = (
                    "无法读取基线 ckpt 的符号表信息，当前 text 字典模式下将忽略基线，"
                    "避免恢复阶段报错。"
                )
                if progress:
                    progress("train", 0.0, msg)
                try:
                    with log_path.open("a", encoding="utf-8") as log_file:
                        log_file.write(msg + "\n")
                        log_file.flush()
                except Exception:
                    pass
                base_ckpt = None

        num_speakers = _prep_num_speakers(prep_dir) or 1
        effective_batch = max(1, min(int(opts.batch_size), max(1, num_lines)))
        if effective_batch != int(opts.batch_size):
            msg = f"自动调整 batch_size: {opts.batch_size} -> {effective_batch}"
            if progress:
                progress("train", 0.16, msg)
            try:
                with log_path.open("a", encoding="utf-8") as log_file:
                    log_file.write("[auto] " + msg + "\n")
                    log_file.flush()
            except Exception:
                pass
        extra_epochs = max(int(opts.epochs), 1)
        train_max_epochs = extra_epochs
        resume_ckpt = None
        resume_single_ckpt = None
        if base_ckpt:
            if num_speakers > 1:
                resume_single_ckpt = base_ckpt
            else:
                resume_ckpt = base_ckpt
                base_epoch = _ckpt_epoch(piper_python, base_ckpt)
                if base_epoch is not None:
                    # Lightning treats max_epochs as an absolute stop point.
                    # Resume from epoch=N and train K additional epochs => N+K+1.
                    train_max_epochs = base_epoch + extra_epochs + 1

        def _wrap_piper_train_args(train_args: list[str]) -> list[str]:
            # Load Windows-incompatible baseline checkpoints (pickled PosixPath)
            # by patching pathlib before importing piper_train internals.
            if os.name == "nt":
                bootstrap = (
                    "import pathlib,runpy,sys; "
                    "pathlib.PosixPath = pathlib.WindowsPath; "
                    "sys.argv=['piper_train'] + sys.argv[1:]; "
                    "runpy.run_module('piper_train', run_name='__main__')"
                )
                return [str(piper_python), "-c", bootstrap, *train_args]
            return [str(piper_python), "-m", "piper_train", *train_args]

        def build_train_cmd(
            max_epochs: int,
            batch_size: int,
            accelerator_name: str,
            resume_single: Optional[Path],
            resume_full: Optional[Path],
        ) -> list[str]:
            args = [
                "--dataset-dir",
                str(prep_dir),
                "--batch-size",
                str(batch_size),
                "--max_epochs",
                str(max_epochs),
                "--accelerator",
                accelerator_name,
                "--devices",
                "1",
                "--precision",
                "32",
                "--default_root_dir",
                str(work_dir),
                "--checkpoint-epochs",
                "1",
                "--validation-split",
                "0.0",
                "--num-test-examples",
                "0",
                "--num_sanity_val_steps",
                "0",
                "--log_every_n_steps",
                "1",
            ]
            if accelerator_name == "cpu":
                # Keep CPU training single-process on Windows; avoids lingering
                # multiprocessing child that can block parent process exit.
                args += ["--strategy", "single_device"]
            if resume_single:
                args += [
                    "--resume_from_single_speaker_checkpoint",
                    str(resume_single),
                ]
            elif resume_full:
                args += ["--resume_from_checkpoint", str(resume_full)]
            return _wrap_piper_train_args(args)

        train_batch = effective_batch
        train_accelerator = accelerator
        current_resume_single = resume_single_ckpt
        current_resume_ckpt = resume_ckpt
        current_max_epochs = train_max_epochs
        baseline_retry_used = False

        while True:
            train_cmd = build_train_cmd(
                current_max_epochs,
                train_batch,
                train_accelerator,
                current_resume_single,
                current_resume_ckpt,
            )
            if progress:
                progress(
                    "train",
                    0.2,
                    f"启动 Piper 训练（device={train_accelerator}, batch={train_batch}）",
                )
            try:
                with log_path.open("a", encoding="utf-8") as log_file:
                    log_file.write("\n== Piper Train ==\n")
                    log_file.write("CMD: " + " ".join(train_cmd) + "\n")
                    log_file.flush()
            except Exception:
                pass

            train_code = run_with_stream(
                train_cmd,
                "train",
                0.2,
                "Piper 训练中...",
            )
            if train_code == 0:
                break

            if train_accelerator == "gpu" and _log_contains_gpu_backend_unavailable():
                train_accelerator = "cpu"
                retry_msg = "未检测到可用的 Piper GPU 后端，自动切换到 CPU 训练。"
                if progress:
                    progress("train", 0.2, retry_msg)
                try:
                    with log_path.open("a", encoding="utf-8") as log_file:
                        log_file.write(retry_msg + "\n")
                        log_file.flush()
                except Exception:
                    pass
                continue

            if train_accelerator == "gpu" and _log_contains_oom():
                if train_batch > 1:
                    train_batch = max(1, train_batch // 2)
                    retry_msg = f"检测到显存不足，自动降低训练 batch_size 到 {train_batch} 后重试。"
                else:
                    train_accelerator = "cpu"
                    retry_msg = "检测到显存不足，自动切换到 CPU 训练。"
                if progress:
                    progress("train", 0.2, retry_msg)
                try:
                    with log_path.open("a", encoding="utf-8") as log_file:
                        log_file.write(retry_msg + "\n")
                    log_file.flush()
                except Exception:
                    pass
                continue

            if (
                (current_resume_single or current_resume_ckpt)
                and not baseline_retry_used
                and _log_contains_baseline_resume_failure()
            ):
                baseline_retry_used = True
                current_resume_single = None
                current_resume_ckpt = None
                current_max_epochs = extra_epochs
                retry_msg = "检测到基线恢复失败，自动改为不使用基线重试训练。"
                if progress:
                    progress("train", 0.2, retry_msg)
                try:
                    with log_path.open("a", encoding="utf-8") as log_file:
                        log_file.write(retry_msg + "\n")
                        log_file.flush()
                except Exception:
                    pass
                continue

            raise RuntimeError(f"Piper 训练失败，详见 {log_path}")

        ckpt = _latest_checkpoint(work_dir / "lightning_logs") or _latest_checkpoint(work_dir)
        if not ckpt:
            if resume_single_ckpt or resume_ckpt:
                ckpt = resume_single_ckpt or resume_ckpt
                if progress and ckpt:
                    progress("train", 1.0, f"训练完成（沿用基线）：{ckpt.name}")
                if ckpt:
                    return ckpt
            raise RuntimeError(f"未找到训练产物，检查目录 {work_dir}")
        if progress:
            progress("train", 1.0, f"训练完成：{ckpt.name}")
        return ckpt

    piper_train = find_executable(["piper_train", "piper-train"])
    if not piper_train:
        raise RuntimeError(
            "未找到 piper_train 可执行文件，请根据 piper/TRAINING.md 安装训练依赖。"
        )

    cmd = [
        piper_train,
        "--dataset",
        str(metadata_csv),
        "--output-dir",
        str(work_dir / "checkpoints"),
        "--epochs",
        str(opts.epochs),
        "--learning-rate",
        str(opts.learning_rate),
        "--batch-size",
        str(opts.batch_size),
        "--device",
        opts.device,
        "--speaker-id",
        str(opts.speaker_id),
    ]
    if opts.piper_base_checkpoint:
        cmd += ["--checkpoint", str(opts.piper_base_checkpoint)]

    if progress:
        progress("train", 0.0, "启动 piper_train")

    with log_path.open("w", encoding="utf-8") as log_file:
        proc = subprocess.Popen(cmd, stdout=log_file, stderr=subprocess.STDOUT, text=True)
        proc.wait()
        if proc.returncode != 0:
            raise RuntimeError(f"piper_train 失败，详见 {log_path}")

    ckpt_dir = work_dir / "checkpoints"
    checkpoints = sorted(
        ckpt_dir.glob("*.ckpt"),
        key=lambda p: p.stat().st_mtime,
        reverse=True,
    )
    if not checkpoints:
        raise RuntimeError(f"未找到训练产物，检查目录 {ckpt_dir}")
    if progress:
        progress("train", 1.0, f"训练完成：{checkpoints[0].name}")
    return checkpoints[0]


def export_onnx(
    checkpoint: Path,
    out_dir: Path,
    opts: TrainingOptions,
    progress: Optional[ProgressCallback] = None,
) -> Path:
    out_dir.mkdir(parents=True, exist_ok=True)
    model_out = out_dir / "model.onnx"
    config_out = out_dir / "model.onnx.json"
    log_path = out_dir / "export.log"

    def _run_export(cmd: list[str], env: Optional[dict] = None, cwd: Optional[Path] = None) -> int:
        timeout_sec = int(os.environ.get("KGTTS_EXPORT_TIMEOUT_SEC", "1800"))
        if progress:
            progress("export", 0.0, "导出 ONNX 中")
        with log_path.open("w", encoding="utf-8") as log_file:
            log_file.write("== Export ONNX ==\n")
            log_file.write("CMD: " + " ".join(cmd) + "\n")
            log_file.flush()
            proc = subprocess.Popen(
                cmd,
                stdout=log_file,
                stderr=subprocess.STDOUT,
                stdin=subprocess.DEVNULL,
                text=True,
                env=env,
                cwd=str(cwd) if cwd else None,
                close_fds=True,
            )
            log_file.write(f"[spawn] pid={proc.pid}\n")
            log_file.flush()
            start = time.monotonic()
            last_tick = 0.0
            while True:
                code = proc.poll()
                if code is not None:
                    if code != 0:
                        log_file.write(f"\n[exit] code={code}\n")
                        log_file.flush()
                    return code
                now = time.monotonic()
                if progress and (now - last_tick >= 2.0):
                    progress("export", 0.0, "导出 ONNX 中...")
                    last_tick = now
                if now - start > timeout_sec:
                    try:
                        proc.kill()
                    except Exception:
                        pass
                    log_file.write(f"\n[timeout] exceeded {timeout_sec}s\n")
                    log_file.flush()
                    return 124
                time.sleep(0.2)

    prefer_cuda = opts.device.lower() in {"cuda", "gpu"}
    piper_python = _find_piper_python(prefer_cuda=prefer_cuda)
    if piper_python:
        env = _piper_env(piper_python)
        cmd = [
            str(piper_python),
            "-m",
            "piper_train.export_onnx",
            str(checkpoint),
            str(model_out),
        ]
        code = _run_export(cmd, env=env, cwd=piper_python.parent)
        if code != 0:
            raise RuntimeError(f"Piper 导出失败，详见 {log_path}")

        prep_config = out_dir.parent / "piper_preprocess" / "config.json"
        if prep_config.exists():
            shutil.copyfile(prep_config, config_out)
        if progress:
            progress("export", 1.0, "导出 ONNX 完成")
        return model_out

    exporter = find_executable(["piper_export", "piper-export"])
    if not exporter:
        raise RuntimeError("未找到 piper_export，请安装 piper 或从源码构建。")

    cmd = [
        exporter,
        "--checkpoint",
        str(checkpoint),
        "--output",
        str(model_out),
        "--config",
        str(config_out),
    ]
    if opts.export_fp16:
        cmd += ["--precision", "fp16"]

    code = _run_export(cmd)
    if code != 0:
        raise RuntimeError(f"piper_export 失败，详见 {log_path}")
    if progress:
        progress("export", 1.0, "导出 ONNX 完成")
    return model_out
