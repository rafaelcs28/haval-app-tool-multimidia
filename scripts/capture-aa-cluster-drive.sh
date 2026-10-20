#!/usr/bin/env bash
set -euo pipefail

TARGET="haval-central"
DURATION_SECONDS=7200
WAIT_SECONDS=1800
OUTPUT_ROOT="${OUTPUT_ROOT:-/root/projects/haval-impulse/test-artifacts/aa-cluster-drive}"
ACTIVE_FILE="${XDG_CACHE_HOME:-$HOME/.cache}/haval-aa-cluster-active"

usage() {
  cat <<'EOF'
Usage: capture-aa-cluster-drive.sh [options]

Options:
  --target HOST          Tailscale host or IPv4 address (default: haval-central)
  --duration SECONDS     Capture duration after connection (default: 7200)
  --wait SECONDS         Maximum wait for the head unit (default: 1800)
  --output-root PATH     Root directory for captures
  -h, --help             Show this help
EOF
}

while (($#)); do
  case "$1" in
    --target)
      TARGET="${2:?missing target}"
      shift 2
      ;;
    --duration)
      DURATION_SECONDS="${2:?missing duration}"
      shift 2
      ;;
    --wait)
      WAIT_SECONDS="${2:?missing wait duration}"
      shift 2
      ;;
    --output-root)
      OUTPUT_ROOT="${2:?missing output root}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

[[ "$DURATION_SECONDS" =~ ^[1-9][0-9]*$ ]] || { echo "Invalid duration" >&2; exit 2; }
[[ "$WAIT_SECONDS" =~ ^[1-9][0-9]*$ ]] || { echo "Invalid wait duration" >&2; exit 2; }

for command in tailscale jq socat timeout gzip; do
  command -v "$command" >/dev/null || { echo "Missing command: $command" >&2; exit 1; }
done

