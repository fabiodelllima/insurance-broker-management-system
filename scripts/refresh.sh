#!/usr/bin/env bash
# ---------------------------------------------------------------
# Tests POST /api/v1/auth/refresh
#
# Usage:
#   ./scripts/refresh.sh                  # uses saved refresh token
#   ./scripts/refresh.sh <refresh_token>  # explicit token
#
# Requires a prior ./scripts/login.sh to have saved tokens,
# or pass the refresh token as argument.
# ---------------------------------------------------------------
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/_helpers.sh"

REFRESH_TOKEN="${1:-$(load_token REFRESH_TOKEN)}"

if [ -z "$REFRESH_TOKEN" ]; then
    echo -e "${C_RED}[ERROR] No refresh token. Run ./scripts/login.sh first.${C_RESET}"
    exit 1
fi

BODY=$(jq -n --arg t "$REFRESH_TOKEN" '{refreshToken: $t}')

echo -e "${C_BOLD}Refreshing access token${C_RESET}"
api_request POST /api/v1/auth/refresh "$BODY"

# Save new access token
RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/auth/refresh" \
    -H 'Content-Type: application/json' \
    -d "$BODY")

ACCESS=$(echo "$RESPONSE" | jq -r '.accessToken // empty')
if [ -n "$ACCESS" ]; then
    save_token "ACCESS_TOKEN" "$ACCESS"
    echo -e "${C_GREEN}[OK] New access token saved.${C_RESET}"
fi
