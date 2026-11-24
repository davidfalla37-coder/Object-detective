#!/usr/bin/env bash
set -euo pipefail
FILE="app/src/main/assets/index-2.html"
required=(
  'id="od-bottom-nav"'
  '.od-screen'
  'indexedDB.open(HISTORY_PHOTOS_DB'
  'restoreHistoryPhotos(item.id)'
  'async function saveInvestigation(data)'
  'const lookupFormat=scannerState.format==="UPC_E"?"UPC-E":scannerState.format'
  'JSON.stringify({code,format:lookupFormat})'
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


FUNCTION="supabase/functions/investigate-object-preview/index.ts"
HELPER="supabase/functions/investigate-object-preview/identity-conflict.ts"
if ! grep -Fq 'applyBrandConflictFallback' "$FUNCTION" || ! grep -Fq 'result.object = retainedObject' "$HELPER"; then
  echo "::error::Protected broad-object identity fallback is missing"
  exit 1
fi
if grep -Fq 'result.object = "Wireless earbuds with charging case"' "$FUNCTION"; then
  echo "::error::Hard-coded earbuds fallback has returned"
  exit 1
fi
if ! grep -Fq 'investigate-object-preview' "app/src/main/assets/index-2.html"; then
  echo "::error::Preview app is not isolated from the live investigation endpoint"
  exit 1
fi
