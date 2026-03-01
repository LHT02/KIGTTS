import json
import os
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Iterable, List, Optional

from .config import SegmentMetadata, TrainingOptions, ProgressCallback
from .utils import find_executable


def _base_dir() -> Path:
    if getattr(sys, "frozen", False):
        return Path(sys.executable).resolve().parent
    return Path(__file__).resolve().parents[1]


def _find_piper_python() -> Optional[Path]:
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
    env["PATH"] = f"{piper_root};{piper_root / 'Scripts'};{env.get('PATH', '')}"
    return env


def _cuda_available(piper_python: Path) -> bool:
    env = _piper_env(piper_python)
    cmd = [
        str(piper_python),
        "-c",
        "import torch; print('1' if torch.cuda.is_available() else '0')",
    ]
    try:
        proc = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            env=env,
            cwd=str(piper_python.parent),
        )
    except Exception:
        return False
    return proc.returncode == 0 and "1" in proc.stdout.strip()


def _default_phonemizer_dict() -> Path:
    base = _base_dir()
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
    base = _base_dir()
    roots = [
        base / "Model" / "piper_checkpoints",
        base / "CKPT",
    ]
    for root in roots:
        candidates.extend(_find_ckpt_files(root))
    if not candidates:
        return None
    return max(candidates, key=lambda p: p.stat().st_mtime)


def _ckpt_num_symbols(piper_python: Path, ckpt_path: Path) -> Optional[int]:
    env = _piper_env(piper_python)
    code = (
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
        )
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
        )
    except Exception:
        return None
    if proc.returncode != 0:
        return None
    out = proc.stdout.strip()
    return out or None


def _find_piper_config(base_ckpt: Path, piper_python: Path) -> Optional[Path]:
    base = _base_dir()
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
    env = _piper_env(piper_python)
    code = (
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
        )
    except Exception:
        return None
    if proc.returncode != 0:
        return None
    out = proc.stdout.strip()
    try:
        return int(out)
    except Exception:
        return None


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
    piper_python = _find_piper_python()
    if piper_python:
        prep_script = _base_dir() / "tools" / "piper_prep.py"
        if not prep_script.exists():
            raise RuntimeError(f"缺少预处理脚本: {prep_script}")
        use_espeak = bool(opts.use_espeak)
        piper_config = opts.piper_config
        if use_espeak and piper_config is None and opts.piper_base_checkpoint:
            piper_config = _find_piper_config(opts.piper_base_checkpoint, piper_python)
        if use_espeak and piper_config is None:
            raise RuntimeError("缺少 Piper config.json（espeak 模式需要）")
        phonemizer = None
        if not use_espeak:
            phonemizer = opts.phonemizer_dict or _default_phonemizer_dict()
            if not phonemizer.exists():
                raise RuntimeError(f"缺少 phonemizer 字典: {phonemizer}")

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

        accelerator = "gpu" if opts.device.lower() in {"cuda", "gpu"} else "cpu"
        if accelerator == "gpu" and not _cuda_available(piper_python):
            if progress:
                progress("train", 0.0, "未检测到 CUDA，自动切换为 CPU 训练")
            accelerator = "cpu"

        base_ckpt = opts.piper_base_checkpoint
        if base_ckpt and not base_ckpt.exists():
            base_ckpt = None
        if base_ckpt is None and use_espeak:
            auto_ckpt = _find_default_base_ckpt(piper_config)
            if auto_ckpt:
                base_ckpt = auto_ckpt
                if progress:
                    progress("train", 0.0, f"自动使用 Piper 基线: {auto_ckpt.name}")
        env = _piper_env(piper_python)
        if progress:
            progress("train", 0.0, "启动 Piper 预处理")
        with log_path.open("w", encoding="utf-8") as log_file:
            proc = subprocess.run(
                prep_cmd,
                stdout=log_file,
                stderr=subprocess.STDOUT,
                text=True,
                env=env,
                cwd=str(piper_python.parent),
            )
            if proc.returncode != 0:
                raise RuntimeError(f"Piper 预处理失败，详见 {log_path}")

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
                        log_file.write(msg + "\n")
                        log_file.flush()
                    except Exception:
                        pass
                    base_ckpt = None

            prep_dataset = prep_dir / "dataset.jsonl"
            try:
                with prep_dataset.open("r", encoding="utf-8") as f:
                    num_lines = sum(1 for _ in f)
            except Exception:
                num_lines = 0
            if (base_ckpt is None) and (num_lines > 0) and (num_lines < 80):
                warn = (
                    f"样本数仅 {num_lines} 且未使用基线 ckpt，"
                    "导出音色可能出现嘟声/杂音，建议选择基线或增加样本量。"
                )
                if progress:
                    progress("train", 0.12, warn)
                try:
                    log_file.write("[warn] " + warn + "\n")
                    log_file.flush()
                except Exception:
                    pass

            num_speakers = _prep_num_speakers(prep_dir) or 1
            effective_batch = max(1, min(int(opts.batch_size), max(1, num_lines)))
            if effective_batch != int(opts.batch_size):
                msg = f"自动调整 batch_size: {opts.batch_size} -> {effective_batch}"
                if progress:
                    progress("train", 0.16, msg)
                try:
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
                        train_max_epochs = base_epoch + extra_epochs

            train_cmd = [
                str(piper_python),
                "-m",
                "piper_train",
                "--dataset-dir",
                str(prep_dir),
                "--batch-size",
                str(effective_batch),
                "--max_epochs",
                str(train_max_epochs),
                "--accelerator",
                accelerator,
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
            ]
            if resume_single_ckpt:
                train_cmd += [
                    "--resume_from_single_speaker_checkpoint",
                    str(resume_single_ckpt),
                ]
            elif resume_ckpt:
                train_cmd += ["--resume_from_checkpoint", str(resume_ckpt)]

            if progress:
                progress("train", 0.2, "启动 Piper 训练")
            proc = subprocess.run(
                train_cmd,
                stdout=log_file,
                stderr=subprocess.STDOUT,
                text=True,
                env=env,
                cwd=str(piper_python.parent),
            )
            if proc.returncode != 0:
                raise RuntimeError(f"Piper 训练失败，详见 {log_path}")

        ckpt = _latest_checkpoint(work_dir / "lightning_logs") or _latest_checkpoint(work_dir)
        if not ckpt:
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

    piper_python = _find_piper_python()
    if piper_python:
        env = _piper_env(piper_python)
        cmd = [
            str(piper_python),
            "-m",
            "piper_train.export_onnx",
            str(checkpoint),
            str(model_out),
        ]
        if progress:
            progress("export", 0.0, "导出 ONNX 中")
        proc = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            env=env,
            cwd=str(piper_python.parent),
        )
        log_path = out_dir / "export.log"
        log_path.write_text(proc.stdout, encoding="utf-8")
        if proc.returncode != 0:
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

    if progress:
        progress("export", 0.0, "导出 ONNX 中")
    proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    log_path = out_dir / "export.log"
    log_path.write_text(proc.stdout, encoding="utf-8")
    if proc.returncode != 0:
        raise RuntimeError(f"piper_export 失败，详见 {log_path}")
    if progress:
        progress("export", 1.0, "导出 ONNX 完成")
    return model_out
