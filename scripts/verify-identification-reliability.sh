#!/usr/bin/env bash
set -euo pipefail

# Regression gate for shared identification and Pro visual-reference safety.
# This runs before any preview APK is signed or uploaded.
LIVE="supabase/functions/investigate-object/index.ts"
PREVIEW="supabase/functions/investigate-object-preview/index.ts"

for file in "$LIVE" "$PREVIEW"; do
  if [[ "$file" == "$PREVIEW" ]]; then
    grep -Fq "rawImages.length > 15" "$file"
    grep -Fq "value.slice(0, 15)" "$file"
  fi
  grep -Fq "Exact-model uncertainty alone must not erase a defensible used-value range" "$file"
  if grep -Fq 'fallback.confidence = Math.min(fallback.confidence, 0.79)' "$file"; then
    echo "Arbitrary 79% confidence cap remains in $file" >&2
    exit 1
  fi
  if grep -Fq 'result.value_estimate = "Unknown (GBP)"' "$file"; then
    echo "Blanket Unknown valuation override remains in $file" >&2
    exit 1
  fi
done

python3 - <<'PY'
from pathlib import Path
source = Path("supabase/functions/investigate-object-preview/index.ts").read_text()
start = source.index("async function fetchReferencePhoto(")
end = source.index("Deno.serve(", start)
body = source[start:end]
if "return {};" not in body or "fetch(" in body:
    raise SystemExit("Pro reference-photo search must fail closed without identity verification.")
print("Identification reliability safeguards passed.")
PY
