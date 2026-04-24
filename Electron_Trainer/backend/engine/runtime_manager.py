from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import threading
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Any, Dict, Optional

from .config import ProgressCallback

MICROMAMBA_DOWNLOAD_URL = "https://github.com/mamba-org/micromamba-releases/releases/latest/download/micromamba-win-64"
ALIYUN_PYPI_INDEX = "https://mirrors.aliyun.com/pypi/simple"
BFSU_PYPI_INDEX = "https://mirrors.bfsu.edu.cn/pypi/web/simple"
TENCENT_PYPI_INDEX = "https://mirrors.cloud.tencent.com/pypi/simple"
SJTU_PYPI_INDEX = "https://mirror.sjtu.edu.cn/pypi/web/simple"
SUSTECH_PYPI_INDEX = "https://mirrors.sustech.edu.cn/pypi/web/simple"
TUNA_PYPI_INDEX = "https://pypi.tuna.tsinghua.edu.cn/simple"
USTC_PYPI_INDEX = "https://pypi.mirrors.ustc.edu.cn/simple"
NJU_PYPI_INDEX = "https://mirrors.nju.edu.cn/pypi/web/simple"
HUAWEI_PYPI_INDEX = "https://repo.huaweicloud.com/repository/pypi/simple"
VOLCES_PYPI_INDEX = "https://mirrors.volces.com/pypi/"
OFFICIAL_PYPI_INDEX = "https://pypi.org/simple"
SUSTECH_CONDA_CHANNELS = (
    "https://mirrors.sustech.edu.cn/anaconda/cloud/conda-forge",
    "https://mirrors.sustech.edu.cn/anaconda/cloud/pytorch",
    "https://mirrors.sustech.edu.cn/anaconda-extra/cloud/nvidia",
)
SJTU_CONDA_CHANNELS = (
    "https://mirror.sjtu.edu.cn/anaconda/cloud/conda-forge",
    "https://mirror.sjtu.edu.cn/anaconda/cloud/pytorch",
    "https://mirror.sjtu.edu.cn/anaconda/cloud/nvidia",
)
TUNA_CONDA_CHANNELS = (
    "https://mirrors.tuna.tsinghua.edu.cn/anaconda/cloud/conda-forge",
    "https://mirrors.tuna.tsinghua.edu.cn/anaconda/cloud/pytorch",
    "https://mirrors.sustech.edu.cn/anaconda-extra/cloud/nvidia",
)
BFSU_CONDA_CHANNELS = (
    "https://mirrors.bfsu.edu.cn/anaconda/cloud/conda-forge",
    "https://mirrors.bfsu.edu.cn/anaconda/cloud/pytorch",
    "https://mirrors.sustech.edu.cn/anaconda-extra/cloud/nvidia",
)
USTC_CONDA_CHANNELS = (
    "https://mirrors.ustc.edu.cn/anaconda/cloud/conda-forge",
    "https://mirrors.ustc.edu.cn/anaconda/cloud/pytorch",
    "https://mirrors.sustech.edu.cn/anaconda-extra/cloud/nvidia",
)
NJU_CONDA_CHANNELS = (
    "https://mirrors.nju.edu.cn/anaconda/cloud/conda-forge",
    "https://mirrors.nju.edu.cn/anaconda/cloud/pytorch",
    "https://mirrors.sustech.edu.cn/anaconda-extra/cloud/nvidia",
)
OFFICIAL_CONDA_CHANNELS = (
    "https://conda.anaconda.org/conda-forge",
    "https://conda.anaconda.org/pytorch",
    "https://conda.anaconda.org/nvidia",
)
CONDA_SOURCE_CANDIDATES = (
    ("sustech", "南科大镜像", SUSTECH_CONDA_CHANNELS),
    ("sjtu", "上交镜像", SJTU_CONDA_CHANNELS),
    ("tuna_sustech_nvidia", "清华镜像 + 南科大 nvidia", TUNA_CONDA_CHANNELS),
    ("bfsu_sustech_nvidia", "北外镜像 + 南科大 nvidia", BFSU_CONDA_CHANNELS),
    ("ustc_sustech_nvidia", "中科大镜像 + 南科大 nvidia", USTC_CONDA_CHANNELS),
    ("nju_sustech_nvidia", "南大镜像 + 南科大 nvidia", NJU_CONDA_CHANNELS),
    ("official", "官方源", OFFICIAL_CONDA_CHANNELS),
)
PYPI_SOURCE_CANDIDATES = (
    ("aliyun", "阿里云 PyPI", ALIYUN_PYPI_INDEX),
    ("bfsu", "北外 PyPI", BFSU_PYPI_INDEX),
    ("tencent", "腾讯云 PyPI", TENCENT_PYPI_INDEX),
    ("sjtu", "上交 PyPI", SJTU_PYPI_INDEX),
    ("sustech", "南科大 PyPI", SUSTECH_PYPI_INDEX),
    ("tuna", "清华 PyPI", TUNA_PYPI_INDEX),
    ("ustc", "中科大 PyPI", USTC_PYPI_INDEX),
    ("nju", "南大 PyPI", NJU_PYPI_INDEX),
    ("huawei", "华为云 PyPI", HUAWEI_PYPI_INDEX),
    ("volces", "火山引擎 PyPI", VOLCES_PYPI_INDEX),
    ("official", "官方 PyPI", OFFICIAL_PYPI_INDEX),
)
PYTORCH_WHEEL_SOURCE_CANDIDATES = (
    ("aliyun", "阿里云 PyTorch CUDA Wheel", "https://mirrors.aliyun.com/pytorch-wheels", "find-links"),
    ("sjtu", "上交 PyTorch CUDA Wheel", "https://mirror.sjtu.edu.cn/pytorch-wheels", "index"),
    ("official", "官方 PyTorch CUDA Wheel", "https://download.pytorch.org/whl", "index"),
)
BASE_CONDA_PACKAGES = (
    "python=3.10",
    "pip",
)
CUDA_CONDA_PACKAGES = (
    "python=3.10",
    "pip",
    "pytorch=1.13.1",
    "torchvision=0.14.1",
    "torchaudio=0.13.1",
    "pytorch-cuda=11.7",
)
VOXCPM_CONDA_PACKAGES = (
    "python=3.10",
    "pip",
    "pytorch=2.5.1=py3.10_cuda12.4_cudnn9_0",
    "torchvision=0.20.1=py310_cu124",
    "torchaudio=2.5.1=py310_cu124",
    "pytorch-cuda=12.4",
)
PIPER_TORCH_WHEEL_PACKAGES = (
    "torch==1.13.1+cu117",
    "torchvision==0.14.1+cu117",
    "torchaudio==0.13.1+cu117",
)
VOXCPM_TORCH_WHEEL_PACKAGES = (
    "torch==2.5.1+cu124",
    "torchvision==0.20.1+cu124",
    "torchaudio==2.5.1+cu124",
)
PIP_TOOLCHAIN_PACKAGES = (
    "pip==24.0",
    "setuptools==80.9.0",
    "wheel",
)
VOXCPM_PIP_PACKAGES = (
    "voxcpm==2.0.2",
    "modelscope",
    "soundfile",
)
META_FILENAME = "runtime_meta.json"
ENV_NAME = "piper_env_cuda"
VOXCPM_ENV_NAME = "voxcpm_env"
VOXCPM_META_FILENAME = "voxcpm_runtime_meta.json"
VOXCPM_MAIN_REPO = "OpenBMB/VoxCPM2"
VOXCPM_DENOISER_REPO = "iic/speech_zipenhancer_ans_multiloss_16k_base"


