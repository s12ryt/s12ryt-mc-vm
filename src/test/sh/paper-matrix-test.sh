#!/usr/bin/env bash
set -euo pipefail

workflow=.github/workflows/ci.yml

for entry in \
  '1.8.8 8 445' \
  '1.12.2 11 1620' \
  '1.16.5 16 794' \
  '1.18.2 17 388' \
  '1.21.4 21 232' \
  '1.21.8 21 60' \
  '1.21.11 21 132' \
  '26.1.2 25 74' \
  '26.2 25 129'; do
  read -r version java build <<< "$entry"
  row="          - { minecraft: '$version', java: '$java', build: '$build' }"
  if [[ $(grep -Fxc -- "$row" "$workflow" || true) != 1 ]]; then
    echo "Expected exactly one Paper $version / Java $java / build $build CI entry" >&2
    exit 1
  fi
done

if [[ $(grep -c '^          - { minecraft:' "$workflow") != 9 ]]; then
  echo 'Expected exactly nine Paper compatibility jobs' >&2
  exit 1
fi

echo 'Paper compatibility matrix contains nine pinned versions'
