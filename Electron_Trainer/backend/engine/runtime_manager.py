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
from .resource_paths import external_resources_parent, external_resources_root, resolve_resources_root

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
PIPER_RUNTIME_META_FILENAME = "piper_runtime_meta.json"
ENV_NAME = "piper_env_cuda"
PIPER_RUNTIME_ENV_NAME = "piper_env"
VOXCPM_ENV_NAME = "voxcpm_env"
VOXCPM_META_FILENAME = "voxcpm_runtime_meta.json"
VOXCPM_MAIN_REPO = "OpenBMB/VoxCPM2"
VOXCPM_DENOISER_REPO = "iic/speech_zipenhancer_ans_multiloss_16k_base"
RUNTIME_ARCHIVE_SOURCES_FILE = "runtime_archive_sources.json"
RUNTIME_ARCHIVE_MANIFEST_NAME = "kigtts_runtime_manifest.json"
RUNTIME_ARCHIVE_MANIFEST_SCHEMA = 1
USER_ARCHIVE_SOURCES_FILE = "runtime_archive_sources.user.json"
TRAINER_RESOURCES_SOURCE_KEY = "trainer_resources"
TRAINER_RESOURCES_ARCHIVE_NAME = "trainer_resources.7z"
TRAINER_RESOURCES_LABEL = "训练资源包"
TRAINER_RESOURCES_META_FILENAME = "trainer_resources_meta.json"
TRAINER_RESOURCES_MANIFEST_NAME = "kigtts_resource_manifest.json"
TRAINER_RESOURCES_MANIFEST_SCHEMA = 1
ARCHIVE_SOURCE_GROUP_LABELS = {
    "piper_runtime": "Piper 基础运行时",
    "piper_cuda_runtime": "Piper CUDA 运行时",
    "voxcpm_runtime": "VoxCPM2 运行时",
    TRAINER_RESOURCES_SOURCE_KEY: "训练资源包",
}


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


def _piper_env_dir() -> Path:
    return _runtime_root() / PIPER_RUNTIME_ENV_NAME


def _voxcpm_env_dir() -> Path:
    return _runtime_root() / VOXCPM_ENV_NAME


def _mamba_root_dir() -> Path:
    return _runtime_root() / "mamba-root"


def _micromamba_cache_path() -> Path:
    return _runtime_root() / "tools" / "micromamba.exe"


def _runtime_meta_path() -> Path:
    return _runtime_root() / META_FILENAME


def _piper_runtime_meta_path() -> Path:
    return _runtime_root() / PIPER_RUNTIME_META_FILENAME


def _voxcpm_runtime_meta_path() -> Path:
    return _runtime_root() / VOXCPM_META_FILENAME


def _trainer_resources_meta_path() -> Path:
    return external_resources_parent() / TRAINER_RESOURCES_META_FILENAME


def _cuda_python_path() -> Path:
    if os.name == "nt":
        return _cuda_env_dir() / "python.exe"
    return _cuda_env_dir() / "bin" / "python3"


def _piper_python_path() -> Path:
    if os.name == "nt":
        return _piper_env_dir() / "python.exe"
    return _piper_env_dir() / "bin" / "python3"


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


def _bundled_7za_candidates() -> list[Path]:
    app_dir = _app_dir()
    node_modules = app_dir / "node_modules"
    return [
        app_dir / "tools" / "7za.exe",
        app_dir / "build" / "7zip" / "7za.exe",
        node_modules / "7zip-bin" / "win" / "x64" / "7za.exe",
        node_modules / "7zip-bin" / "win" / "ia32" / "7za.exe",
        node_modules / "7zip-bin" / "win" / "arm64" / "7za.exe",
    ]


def _resolve_7za() -> Optional[Path]:
    for candidate in _bundled_7za_candidates():
        if candidate.exists():
            return candidate
    external = shutil.which("7z") or shutil.which("7za")
    return Path(external) if external else None


def _requirements_path() -> Path:
    return Path(__file__).with_name("piper_cuda_requirements.txt")


def _archive_source_config_path() -> Path:
    return Path(__file__).with_name(RUNTIME_ARCHIVE_SOURCES_FILE)


def _user_archive_source_config_path() -> Path:
    return _user_data_dir() / USER_ARCHIVE_SOURCES_FILE


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


def _read_piper_runtime_meta() -> Dict[str, Any]:
    meta_path = _piper_runtime_meta_path()
    if not meta_path.exists():
        return {}
    try:
        return json.loads(meta_path.read_text(encoding="utf-8"))
    except Exception:
        return {}


def _write_piper_runtime_meta(payload: Dict[str, Any]) -> None:
    meta_path = _piper_runtime_meta_path()
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


def _read_trainer_resources_meta() -> Dict[str, Any]:
    meta_path = _trainer_resources_meta_path()
    if not meta_path.exists():
        return {}
    try:
        return json.loads(meta_path.read_text(encoding="utf-8"))
    except Exception:
        return {}


def _write_trainer_resources_meta(payload: Dict[str, Any]) -> None:
    meta_path = _trainer_resources_meta_path()
    meta_path.parent.mkdir(parents=True, exist_ok=True)
    meta_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def _trim_output(text: str, limit: int = 4000) -> str:
    text = (text or "").strip()
    if len(text) <= limit:
        return text
    return text[-limit:]


