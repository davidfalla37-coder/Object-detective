/**
 * Canonicalizes known product-line names to their manufacturer for cross-checks.
 * G-SHOCK and BABY-G are Casio product lines, so those marks must not be treated
 * as conflicting manufacturers when another photo or pass reads CASIO.
 */
export function normalizeManufacturerName(value: string): string {
  const normalized = value.trim().toLowerCase().replace(/[^a-z0-9]+/g, "");
  if (normalized.includes("gshock") || normalized.includes("babyg")) return "casio";
  return normalized;
}

function extractModelIdentifiers(value: string): Set<string> {
  const matches = value.match(/\b[A-Z]{1,10}(?:[- ]+[A-Z]{1,6})?[- ]?\d+[A-Z0-9-]*\b/gi) ?? [];
  return new Set(matches.map((match) => match.toLowerCase().replace(/[^a-z0-9]/g, "")));
}

/**
 * Returns true only when both independent checks provide at least one
 * model-like alphanumeric identifier and none of those identifiers overlap.
 * Generic model names without a numeric identifier are left to the broader
 * identity comparison.
 */
export function hasConflictingModelIdentifiers(primary: string, verification: string): boolean {
  const first = extractModelIdentifiers(primary);
  const second = extractModelIdentifiers(verification);
  if (first.size === 0 || second.size === 0) return false;
  return ![...first].some((identifier) => second.has(identifier));
}