def _app_dir() -> Path:
    env_dir = os.environ.get("KGTTS_APP_DIR")
    if env_dir:
        return Path(env_dir)
    return Path(__file__).resolve().parents[2]


def _user_data_dir() -> Path:
    env_dir = os.environ.get("KGTTS_USER_DATA")
    if env_dir:
        return Path(env_dir)
    if os.name == "nt":
        local = os.environ.get("LOCALAPPDATA")
        if local:
            return Path(local) / "kgtts-trainer"
    return Path.home() / ".kgtts-trainer"


def _runtime_root() -> Path:
    return _user_data_dir() / "runtimes"


def _cuda_env_dir() -> Path:
    return _runtime_root() / ENV_NAME


def _voxcpm_env_dir() -> Path:
    return _runtime_root() / VOXCPM_ENV_NAME


def _mamba_root_dir() -> Path:
    return _runtime_root() / "mamba-root"


def _micromamba_cache_path() -> Path:
    return _runtime_root() / "tools" / "micromamba.exe"


def _runtime_meta_path() -> Path:
    return _runtime_root() / META_FILENAME


def _voxcpm_runtime_meta_path() -> Path:
    return _runtime_root() / VOXCPM_META_FILENAME


def _cuda_python_path() -> Path:
    if os.name == "nt":
        return _cuda_env_dir() / "python.exe"
    return _cuda_env_dir() / "bin" / "python3"


def _voxcpm_python_path() -> Path:
    if os.name == "nt":
        return _voxcpm_env_dir() / "python.exe"
    return _voxcpm_env_dir() / "bin" / "python3"


def _models_root() -> Path:
    return _user_data_dir() / "models" / "voxcpm2"


def _voxcpm_main_model_dir() -> Path:
    return _models_root() / "OpenBMB" / "VoxCPM2"


def _voxcpm_denoiser_model_dir() -> Path:
    return _models_root() / "iic" / "speech_zipenhancer_ans_multiloss_16k_base"


def _stage(progress: Optional[ProgressCallback], value: float, message: str) -> None:
    if progress:
        progress("runtime", value, message)


def _bundled_micromamba_candidates() -> list[Path]:
    app_dir = _app_dir()
    return [
        app_dir / "micromamba" / "micromamba.exe",
        app_dir / "build" / "micromamba" / "micromamba.exe",
        app_dir.parent / "Electron_Trainer" / "build" / "micromamba" / "micromamba.exe",
    ]


def _resolve_bundled_micromamba() -> Optional[Path]:
    for candidate in _bundled_micromamba_candidates():
        if candidate.exists():
            return candidate
    return None


def _requirements_path() -> Path:
    return Path(__file__).with_name("piper_cuda_requirements.txt")


def _piper_train_wheel_candidates() -> list[Path]:
    app_dir = _app_dir()
    return [
        app_dir / "runtime_assets" / "piper_train-1.0.0-py3-none-any.whl",
        app_dir / "runtime_assets" / "piper_wheels" / "piper_train-1.0.0-py3-none-any.whl",
        app_dir.parent / "pc_trainer" / "piper_wheels" / "piper_train-1.0.0-py3-none-any.whl",
        app_dir.parent.parent / "pc_trainer" / "piper_wheels" / "piper_train-1.0.0-py3-none-any.whl",
    ]


def _local_wheel_dirs() -> list[Path]:
    app_dir = _app_dir()
    return [
        app_dir / "runtime_assets" / "piper_wheels",
        app_dir.parent / "pc_trainer" / "piper_wheels",
        app_dir.parent.parent / "pc_trainer" / "piper_wheels",
    ]


def _existing_local_wheel_dirs() -> list[Path]:
    return [path for path in _local_wheel_dirs() if path.exists()]