def _read_json_file(path: Path) -> Dict[str, Any]:
    if not path.exists():
        return {}
    try:
        data = json.loads(path.read_text(encoding="utf-8-sig"))
    except Exception:
        return {}
    if not isinstance(data, dict):
        return {}
    return data


def _normalize_archive_source_item(item: Any, *, allow_empty_url: bool) -> Optional[dict[str, str]]:
    if not isinstance(item, dict):
        return None
    source_id = str(item.get("id") or "").strip()
    label = str(item.get("label") or source_id or "").strip()
    url = str(item.get("url") or "").strip()
    if not source_id or not label:
        return None
    if not allow_empty_url and not url:
        return None
    return {"id": source_id, "label": label, "url": url}


def _source_groups_from_config(data: Dict[str, Any], *, allow_empty_url: bool) -> dict[str, list[dict[str, str]]]:
    source_data = data.get("sources") if isinstance(data.get("sources"), dict) else data
    normalized: dict[str, list[dict[str, str]]] = {}
    for key, value in source_data.items():
        key = str(key)
        if key.startswith("_") or key in {"schema_version", "preferred_sources", "sources"}:
            continue
        if not isinstance(value, list):
            continue
        items: list[dict[str, str]] = []
        for item in value:
            normalized_item = _normalize_archive_source_item(item, allow_empty_url=allow_empty_url)
            if normalized_item is None:
                continue
            items.append(normalized_item)
        if items:
            normalized[key] = items
    return normalized


def _merge_archive_source_groups(*, allow_empty_url: bool) -> dict[str, list[dict[str, str]]]:
    builtin = _source_groups_from_config(_read_json_file(_archive_source_config_path()), allow_empty_url=True)
    user = _source_groups_from_config(_read_json_file(_user_archive_source_config_path()), allow_empty_url=True)
    merged: dict[str, list[dict[str, str]]] = {}
    for key in [*builtin.keys(), *(k for k in user.keys() if k not in builtin)]:
        by_id: dict[str, dict[str, str]] = {}
        order: list[str] = []
        for item in builtin.get(key, []):
            by_id[item["id"]] = dict(item)
            order.append(item["id"])
        for item in user.get(key, []):
            if item["id"] not in by_id:
                order.append(item["id"])
            by_id[item["id"]] = dict(item)
        items = [by_id[source_id] for source_id in order if source_id in by_id]
        if not allow_empty_url:
            items = [item for item in items if item.get("url")]
        if items:
            merged[key] = items
    return merged


def _preferred_archive_source_ids() -> dict[str, str]:
    data = _read_json_file(_user_archive_source_config_path())
    raw = data.get("preferred_sources")
    if not isinstance(raw, dict):
        return {}
    preferred: dict[str, str] = {}
    for key, value in raw.items():
        source_id = str(value or "").strip()
        if source_id:
            preferred[str(key)] = source_id
    return preferred


def _preferred_archive_source_id(source_key: str) -> str:
    return _preferred_archive_source_ids().get(source_key, "")


def _order_sources_by_preference(source_key: str, sources: list[dict[str, str]]) -> list[dict[str, str]]:
    preferred = _preferred_archive_source_id(source_key)
    if not preferred:
        return sources
    preferred_items = [item for item in sources if item.get("id") == preferred]
    if not preferred_items:
        return sources
    return [*preferred_items, *(item for item in sources if item.get("id") != preferred)]


def _load_runtime_archive_sources() -> dict[str, list[dict[str, str]]]:
    return {
        key: _order_sources_by_preference(key, value)
        for key, value in _merge_archive_source_groups(allow_empty_url=False).items()
    }


def describe_download_sources() -> Dict[str, Any]:
    groups = _merge_archive_source_groups(allow_empty_url=True)
    preferred = _preferred_archive_source_ids()
    return {
        "ok": True,
        "config_path": str(_user_archive_source_config_path()),
        "builtin_config_path": str(_archive_source_config_path()),
        "preferred_sources": preferred,
        "groups": [
            {
                "key": key,
                "label": ARCHIVE_SOURCE_GROUP_LABELS.get(key, key),
                "preferred_source_id": preferred.get(key, ""),
                "sources": groups.get(key, []),
            }
            for key in [*ARCHIVE_SOURCE_GROUP_LABELS.keys(), *(k for k in groups.keys() if k not in ARCHIVE_SOURCE_GROUP_LABELS)]
        ],
    }


