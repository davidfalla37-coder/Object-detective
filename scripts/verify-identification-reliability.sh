#!/usr/bin/env bash
set -euo pipefail
# Regression gate for the Free investigation route.
# Run before signing and publishing the isolated test APK.
for file in supabase/functions/investigate-object/index.ts supabase/functions/investigate-object-preview/index.ts; do
  grep -Fq "Exact-model uncertainty alone must not erase a defensible used-value range" "$file"
  ! grep -Fq 'fallback.confidence = Math.min(fallback.confidence, 0.79)' "$file"
  ! grep -Fq 'result.value_estimate = "Unknown (GBP)"' "$file"
done
echo "Free identification reliability safeguards passed."
