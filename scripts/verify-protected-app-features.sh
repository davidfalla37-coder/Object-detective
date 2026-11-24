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
# Keep the photo-resolution warning based on total pixels, not the shortest edge.
grep -Fq 'width*height<200000' "$FILE" || {
  echo "::error::Photo resolution check must use the conservative total-pixel threshold"
  exit 1
}
grep -Fq 'use the original photo if available' "$FILE" || {
  echo "::error::Photo resolution warning must not tell users to retake useful narrow images"
  exit 1
}
echo "Protected refreshed layout, history-photo restore, photo-resolution guard, and separate test package verified."


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
LIVE_URL="https://gboyflbwcobhzbvpdtat.supabase.co/functions/v1/investigate-object"
if ! grep -Fq "$LIVE_URL" "app/src/main/assets/index-2.html" || grep -Fq 'investigate-object-preview' "app/src/main/assets/index-2.html"; then
  echo "::error::Production source must use the live endpoint, not the preview endpoint"
  exit 1
fi
LIVE_FUNCTION="supabase/functions/investigate-object/index.ts"
LIVE_HELPER="supabase/functions/investigate-object/identity-conflict.ts"
if ! grep -Fq 'applyBrandConflictFallback' "$LIVE_FUNCTION" || ! grep -Fq 'result.object = retainedObject' "$LIVE_HELPER"; then
  echo "::error::Live broad-object identity safeguard is missing"
  exit 1
fi
if grep -Fq 'result.object = "Wireless earbuds with charging case"' "$LIVE_FUNCTION"; then
  echo "::error::Hard-coded earbuds fallback has returned to the live service"
  exit 1
fi
