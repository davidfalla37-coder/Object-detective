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
    // Match complete words/phrases only. A short brand such as "TCL" must
    // never be stripped from an unrelated word such as "spectacle".
    const pattern = new RegExp(
      "(^|[^\\p{L}\\p{N}])" + escaped + "(?=$|[^\\p{L}\\p{N}])",
      "giu",
    );
    generic = generic.replace(pattern, "$1 ");
  }

  return generic
    .replace(/\s+/g, " ")
    .replace(/^[\s,;:–—-]+|[\s,;:–—-]+$/g, "")
    .trim() || "Unknown";
}


/**
 * Choose the broadest usable object description after disputed identity claims
 * are removed. This is deliberately category-neutral.
 */
export function resolveBroadObject(
  primaryDescription: string,
  verificationDescription: string,
  claims: string[],
  fallbackCategory: string,
): string {
  const primary = stripIdentityClaims(primaryDescription, claims);
  if (primary !== "Unknown") return primary;

  const verification = stripIdentityClaims(verificationDescription, claims);
  if (verification !== "Unknown") return verification;

  const category = fallbackCategory.trim();
  return category.length > 0 && category.toLowerCase() !== "unknown" ? category : "Object";
}
