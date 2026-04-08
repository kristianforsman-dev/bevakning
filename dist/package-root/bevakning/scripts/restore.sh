#!/usr/bin/env bash
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Användning: ./scripts/restore.sh backups/<katalog>"
  exit 1
fi

SOURCE="$1"

cp "$SOURCE/monitoring-rules.json" src/main/resources/data/
cp "$SOURCE/alert-history.json" src/main/resources/data/
cp "$SOURCE/acknowledgements.json" src/main/resources/data/

echo "Restore klar från: $SOURCE"
