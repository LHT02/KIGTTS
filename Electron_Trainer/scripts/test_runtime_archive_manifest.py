from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from backend.engine import runtime_manager as rm


def _write_runtime_fixture(root: Path, package_type: str, env_name: str, *, with_manifest: bool = True) -> None:
    root.mkdir(parents=True, exist_ok=True)
    (root / "python.exe").write_text("fake python", encoding="utf-8")
    if with_manifest:
        (root / rm.RUNTIME_ARCHIVE_MANIFEST_NAME).write_text(
            json.dumps(
                {
                    "schema_version": rm.RUNTIME_ARCHIVE_MANIFEST_SCHEMA,
                    "app": "KIGTTS Trainer",
                    "package_type": package_type,
                    "env_name": env_name,
                    "remark": "runtime archive manifest test",
                },
                ensure_ascii=False,
                indent=2,
            ),
            encoding="utf-8",
        )


def _pack_archive(source_dir: Path, archive_path: Path, seven_zip: Path) -> None:
    if archive_path.exists():
        archive_path.unlink()
    proc = subprocess.run(
        [str(seven_zip), "a", "-t7z", "-mx=1", str(archive_path), "*"],
        cwd=source_dir,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=120,
    )
    if proc.returncode != 0:
        raise RuntimeError(proc.stdout)


def _expect_runtime_error(label: str, func, expected: str) -> None:
    try:
        func()
    except RuntimeError as exc:
        if expected not in str(exc):
            raise AssertionError(f"{label}: expected {expected!r}, got {exc!r}") from exc
        return
    raise AssertionError(f"{label}: expected RuntimeError")


def main() -> None:
    trainer_root = Path(__file__).resolve().parents[1]
    os.environ["KGTTS_APP_DIR"] = str(trainer_root)
    seven_zip = rm._resolve_7za()
    if seven_zip is None:
        raise RuntimeError("7za.exe not found")

    temp_root = Path(tempfile.mkdtemp(prefix="kigtts-runtime-archive-test-"))
    try:
        os.environ["KGTTS_USER_DATA"] = str(temp_root / "user-data")
        valid_src = temp_root / "valid"
        wrong_src = temp_root / "wrong"
        missing_src = temp_root / "missing"
        archives = temp_root / "archives"
        archives.mkdir()

        _write_runtime_fixture(valid_src, "piper_runtime", "piper_env")
        _write_runtime_fixture(wrong_src, "voxcpm_runtime", "voxcpm_env")
        _write_runtime_fixture(missing_src, "piper_runtime", "piper_env", with_manifest=False)

        valid_archive = archives / "piper_env.7z"
        wrong_archive = archives / "wrong.7z"
        missing_archive = archives / "missing.7z"
        _pack_archive(valid_src, valid_archive, seven_zip)
        _pack_archive(wrong_src, wrong_archive, seven_zip)
        _pack_archive(missing_src, missing_archive, seven_zip)

        probe, _sources, source_id, source_label, manifest = rm._install_python_runtime_archive(
            source_key="piper_runtime",
            runtime_label="Piper 基础运行时",
            env_dir=temp_root / "user-data" / "runtimes" / "piper_env",
            archive_filename="piper_env.7z",
            extract_dirname="piper_env_extract",
            progress=None,
            force=True,
            probe_python=lambda python_path: {"python_path": str(python_path), "ok": True},
            local_archive_path=valid_archive,
        )
        assert source_id == "local"
        assert "本地文件" in source_label
        assert manifest["package_type"] == "piper_runtime"
        assert Path(probe["python_path"]).exists()

        _expect_runtime_error(
            "wrong package type",
            lambda: rm._install_python_runtime_archive(
                source_key="piper_runtime",
                runtime_label="Piper 基础运行时",
                env_dir=temp_root / "user-data" / "runtimes" / "piper_env",
                archive_filename="piper_env.7z",
                extract_dirname="wrong_extract",
                progress=None,
                force=True,
                probe_python=lambda python_path: {"ok": True},
                local_archive_path=wrong_archive,
            ),
            "不是 Piper 基础运行时",
        )

        _expect_runtime_error(
            "missing manifest",
            lambda: rm._install_python_runtime_archive(
                source_key="piper_runtime",
                runtime_label="Piper 基础运行时",
                env_dir=temp_root / "user-data" / "runtimes" / "piper_env",
                archive_filename="piper_env.7z",
                extract_dirname="missing_extract",
                progress=None,
                force=True,
                probe_python=lambda python_path: {"ok": True},
                local_archive_path=missing_archive,
            ),
            "不是有效的 Piper 基础运行时安装包",
        )
    finally:
        shutil.rmtree(temp_root, ignore_errors=True)

    print("runtime archive manifest tests passed")


if __name__ == "__main__":
    main()