resolve_target_ip() {
  if [[ "$TARGET" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    printf '%s\n' "$TARGET"
    return
  fi

  tailscale status --json | jq -r --arg target "$TARGET" '
    [.Peer[]
      | select(
          (.HostName | ascii_downcase) == ($target | ascii_downcase)
          or (.DNSName | split(".")[0] | ascii_downcase) == ($target | ascii_downcase)
        )
      | .TailscaleIPs[]
      | select(test("^[0-9.]+$"))][0] // empty
  '
}

IP="$(resolve_target_ip)"
[[ -n "$IP" ]] || { echo "Unable to resolve Tailscale target: $TARGET" >&2; exit 1; }

SESSION_ID="$(date +%Y%m%d-%H%M%S)"
OUTPUT_DIR="$OUTPUT_ROOT/$SESSION_ID"
mkdir -p "$OUTPUT_DIR" "$(dirname "$ACTIVE_FILE")"
chmod 700 "$OUTPUT_DIR"
printf 'output=%s\nstate=waiting\ntarget=%s\nip=%s\n' "$OUTPUT_DIR" "$TARGET" "$IP" > "$ACTIVE_FILE"
chmod 600 "$ACTIVE_FILE"

LOGCAT_PID=""
PROBE_PID=""

finish() {
  local status=$?
  trap - EXIT INT TERM
  [[ -z "$LOGCAT_PID" ]] || kill "$LOGCAT_PID" 2>/dev/null || true
  [[ -z "$PROBE_PID" ]] || kill "$PROBE_PID" 2>/dev/null || true

  if [[ -f "$OUTPUT_DIR/logcat-all.txt" ]]; then
    grep -aEi \
      'AaClusterCtrl|ImpulseAaCluster|ClusterPersist|CLUSTER|frame stall|frame_stall_recovery|MediaCodec|CCodec|ACodec|SurfaceFlinger|androidauto|FATAL EXCEPTION|ANR in' \
      "$OUTPUT_DIR/logcat-all.txt" > "$OUTPUT_DIR/logcat-focused.txt" || true
    gzip -f "$OUTPUT_DIR/logcat-all.txt"
  fi

  printf 'output=%s\nstate=finished\ntarget=%s\nip=%s\nfinished_at=%s\n' \
    "$OUTPUT_DIR" "$TARGET" "$IP" "$(date --iso-8601=seconds)" > "$ACTIVE_FILE"
  echo "Capture saved at: $OUTPUT_DIR"
  exit "$status"
}
trap finish EXIT INT TERM

port_open() {
  timeout 3 bash -c "exec 3<>/dev/tcp/$IP/23" 2>/dev/null
}

deadline=$((SECONDS + WAIT_SECONDS))
echo "Waiting for $TARGET ($IP) and Telnet port 23..."
until tailscale ping -c 1 "$IP" >/dev/null 2>&1 && port_open; do
  if ((SECONDS >= deadline)); then
    echo "Timed out waiting for $TARGET" >&2
    exit 1
  fi
  sleep 5
done

printf 'output=%s\nstate=capturing\ntarget=%s\nip=%s\nstarted_at=%s\n' \
  "$OUTPUT_DIR" "$TARGET" "$IP" "$(date --iso-8601=seconds)" > "$ACTIVE_FILE"

cat > "$OUTPUT_DIR/session.txt" <<EOF
session_id=$SESSION_ID
coordinator_started_at=$(date --iso-8601=seconds)
target=$TARGET
tailscale_ip=$IP
duration_seconds=$DURATION_SECONDS
expected_package=br.com.redesurftank.havalshisuku
expected_version_code=74118
expected_version_name=1.0.0.74.118-preview
watchdog_event=aa_cluster_frame_stall_recovery
watchdog_log_tag=AaClusterCtrl
integration_log_tag=ImpulseAaCluster
EOF

tailscale ping -c 3 "$IP" > "$OUTPUT_DIR/tailscale-ping-start.txt" 2>&1 || true

run_telnet_once() {
  local output_file=$1
  local remote_command=$2
  {
    printf '%s\r\n' "$remote_command"
    sleep 3
    printf 'exit\r\n'
  } | timeout 30 socat - "TCP:$IP:23,connect-timeout=8" > "$output_file" 2>&1 || true
}

snapshot_command='echo ===TIME===; date; uptime; echo ===VERSION===; dumpsys package br.com.redesurftank.havalshisuku 2>/dev/null | grep -E "versionCode=|versionName=" | head -n 4; echo ===PROCESSES===; ps -A 2>/dev/null | grep -Ei "havalshisuku|androidauto|shizuku|surfaceflinger|media.codec"; echo ===SURFACES===; dumpsys SurfaceFlinger --list 2>/dev/null | grep -Ei "cluster|androidauto|haval|impulse|presentation"; echo ===MEDIA_CODEC===; dumpsys media.codec 2>/dev/null | grep -Ei -A3 -B2 "androidauto|haval|avc|decoder|surface" | head -n 300; echo ===AA_SERVICES===; dumpsys activity services com.ts.androidauto 2>/dev/null | head -n 240; echo ===IMPULSE_SERVICES===; dumpsys activity services br.com.redesurftank.havalshisuku 2>/dev/null | head -n 240'
run_telnet_once "$OUTPUT_DIR/snapshot-start.txt" "$snapshot_command"

logcat_command='logcat -b all -v threadtime'
{
  printf '%s\r\n' "$logcat_command"
  sleep "$DURATION_SECONDS"
} | timeout "$((DURATION_SECONDS + 20))" socat - "TCP:$IP:23,connect-timeout=8" > "$OUTPUT_DIR/logcat-all.txt" 2>&1 &
LOGCAT_PID=$!

probe_command="end=\$((\$(date +%s)+$DURATION_SECONDS)); while [ \$(date +%s) -lt \$end ]; do echo ===PROBE===; date; uptime; echo --pids--; pidof br.com.redesurftank.havalshisuku com.ts.androidauto surfaceflinger 2>/dev/null; echo --surfaces--; dumpsys SurfaceFlinger --list 2>/dev/null | grep -Ei 'cluster|androidauto|haval|impulse|presentation'; echo --codec--; dumpsys media.codec 2>/dev/null | grep -Ei -A2 -B1 'androidauto|haval|avc|decoder|surface' | head -n 120; sleep 5; done"
{
  printf '%s\r\n' "$probe_command"
  sleep "$DURATION_SECONDS"
} | timeout "$((DURATION_SECONDS + 20))" socat - "TCP:$IP:23,connect-timeout=8" > "$OUTPUT_DIR/pipeline-probes.txt" 2>&1 &
PROBE_PID=$!

echo "Capture active for $DURATION_SECONDS seconds."
echo "Mark an incident with: scripts/mark-aa-cluster-event.sh travou"
echo "Output: $OUTPUT_DIR"

wait "$LOGCAT_PID" || true
LOGCAT_PID=""
wait "$PROBE_PID" || true
PROBE_PID=""

run_telnet_once "$OUTPUT_DIR/snapshot-end.txt" "$snapshot_command"
persistent_command='echo ===PERSISTENT_CLUSTER_EVENTS===; for file in /storage/emulated/0/Android/data/br.com.redesurftank.havalshisuku/files/cluster-diagnostics/cluster-events-*.log /data/user_de/0/br.com.redesurftank.havalshisuku/files/cluster-diagnostics/cluster-events-*.log; do [ -f "$file" ] && { echo ---$file---; tail -n 1200 "$file"; }; done'
run_telnet_once "$OUTPUT_DIR/cluster-events.txt" "$persistent_command"
tailscale ping -c 3 "$IP" > "$OUTPUT_DIR/tailscale-ping-end.txt" 2>&1 || true
