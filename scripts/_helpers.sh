#!/usr/bin/env bash
# ---------------------------------------------------------------
# Shared helpers for API test scripts.
# Source this file — do not execute it directly.
#
# Provides:
#   api_request METHOD URL [BODY]
#     Executes an HTTP request and prints a formatted response
#     similar to Insomnia/Postman: status line, headers, and
#     pretty-printed JSON body with ANSI colors.
#
#   save_token KEY VALUE
#     Persists a token to .tokens.tmp so other scripts can read it.
#
#   load_token KEY
#     Reads a token saved by save_token.
#
# Requirements: curl, jq
# ---------------------------------------------------------------

BASE_URL="${API_BASE_URL:-http://localhost:8080}"
TOKEN_FILE="$(dirname "${BASH_SOURCE[0]}")/.tokens.tmp"

# ANSI color codes
C_RESET='\033[0m'
C_GREEN='\033[32m'
C_RED='\033[31m'
C_YELLOW='\033[33m'
C_CYAN='\033[36m'
C_DIM='\033[2m'
C_BOLD='\033[1m'

# Checks that curl and jq are available.
check_deps() {
    for cmd in curl jq; do
        if ! command -v "$cmd" > /dev/null 2>&1; then
            echo -e "${C_RED}[ERROR] '$cmd' is required but not installed.${C_RESET}"
            exit 1
        fi
    done
}

# Formats and prints the HTTP response with colors.
# Usage: api_request METHOD PATH [JSON_BODY]
api_request() {
    local method="$1"
    local path="$2"
    local body="${3:-}"
    local url="${BASE_URL}${path}"

    check_deps

    # Build curl arguments
    local -a curl_args=(
        -s
        -w '\n__STATUS__:%{http_code}\n__TIME__:%{time_total}'
        -D -
        -X "$method"
        -H 'Content-Type: application/json'
    )

    # Add Authorization header if ACCESS_TOKEN is set
    if [ -n "${ACCESS_TOKEN:-}" ]; then
        curl_args+=(-H "Authorization: Bearer $ACCESS_TOKEN")
    fi

    # Add body if provided
    if [ -n "$body" ]; then
        curl_args+=(-d "$body")
    fi

    # Execute request and capture full output
    local raw
    raw=$(curl "${curl_args[@]}" "$url")

    # Extract status and time from the appended metadata
    local status_code elapsed
    status_code=$(echo "$raw" | grep '^__STATUS__:' | cut -d: -f2)
    elapsed=$(echo "$raw" | grep '^__TIME__:' | cut -d: -f2)

    # Remove metadata lines from raw output
    raw=$(echo "$raw" | grep -v '^__STATUS__:' | grep -v '^__TIME__:')

    # Split on the first blank line: headers above, body below
    local headers body_json
    headers=$(echo "$raw" | sed '/^\r*$/q')
    body_json=$(echo "$raw" | sed '1,/^\r*$/d')

    # Color the status code
    local status_color="$C_GREEN"
    if [ "${status_code:-0}" -ge 400 ] 2>/dev/null; then
        status_color="$C_RED"
    elif [ "${status_code:-0}" -ge 300 ] 2>/dev/null; then
        status_color="$C_YELLOW"
    fi

    # Print formatted output
    echo ""
    echo -e "${C_BOLD}${C_CYAN}${method} ${path}${C_RESET}"
    echo -e "${C_DIM}────────────────────────────────────────${C_RESET}"
    echo -e "${C_BOLD}Status:${C_RESET}  ${status_color}${status_code}${C_RESET}    ${C_DIM}(${elapsed}s)${C_RESET}"
    echo ""

    # Print response headers (dimmed)
    echo -e "${C_DIM}${headers}${C_RESET}"
    echo ""

    # Print formatted JSON body
    if [ -n "$body_json" ]; then
        echo -e "${C_BOLD}Body:${C_RESET}"
        echo "$body_json" | jq . 2>/dev/null || echo "$body_json"
    fi

    echo -e "${C_DIM}────────────────────────────────────────${C_RESET}"
    echo ""
}

save_token() {
    local key="$1"
    local value="$2"
    touch "$TOKEN_FILE"
    grep -v "^${key}=" "$TOKEN_FILE" > "${TOKEN_FILE}.tmp" 2>/dev/null || true
    echo "${key}=${value}" >> "${TOKEN_FILE}.tmp"
    mv "${TOKEN_FILE}.tmp" "$TOKEN_FILE"
}

load_token() {
    local key="$1"
    if [ -f "$TOKEN_FILE" ]; then
        grep "^${key}=" "$TOKEN_FILE" 2>/dev/null | cut -d= -f2- || true
    fi
}
