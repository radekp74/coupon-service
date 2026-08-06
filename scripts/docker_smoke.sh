#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

DOCKER="${DOCKER:-/Applications/Docker.app/Contents/Resources/bin/docker}"
APP_PORT="${APP_PORT:-18080}"
COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-coupon-service-verify}"

cleanup() {
  "$DOCKER" compose \
    -p "$COMPOSE_PROJECT_NAME" \
    -f docker-compose.yml \
    down --volumes --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

cleanup

APP_PORT="$APP_PORT" "$DOCKER" compose \
  -p "$COMPOSE_PROJECT_NAME" \
  -f docker-compose.yml \
  up -d --build --wait --wait-timeout 180

health_url="http://localhost:${APP_PORT}/actuator/health"
health_payload="$(curl --fail --silent --show-error "$health_url")"
printf '%s' "$health_payload" | grep -Fq '"status":"UP"' || {
  echo "ERROR: unexpected health response: $health_payload" >&2
  exit 1
}

echo "SUCCESS: Docker Compose stack is healthy at $health_url"
