#!/usr/bin/env bash
# ---------------------------------------------------------------
# Tests POST /api/v1/auth/login
#
# Usage:
#   ./scripts/login.sh                          # default test user
#   ./scripts/login.sh user@email.com password   # custom credentials
#
# On success, saves accessToken and refreshToken to .tokens.tmp
# so that other scripts (refresh.sh, protected.sh) can use them.
# ---------------------------------------------------------------
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/_helpers.sh"

EMAIL="${1:-admin@ibms.test}"
PASSWORD="${2:-password123}"

BODY=$(jq -n --arg e "$EMAIL" --arg p "$PASSWORD" \
    '{email: $e, password: $p}')

echo -e "${C_BOLD}Logging in as ${C_CYAN}${EMAIL}${C_RESET}"
api_request POST /api/v1/auth/login "$BODY"

# Attempt to extract and save tokens for use by other scripts
RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    -d "$BODY")

ACCESS=$(echo "$RESPONSE" | jq -r '.accessToken // empty')
REFRESH=$(echo "$RESPONSE" | jq -r '.refreshToken // empty')

if [ -n "$ACCESS" ]; then
    save_token "ACCESS_TOKEN" "$ACCESS"
    save_token "REFRESH_TOKEN" "$REFRESH"
    echo -e "${C_GREEN}[OK] Tokens saved. Other scripts will use them automatically.${C_RESET}"
fi