def save_download_sources(payload: Dict[str, Any]) -> Dict[str, Any]:
    raw_groups = payload.get("groups")
    if not isinstance(raw_groups, list):
        raise RuntimeError("下载源设置格式不正确，请重新打开“下载源设置”检查链接。")
    sources: dict[str, list[dict[str, str]]] = {}
    for group in raw_groups:
        if not isinstance(group, dict):
            continue
        key = str(group.get("key") or "").strip()
        if not key:
            continue
        raw_sources = group.get("sources")
        if not isinstance(raw_sources, list):
            raw_sources = []
        items: list[dict[str, str]] = []
        seen: set[str] = set()
        for item in raw_sources:
            normalized = _normalize_archive_source_item(item, allow_empty_url=True)
            if normalized is None or normalized["id"] in seen:
                continue
            seen.add(normalized["id"])
            items.append(normalized)
        sources[key] = items

    raw_preferred = payload.get("preferred_sources")
    preferred_sources: dict[str, str] = {}
    if isinstance(raw_preferred, dict):
        for key, value in raw_preferred.items():
            source_id = str(value or "").strip()
            if source_id:
                preferred_sources[str(key)] = source_id

    config = {
        "schema_version": 1,
        "preferred_sources": preferred_sources,
        "sources": sources,
    }
    config_path = _user_archive_source_config_path()
    config_path.parent.mkdir(parents=True, exist_ok=True)
    config_path.write_text(json.dumps(config, ensure_ascii=False, indent=2), encoding="utf-8")
    return describe_download_sources()


def _piper_runtime_archive_sources() -> list[dict[str, str]]:
    return _runtime_archive_sources("piper_runtime")


def _piper_cuda_runtime_archive_sources() -> list[dict[str, str]]:
    return _runtime_archive_sources("piper_cuda_runtime")


def _voxcpm_runtime_archive_sources() -> list[dict[str, str]]:
    return _runtime_archive_sources("voxcpm_runtime")


def _trainer_resources_archive_sources() -> list[dict[str, str]]:
    return _runtime_archive_sources(TRAINER_RESOURCES_SOURCE_KEY)


def _runtime_archive_sources(source_key: str) -> list[dict[str, str]]:
    return _load_runtime_archive_sources().get(source_key, [])


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
    probe_code = """
import importlib
import json

import piper_train
import pytorch_lightning
import torch

torchaudio_version = ""
torchaudio_available = False
try:
    torchaudio = importlib.import_module("torchaudio")
    torchaudio_version = getattr(torchaudio, "__version__", "")
    torchaudio_available = True
except ModuleNotFoundError:
    pass

payload = {
    "torch_version": torch.__version__,
    "torch_cuda_version": getattr(torch.version, "cuda", None),
    "cuda_available": bool(torch.cuda.is_available()),
    "torchaudio_version": torchaudio_version,
    "torchaudio_available": torchaudio_available,
    "pytorch_lightning_version": getattr(pytorch_lightning, "__version__", ""),
    "piper_train_path": getattr(piper_train, "__file__", ""),
}
print(json.dumps(payload, ensure_ascii=False))
"""
    code, output = _run_command([str(python_path), "-c", probe_code], env=env, cwd=python_path.parent, timeout=240)
    if code != 0:
        trimmed = _trim_output(output)
        if "ModuleNotFoundError" in trimmed:
            missing = ""
            marker = "No module named "
            if marker in trimmed:
                missing = trimmed.split(marker, 1)[1].splitlines()[0].strip().strip("'\"")
            if missing:
                raise RuntimeError(f"运行时缺少必要组件：{missing}。请重新安装对应运行时包。")
        raise RuntimeError(trimmed or "运行时探测失败")
    last_line = next((line for line in reversed(output.splitlines()) if line.strip()), "")
    if not last_line:
        raise RuntimeError("运行时探测无输出")
    try:
        return json.loads(last_line)
    except Exception as exc:
        raise RuntimeError(f"运行时探测输出无法解析: {last_line}") from exc


def _download_file_with_progress(
    source_url: str,
    target_path: Path,
    *,
    progress: Optional[ProgressCallback],
    start_value: float,
    end_value: float,
    label: str,
) -> None:
    target_path.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(source_url, headers={"User-Agent": "KIGTTS-Trainer/1.0"})
    with urllib.request.urlopen(request, timeout=120) as response:  # noqa: S310
        total = int(response.headers.get("Content-Length") or 0)
        written = 0
        next_heartbeat = time.monotonic()
        with target_path.open("wb") as handle:
            while True:
                chunk = response.read(1024 * 1024)
                if not chunk:
                    break
                handle.write(chunk)
                written += len(chunk)
                now = time.monotonic()
                if progress and (total > 0 or now >= next_heartbeat):
                    ratio = (written / total) if total > 0 else 0.0
                    value = start_value + (end_value - start_value) * min(1.0, max(0.0, ratio))
                    size_text = f"{written / (1024 * 1024):.1f} MiB"
                    if total > 0:
                        size_text += f" / {total / (1024 * 1024):.1f} MiB"
                    _stage(progress, value, f"{label}（{size_text}）")
                    next_heartbeat = now + 1.0
    if progress:
        _stage(progress, end_value, f"{label}完成")


def _extract_7z_archive(
    archive_path: Path,
    output_dir: Path,
    *,
    progress: Optional[ProgressCallback],
    progress_value: float,
) -> None:
    seven_zip = _resolve_7za()
    if seven_zip is None:
        raise RuntimeError("安装程序缺少解压组件，请重新安装 KIGTTS Trainer 后再试。")
    if output_dir.exists():
        shutil.rmtree(output_dir, ignore_errors=True)
    output_dir.mkdir(parents=True, exist_ok=True)
    _stage(progress, progress_value, "正在解压安装包...")
    code, output = _run_command(
        [str(seven_zip), "x", "-y", str(archive_path), f"-o{output_dir}"],
        cwd=output_dir,
        timeout=4 * 60 * 60,
    )
    if code != 0:
        raise RuntimeError(_trim_output(output) or "解压失败，请确认文件完整后重试。")


