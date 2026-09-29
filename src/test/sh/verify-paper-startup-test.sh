#!/usr/bin/env bash
set -euo pipefail

docker() {
  case "$1" in
    logs) printf '%s\n' "$MOCK_LOG" ;;
    inspect) printf '%s\n' "$MOCK_RUNNING" ;;
    *) return 1 ;;
  esac
}
export -f docker
export PAPER_BOOT_TIMEOUT_SECONDS=0

MOCK_RUNNING=true
MOCK_LOG=$'This server is running Paper version git-Paper-445 (MC: 1.8.8)\n[Server thread/INFO]: [McVm] Enabling McVm v1.0.0\n[Server thread/SEVERE]: [McVm] VM not started: configure a valid ssh-public-key in config.yml\n[Server thread/INFO]: Done (3.12s)!'
export MOCK_LOG MOCK_RUNNING
bash scripts/verify-paper-startup.sh paper-test 1.8.8

expect_failure() {
  if bash scripts/verify-paper-startup.sh paper-test 1.8.8; then
    echo "Unexpected startup success: $1" >&2
    exit 1
  fi
}

MOCK_LOG=${MOCK_LOG//MC: 1.8.8/MC: 1.12.2}
export MOCK_LOG
expect_failure 'wrong Minecraft version'

MOCK_LOG='Done (3.12s)!'
export MOCK_LOG
expect_failure 'plugin missing'

MOCK_RUNNING=false
export MOCK_RUNNING
expect_failure 'container stopped'

echo 'Paper startup verification tests passed'
