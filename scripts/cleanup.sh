#!/usr/bin/env bash
# ---------------------------------------------------------------
# Removes saved tokens. Run between test sessions.
# ---------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
rm -f "$SCRIPT_DIR/.tokens.tmp"
echo "Tokens cleared."