def _runtime_manifest_candidates(extract_root: Path, env_name: str) -> list[Path]:
    return [
        extract_root / RUNTIME_ARCHIVE_MANIFEST_NAME,
        extract_root / env_name / RUNTIME_ARCHIVE_MANIFEST_NAME,
    ]


def _validate_runtime_archive_manifest(
    extract_root: Path,
    *,
    expected_package_type: str,
    expected_env_name: str,
    runtime_label: str,
) -> Dict[str, Any]:
    manifest_path = next((path for path in _runtime_manifest_candidates(extract_root, expected_env_name) if path.exists()), None)
    if manifest_path is None:
        raise RuntimeError(
            f"这个文件不是有效的 {runtime_label}安装包。请重新下载，或选择对应的 7z 文件。"
        )
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8-sig"))
    except Exception as exc:
        raise RuntimeError(f"无法读取 {runtime_label} 安装包信息，文件可能不完整或已损坏。") from exc
    if not isinstance(manifest, dict):
        raise RuntimeError(f"{runtime_label} 安装包信息格式不正确，请重新下载。")
    schema_version = int(manifest.get("schema_version") or 0)
    package_type = str(manifest.get("package_type") or "").strip()
    env_name = str(manifest.get("env_name") or "").strip()
    if schema_version != RUNTIME_ARCHIVE_MANIFEST_SCHEMA:
        raise RuntimeError(
            f"{runtime_label} 安装包版本与当前软件不匹配，请下载最新的运行时包。"
        )
    if package_type != expected_package_type:
        raise RuntimeError(
            f"选择的 7z 文件不是 {runtime_label}，请确认没有选错运行时包。"
        )
    if env_name != expected_env_name:
        raise RuntimeError(
            f"{runtime_label} 安装包内容不完整，请重新下载后再试。"
        )
    return manifest


def _trainer_resource_manifest_candidates(extract_root: Path) -> list[Path]:
    return [
        extract_root / TRAINER_RESOURCES_MANIFEST_NAME,
        extract_root / "resources" / TRAINER_RESOURCES_MANIFEST_NAME,
        extract_root / "trainer_resources" / TRAINER_RESOURCES_MANIFEST_NAME,
    ]


def _validate_trainer_resources_manifest(extract_root: Path) -> Dict[str, Any]:
    manifest_path = next((path for path in _trainer_resource_manifest_candidates(extract_root) if path.exists()), None)
    if manifest_path is None:
        raise RuntimeError(
            f"这个文件不是有效的{TRAINER_RESOURCES_LABEL}。请重新下载，或选择正确的 7z 文件。"
        )
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8-sig"))
    except Exception as exc:
        raise RuntimeError(f"无法读取{TRAINER_RESOURCES_LABEL}信息，文件可能不完整或已损坏。") from exc
    if not isinstance(manifest, dict):
        raise RuntimeError(f"{TRAINER_RESOURCES_LABEL}信息格式不正确，请重新下载。")
    schema_version = int(manifest.get("schema_version") or 0)
    package_type = str(manifest.get("package_type") or "").strip()
    resources_dir = str(manifest.get("resources_dir") or "").strip()
    if schema_version != TRAINER_RESOURCES_MANIFEST_SCHEMA:
        raise RuntimeError(
            f"{TRAINER_RESOURCES_LABEL}版本与当前软件不匹配，请下载最新资源包。"
        )
    if package_type != TRAINER_RESOURCES_SOURCE_KEY:
        raise RuntimeError(
            f"选择的 7z 文件不是{TRAINER_RESOURCES_LABEL}，请确认没有选错文件。"
        )
    if resources_dir and resources_dir != "resources_pack":
        raise RuntimeError(
            f"{TRAINER_RESOURCES_LABEL}内容不完整，请重新下载后再试。"
        )
    return manifest


def _looks_like_resources_root(path: Path) -> bool:
    if not path.exists() or not path.is_dir():
        return False
    markers = [
        path / "data" / "phonemizer_zh.dict",
        path / "tools" / "espeak-ng",
        path / "Model",
    ]
    return any(marker.exists() for marker in markers)


def _locate_extracted_resources_package_root(extract_root: Path) -> Path:
    candidates = [
        extract_root,
        extract_root / "resources",
        extract_root / "trainer_resources",
    ]
    for candidate in candidates:
        if _looks_like_resources_root(candidate / "resources_pack"):
            return candidate
    for candidate in extract_root.iterdir():
        if candidate.is_dir() and _looks_like_resources_root(candidate / "resources_pack"):
            return candidate
    raise RuntimeError(f"{TRAINER_RESOURCES_LABEL}解压后未找到可用资源，请确认文件完整后重试。")


