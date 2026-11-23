export interface IdentityCandidate {
  object: string;
  category?: string;
  brand: string;
}

export type VerificationVerdict = "agree" | "partial" | "disagree" | "insufficient";

const IGNORED_TERMS = new Set([
  "the", "and", "with", "from", "used", "item", "device", "product",
  "this", "that", "for", "into", "its", "part", "unknown",
]);

function known(value: string): boolean {
  const normalized = value.trim().toLowerCase();
  return normalized.length > 0 && normalized !== "unknown";
}

function normalize(value: string): string {
  return value.trim().toLowerCase().replace(/[^a-z0-9]+/g, "");
}

function terms(value: string): Set<string> {
  if (!known(value)) return new Set();
  return new Set(
    value.toLowerCase().replace(/[^a-z0-9]+/g, " ").split(/\s+/)
      .map((word) => word.length > 3 && word.endsWith("s") ? word.slice(0, -1) : word)
      .filter((word) => word.length > 2 && !IGNORED_TERMS.has(word)),
  );
}

/** Compare two independent descriptions without treating one shared word as proof. */
export function deriveVerificationVerdict(
  primary: IdentityCandidate,
  verification: IdentityCandidate & { verdict: VerificationVerdict },
): VerificationVerdict {
  const primaryBrand = known(primary.brand) ? normalize(primary.brand) : "";
  const verificationBrand = known(verification.brand) ? normalize(verification.brand) : "";

  if (primaryBrand && verificationBrand && primaryBrand !== verificationBrand) {
    return "disagree";
  }
  if (verification.verdict === "insufficient") return "insufficient";

  const primaryTerms = terms(primary.object + " " + (primary.category ?? ""));
  const verificationTerms = terms(verification.object + " " + (verification.category ?? ""));
  // Brand and model tokens can be shared by a whole product and one of its
  // components. Do not let those tokens alone establish object-level agreement.
  for (const word of terms(primary.brand)) primaryTerms.delete(word);
  for (const word of terms(verification.brand)) verificationTerms.delete(word);
  const shared = [...primaryTerms].filter((word) => verificationTerms.has(word)).length;

  // One shared descriptive word is partial evidence, never agreement.
  if (shared >= 2) return primaryBrand && verificationBrand === primaryBrand ? "agree" : "partial";
  if (shared === 1) return "partial";
  if (primaryBrand && verificationBrand === primaryBrand) return "partial";
  if (primaryTerms.size === 0 || verificationTerms.size === 0) return "insufficient";
  return "disagree";
}
