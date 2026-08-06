#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

python3 scripts/check_documentation.py
python3 scripts/check_bootstrap.py
python3 scripts/check_emp003.py
python3 scripts/check_emp004_refinement.py
python3 scripts/check_emp004.py
python3 scripts/check_emp006_refinement.py
python3 scripts/check_emp007.py
python3 scripts/generate_checksums.py

OUTPUT_DIR="${SOURCE_EXPORT_DIR:-$ROOT/dist}"
mkdir -p "$OUTPUT_DIR"
OUTPUT_DIR="$(cd "$OUTPUT_DIR" && pwd)"

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
GIT_SUFFIX=""
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  GIT_SUFFIX="-$(git rev-parse --short=12 HEAD)"
fi
PACKAGE="coupon-service-source-${STAMP}${GIT_SUFFIX}"
ARCHIVE="$OUTPUT_DIR/$PACKAGE.zip"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

mkdir -p "$TMP/$PACKAGE"

find . -type f \
  ! -path './.git/*' \
  ! -path './target/*' \
  ! -path './build/*' \
  ! -path './dist/*' \
  ! -path './.idea/*' \
  ! -path './.vscode/*' \
  ! -path './.settings/*' \
  ! -path './.gradle/*' \
  ! -path './coverage/*' \
  ! -path '*/__pycache__/*' \
  ! -name '.DS_Store' \
  ! -name '*.zip' \
  ! -name '*.log' \
  ! -name '*.pem' \
  ! -name '*.key' \
  ! -name 'hs_err_pid*.log' \
  -print0 \
  | while IFS= read -r -d '' file; do
      base="$(basename "$file")"
      if [[ "$base" == ".env" || ( "$base" == .env.* && "$base" != ".env.example" ) ]]; then
        continue
      fi
      mkdir -p "$TMP/$PACKAGE/$(dirname "$file")"
      cp -p "$file" "$TMP/$PACKAGE/$file"
    done

(
  cd "$TMP"
  zip -qr "$ARCHIVE" "$PACKAGE"
)

SHA256="$(shasum -a 256 "$ARCHIVE" | awk '{print $1}')"
printf 'SUCCESS: source archive created\n'
printf 'Path: %s\n' "$ARCHIVE"
printf 'SHA-256: %s\n' "$SHA256"
