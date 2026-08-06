#!/usr/bin/env python3
"""Generate portable SHA-256 checksums for source files."""

from __future__ import annotations

import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "CHECKSUMS.sha256"
EXCLUDED_PARTS = {".git", "target", "build", "dist", ".idea", ".vscode", "__pycache__"}
EXCLUDED_NAMES = {OUTPUT.name, ".DS_Store"}
EXCLUDED_SUFFIXES = {".zip", ".log", ".pem", ".key"}


def included(path: Path) -> bool:
    relative = path.relative_to(ROOT)
    return (
        path.is_file()
        and not any(part in EXCLUDED_PARTS for part in relative.parts)
        and path.name not in EXCLUDED_NAMES
        and path.suffix not in EXCLUDED_SUFFIXES
        and not (
            path.name == ".env"
            or (path.name.startswith(".env.") and path.name != ".env.example")
        )
    )


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> None:
    paths = sorted(path for path in ROOT.rglob("*") if included(path))
    lines = [f"{sha256(path)}  {path.relative_to(ROOT).as_posix()}" for path in paths]
    OUTPUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"SUCCESS: wrote {len(lines)} checksums to {OUTPUT.name}")


if __name__ == "__main__":
    main()
