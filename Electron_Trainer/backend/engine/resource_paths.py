from __future__ import annotations

import os
import sys
from pathlib import Path
from typing import Optional

RESOURCE_PARENT_DIRNAME = "resources"
RESOURCE_PACK_DIRNAME = "resources_pack"


def user_data_dir() -> Path:
    env_dir = os.environ.get("KGTTS_USER_DATA")
    if env_dir:
        return Path(env_dir)
    if os.name == "nt":
        local = os.environ.get("LOCALAPPDATA")
        if local:
            return Path(local) / "kgtts-trainer"
    return Path.home() / ".kgtts-trainer"


def external_resources_parent() -> Path:
    return user_data_dir() / RESOURCE_PARENT_DIRNAME


def external_resources_root() -> Path:
    return external_resources_parent() / RESOURCE_PACK_DIRNAME


def resources_root_from_env() -> Optional[Path]:
    env_resources = os.environ.get("KGTTS_RESOURCES")
    if env_resources:
        path = Path(env_resources)
        if path.exists():
            return path

    env_base = os.environ.get("KGTTS_BASE_DIR")
    if env_base:
        base = Path(env_base)
        candidate = base / RESOURCE_PACK_DIRNAME
        if candidate.exists():
            return candidate

    return None


def resolve_resources_root(fallback_base: Optional[Path] = None) -> Optional[Path]:
    external = external_resources_root()
    if external.exists():
        return external

    env_root = resources_root_from_env()
    if env_root is not None:
        return env_root

    if fallback_base is not None:
        candidate = fallback_base / RESOURCE_PACK_DIRNAME
        if candidate.exists():
            return candidate

    if getattr(sys, "frozen", False):
        candidate = Path(sys.executable).resolve().parent / RESOURCE_PACK_DIRNAME
        if candidate.exists():
            return candidate

    return None
