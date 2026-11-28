export interface ValuationIdentity {
  value_estimate: string;
  confidence: number;
  exact_match_status: string;
}

export interface VerificationValuationIdentity extends ValuationIdentity {
  verdict: "agree" | "partial" | "disagree" | "insufficient";
}

interface GbpRange { low: number; high: number; }

function isKnownText(value: string): boolean {
  const normalized = value.trim().toLowerCase();
  return normalized.length > 0 && normalized !== "unknown" && normalized !== "unknown (gbp)";
}

function parseGbpRange(value: string): GbpRange | null {
  if (!isKnownText(value) || !value.includes("£")) return null;
  const amounts = [...value.matchAll(/£\s*([\d,]+(?:\.\d{1,2})?)/g)]
    .map((match) => Number(match[1].replace(/,/g, "")))
    .filter((amount) => Number.isFinite(amount) && amount >= 0 && amount <= 10_000_000);
  if (amounts.length === 0) return null;
  return { low: Math.min(...amounts), high: Math.max(...amounts) };
}

/**
 * Shows a used-value range only when the independent identity checks agree,
 * confidence is adequate, and both GBP estimates overlap. Exact-model certainty
 * is reported separately and is not required to estimate a clear broad item.
 */
export function reconcileSecondhandValue(
  primary: ValuationIdentity,
  verification: VerificationValuationIdentity,
  brandConflict: boolean,
): { estimate: string; reasoning: string } {
  const unknown = (reason: string) => ({ estimate: "Unknown (GBP)", reasoning: reason });
  if (brandConflict || verification.verdict !== "agree") {
    return unknown("No price shown because the independent identification checks did not agree. Confirm the item identity before estimating its second-hand value.");
  }
  if (primary.confidence < 0.55 || verification.confidence < 0.55) {
    return unknown("No price shown because confidence in the broad item identification is too low to support a useful estimate.");
  }

  const first = parseGbpRange(primary.value_estimate);
  const second = parseGbpRange(verification.value_estimate);
  if (!first || !second) {
    return unknown("No price shown because both independent checks did not provide a usable GBP estimate.");
  }

  const overlapLow = Math.max(first.low, second.low);
  const overlapHigh = Math.min(first.high, second.high);
  if (overlapLow > overlapHigh) {
    return unknown("No price shown because the independent value ranges did not overlap. Confirm the exact item and check completed UK sales.");
  }

  // Keep the shared part of the ranges with a modest margin; this remains an
  // estimate, not a completed-sale lookup.
  const low = Math.max(0, Math.floor(overlapLow * 0.9));
  const high = Math.ceil(overlapHigh * 1.1);
  const estimate = "£" + low + "–£" + high;
  const exactModelConfirmed = primary.exact_match_status !== "not_confirmed" &&
    verification.exact_match_status !== "not_confirmed";
  const identityNote = exactModelConfirmed
    ? "The exact model is supported by both identification passes."
    : "The broad item is supported, but the exact model is not confirmed.";
  return {
    estimate,
    reasoning: "Approximate UK private-sale estimate: " + estimate + ". " + identityNote + " The two independent visual estimates overlapped after accounting for visible condition and unverified functionality. No live completed-sale feed was checked, so compare recent sold listings before relying on this figure.",
  };
}
