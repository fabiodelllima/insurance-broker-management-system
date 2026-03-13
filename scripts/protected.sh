#!/usr/bin/env bash
# ---------------------------------------------------------------
# Tests a protected endpoint using the saved access token.
#
# Usage:
#   ./scripts/protected.sh                   # default: GET /api/v1/brokers
#   ./scripts/protected.sh GET /api/v1/some  # custom method and path
#
# Demonstrates that authenticated requests pass the security chain
# and unauthenticated requests are rejected with 401.
# ---------------------------------------------------------------
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/_helpers.sh"

METHOD="${1:-GET}"
PATH_URL="${2:-/api/v1/brokers}"

# Test 1: without token — expect 401
echo -e "${C_BOLD}Test 1: Without token (expect 401)${C_RESET}"
api_request "$METHOD" "$PATH_URL"

# Test 2: with saved access token
ACCESS_TOKEN="$(load_token ACCESS_TOKEN)"
if [ -z "$ACCESS_TOKEN" ]; then
    echo -e "${C_YELLOW}[WARN] No access token saved. Run ./scripts/login.sh first.${C_RESET}"
    exit 0
fi

echo -e "${C_BOLD}Test 2: With valid token${C_RESET}"
export ACCESS_TOKEN
api_request "$METHOD" "$PATH_URL"