def _find_piper_train_wheel() -> Optional[Path]:
    for candidate in _piper_train_wheel_candidates():
        if candidate.exists():
            return candidate
    return None


def _read_meta() -> Dict[str, Any]:
    meta_path = _runtime_meta_path()
    if not meta_path.exists():
        return {}
    try:
        return json.loads(meta_path.read_text(encoding="utf-8"))
    except Exception:
        return {}


def _write_meta(payload: Dict[str, Any]) -> None:
    meta_path = _runtime_meta_path()
    meta_path.parent.mkdir(parents=True, exist_ok=True)
    meta_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def _read_voxcpm_meta() -> Dict[str, Any]:
    meta_path = _voxcpm_runtime_meta_path()
    if not meta_path.exists():
        return {}
    try:
        return json.loads(meta_path.read_text(encoding="utf-8"))
    except Exception:
        return {}


def _write_voxcpm_meta(payload: Dict[str, Any]) -> None:
    meta_path = _voxcpm_runtime_meta_path()
    meta_path.parent.mkdir(parents=True, exist_ok=True)
    meta_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def _trim_output(text: str, limit: int = 4000) -> str:
    text = (text or "").strip()
    if len(text) <= limit:
        return text
    return text[-limit:]


def _probe_url(url: str, timeout: float = 4.0) -> tuple[bool, float, str]:
    started = time.monotonic()
    request = urllib.request.Request(url, method="HEAD", headers={"User-Agent": "KIGTTS-Trainer/1.0"})
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:  # noqa: S310
            status = int(getattr(response, "status", 0) or 0)
        elapsed = time.monotonic() - started
        return 200 <= status < 400, elapsed, str(status)
    except Exception as exc:
        elapsed = time.monotonic() - started
        return False, elapsed, str(exc)


def _rank_source_candidates(
    candidates: tuple[tuple[Any, ...], ...],
    probe_urls: Any,
    *,
    progress: Optional[ProgressCallback],
    label: str,
    progress_value: float = 0.1,
) -> list[tuple[Any, ...]]:
    if len(candidates) <= 1:
        return list(candidates)

    _stage(progress, progress_value, f"正在测速 {label}...")

    def probe_one(index: int, candidate: tuple[Any, ...]) -> tuple[bool, float, int, tuple[Any, ...], str]:
        total_elapsed = 0.0
        last_error = ""
        for url in probe_urls(candidate):
            ok, elapsed, message = _probe_url(url)
            total_elapsed += elapsed
            if not ok:
                last_error = message
                return False, total_elapsed, index, candidate, last_error
        return True, total_elapsed, index, candidate, ""

    results: list[tuple[bool, float, int, tuple[Any, ...], str]] = []
    worker_count = max(1, min(8, len(candidates)))
    with ThreadPoolExecutor(max_workers=worker_count) as executor:
        futures = {
            executor.submit(probe_one, index, candidate): (index, candidate)
            for index, candidate in enumerate(candidates)
        }
        for future in as_completed(futures):
            index, candidate = futures[future]
            try:
                results.append(future.result())
            except Exception as exc:
                results.append((False, 9999.0, index, candidate, str(exc)))

    available = sorted((item for item in results if item[0]), key=lambda item: (item[1], item[2]))
    unavailable = sorted((item for item in results if not item[0]), key=lambda item: item[2])
    ordered = [item[3] for item in [*available, *unavailable]]
    if available:
        preview = " -> ".join(str(item[3][1]) for item in available[:3])
        _stage(progress, progress_value, f"{label}测速完成，优先使用：{preview}")
    else:
        _stage(progress, progress_value, f"{label}测速未发现可用源，将按预设顺序尝试。")
    return ordered


def _run_command(
    cmd: list[str],
    *,
    env: Optional[Dict[str, str]] = None,
    cwd: Optional[Path] = None,
    timeout: int = 7200,
) -> tuple[int, str]:
    proc = subprocess.run(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
        errors="replace",
        env=env,
        cwd=str(cwd) if cwd else None,
        timeout=timeout,
    )
    return proc.returncode, proc.stdout or ""


