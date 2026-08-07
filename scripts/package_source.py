#!/usr/bin/env python3
"""Create a byte-reproducible source ZIP from immutable blobs of a clean Git commit."""
from __future__ import annotations

import hashlib
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
CHECKSUM_NAME = "CHECKSUMS.sha256"
FIXED_TIME = (1980, 1, 1, 0, 0, 0)
FORBIDDEN_PARTS = {
    ".git", "target", "build", "dist", ".idea", ".vscode", ".settings", ".gradle",
    "coverage", "__pycache__",
}
FORBIDDEN_SUFFIXES = {".zip", ".log", ".pem", ".key", ".p12", ".pfx", ".crt", ".cer"}


def run(*args: str, input_bytes: bytes | None = None) -> bytes:
    return subprocess.check_output(args, cwd=ROOT, input=input_bytes)


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def forbidden(path: str) -> bool:
    parts = PurePosixPath(path).parts
    name = parts[-1]
    return (
        any(part in FORBIDDEN_PARTS for part in parts)
        or name == ".DS_Store"
        or Path(name).suffix.lower() in FORBIDDEN_SUFFIXES
        or name == "CODEX_PROMPT.md"
        or name == ".env"
        or (name.startswith(".env.") and name != ".env.example")
        or name in {"id_rsa", "id_ed25519"}
    )


def tracked_entries() -> dict[str, tuple[int, str]]:
    """Return path -> (normalized permission, Git blob id) for ordinary tracked files."""
    raw = run("git", "ls-files", "--stage", "-z")
    result: dict[str, tuple[int, str]] = {}
    for record in raw.split(b"\0"):
        if not record:
            continue
        metadata, path_bytes = record.split(b"\t", 1)
        mode_bytes, object_id_bytes, stage_bytes = metadata.split(b" ", 2)
        mode = mode_bytes.decode("ascii")
        stage = stage_bytes.decode("ascii")
        path = path_bytes.decode("utf-8")
        if stage != "0":
            raise SystemExit(f"ERROR: unmerged tracked path cannot be packaged: {path}")
        if mode not in {"100644", "100755"}:
            raise SystemExit(f"ERROR: unsupported tracked file mode {mode}: {path}")
        object_id = object_id_bytes.decode("ascii")
        if not re.fullmatch(r"[0-9a-f]{40,64}", object_id):
            raise SystemExit(f"ERROR: malformed Git object id for tracked file: {path}")
        result[path] = (0o755 if mode == "100755" else 0o644, object_id)
    return result


def blob_bytes(object_id: str) -> bytes:
    return run("git", "cat-file", "blob", object_id)


def parse_checksums(data: bytes) -> dict[str, str]:
    try:
        text = data.decode("utf-8")
    except UnicodeDecodeError as exc:
        raise SystemExit("ERROR: CHECKSUMS.sha256 must be UTF-8") from exc
    entries: dict[str, str] = {}
    for line in text.splitlines():
        match = re.fullmatch(r"([0-9a-f]{64})  (.+)", line)
        if not match:
            raise SystemExit(f"ERROR: malformed CHECKSUMS.sha256 line: {line!r}")
        digest, path = match.groups()
        if path in entries:
            raise SystemExit(f"ERROR: duplicate checksum entry: {path}")
        entries[path] = digest
    return entries


def verify_source(tracked: dict[str, tuple[int, str]]) -> tuple[list[str], dict[str, bytes]]:
    if run("git", "status", "--porcelain=v1", "-z"):
        raise SystemExit("ERROR: deterministic source export requires a clean working tree")
    if CHECKSUM_NAME not in tracked:
        raise SystemExit("ERROR: CHECKSUMS.sha256 is not tracked")
    bad = sorted(path for path in tracked if forbidden(path))
    if bad:
        raise SystemExit("ERROR: forbidden tracked source file(s): " + ", ".join(bad))

    blobs = {path: blob_bytes(entry[1]) for path, entry in tracked.items()}
    included_without_manifest = sorted(path for path in tracked if path != CHECKSUM_NAME)
    manifest = parse_checksums(blobs[CHECKSUM_NAME])
    if set(manifest) != set(included_without_manifest):
        missing = sorted(set(included_without_manifest) - set(manifest))
        stale = sorted(set(manifest) - set(included_without_manifest))
        raise SystemExit(
            f"ERROR: checksum manifest does not match tracked source; missing={missing} stale={stale}"
        )
    for path in included_without_manifest:
        actual = sha256_bytes(blobs[path])
        if actual != manifest[path]:
            raise SystemExit(f"ERROR: stale checksum for tracked source file: {path}")
    return sorted(tracked), blobs


def main() -> int:
    if run("git", "rev-parse", "--is-inside-work-tree").strip() != b"true":
        raise SystemExit("ERROR: source export requires a Git working tree")
    commit = run("git", "rev-parse", "HEAD").decode("ascii").strip()
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise SystemExit("ERROR: could not resolve full Git commit SHA")

    tracked = tracked_entries()
    paths, blobs = verify_source(tracked)
    package = f"coupon-service-source-{commit[:12]}"
    output_dir = Path(os.environ.get("SOURCE_EXPORT_DIR", str(ROOT / "dist"))).expanduser().resolve()
    output_dir.mkdir(parents=True, exist_ok=True)
    archive = output_dir / f"{package}.zip"
    if archive.exists():
        archive.unlink()

    with zipfile.ZipFile(
        archive,
        "w",
        compression=zipfile.ZIP_DEFLATED,
        compresslevel=9,
        strict_timestamps=True,
    ) as zf:
        for path in paths:
            permission = tracked[path][0]
            info = zipfile.ZipInfo(f"{package}/{path}", FIXED_TIME)
            info.create_system = 3
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = (permission & 0xFFFF) << 16
            zf.writestr(info, blobs[path], compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)

    digest = sha256_file(archive)
    print("SUCCESS: deterministic source archive created")
    print(f"Path: {archive}")
    print(f"SHA-256: {digest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
