/**
 * Remove unsupported identity claims from an object description while keeping
 * the useful broad object type. Used when independent checks disagree about
 * the manufacturer; the fallback must never substitute a hard-coded category.
 */
export function stripIdentityClaims(description: string, claims: string[]): string {
  let generic = description;
  const claimedIdentity = claims
    .map((claim) => claim.trim())
    .filter((claim) => claim.length > 0 && claim.toLowerCase() !== "unknown")
    .sort((a, b) => b.length - a.length);

  for (const claim of claimedIdentity) {
    const escaped = Array.from(claim, (character) =>
      "\\.^$|?*+()[]{}".includes(character) ? "\\" + character : character
    ).join("");
    generic = generic.replace(new RegExp(escaped, "gi"), " ");
  }

  return generic
    .replace(/\s+/g, " ")
    .replace(/^[\s,;:–—-]+|[\s,;:–—-]+$/g, "")
    .trim() || "Unknown";
}
