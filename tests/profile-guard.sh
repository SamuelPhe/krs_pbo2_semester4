#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${1:-http://127.0.0.1:8081}"
status="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' "$BASE_URL/profile.php")"
if [[ "$status" != "302" ]]; then
  echo "Expected unauthenticated profile request to redirect (302); got HTTP $status" >&2
  exit 1
fi
echo "PASS: unauthenticated profile requests are redirected to login."
