#!/usr/bin/env bash
set -euo pipefail

LIVE="supabase/functions/investigate-object/index.ts"
PREVIEW="supabase/functions/investigate-object-preview/index.ts"

for file in "$LIVE" "$PREVIEW"; do
  grep -Fq "Do not discard a defensible broad-category valuation solely because the exact model is uncertain." "$file"
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
if "return {};" not in body:
    raise SystemExit("Reference search must fail closed when no verified image matcher exists.")
if "fetch(" in body:
    raise SystemExit("Unverified external image search must not run in reference-photo handler.")
print("Identification reliability safeguards passed.")
PY