def _install_python_runtime_archive(
    *,
    source_key: str,
    runtime_label: str,
    env_dir: Path,
    archive_filename: str,
    extract_dirname: str,
    progress: Optional[ProgressCallback],
    force: bool,
    probe_python,
    local_archive_path: Optional[Path] = None,
) -> tuple[Dict[str, Any], list[dict[str, str]], str, str, Dict[str, Any]]:
    archive_sources = _runtime_archive_sources(source_key)
    local_archive = local_archive_path.expanduser().resolve() if local_archive_path else None
    if local_archive is not None:
        if not local_archive.exists() or not local_archive.is_file():
            raise RuntimeError(f"找不到选择的本地安装包：{local_archive}")
        if local_archive.suffix.lower() != ".7z":
            raise RuntimeError("请选择 .7z 格式的运行时安装包。")
        ordered_sources = [("local", f"本地文件 {local_archive.name}", str(local_archive))]
    elif not archive_sources:
        raise RuntimeError(f"{runtime_label}还没有可用下载链接。请在“下载源设置”中填写链接，或选择本地 7z 包安装。")
    else:
        source_candidates = tuple((item["id"], item["label"], item["url"]) for item in archive_sources)
        preferred_source = _preferred_archive_source_id(source_key)
        if preferred_source:
            ordered_sources = list(source_candidates)
            preferred_label = next((item["label"] for item in archive_sources if item["id"] == preferred_source), preferred_source)
            _stage(progress, 0.08, f"{runtime_label}优先使用下载源：{preferred_label}")
        else:
            ordered_sources = _rank_source_candidates(
                source_candidates,
                lambda candidate: [str(candidate[2])],
                progress=progress,
                label=f"{runtime_label}下载源",
                progress_value=0.08,
            )

    _runtime_root().mkdir(parents=True, exist_ok=True)
    if force and env_dir.exists():
        _stage(progress, 0.12, f"清理旧的 {runtime_label}...")
        shutil.rmtree(env_dir, ignore_errors=True)

    download_dir = _runtime_root() / "downloads"
    archive_path = download_dir / archive_filename
    extract_root = _runtime_root() / extract_dirname
    last_error = ""
    used_source_id = ""
    used_source_label = ""
    used_manifest: Dict[str, Any] = {}
    for source_id, source_label, source_url in ordered_sources:
        try:
            if source_id == "local":
                archive_path = Path(str(source_url))
                _stage(progress, 0.62, f"正在使用本地文件安装 {runtime_label}...")
            else:
                archive_path = download_dir / archive_filename
                _stage(progress, 0.12, f"准备从 {source_label} 下载 {runtime_label}...")
                if archive_path.exists():
                    archive_path.unlink()
                _download_file_with_progress(
                    source_url,
                    archive_path,
                    progress=progress,
                    start_value=0.15,
                    end_value=0.62,
                    label=f"下载 {runtime_label}：{source_label}",
                )
            _extract_7z_archive(archive_path, extract_root, progress=progress, progress_value=0.72)
            used_manifest = _validate_runtime_archive_manifest(
                extract_root,
                expected_package_type=source_key,
                expected_env_name=env_dir.name,
                runtime_label=runtime_label,
            )
            python_parent = _locate_extracted_python_root(extract_root, env_dir.name)
            if env_dir.exists():
                shutil.rmtree(env_dir, ignore_errors=True)
            source_root = python_parent.parent if python_parent.name.lower() == "bin" else python_parent
            shutil.move(str(source_root), str(env_dir))
            if extract_root.exists():
                shutil.rmtree(extract_root, ignore_errors=True)
            used_source_id = str(source_id)
            used_source_label = str(source_label)
            break
        except Exception as exc:
            last_error = str(exc)
            if extract_root.exists():
                shutil.rmtree(extract_root, ignore_errors=True)
            if source_id != "local" and archive_path.exists():
                archive_path.unlink(missing_ok=True)
            _stage(progress, 0.2, f"{source_label} 暂时不可用，正在尝试下一个下载源...")
    else:
        raise RuntimeError(last_error or f"{runtime_label} 下载失败")

    python_path = env_dir / "python.exe" if os.name == "nt" else env_dir / "bin" / "python3"
    if not python_path.exists():
        raise RuntimeError(f"{runtime_label}解压完成，但没有找到可启动的运行环境。请重新下载后再试。")
    probe = probe_python(python_path)
    return probe, archive_sources, used_source_id, used_source_label, used_manifest


def _locate_extracted_python_root(extract_root: Path, env_name: str) -> Path:
    candidates: list[Path] = []
    if os.name == "nt":
        candidates.extend(
            [
                extract_root / "python.exe",
                extract_root / env_name / "python.exe",
            ]
        )
    else:
        candidates.extend(
            [
                extract_root / "bin" / "python3",
                extract_root / env_name / "bin" / "python3",
            ]
        )
    for candidate in candidates:
        if candidate.exists():
            return candidate.parent
    for candidate in extract_root.rglob("python.exe" if os.name == "nt" else "python3"):
        return candidate.parent
    raise RuntimeError("运行时包内容不完整：没有找到可启动的 Python 环境。")


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
        "archive_source_config": str(_archive_source_config_path()),
        "archive_sources_configured": len(_piper_cuda_runtime_archive_sources()),
        "seven_zip_path": str(_resolve_7za() or ""),
    }
    payload.update(_read_meta())
    payload.update(_probe_nvidia_smi())
    if extra:
        payload.update(extra)
    return payload


