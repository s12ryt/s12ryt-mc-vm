#!/usr/bin/env bash
set -euo pipefail

container=${1:?Container name is required}
version=${2:?Minecraft version is required}
deadline=$((SECONDS + ${PAPER_BOOT_TIMEOUT_SECONDS:-420}))

while :; do
  logs=$(docker logs "$container" 2>&1)
  if grep -Fq "(MC: $version)" <<< "$logs" \
      && grep -Fq '[McVm] Enabling McVm v1.0.0' <<< "$logs" \
      && grep -Fq '[McVm] VM not started: configure a valid ssh-public-key in config.yml' <<< "$logs" \
      && grep -Eq 'Done \([0-9]+(\.[0-9]+)?s\)!' <<< "$logs"; then
    echo "Paper $version started and McVm onEnable ran"
    exit 0
  fi
  if [[ $(docker inspect --format '{{.State.Running}}' "$container") != true ]]; then
    echo "Paper $version stopped before McVm loaded" >&2
    exit 1
  fi
  if (( SECONDS >= deadline )); then
    echo "Paper $version startup or McVm loading timed out" >&2
    exit 1
  fi
  sleep 5
done
