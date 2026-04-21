#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
USERNAME="${OPENAPI_SMOKE_USERNAME:-admin01}"
PASSWORD="${OPENAPI_SMOKE_PASSWORD:-ChangeMe123!}"
MAX_EXAMPLES="${OPENAPI_SMOKE_MAX_EXAMPLES:-3}"
REPORT_DIR="${OPENAPI_SMOKE_REPORT_DIR:-test-results/schemathesis}"

# Keep the first Schemathesis pass low-side-effect. Mutating endpoints, file upload,
# AI execution, and path-id fuzzing are covered by dedicated integration/E2E tests.
OPERATION_ID_REGEX="${OPENAPI_SMOKE_OPERATION_ID_REGEX:-^(listTickets|currentUser|info|dashboard|listDocuments|pending)$}"

mkdir -p "$REPORT_DIR"

TOKEN="$(
  curl -sS \
    -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}" \
    | python3 -c 'import json, sys; print(json.load(sys.stdin)["data"]["accessToken"])'
)"

schemathesis run "$BASE_URL/api-docs" \
  --url "$BASE_URL" \
  --include-operation-id-regex "$OPERATION_ID_REGEX" \
  --header "Authorization: Bearer $TOKEN" \
  --mode positive \
  --phases fuzzing \
  --max-examples "$MAX_EXAMPLES" \
  --checks not_a_server_error,status_code_conformance,content_type_conformance,response_schema_conformance \
  --request-timeout 5 \
  --max-failures 1 \
  --report junit \
  --report-dir "$REPORT_DIR" \
  --no-color