def _run_command_streaming(
    cmd: list[str],
    *,
    env: Optional[Dict[str, str]] = None,
    cwd: Optional[Path] = None,
    timeout: int = 86400,
    progress: Optional[ProgressCallback] = None,
    progress_start: float = 0.0,
    progress_end: float = 1.0,
    progress_message: str = "正在处理...",
) -> tuple[int, str]:
    proc = subprocess.Popen(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
        errors="replace",
        env=env,
        cwd=str(cwd) if cwd else None,
    )
    output_chunks: list[str] = []
    current_value = progress_start

    def read_output() -> None:
        nonlocal current_value
        if proc.stdout is None:
            return
        for raw_line in proc.stdout:
            output_chunks.append(raw_line)
            line = raw_line.strip()
            if line and progress:
                current_value = max(current_value, progress_start + (progress_end - progress_start) * 0.08)
                _stage(progress, min(progress_end, current_value), line[-180:])

    reader = threading.Thread(target=read_output, daemon=True)
    reader.start()
    started = time.monotonic()
    next_report = started
    try:
        while proc.poll() is None:
            now = time.monotonic()
            if now - started > timeout:
                proc.kill()
                reader.join(timeout=5)
                output_chunks.append(f"\nTimeout after {timeout} seconds\n")
                return -1, "".join(output_chunks)
            if progress and now >= next_report:
                elapsed = max(0.0, now - started)
                # Download tools do not always expose byte progress; keep a bounded heartbeat so the UI stays alive.
                soft_fraction = min(0.95, elapsed / max(900.0, elapsed + 180.0))
                current_value = max(current_value, progress_start + (progress_end - progress_start) * soft_fraction)
                minutes = int(elapsed // 60)
                seconds = int(elapsed % 60)
                _stage(progress, current_value, f"{progress_message}（已用时 {minutes:02d}:{seconds:02d}）")
                next_report = now + 5.0
            time.sleep(0.5)
        reader.join(timeout=10)
        return proc.returncode or 0, "".join(output_chunks)
    finally:
        if proc.poll() is None:
            proc.kill()


def _micromamba_env() -> Dict[str, str]:
    env = os.environ.copy()
    env["MAMBA_ROOT_PREFIX"] = str(_mamba_root_dir())
    env["MAMBA_NO_BANNER"] = "1"
    env["PYTHONUTF8"] = "1"
    env["PYTHONIOENCODING"] = "utf-8"
    return env


def _pip_env(python_path: Path) -> Dict[str, str]:
    root = python_path.parent
    path_entries = [
        str(root),
        str(root / "Scripts"),
        str(root / "Library" / "bin"),
        str(root / "Library" / "usr" / "bin"),
        str(root / "Library" / "mingw-w64" / "bin"),
        os.environ.get("PATH", ""),
    ]
    env = os.environ.copy()
    env["PATH"] = os.pathsep.join(entry for entry in path_entries if entry)
    env["PYTHONUTF8"] = "1"
    env["PYTHONIOENCODING"] = "utf-8"
    return env


def _probe_nvidia_smi() -> Dict[str, str]:
    nvidia_smi = shutil.which("nvidia-smi")
    if not nvidia_smi:
        return {}
    cmd = [
        nvidia_smi,
        "--query-gpu=driver_version,name,memory.total",
        "--format=csv,noheader",
    ]
    try:
        code, output = _run_command(cmd, timeout=20)
    except Exception:
        return {}
    if code != 0:
        return {}
    first_line = next((line.strip() for line in output.splitlines() if line.strip()), "")
    if not first_line:
        return {}
    parts = [part.strip() for part in first_line.split(",")]
    return {
        "nvidia_smi_path": nvidia_smi,
        "driver_version": parts[0] if len(parts) > 0 else "",
        "gpu_name": parts[1] if len(parts) > 1 else "",
        "gpu_memory": parts[2] if len(parts) > 2 else "",
    }


def _probe_env_python(python_path: Path) -> Dict[str, Any]:
    env = _pip_env(python_path)
    probe_code = (
        "import json; "
        "import torch, torchaudio, pytorch_lightning, piper_train; "
        "payload = {"
        "'torch_version': torch.__version__, "
        "'torch_cuda_version': getattr(torch.version, 'cuda', None), "
        "'cuda_available': bool(torch.cuda.is_available()), "
        "'torchaudio_version': getattr(torchaudio, '__version__', ''), "
        "'pytorch_lightning_version': getattr(pytorch_lightning, '__version__', ''), "
        "'piper_train_path': getattr(piper_train, '__file__', ''), "
        "}; "
        "print(json.dumps(payload, ensure_ascii=False))"
    )
    code, output = _run_command([str(python_path), "-c", probe_code], env=env, cwd=python_path.parent, timeout=240)
    if code != 0:
        raise RuntimeError(_trim_output(output) or "运行时探测失败")
    last_line = next((line for line in reversed(output.splitlines()) if line.strip()), "")
    if not last_line:
        raise RuntimeError("运行时探测无输出")
    try:
        return json.loads(last_line)
    except Exception as exc:
        raise RuntimeError(f"运行时探测输出无法解析: {last_line}") from exc


def _probe_voxcpm_env_python(python_path: Path) -> Dict[str, Any]:
    env = _pip_env(python_path)
    probe_code = (
        "import json; "
        "import importlib.metadata as md; "
        "import torch; "
        "payload = {"
        "'torch_version': torch.__version__, "
        "'torch_cuda_version': getattr(torch.version, 'cuda', None), "
        "'cuda_available': bool(torch.cuda.is_available()), "
        "'voxcpm_version': md.version('voxcpm'), "
        "'modelscope_version': md.version('modelscope'), "
        "}; "
        "print(json.dumps(payload, ensure_ascii=False))"
    )
    code, output = _run_command([str(python_path), "-c", probe_code], env=env, cwd=python_path.parent, timeout=240)
    if code != 0:
        raise RuntimeError(_trim_output(output) or "VoxCPM2 运行时探测失败")
    last_line = next((line for line in reversed(output.splitlines()) if line.strip()), "")
    if not last_line:
        raise RuntimeError("VoxCPM2 运行时探测无输出")
    try:
        return json.loads(last_line)
    except Exception as exc:
        raise RuntimeError(f"VoxCPM2 运行时探测输出无法解析: {last_line}") from exc


def _build_status(
    *,
    available: bool,
    status: str,
    message: str,
    extra: Optional[Dict[str, Any]] = None,
) -> Dict[str, Any]:
    payload: Dict[str, Any] = {
        "ok": True,
        "available": available,
        "status": status,
        "message": message,
        "runtime_root": str(_runtime_root()),
        "env_path": str(_cuda_env_dir()),
        "python_path": str(_cuda_python_path()),
        "micromamba_path": str(_micromamba_cache_path()),
        "bundled_micromamba_path": str(_resolve_bundled_micromamba() or ""),
        "requirements_path": str(_requirements_path()),
        "piper_train_wheel": str(_find_piper_train_wheel() or ""),
        "local_wheel_dirs": [str(path) for path in _existing_local_wheel_dirs()],
    }
    payload.update(_read_meta())
    payload.update(_probe_nvidia_smi())
    if extra:
        payload.update(extra)
    return payload


def _build_voxcpm_runtime_status(
    *,
    available: bool,
    status: str,
    message: str,
    extra: Optional[Dict[str, Any]] = None,
) -> Dict[str, Any]:
    payload: Dict[str, Any] = {
        "ok": True,
        "available": available,
        "status": status,
        "message": message,
        "runtime_root": str(_runtime_root()),
        "env_path": str(_voxcpm_env_dir()),
        "python_path": str(_voxcpm_python_path()),
        "micromamba_path": str(_micromamba_cache_path()),
        "bundled_micromamba_path": str(_resolve_bundled_micromamba() or ""),
    }
    payload.update(_read_voxcpm_meta())
    payload.update(_probe_nvidia_smi())
    if extra:
        payload.update(extra)
    return payload


def _ensure_micromamba(progress: Optional[ProgressCallback]) -> Path:
    cache_path = _micromamba_cache_path()
    if cache_path.exists():
        return cache_path

    cache_path.parent.mkdir(parents=True, exist_ok=True)

    bundled = _resolve_bundled_micromamba()
    if bundled and bundled.exists():
        _stage(progress, 0.05, "复制内置 micromamba...")
        shutil.copy2(bundled, cache_path)
        return cache_path

    _stage(progress, 0.05, "内置 micromamba 不存在，回退官方下载...")
    urllib.request.urlretrieve(MICROMAMBA_DOWNLOAD_URL, cache_path)  # noqa: S310
    return cache_path


def describe_piper_cuda_runtime() -> Dict[str, Any]:
    python_path = _cuda_python_path()
    if not python_path.exists():
        return _build_status(
            available=False,
            status="missing",
            message="Piper CUDA 运行时未安装。",
        )

    try:
        probe = _probe_env_python(python_path)
    except Exception as exc:
        return _build_status(
            available=False,
            status="error",
            message=f"Piper CUDA 运行时存在但不可用：{exc}",
        )

    cuda_available = bool(probe.get("cuda_available"))
    if cuda_available:
        message = "Piper CUDA 运行时已就绪。"
    else:
        message = "Piper CUDA 运行时已安装，但当前机器未检测到可用 CUDA。"
    return _build_status(
        available=True,
        status="ready",
        message=message,
        extra=probe,
    )


def describe_voxcpm_runtime() -> Dict[str, Any]:
    python_path = _voxcpm_python_path()
    if not python_path.exists():
        return _build_voxcpm_runtime_status(
            available=False,
            status="missing",
            message="VoxCPM2 运行时未安装。",
        )

    try:
        probe = _probe_voxcpm_env_python(python_path)
    except Exception as exc:
        return _build_voxcpm_runtime_status(
            available=False,
            status="error",
            message=f"VoxCPM2 运行时存在但不可用：{exc}",
        )

    cuda_available = bool(probe.get("cuda_available"))
    message = "VoxCPM2 运行时已就绪。" if cuda_available else "VoxCPM2 运行时已安装，但当前机器未检测到可用 CUDA，CPU 推理会非常慢。"
    return _build_voxcpm_runtime_status(
        available=True,
        status="ready",
        message=message,
        extra=probe,
    )


def resolve_cuda_python() -> Optional[Path]:
    python_path = _cuda_python_path()
    if not python_path.exists():
        return None
    try:
        _probe_env_python(python_path)
    except Exception:
        return None
    return python_path


def resolve_voxcpm_python() -> Optional[Path]:
    python_path = _voxcpm_python_path()
    if not python_path.exists():
        return None
    try:
        _probe_voxcpm_env_python(python_path)
    except Exception:
        return None
    return python_path


def get_voxcpm_python_path() -> Optional[Path]:
    python_path = _voxcpm_python_path()
    return python_path if python_path.exists() else None


def _remove_existing_env() -> None:
    env_dir = _cuda_env_dir()
    if env_dir.exists():
        shutil.rmtree(env_dir, ignore_errors=True)


def _remove_existing_voxcpm_env() -> None:
    env_dir = _voxcpm_env_dir()
    if env_dir.exists():
        shutil.rmtree(env_dir, ignore_errors=True)


def _run_micromamba_create_env(
    micromamba_path: Path,
    env_dir: Path,
    packages: tuple[str, ...],
    channels: tuple[str, ...],
    *,
    progress: Optional[ProgressCallback],
) -> tuple[bool, str]:
    env = _micromamba_env()
    cmd = [
        str(micromamba_path),
        "create",
        "-y",
        "--no-rc",
        "--override-channels",
        "-r",
        str(_mamba_root_dir()),
        "-p",
        str(env_dir),
        *packages,
    ]
    for channel in channels:
        cmd.extend(["-c", channel])

    code, output = _run_command(cmd, env=env, cwd=_runtime_root(), timeout=10800)
    if code == 0:
        return True, output
    _stage(progress, 0.34, "Conda 依赖解析失败，准备切换源重试...")
    return False, output


def _run_micromamba_create(
    micromamba_path: Path,
    channels: tuple[str, ...],
    *,
    progress: Optional[ProgressCallback],
) -> tuple[bool, str]:
    return _run_micromamba_create_env(
        micromamba_path,
        _cuda_env_dir(),
        CUDA_CONDA_PACKAGES,
        channels,
        progress=progress,
    )


def _create_env_with_conda_sources(
    micromamba_path: Path,
    env_dir: Path,
    packages: tuple[str, ...],
    *,
    progress: Optional[ProgressCallback],
    env_label: str,
) -> tuple[str, str]:
    last_output = ""
    ordered_sources = _rank_source_candidates(
        CONDA_SOURCE_CANDIDATES,
        lambda candidate: [f"{str(channel).rstrip('/')}/win-64/repodata.json" for channel in candidate[2]],
        progress=progress,
        label=f"{env_label} Conda 源",
        progress_value=0.1,
    )
    for source_id, source_label, channels in ordered_sources:
        if env_dir.exists():
            shutil.rmtree(env_dir, ignore_errors=True)
        _stage(progress, 0.12, f"使用{source_label}创建 {env_label} 环境...")
        ok, output = _run_micromamba_create_env(
            micromamba_path,
            env_dir,
            packages,
            channels,
            progress=progress,
        )
        if ok:
            return source_id, output
        last_output = output
        _stage(progress, 0.18, f"{source_label}不可用或缺包，准备切换源重试...")
    raise RuntimeError(_trim_output(last_output) or f"micromamba 创建 {env_label} 环境失败")


def _pip_index_args(index_url: str) -> list[str]:
    args = ["--index-url", index_url]
    if index_url != OFFICIAL_PYPI_INDEX:
        args.extend(["--extra-index-url", OFFICIAL_PYPI_INDEX])
    return args


def _run_pip_install_with_sources(
    python_path: Path,
    pip_args: list[str],
    *,
    progress: Optional[ProgressCallback],
    label_template: str,
    probe_progress_value: float = 0.56,
) -> tuple[bool, str, str]:
    last_output = ""
    ordered_sources = _rank_source_candidates(
        PYPI_SOURCE_CANDIDATES,
        lambda candidate: [str(candidate[2]).rstrip("/") + "/"],
        progress=progress,
        label="PyPI 源",
        progress_value=probe_progress_value,
    )
    for source_id, source_label, index_url in ordered_sources:
        ok, output = _run_pip_install(
            python_path,
            [*_pip_index_args(index_url), *pip_args],
            progress=progress,
            label=label_template.format(source=source_label),
        )
        if ok:
            return True, output, source_id
        last_output = output
    return False, last_output, ""


def _pytorch_wheel_source_args(base_url: str, cuda_tag: str, mode: str) -> list[str]:
    index_url = f"{base_url.rstrip('/')}/{cuda_tag}"
    args = ["--index-url", ALIYUN_PYPI_INDEX, "--extra-index-url", OFFICIAL_PYPI_INDEX]
    if mode == "find-links":
        args.extend(["--find-links", index_url])
    else:
        args = ["--index-url", index_url, "--extra-index-url", ALIYUN_PYPI_INDEX, "--extra-index-url", OFFICIAL_PYPI_INDEX]
    return args


def _run_pytorch_cuda_wheel_install(
    python_path: Path,
    *,
    cuda_tag: str,
    packages: tuple[str, ...],
    progress: Optional[ProgressCallback],
    env_label: str,
) -> tuple[bool, str, str]:
    last_output = ""
    ordered_sources = _rank_source_candidates(
        PYTORCH_WHEEL_SOURCE_CANDIDATES,
        lambda candidate: [f"{str(candidate[2]).rstrip('/')}/{cuda_tag}/"],
        progress=progress,
        label=f"PyTorch {cuda_tag} Wheel 源",
        progress_value=0.32,
    )
    for source_id, source_label, base_url, mode in ordered_sources:
        ok, output = _run_pip_install(
            python_path,
            [*_pytorch_wheel_source_args(base_url, cuda_tag, mode), "--prefer-binary", *packages],
            progress=progress,
            label=f"使用{source_label}安装 {env_label} PyTorch CUDA 依赖...",
        )
        if ok:
            return True, output, source_id
        last_output = output
    return False, last_output, ""


def _run_pip_install(
    python_path: Path,
    pip_args: list[str],
    *,
    progress: Optional[ProgressCallback],
    label: str,
) -> tuple[bool, str]:
    env = _pip_env(python_path)
    cmd = [str(python_path), "-m", "pip", "install", "--disable-pip-version-check", "--no-input", *pip_args]
    _stage(progress, 0.62, label)
    code, output = _run_command(cmd, env=env, cwd=python_path.parent, timeout=10800)
    return code == 0, output


def _with_local_wheel_args(pip_args: list[str]) -> list[str]:
    local_dirs = _existing_local_wheel_dirs()
    if not local_dirs:
        return pip_args
    out = ["--prefer-binary"]
    for wheel_dir in local_dirs:
        out.extend(["--find-links", str(wheel_dir)])
    out.extend(pip_args)
    return out


def install_piper_cuda_runtime(
    progress: Optional[ProgressCallback] = None,
    *,
    force: bool = False,
) -> Dict[str, Any]:
    status = describe_piper_cuda_runtime()
    if status.get("available") and not force:
        return status

    _runtime_root().mkdir(parents=True, exist_ok=True)
    _mamba_root_dir().mkdir(parents=True, exist_ok=True)

    micromamba_path = _ensure_micromamba(progress)
    if force:
        _stage(progress, 0.08, "清理旧的 Piper CUDA 运行时...")
        _remove_existing_env()

    requirements_path = _requirements_path()
    if not requirements_path.exists():
        raise RuntimeError(f"缺少 CUDA 运行时依赖锁文件：{requirements_path}")

    piper_train_wheel = _find_piper_train_wheel()
    if piper_train_wheel is None:
        raise RuntimeError("缺少内置 piper_train wheel，无法创建 Piper CUDA 运行时。")

    conda_source = ""
    torch_source = "conda"
    toolchain_source = ""
    dependency_source = ""
    try:
        try:
            conda_source, _ = _create_env_with_conda_sources(
                micromamba_path,
                _cuda_env_dir(),
                CUDA_CONDA_PACKAGES,
                progress=progress,
                env_label="Piper CUDA 基础",
            )
        except RuntimeError as conda_exc:
            _stage(progress, 0.2, f"Conda CUDA 环境创建失败，改用 PyTorch CUDA wheel 重试：{_trim_output(str(conda_exc), 240)}")
            conda_source, _ = _create_env_with_conda_sources(
                micromamba_path,
                _cuda_env_dir(),
                BASE_CONDA_PACKAGES,
                progress=progress,
                env_label="Piper 基础",
            )
            python_path = _cuda_python_path()
            if not python_path.exists():
                raise RuntimeError("基础环境创建完成，但未找到 python.exe") from conda_exc
            ok, pip_output, torch_source = _run_pytorch_cuda_wheel_install(
                python_path,
                cuda_tag="cu117",
                packages=PIPER_TORCH_WHEEL_PACKAGES,
                progress=progress,
                env_label="Piper",
            )
            if not ok:
                raise RuntimeError(_trim_output(pip_output) or "Piper PyTorch CUDA wheel 安装失败") from conda_exc

        python_path = _cuda_python_path()
        if not python_path.exists():
            raise RuntimeError("基础环境创建完成，但未找到 python.exe")

        _stage(progress, 0.44, "升级 pip / setuptools / wheel...")
        ok, pip_output, toolchain_source = _run_pip_install_with_sources(
            python_path,
            ["--upgrade", *PIP_TOOLCHAIN_PACKAGES],
            progress=progress,
            label_template="使用{source}准备 pip 工具链...",
            probe_progress_value=0.46,
        )
        if not ok:
            raise RuntimeError(_trim_output(pip_output) or "pip 工具链安装失败")

        ok, pip_output, dependency_source = _run_pip_install_with_sources(
            python_path,
            _with_local_wheel_args([
                "-r",
                str(requirements_path),
            ]),
            progress=progress,
            label_template="使用{source}补齐 Piper 训练依赖...",
            probe_progress_value=0.58,
        )
        if not ok:
            raise RuntimeError(_trim_output(pip_output) or "Piper 依赖安装失败")

        ok, pip_output = _run_pip_install(
            python_path,
            [
                "--no-deps",
                str(piper_train_wheel),
            ],
            progress=progress,
            label="安装 Piper 训练入口...",
        )
        if not ok:
            raise RuntimeError(_trim_output(pip_output) or "piper_train 安装失败")

        _stage(progress, 0.9, "校验 Piper CUDA 运行时...")
        probe = _probe_env_python(python_path)
        _write_meta(
            {
                "source": conda_source,
                "conda_source": conda_source,
                "torch_source": torch_source,
                "pip_toolchain_source": toolchain_source,
                "pip_dependency_source": dependency_source,
                "installed_with": "micromamba",
                "micromamba_path": str(micromamba_path),
                "installed_env_path": str(_cuda_env_dir()),
            }
        )
        _stage(progress, 1.0, "Piper CUDA 运行时已准备完成。")
        return _build_status(
            available=True,
            status="ready",
            message="Piper CUDA 运行时已准备完成。",
            extra=probe,
        )
    except Exception:
        _remove_existing_env()
        raise


def install_voxcpm_runtime(
    progress: Optional[ProgressCallback] = None,
    *,
    force: bool = False,
) -> Dict[str, Any]:
    status = describe_voxcpm_runtime()
    if status.get("available") and not force:
        return status

    _runtime_root().mkdir(parents=True, exist_ok=True)
    _mamba_root_dir().mkdir(parents=True, exist_ok=True)

    micromamba_path = _ensure_micromamba(progress)
    if force:
        _stage(progress, 0.08, "清理旧的 VoxCPM2 运行时...")
        _remove_existing_voxcpm_env()

    conda_source = ""
    torch_source = "conda"
    toolchain_source = ""
    dependency_source = ""
    try:
        try:
            conda_source, _ = _create_env_with_conda_sources(
                micromamba_path,
                _voxcpm_env_dir(),
                VOXCPM_CONDA_PACKAGES,
                progress=progress,
                env_label="VoxCPM2 CUDA 基础",
            )
        except RuntimeError as conda_exc:
            _stage(progress, 0.2, f"Conda CUDA 环境创建失败，改用 PyTorch CUDA wheel 重试：{_trim_output(str(conda_exc), 240)}")
            conda_source, _ = _create_env_with_conda_sources(
                micromamba_path,
                _voxcpm_env_dir(),
                BASE_CONDA_PACKAGES,
                progress=progress,
                env_label="VoxCPM2 基础",
            )
            python_path = _voxcpm_python_path()
            if not python_path.exists():
                raise RuntimeError("VoxCPM2 基础环境创建完成，但未找到 python.exe") from conda_exc
            ok, pip_output, torch_source = _run_pytorch_cuda_wheel_install(
                python_path,
                cuda_tag="cu124",
                packages=VOXCPM_TORCH_WHEEL_PACKAGES,
                progress=progress,
                env_label="VoxCPM2",
            )
            if not ok:
                raise RuntimeError(_trim_output(pip_output) or "VoxCPM2 PyTorch CUDA wheel 安装失败") from conda_exc

        python_path = _voxcpm_python_path()
        if not python_path.exists():
            raise RuntimeError("VoxCPM2 基础环境创建完成，但未找到 python.exe")

        ok, pip_output, toolchain_source = _run_pip_install_with_sources(
            python_path,
            ["--upgrade", *PIP_TOOLCHAIN_PACKAGES],
            progress=progress,
            label_template="使用{source}准备 VoxCPM2 pip 工具链...",
            probe_progress_value=0.46,
        )
        if not ok:
            raise RuntimeError(_trim_output(pip_output) or "VoxCPM2 pip 工具链安装失败")

        ok, pip_output, dependency_source = _run_pip_install_with_sources(
            python_path,
            [
                "--prefer-binary",
                *VOXCPM_PIP_PACKAGES,
            ],
            progress=progress,
            label_template="使用{source}安装 VoxCPM2 依赖...",
            probe_progress_value=0.58,
        )
        if not ok:
            raise RuntimeError(_trim_output(pip_output) or "VoxCPM2 依赖安装失败")

        _stage(progress, 0.9, "校验 VoxCPM2 运行时...")
        probe = _probe_voxcpm_env_python(python_path)
        _write_voxcpm_meta(
            {
                "source": conda_source,
                "conda_source": conda_source,
                "torch_source": torch_source,
                "pip_toolchain_source": toolchain_source,
                "pip_dependency_source": dependency_source,
                "installed_with": "micromamba",
                "micromamba_path": str(micromamba_path),
                "installed_env_path": str(_voxcpm_env_dir()),
            }
        )
        _stage(progress, 1.0, "VoxCPM2 运行时已准备完成。")
        return _build_voxcpm_runtime_status(
            available=True,
            status="ready",
            message="VoxCPM2 运行时已准备完成。",
            extra=probe,
        )
    except Exception:
        _remove_existing_voxcpm_env()
        raise


def _looks_like_model_dir(path: Path) -> bool:
    if not path.exists() or not path.is_dir():
        return False
    markers = [
        "config.json",
        "configuration.json",
        "model.safetensors.index.json",
        "pytorch_model.bin",
    ]
    if any((path / marker).exists() for marker in markers):
        return True
    suffixes = {".safetensors", ".bin", ".pt", ".pth", ".onnx"}
    return any(item.is_file() and item.suffix.lower() in suffixes for item in path.rglob("*"))


def describe_voxcpm_models() -> Dict[str, Any]:
    main_dir = _voxcpm_main_model_dir()
    denoiser_dir = _voxcpm_denoiser_model_dir()
    main_available = _looks_like_model_dir(main_dir)
    denoiser_available = _looks_like_model_dir(denoiser_dir)
    if main_available and denoiser_available:
        message = "VoxCPM2 主模型和 denoiser 已就绪。"
    elif main_available:
        message = "VoxCPM2 主模型已就绪，denoiser 未下载。"
    else:
        message = "VoxCPM2 模型未下载。"
    return {
        "ok": True,
        "main_available": main_available,
        "denoiser_available": denoiser_available,
        "message": message,
        "model_root": str(_models_root()),
        "main_model_dir": str(main_dir),
        "denoiser_model_dir": str(denoiser_dir),
        "main_repo": VOXCPM_MAIN_REPO,
        "denoiser_repo": VOXCPM_DENOISER_REPO,
    }


def _download_modelscope_repo(
    python_path: Path,
    repo_id: str,
    target_dir: Path,
    *,
    progress: Optional[ProgressCallback],
    progress_value: float,
    progress_end: float,
) -> tuple[bool, str]:
    target_dir.parent.mkdir(parents=True, exist_ok=True)
    script = (
        "import sys; "
        "from modelscope import snapshot_download; "
        "repo_id = sys.argv[1]; target = sys.argv[2]; "
        "snapshot_download(model_id=repo_id, local_dir=target)"
    )
    _stage(progress, progress_value, f"正在从 ModelScope 下载 {repo_id}...")
    code, output = _run_command_streaming(
        [str(python_path), "-c", script, repo_id, str(target_dir)],
        env=_pip_env(python_path),
        cwd=_models_root(),
        timeout=86400,
        progress=progress,
        progress_start=progress_value,
        progress_end=progress_end,
        progress_message=f"正在从 ModelScope 下载 {repo_id}",
    )
    return code == 0, output


def download_voxcpm_models(
    progress: Optional[ProgressCallback] = None,
    *,
    include_denoiser: bool = True,
    force: bool = False,
) -> Dict[str, Any]:
    python_path = resolve_voxcpm_python()
    if python_path is None:
        raise RuntimeError("VoxCPM2 运行时未就绪，请先安装运行时。")

    _models_root().mkdir(parents=True, exist_ok=True)
    targets = [(VOXCPM_MAIN_REPO, _voxcpm_main_model_dir())]
    if include_denoiser:
        targets.append((VOXCPM_DENOISER_REPO, _voxcpm_denoiser_model_dir()))

    if force:
        for _, target in targets:
            if target.exists():
                shutil.rmtree(target, ignore_errors=True)

    status = describe_voxcpm_models()
    if status.get("main_available") and (not include_denoiser or status.get("denoiser_available")) and not force:
        return status

    for index, (repo_id, target) in enumerate(targets, start=1):
        ok, output = _download_modelscope_repo(
            python_path,
            repo_id,
            target,
            progress=progress,
            progress_value=0.15 + 0.75 * ((index - 1) / max(1, len(targets))),
            progress_end=0.15 + 0.75 * (index / max(1, len(targets))),
        )
        if not ok:
            raise RuntimeError(_trim_output(output) or f"ModelScope 下载失败: {repo_id}")
        _stage(progress, 0.15 + 0.75 * (index / max(1, len(targets))), f"{repo_id} 下载完成。")

    final_status = describe_voxcpm_models()
    if not final_status.get("main_available"):
        raise RuntimeError("VoxCPM2 主模型下载完成后仍不可用，请检查模型目录。")
    if include_denoiser and not final_status.get("denoiser_available"):
        raise RuntimeError("VoxCPM2 denoiser 下载完成后仍不可用，请检查模型目录。")
    _stage(progress, 1.0, "VoxCPM2 模型已准备完成。")
    return final_status
