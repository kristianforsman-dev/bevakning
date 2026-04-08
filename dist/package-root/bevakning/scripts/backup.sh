#!/usr/bin/env bash
set -euo pipefail

TS="$(date +%Y%m%d-%H%M%S)"
TARGET="backups/$TS"

mkdir -p "$TARGET"

cp src/main/resources/data/monitoring-rules.json "$TARGET"/
cp src/main/resources/data/alert-history.json "$TARGET"/
cp src/main/resources/data/acknowledgements.json "$TARGET"/

echo "Backup skapad: $TARGET"
