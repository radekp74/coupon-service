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

openapi_payload="$(curl --fail --silent --show-error "${base_url}/openapi.yaml")"
printf '%s' "$openapi_payload" | grep -Fq 'operationId: createCoupon' || {
  echo "ERROR: canonical OpenAPI is not available from the runtime artifact" >&2
  exit 1
}
if printf '%s' "$openapi_payload" | grep -Fq '/api/v1/coupons/{code}/redemptions'; then
  echo "ERROR: OpenAPI describes redemption before the endpoint exists" >&2
  exit 1
fi

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

echo "SUCCESS: Docker Compose stack is healthy at $health_url"
echo "SUCCESS: canonical OpenAPI is available at ${base_url}/openapi.yaml"
echo "SUCCESS: Swagger UI is available at ${base_url}/swagger-ui"