def _build_piper_runtime_status(
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
        "env_path": str(_piper_env_dir()),
        "python_path": str(_piper_python_path()),
        "archive_source_config": str(_archive_source_config_path()),
        "archive_sources_configured": len(_piper_runtime_archive_sources()),
        "seven_zip_path": str(_resolve_7za() or ""),
    }
    payload.update(_read_piper_runtime_meta())
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
        "archive_source_config": str(_archive_source_config_path()),
        "archive_sources_configured": len(_voxcpm_runtime_archive_sources()),
        "seven_zip_path": str(_resolve_7za() or ""),
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
        archive_sources = _piper_cuda_runtime_archive_sources()
        message = "Piper CUDA 运行时未安装。"
        if not archive_sources:
            message += " 请在“下载源设置”中填写链接，或使用本地安装。"
        return _build_status(
            available=False,
            status="missing",
            message=message,
            extra={"archive_sources": archive_sources},
        )

    try:
        probe = _probe_env_python(python_path)
    except Exception as exc:
        return _build_status(
            available=False,
            status="error",
            message=f"Piper CUDA 运行时安装不完整或无法启动：{exc}",
            extra={"archive_sources": _piper_cuda_runtime_archive_sources()},
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
        extra={**probe, "archive_sources": _piper_cuda_runtime_archive_sources()},
    )


def describe_piper_runtime() -> Dict[str, Any]:
    python_path = _piper_python_path()
    if not python_path.exists():
        archive_sources = _piper_runtime_archive_sources()
        message = "Piper 基础运行时未安装。"
        if not archive_sources:
            message += " 请在“下载源设置”中填写链接，或使用本地安装。"
        return _build_piper_runtime_status(
            available=False,
            status="missing",
            message=message,
            extra={
                "archive_sources": archive_sources,
            },
        )

    try:
        probe = _probe_env_python(python_path)
    except Exception as exc:
        return _build_piper_runtime_status(
            available=False,
            status="error",
            message=f"Piper 基础运行时安装不完整或无法启动：{exc}",
            extra={
                "archive_sources": _piper_runtime_archive_sources(),
            },
        )

    return _build_piper_runtime_status(
        available=True,
        status="ready",
        message="Piper 基础运行时已就绪。",
        extra={
            **probe,
            "archive_sources": _piper_runtime_archive_sources(),
        },
    )


def describe_voxcpm_runtime() -> Dict[str, Any]:
    python_path = _voxcpm_python_path()
    if not python_path.exists():
        archive_sources = _voxcpm_runtime_archive_sources()
        message = "VoxCPM2 运行时未安装。"
        if not archive_sources:
            message += " 请在“下载源设置”中填写链接，或使用本地安装。"
        return _build_voxcpm_runtime_status(
            available=False,
            status="missing",
            message=message,
            extra={"archive_sources": archive_sources},
        )

    try:
        probe = _probe_voxcpm_env_python(python_path)
    except Exception as exc:
        return _build_voxcpm_runtime_status(
            available=False,
            status="error",
            message=f"VoxCPM2 运行时安装不完整或无法启动：{exc}",
            extra={"archive_sources": _voxcpm_runtime_archive_sources()},
        )

    cuda_available = bool(probe.get("cuda_available"))
    message = "VoxCPM2 运行时已就绪。" if cuda_available else "VoxCPM2 运行时已安装，但当前机器未检测到可用 CUDA，CPU 推理会非常慢。"
    return _build_voxcpm_runtime_status(
        available=True,
        status="ready",
        message=message,
        extra={**probe, "archive_sources": _voxcpm_runtime_archive_sources()},
    )


def _count_resource_files(resources_root: Optional[Path]) -> Dict[str, Any]:
    if resources_root is None or not resources_root.exists():
        return {
            "asr_model_count": 0,
            "piper_checkpoint_count": 0,
            "phonemizer_available": False,
            "espeak_available": False,
        }
    model_root = resources_root / "Model"
    if not model_root.exists():
        model_root = resources_root
    ckpt_roots = [
        model_root / "piper_checkpoints",
        resources_root / "CKPT",
        resources_root.parent / "CKPT",
    ]
    ckpt_paths: set[str] = set()
    for root in ckpt_roots:
        if root.exists():
            ckpt_paths.update(str(path) for path in root.rglob("*.ckpt"))
    return {
        "asr_model_count": len(list(model_root.rglob("*.zip"))) if model_root.exists() else 0,
        "piper_checkpoint_count": len(ckpt_paths),
        "phonemizer_available": (resources_root / "data" / "phonemizer_zh.dict").exists(),
        "espeak_available": (resources_root / "tools" / "espeak-ng").exists(),
    }


def _build_trainer_resources_status(
    *,
    available: bool,
    status: str,
    message: str,
    active_root: Optional[Path],
    external_available: bool,
    extra: Optional[Dict[str, Any]] = None,
) -> Dict[str, Any]:
    payload: Dict[str, Any] = {
        "ok": True,
        "available": available,
        "external_available": external_available,
        "status": status,
        "message": message,
        "resources_parent": str(external_resources_parent()),
        "resources_root": str(external_resources_root()),
        "active_resources_root": str(active_root or ""),
        "archive_source_config": str(_archive_source_config_path()),
        "archive_sources_configured": len(_trainer_resources_archive_sources()),
        "seven_zip_path": str(_resolve_7za() or ""),
    }
    payload.update(_read_trainer_resources_meta())
    payload.update(_count_resource_files(active_root))
    if extra:
        payload.update(extra)
    return payload


