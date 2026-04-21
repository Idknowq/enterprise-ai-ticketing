#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
RESULT_DIR="$ROOT_DIR/test-results/release-check"
BACKEND_URL="${BACKEND_URL:-http://127.0.0.1:8080}"
FRONTEND_URL="${FRONTEND_URL:-http://127.0.0.1:3000}"
RUN_PERF="${RUN_PERF:-1}"
RUN_SECURITY="${RUN_SECURITY:-0}"
RUN_ZAP="${RUN_ZAP:-0}"
RUN_VIDEO="${RUN_VIDEO:-1}"
START_STACK="${START_STACK:-1}"
KEEP_SERVERS="${KEEP_SERVERS:-0}"

BACKEND_PID=""
FRONTEND_PID=""

log() {
  printf "\n[%s] %s\n" "$(date '+%H:%M:%S')" "$*"
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

wait_for_url() {
  local url="$1"
  local name="$2"
  local attempts="${3:-60}"

  for _ in $(seq 1 "$attempts"); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      log "$name is ready: $url"
      return 0
    fi
    sleep 2
  done

  echo "Timed out waiting for $name: $url" >&2
  return 1
}

cleanup() {
  if [[ "$KEEP_SERVERS" == "1" ]]; then
    log "KEEP_SERVERS=1, leaving started servers running"
    return 0
  fi

  if [[ -n "$FRONTEND_PID" ]] && kill -0 "$FRONTEND_PID" >/dev/null 2>&1; then
    log "Stopping frontend pid $FRONTEND_PID"
    kill "$FRONTEND_PID" >/dev/null 2>&1 || true
  fi

  if [[ -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" >/dev/null 2>&1; then
    log "Stopping backend pid $BACKEND_PID"
    kill "$BACKEND_PID" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

mkdir -p "$RESULT_DIR"

log "Checking required local tools"
require_command mvn
require_command npm
require_command curl
require_command python3
require_command schemathesis

if [[ "$START_STACK" == "1" ]]; then
  require_command docker
fi

if [[ "$RUN_PERF" == "1" ]]; then
  require_command k6
fi

if [[ "$RUN_SECURITY" == "1" ]]; then
  require_command trivy
fi

if [[ "$RUN_ZAP" == "1" ]] && [[ ! -x "${ZAP_PATH:-/Applications/ZAP.app/Contents/MacOS/zap.sh}" ]]; then
  echo "Missing ZAP executable: ${ZAP_PATH:-/Applications/ZAP.app/Contents/MacOS/zap.sh}" >&2
  exit 1
fi

log "Running backend unit and WebMvc tests"
(cd "$BACKEND_DIR" && mvn test)

log "Running frontend unit, component, and MSW tests"
(cd "$FRONTEND_DIR" && npm test)

log "Running frontend lint"
(cd "$FRONTEND_DIR" && npm run lint)

log "Running frontend typecheck"
(cd "$FRONTEND_DIR" && npm run typecheck)

log "Building frontend production bundle"
(cd "$FRONTEND_DIR" && npm run build)

if [[ "$START_STACK" == "1" ]]; then
  log "Starting backend infrastructure with Docker Compose"
  (cd "$ROOT_DIR" && docker compose up -d postgres redis qdrant rabbitmq temporal-postgresql temporal jaeger otel-collector)
fi

if curl -fsS "$BACKEND_URL/api/platform/info" >/dev/null 2>&1; then
  log "Using existing backend at $BACKEND_URL"
else
  log "Starting backend application"
  (cd "$BACKEND_DIR" && mvn spring-boot:run >"$RESULT_DIR/backend.log" 2>&1) &
  BACKEND_PID="$!"
fi
wait_for_url "$BACKEND_URL/api/platform/info" "backend"

if curl -fsS "$FRONTEND_URL/login" >/dev/null 2>&1; then
  log "Using existing frontend at $FRONTEND_URL"
else
  log "Starting frontend production server"
  (cd "$FRONTEND_DIR" && npm run start -- -H 127.0.0.1 -p 3000 >"$RESULT_DIR/frontend.log" 2>&1) &
  FRONTEND_PID="$!"
fi
wait_for_url "$FRONTEND_URL/login" "frontend"

log "Running OpenAPI Schemathesis smoke"
(cd "$ROOT_DIR" && BASE_URL="$BACKEND_URL" ./scripts/openapi-smoke.sh)

log "Running Playwright E2E smoke"
(cd "$FRONTEND_DIR" && PLAYWRIGHT_BASE_URL="$FRONTEND_URL" npm run test:e2e)

if [[ "$RUN_VIDEO" == "1" ]]; then
  log "Recording frontend release tour video"
  rm -rf "$FRONTEND_DIR/test-results/release-video"
  (cd "$FRONTEND_DIR" && PLAYWRIGHT_BASE_URL="$FRONTEND_URL" npm run test:e2e:video)
  log "Video output directory: $FRONTEND_DIR/test-results/release-video"
fi

if [[ "$RUN_PERF" == "1" ]]; then
  log "Running k6 performance smoke"
  (cd "$ROOT_DIR" && BASE_URL="$BACKEND_URL" k6 run scripts/k6-smoke.js)
fi

if [[ "$RUN_SECURITY" == "1" ]]; then
  log "Running Trivy filesystem vulnerability smoke"
  (cd "$ROOT_DIR" && trivy fs --scanners vuln --severity HIGH,CRITICAL --ignore-unfixed --exit-code 1 .)
fi

if [[ "$RUN_ZAP" == "1" ]]; then
  log "Running ZAP quick baseline scan"
  ZAP_BIN="${ZAP_PATH:-/Applications/ZAP.app/Contents/MacOS/zap.sh}"
  ZAP_TARGET_URL="${ZAP_TARGET_URL:-$FRONTEND_URL/login}"
  ZAP_REPORT="$RESULT_DIR/zap-quick.html"
  ZAP_LOG="$RESULT_DIR/zap-quick.log"
  "$ZAP_BIN" -cmd -quickurl "$ZAP_TARGET_URL" -quickout "$ZAP_REPORT" -quickprogress 2>&1 | tee "$ZAP_LOG"
  if grep -E "Cannot attack|Unable to attack|无法攻击|找不到主机" "$ZAP_LOG" >/dev/null 2>&1; then
    echo "ZAP did not scan the target successfully. See $ZAP_LOG" >&2
    exit 1
  fi
  log "ZAP report: $RESULT_DIR/zap-quick.html"
fi

log "Release check passed"
