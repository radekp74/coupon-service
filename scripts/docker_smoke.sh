#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

DOCKER="${DOCKER:-/Applications/Docker.app/Contents/Resources/bin/docker}"
APP_PORT="${APP_PORT:-0}"
COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-coupon-service-verify-$$}"

cleanup() {
  "$DOCKER" compose \
    -p "$COMPOSE_PROJECT_NAME" \
    -f docker-compose.yml \
    down --volumes --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

APP_PORT="$APP_PORT" "$DOCKER" compose \
  -p "$COMPOSE_PROJECT_NAME" \
  -f docker-compose.yml \
  up -d --build --wait --wait-timeout 180

port_mapping="$($DOCKER compose -p "$COMPOSE_PROJECT_NAME" -f docker-compose.yml port app 8080)"
smoke_port="${port_mapping##*:}"
case "$smoke_port" in
  ''|*[!0-9]*) echo "ERROR: could not parse smoke port: $port_mapping" >&2; exit 1 ;;
esac
base_url="http://127.0.0.1:${smoke_port}"
health_url="${base_url}/actuator/health"
health_payload="$(curl --fail --silent --show-error "$health_url")"
printf '%s' "$health_payload" | grep -Fq '"status":"UP"' || {
  echo "ERROR: unexpected health response: $health_payload" >&2
  exit 1
}

headers_file="$(mktemp)"
logs_file="$(mktemp)"
trap 'rm -f "$headers_file" "$logs_file"; cleanup' EXIT

openapi_payload="$(curl --fail --silent --show-error -D "$headers_file" \
  -H 'X-Request-Id: smoke-request-010' "${base_url}/openapi.yaml")"
printf '%s' "$openapi_payload" | grep -Fq 'operationId: createCoupon' || {
  echo "ERROR: canonical OpenAPI is not available from the runtime artifact" >&2
  exit 1
}
printf '%s' "$openapi_payload" | grep -Fq 'operationId: redeemCoupon' || {
  echo "ERROR: canonical OpenAPI does not describe the implemented redemption endpoint" >&2
  exit 1
}
tr -d '\r' < "$headers_file" | grep -Eiq '^X-Request-Id:[[:space:]]*smoke-request-010[[:space:]]*$' || {
  echo "ERROR: runtime did not echo the accepted X-Request-Id" >&2
  cat "$headers_file" >&2
  exit 1
}

swagger_payload="$(curl --location --fail --silent --show-error "${base_url}/swagger-ui")"
printf '%s' "$swagger_payload" | grep -Fiq 'swagger ui' || {
  echo "ERROR: Swagger UI is not available" >&2
  exit 1
}

swagger_config="$(curl --fail --silent --show-error "${base_url}/v3/api-docs/swagger-config")"
printf '%s' "$swagger_config" | grep -Fq '"url":"/openapi.yaml"' || {
  echo "ERROR: Swagger UI is not configured to use canonical /openapi.yaml" >&2
  exit 1
}

: > "$headers_file"
prometheus_payload="$(curl --fail --silent --show-error -D "$headers_file" "${base_url}/actuator/prometheus")"
grep -Eiq '^Content-Type:[[:space:]]*text/plain' "$headers_file" || {
  echo "ERROR: Prometheus endpoint has unexpected content type" >&2
  cat "$headers_file" >&2
  exit 1
}
printf '%s' "$prometheus_payload" | grep -Eq '^# (HELP|TYPE) ' || {
  echo "ERROR: Prometheus endpoint did not return a scrape payload" >&2
  exit 1
}

"$DOCKER" compose -p "$COMPOSE_PROJECT_NAME" -f docker-compose.yml logs --no-color app > "$logs_file"
python3 - "$logs_file" <<'PY'
import json
import sys
from pathlib import Path
lines = Path(sys.argv[1]).read_text(encoding="utf-8", errors="replace").splitlines()
parsed = []
for line in lines:
    candidate = line[line.find("{"):] if "{" in line else ""
    if not candidate:
        continue
    try:
        value = json.loads(candidate)
    except json.JSONDecodeError:
        continue
    if isinstance(value, dict) and "message" in value and "level" in value:
        parsed.append(value)
if not parsed:
    raise SystemExit("ERROR: container console did not contain Spring Boot Logstash JSON lines")
print("SUCCESS: container structured JSON logging detected")
PY

echo "SUCCESS: Docker Compose stack is healthy at $health_url"
echo "SUCCESS: canonical OpenAPI is available at ${base_url}/openapi.yaml"
echo "SUCCESS: Swagger UI is available at ${base_url}/swagger-ui"
echo "SUCCESS: Prometheus scrape is available at ${base_url}/actuator/prometheus"
echo "SUCCESS: X-Request-Id and structured JSON logging runtime contracts are valid"
