#!/usr/bin/env bash
set -euo pipefail

ACTIVE_FILE="${XDG_CACHE_HOME:-$HOME/.cache}/haval-aa-cluster-active"
NOTE="${*:-incident}"

[[ -f "$ACTIVE_FILE" ]] || { echo "No AA cluster capture has been started." >&2; exit 1; }
OUTPUT_DIR="$(sed -n 's/^output=//p' "$ACTIVE_FILE" | head -n 1)"
STATE="$(sed -n 's/^state=//p' "$ACTIVE_FILE" | head -n 1)"
[[ -n "$OUTPUT_DIR" && -d "$OUTPUT_DIR" ]] || { echo "Invalid active capture metadata." >&2; exit 1; }

printf '%s epoch=%s state=%s note=%q\n' \
  "$(date --iso-8601=milliseconds)" "$(date +%s%3N)" "$STATE" "$NOTE" \
  | tee -a "$OUTPUT_DIR/markers.log"
