#!/usr/bin/env bash
set -euo pipefail
FILE="app/src/main/assets/index-2.html"
required=(
  'id="od-bottom-nav"'
  'class="od-screen'
  'indexedDB.open(HISTORY_PHOTOS_DB'
  'restoreHistoryPhotos(item.id)'
  'async function saveInvestigation(data)'
)
for pattern in "${required[@]}"; do
  if ! grep -Fq "$pattern" "$FILE"; then
    echo "::error::Protected Object Detective feature missing: $pattern"
    exit 1
  fi
done
grep -Fq 'applicationIdSuffix ".test"' app/build.gradle || {
  echo "::error::Separate test application ID safeguard is missing"
  exit 1
}
echo "Protected refreshed layout, history-photo restore, and separate test package verified."
