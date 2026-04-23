from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import urllib.request
from pathlib import Path
from typing import Any, Dict, Optional

from .config import ProgressCallback

MICROMAMBA_DOWNLOAD_URL = "https://github.com/mamba-org/micromamba-releases/releases/latest/download/micromamba-win-64"
DOMESTIC_PYPI_INDEX = "https://mirrors.sustech.edu.cn/pypi/web/simple"
OFFICIAL_PYPI_INDEX = "https://pypi.org/simple"
DOMESTIC_CONDA_CHANNELS = (
    "https://mirrors.sustech.edu.cn/anaconda/cloud/conda-forge",
    "https://mirrors.sustech.edu.cn/anaconda/cloud/pytorch",
    "https://mirrors.sustech.edu.cn/anaconda-extra/cloud/nvidia",
)
OFFICIAL_CONDA_CHANNELS = (
    "https://conda.anaconda.org/conda-forge",
    "https://conda.anaconda.org/pytorch",
    "https://conda.anaconda.org/nvidia",
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

    create_output = ""
    source_name = ""
    try:
        _remove_existing_env()
        _stage(progress, 0.12, "使用国内镜像创建 Piper CUDA 基础环境...")
        domestic_ok, create_output = _run_micromamba_create(micromamba_path, DOMESTIC_CONDA_CHANNELS, progress=progress)
        if domestic_ok:
            source_name = "domestic"
        else:
            _remove_existing_env()
            _stage(progress, 0.18, "国内镜像不可用或缺包，回退官方源创建基础环境...")
            official_ok, create_output = _run_micromamba_create(micromamba_path, OFFICIAL_CONDA_CHANNELS, progress=progress)
            if not official_ok:
                raise RuntimeError(_trim_output(create_output) or "micromamba 创建环境失败")
            source_name = "official"

        python_path = _cuda_python_path()
        if not python_path.exists():
            raise RuntimeError("基础环境创建完成，但未找到 python.exe")

        _stage(progress, 0.44, "升级 pip / setuptools / wheel...")
        ok, pip_output = _run_pip_install(
            python_path,
            [
                "--index-url",
                DOMESTIC_PYPI_INDEX,
                "--extra-index-url",
                OFFICIAL_PYPI_INDEX,
                "--upgrade",
                *PIP_TOOLCHAIN_PACKAGES,
            ],
            progress=progress,
            label="正在准备 pip 工具链...",
        )
        if not ok:
            ok, pip_output = _run_pip_install(
                python_path,
                [
                    "--index-url",
                    OFFICIAL_PYPI_INDEX,
                    "--upgrade",
                    *PIP_TOOLCHAIN_PACKAGES,
                ],
                progress=progress,
                label="国内镜像升级 pip 失败，改用官方源重试...",
            )
            if not ok:
                raise RuntimeError(_trim_output(pip_output) or "pip 工具链安装失败")

        ok, pip_output = _run_pip_install(
            python_path,
            _with_local_wheel_args([
                "--index-url",
                DOMESTIC_PYPI_INDEX,
                "--extra-index-url",
                OFFICIAL_PYPI_INDEX,
                "-r",
                str(requirements_path),
            ]),
            progress=progress,
            label="使用国内镜像补齐 Piper 训练依赖...",
        )
        if not ok:
            ok, pip_output = _run_pip_install(
                python_path,
                _with_local_wheel_args([
                    "--index-url",
                    OFFICIAL_PYPI_INDEX,
                    "-r",
                    str(requirements_path),
                ]),
                progress=progress,
                label="国内镜像安装依赖失败，改用官方源重试...",
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
                "source": source_name,
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

    create_output = ""
    source_name = ""
    try:
        _remove_existing_voxcpm_env()
        _stage(progress, 0.12, "使用国内镜像创建 VoxCPM2 CUDA 基础环境...")
        domestic_ok, create_output = _run_micromamba_create_env(
            micromamba_path,
            _voxcpm_env_dir(),
            VOXCPM_CONDA_PACKAGES,
            DOMESTIC_CONDA_CHANNELS,
            progress=progress,
        )
        if domestic_ok:
            source_name = "domestic"
        else:
            _remove_existing_voxcpm_env()
            _stage(progress, 0.18, "国内镜像不可用或缺包，回退官方源创建基础环境...")
            official_ok, create_output = _run_micromamba_create_env(
                micromamba_path,
                _voxcpm_env_dir(),
                VOXCPM_CONDA_PACKAGES,
                OFFICIAL_CONDA_CHANNELS,
                progress=progress,
            )
            if not official_ok:
                raise RuntimeError(_trim_output(create_output) or "micromamba 创建 VoxCPM2 环境失败")
            source_name = "official"

        python_path = _voxcpm_python_path()
        if not python_path.exists():
            raise RuntimeError("VoxCPM2 基础环境创建完成，但未找到 python.exe")

        ok, pip_output = _run_pip_install(
            python_path,
            [
                "--index-url",
                DOMESTIC_PYPI_INDEX,
                "--extra-index-url",
                OFFICIAL_PYPI_INDEX,
                "--upgrade",
                *PIP_TOOLCHAIN_PACKAGES,
            ],
            progress=progress,
            label="正在准备 VoxCPM2 pip 工具链...",
        )
        if not ok:
            ok, pip_output = _run_pip_install(
                python_path,
                [
                    "--index-url",
                    OFFICIAL_PYPI_INDEX,
                    "--upgrade",
                    *PIP_TOOLCHAIN_PACKAGES,
                ],
                progress=progress,
                label="国内镜像升级 pip 失败，改用官方源重试...",
            )
            if not ok:
                raise RuntimeError(_trim_output(pip_output) or "VoxCPM2 pip 工具链安装失败")

        ok, pip_output = _run_pip_install(
            python_path,
            [
                "--prefer-binary",
                "--index-url",
                DOMESTIC_PYPI_INDEX,
                "--extra-index-url",
                OFFICIAL_PYPI_INDEX,
                *VOXCPM_PIP_PACKAGES,
            ],
            progress=progress,
            label="使用国内镜像安装 VoxCPM2 依赖...",
        )
        if not ok:
            ok, pip_output = _run_pip_install(
                python_path,
                [
                    "--prefer-binary",
                    "--index-url",
                    OFFICIAL_PYPI_INDEX,
                    *VOXCPM_PIP_PACKAGES,
                ],
                progress=progress,
                label="国内镜像安装依赖失败，改用官方源重试...",
            )
            if not ok:
                raise RuntimeError(_trim_output(pip_output) or "VoxCPM2 依赖安装失败")

        _stage(progress, 0.9, "校验 VoxCPM2 运行时...")
        probe = _probe_voxcpm_env_python(python_path)
        _write_voxcpm_meta(
            {
                "source": source_name,
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
) -> tuple[bool, str]:
    target_dir.parent.mkdir(parents=True, exist_ok=True)
    script = (
        "import sys; "
        "from modelscope import snapshot_download; "
        "repo_id = sys.argv[1]; target = sys.argv[2]; "
        "snapshot_download(model_id=repo_id, local_dir=target)"
    )
    _stage(progress, progress_value, f"正在从 ModelScope 下载 {repo_id}...")
    code, output = _run_command(
        [str(python_path), "-c", script, repo_id, str(target_dir)],
        env=_pip_env(python_path),
        cwd=_models_root(),
        timeout=86400,
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