def describe_trainer_resources() -> Dict[str, Any]:
    external_root = external_resources_root()
    external_available = _looks_like_resources_root(external_root)
    active_root = resolve_resources_root()
    active_available = active_root is not None and _looks_like_resources_root(active_root)
    archive_sources = _trainer_resources_archive_sources()
    if external_available:
        message = "训练资源包已就绪。"
        status = "ready"
    elif active_available:
        message = "已找到可用训练资源。建议安装外置训练资源包，迁移到其它电脑时会更稳定。"
        status = "fallback"
    else:
        message = "训练资源包未安装。"
        if not archive_sources:
            message += " 请在“下载源设置”中填写链接，或使用本地安装。"
        status = "missing"
    return _build_trainer_resources_status(
        available=active_available,
        status=status,
        message=message,
        active_root=active_root,
        external_available=external_available,
        extra={"archive_sources": archive_sources},
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


def resolve_piper_runtime_python() -> Optional[Path]:
    python_path = _piper_python_path()
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


def _remove_existing_piper_env() -> None:
    env_dir = _piper_env_dir()
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


def install_piper_runtime(
    progress: Optional[ProgressCallback] = None,
    *,
    force: bool = False,
    local_archive_path: Optional[Path] = None,
) -> Dict[str, Any]:
    status = describe_piper_runtime()
    if status.get("available") and not force and local_archive_path is None:
        return status

    probe, archive_sources, used_source_id, used_source_label, manifest = _install_python_runtime_archive(
        source_key="piper_runtime",
        runtime_label="Piper 基础运行时",
        env_dir=_piper_env_dir(),
        archive_filename="piper_env.7z",
        extract_dirname="piper_env_extract",
        progress=progress,
        force=force,
        probe_python=_probe_env_python,
        local_archive_path=local_archive_path,
    )
    _write_piper_runtime_meta(
        {
            "source": used_source_id,
            "source_label": used_source_label,
            "installed_with": "downloaded_7z",
            "archive_name": "piper_env.7z",
            "runtime_manifest": manifest,
        }
    )
    _stage(progress, 1.0, "Piper 基础运行时安装完成")
    return _build_piper_runtime_status(
        available=True,
        status="ready",
        message="Piper 基础运行时已就绪。",
        extra={
            **probe,
            "source": used_source_id,
            "source_label": used_source_label,
            "archive_sources": archive_sources,
        },
    )


def install_piper_cuda_runtime(
    progress: Optional[ProgressCallback] = None,
    *,
    force: bool = False,
    local_archive_path: Optional[Path] = None,
) -> Dict[str, Any]:
    status = describe_piper_cuda_runtime()
    if status.get("available") and not force and local_archive_path is None:
        return status

    try:
        probe, archive_sources, used_source_id, used_source_label, manifest = _install_python_runtime_archive(
            source_key="piper_cuda_runtime",
            runtime_label="Piper CUDA 运行时",
            env_dir=_cuda_env_dir(),
            archive_filename="piper_env_cuda.7z",
            extract_dirname="piper_env_cuda_extract",
            progress=progress,
            force=force,
            probe_python=_probe_env_python,
            local_archive_path=local_archive_path,
        )
        _write_meta(
            {
                "source": used_source_id,
                "source_label": used_source_label,
                "installed_with": "downloaded_7z",
                "archive_name": "piper_env_cuda.7z",
                "installed_env_path": str(_cuda_env_dir()),
                "runtime_manifest": manifest,
            }
        )
        _stage(progress, 1.0, "Piper CUDA 运行时已准备完成。")
        return _build_status(
            available=True,
            status="ready",
            message="Piper CUDA 运行时已准备完成。",
            extra={**probe, "source": used_source_id, "source_label": used_source_label, "archive_sources": archive_sources},
        )
    except Exception:
        _remove_existing_env()
        raise


def install_voxcpm_runtime(
    progress: Optional[ProgressCallback] = None,
    *,
    force: bool = False,
    local_archive_path: Optional[Path] = None,
) -> Dict[str, Any]:
    status = describe_voxcpm_runtime()
    if status.get("available") and not force and local_archive_path is None:
        return status

    try:
        probe, archive_sources, used_source_id, used_source_label, manifest = _install_python_runtime_archive(
            source_key="voxcpm_runtime",
            runtime_label="VoxCPM2 运行时",
            env_dir=_voxcpm_env_dir(),
            archive_filename="voxcpm_env.7z",
            extract_dirname="voxcpm_env_extract",
            progress=progress,
            force=force,
            probe_python=_probe_voxcpm_env_python,
            local_archive_path=local_archive_path,
        )
        _write_voxcpm_meta(
            {
                "source": used_source_id,
                "source_label": used_source_label,
                "installed_with": "downloaded_7z",
                "archive_name": "voxcpm_env.7z",
                "installed_env_path": str(_voxcpm_env_dir()),
                "runtime_manifest": manifest,
            }
        )
        _stage(progress, 1.0, "VoxCPM2 运行时已准备完成。")
        return _build_voxcpm_runtime_status(
            available=True,
            status="ready",
            message="VoxCPM2 运行时已准备完成。",
            extra={**probe, "source": used_source_id, "source_label": used_source_label, "archive_sources": archive_sources},
        )
    except Exception:
        _remove_existing_voxcpm_env()
        raise


def install_trainer_resources(
    progress: Optional[ProgressCallback] = None,
    *,
    force: bool = False,
    local_archive_path: Optional[Path] = None,
) -> Dict[str, Any]:
    status = describe_trainer_resources()
    if status.get("external_available") and not force and local_archive_path is None:
        return status

    archive_sources = _trainer_resources_archive_sources()
    local_archive = local_archive_path.expanduser().resolve() if local_archive_path else None
    if local_archive is not None:
        if not local_archive.exists() or not local_archive.is_file():
            raise RuntimeError(f"找不到选择的本地资源包：{local_archive}")
        if local_archive.suffix.lower() != ".7z":
            raise RuntimeError("请选择 .7z 格式的训练资源包。")
        ordered_sources = [("local", f"本地文件 {local_archive.name}", str(local_archive))]
    elif not archive_sources:
        raise RuntimeError(f"{TRAINER_RESOURCES_LABEL}还没有可用下载链接。请在“下载源设置”中填写链接，或选择本地 7z 包安装。")
    else:
        source_candidates = tuple((item["id"], item["label"], item["url"]) for item in archive_sources)
        preferred_source = _preferred_archive_source_id(TRAINER_RESOURCES_SOURCE_KEY)
        if preferred_source:
            ordered_sources = list(source_candidates)
            preferred_label = next((item["label"] for item in archive_sources if item["id"] == preferred_source), preferred_source)
            _stage(progress, 0.08, f"{TRAINER_RESOURCES_LABEL}优先使用下载源：{preferred_label}")
        else:
            ordered_sources = _rank_source_candidates(
                source_candidates,
                lambda candidate: [str(candidate[2])],
                progress=progress,
                label=f"{TRAINER_RESOURCES_LABEL}下载源",
                progress_value=0.08,
            )

    resources_parent = external_resources_parent()
    download_dir = _user_data_dir() / "downloads"
    archive_path = download_dir / TRAINER_RESOURCES_ARCHIVE_NAME
    extract_root = _user_data_dir() / "trainer_resources_extract"
    last_error = ""
    used_source_id = ""
    used_source_label = ""
    used_manifest: Dict[str, Any] = {}

    for source_id, source_label, source_url in ordered_sources:
        try:
            if source_id == "local":
                archive_path = Path(str(source_url))
                _stage(progress, 0.62, f"正在使用本地文件安装{TRAINER_RESOURCES_LABEL}...")
            else:
                archive_path = download_dir / TRAINER_RESOURCES_ARCHIVE_NAME
                _stage(progress, 0.12, f"准备从 {source_label} 下载 {TRAINER_RESOURCES_LABEL}...")
                if archive_path.exists():
                    archive_path.unlink()
                _download_file_with_progress(
                    source_url,
                    archive_path,
                    progress=progress,
                    start_value=0.15,
                    end_value=0.62,
                    label=f"下载 {TRAINER_RESOURCES_LABEL}：{source_label}",
                )
            _extract_7z_archive(archive_path, extract_root, progress=progress, progress_value=0.72)
            used_manifest = _validate_trainer_resources_manifest(extract_root)
            package_root = _locate_extracted_resources_package_root(extract_root)
            if resources_parent.exists():
                _stage(progress, 0.82, "清理旧的训练资源包...")
                shutil.rmtree(resources_parent, ignore_errors=True)
            resources_parent.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(package_root), str(resources_parent))
            if extract_root.exists():
                shutil.rmtree(extract_root, ignore_errors=True)
            used_source_id = str(source_id)
            used_source_label = str(source_label)
            break
        except Exception as exc:
            last_error = str(exc)
            if extract_root.exists():
                shutil.rmtree(extract_root, ignore_errors=True)
            if source_id != "local" and archive_path.exists():
                archive_path.unlink(missing_ok=True)
            _stage(progress, 0.2, f"{source_label} 暂时不可用，正在尝试下一个下载源...")
    else:
        raise RuntimeError(last_error or f"{TRAINER_RESOURCES_LABEL} 下载失败")

    if not _looks_like_resources_root(external_resources_root()):
        raise RuntimeError(f"{TRAINER_RESOURCES_LABEL}解压完成，但没有找到可用训练资源。请重新下载后再试。")

    _write_trainer_resources_meta(
        {
            "source": used_source_id,
            "source_label": used_source_label,
            "installed_with": "downloaded_7z" if used_source_id != "local" else "local_7z",
            "archive_name": TRAINER_RESOURCES_ARCHIVE_NAME,
            "resource_manifest": used_manifest,
        }
    )
    _stage(progress, 1.0, f"{TRAINER_RESOURCES_LABEL}已准备完成。")
    return _build_trainer_resources_status(
        available=True,
        status="ready",
        message=f"{TRAINER_RESOURCES_LABEL}已准备完成。",
        active_root=external_resources_root(),
        external_available=True,
        extra={
            "source": used_source_id,
            "source_label": used_source_label,
            "archive_sources": archive_sources,
        },
    )


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
