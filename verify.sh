#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

DOCKER="${DOCKER:-/Applications/Docker.app/Contents/Resources/bin/docker}"
MAVEN="${MAVEN:-./mvnw}"

resolve_command() {
  local configured="$1"
  if [[ "$configured" == */* ]]; then
    test -x "$configured" || {
      echo "ERROR: executable not found: $configured" >&2
      return 1
    }
    printf '%s\n' "$configured"
  else
    command -v "$configured"
  fi
}

echo "=== DOCUMENTATION GOVERNANCE ==="
python3 scripts/check_documentation.py

echo
echo "=== EMP-002 STATIC BOOTSTRAP CONTRACT ==="
python3 scripts/check_bootstrap.py

echo
echo "=== EMP-003 STATIC CREATE COUPON CONTRACT ==="
python3 scripts/check_emp003.py

echo
echo "=== EMP-004 REFINEMENT CONTRACT ==="
python3 scripts/check_emp004_refinement.py

echo
echo "=== EMP-004 IMPLEMENTATION CONTRACT ==="
python3 scripts/check_emp004.py

echo
echo "=== EMP-006 REFINEMENT CONTRACT ==="
python3 scripts/check_emp006_refinement.py

echo
echo "=== EMP-006 IMPLEMENTATION CONTRACT ==="
python3 scripts/check_emp006.py

echo
echo "=== EMP-007 STATIC OPENAPI AND JAVADOC CONTRACT ==="
python3 scripts/check_emp007.py

echo
echo "=== EMP-008 REFINEMENT CONTRACT ==="
python3 scripts/check_emp008_refinement.py

echo
echo "=== EMP-008 JACOCO IMPLEMENTATION CONTRACT ==="
python3 scripts/check_emp008.py

echo
echo "=== EMP-009 REFINEMENT CONTRACT ==="
python3 scripts/check_emp009_refinement.py

echo
echo "=== EMP-009 IMPLEMENTATION CONTRACT ==="
python3 scripts/check_emp009.py

echo
echo "=== EMP-010 REFINEMENT CONTRACT ==="
python3 scripts/check_emp010_refinement.py

echo
echo "=== SCRIPT SYNTAX ==="
PYCACHE_DIR="$(mktemp -d)"
trap 'rm -rf "$PYCACHE_DIR"' EXIT
PYTHONPYCACHEPREFIX="$PYCACHE_DIR" python3 -m py_compile \
  scripts/check_documentation.py \
  scripts/check_bootstrap.py \
  scripts/check_emp003.py \
  scripts/check_emp004_refinement.py \
  scripts/check_emp004.py \
  scripts/check_emp006_refinement.py \
  scripts/check_emp006.py \
  scripts/check_emp007.py \
  scripts/check_emp008_refinement.py \
  scripts/check_emp008.py \
  scripts/check_emp009_refinement.py \
  scripts/check_emp009.py \
  scripts/check_emp010_refinement.py \
  scripts/generate_checksums.py
bash -n verify.sh scripts/package_source.sh scripts/docker_smoke.sh mvnw

echo
echo "=== MAKEFILE CONTRACT ==="
grep -Fqx 'DOCKER ?= /Applications/Docker.app/Contents/Resources/bin/docker' Makefile
grep -Fqx 'MAVEN ?= ./mvnw' Makefile
grep -Fqx 'SOURCE_EXPORT_DIR ?= $(HOME)/Downloads' Makefile
grep -Eq '^bootstrap-check:$' Makefile
grep -Eq '^emp003-check:$' Makefile
grep -Eq '^emp004-refinement-check:$' Makefile
grep -Eq '^emp004-check:$' Makefile
grep -Eq '^emp006-refinement-check:$' Makefile
grep -Eq '^emp006-check:$' Makefile
grep -Eq '^emp007-check:$' Makefile
grep -Eq '^emp008-refinement-check:$' Makefile
grep -Eq '^emp008-check:$' Makefile
grep -Eq '^emp008-report-check:$' Makefile
grep -Eq '^emp009-refinement-check:$' Makefile
grep -Eq '^emp009-check:$' Makefile
grep -Eq '^emp010-refinement-check:$' Makefile
grep -Eq '^docker-check:$' Makefile
grep -Eq '^compose-config: docker-check$' Makefile
grep -Eq '^docker-build: compose-config$' Makefile
grep -Eq '^docker-up: compose-config$' Makefile
grep -Eq '^docker-down: docker-check$' Makefile
grep -Eq '^docker-smoke: docker-check$' Makefile
grep -Eq '^maven-verify: java-check docker-check$' Makefile
grep -Eq '^export-source:$' Makefile
make -n bootstrap-check >/dev/null
make -n emp003-check >/dev/null
make -n emp004-refinement-check >/dev/null
make -n emp004-check >/dev/null
make -n emp006-refinement-check >/dev/null
make -n emp006-check >/dev/null
make -n emp007-check >/dev/null
make -n emp008-refinement-check >/dev/null
make -n emp008-check >/dev/null
make -n emp008-report-check >/dev/null
make -n emp009-refinement-check >/dev/null
make -n emp009-check >/dev/null
make -n emp010-refinement-check >/dev/null
make -n docker-check >/dev/null
make -n compose-config >/dev/null
make -n docker-build >/dev/null
make -n docker-up >/dev/null
make -n docker-down >/dev/null
make -n docker-smoke >/dev/null
make -n maven-verify >/dev/null
make -n export-source SOURCE_EXPORT_DIR=/tmp >/dev/null
echo "SUCCESS: Makefile contracts are valid"

echo
echo "=== JAVA 21 ==="
JAVA_BIN="$(resolve_command java)"
JAVA_VERSION="$($JAVA_BIN -version 2>&1 | awk -F '"' '/version/ { print $2; exit }')"
JAVA_MAJOR="${JAVA_VERSION%%.*}"
if [[ "$JAVA_MAJOR" != "21" ]]; then
  echo "ERROR: Java 21 is required, found: $JAVA_VERSION" >&2
  exit 1
fi
echo "SUCCESS: Java $JAVA_VERSION"

echo
echo "=== DOCKER DAEMON ==="
DOCKER_BIN="$(resolve_command "$DOCKER")"
"$DOCKER_BIN" version

echo
echo "=== DOCKER COMPOSE CONTRACT ==="
"$DOCKER_BIN" compose -p coupon-service-verify-config -f docker-compose.yml config --quiet
echo "SUCCESS: docker-compose.yml is valid"

echo
echo "=== MAVEN CLEAN VERIFY ==="
"$MAVEN" -B -ntp clean verify

test -s target/site/jacoco/index.html || {
  echo "ERROR: JaCoCo HTML report was not produced" >&2
  exit 1
}
test -s target/site/jacoco/jacoco.xml || {
  echo "ERROR: JaCoCo XML report was not produced" >&2
  exit 1
}
echo "SUCCESS: JaCoCo HTML and XML reports are present"

echo
echo "=== EMP-008 MEASURED COVERAGE CONTRACT ==="
python3 scripts/check_emp008.py --report target/site/jacoco/jacoco.xml --self-test

test -f target/coupon-service-0.0.1-SNAPSHOT.jar || {
  echo "ERROR: expected Spring Boot artifact was not produced" >&2
  exit 1
}

jar tf target/coupon-service-0.0.1-SNAPSHOT.jar   | grep -Fq 'BOOT-INF/classes/static/openapi.yaml' || {
    echo "ERROR: canonical OpenAPI was not packaged into the Spring Boot artifact" >&2
    exit 1
  }
echo "SUCCESS: canonical OpenAPI is present in the application artifact"

echo
echo "=== CONTAINER BUILD AND RUNTIME SMOKE ==="
DOCKER="$DOCKER_BIN" \
APP_PORT=0 \
COMPOSE_PROJECT_NAME="coupon-service-verify-$$" \
  bash scripts/docker_smoke.sh

if command -v git >/dev/null 2>&1 && git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo
  echo "=== GIT WHITESPACE CHECK ==="
  git diff --check
else
  echo
  echo "INFO: brak repozytorium Git — pominięto git diff --check"
fi

echo
echo "SUCCESS: repository verification passed"
