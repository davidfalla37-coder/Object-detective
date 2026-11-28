import { reconcileSecondhandValue, type ValuationIdentity, type VerificationValuationIdentity } from "./resale-value.ts";

function primary(value_estimate: string, exact_match_status = "exact", confidence = 0.9): ValuationIdentity {
  return { value_estimate, exact_match_status, confidence };
}

function verification(
  value_estimate: string,
  exact_match_status = "exact",
  confidence = 0.9,
  verdict: VerificationValuationIdentity["verdict"] = "agree",
): VerificationValuationIdentity {
  return { value_estimate, exact_match_status, confidence, verdict };
}

Deno.test("overlapping GBP checks produce the expected cautious range", () => {
  const result = reconcileSecondhandValue(
    primary("£50–£80"),
    verification("£45–£70"),
    false,
  );
  if (result.estimate !== "£45–£77") {
    throw new Error("Expected overlapping ranges to produce £45–£77, got " + result.estimate);
  }
});

Deno.test("uncertain exact model does not hide a supported broad-item value", () => {
  const result = reconcileSecondhandValue(
    primary("£50–£80", "not_confirmed"),
    verification("£45–£70", "not_confirmed"),
    false,
  );
  if (result.estimate === "Unknown (GBP)") {
    throw new Error("Expected a broad-item value when both identity checks agree");
  }
  if (!result.reasoning.includes("exact model is not confirmed")) {
    throw new Error("Expected the estimate to disclose that the exact model is unconfirmed");
  }
});

Deno.test("manufacturer conflict withholds the value", () => {
  const result = reconcileSecondhandValue(
    primary("£50–£80"),
    verification("£45–£70"),
    true,
  );
  if (result.estimate !== "Unknown (GBP)") {
    throw new Error("Expected an unresolved manufacturer conflict to withhold price");
  }
});

Deno.test("non-overlapping ranges withhold a misleading price", () => {
  const result = reconcileSecondhandValue(
    primary("£20–£30"),
    verification("£60–£80"),
    false,
  );
  if (result.estimate !== "Unknown (GBP)") {
    throw new Error("Expected non-overlapping estimates to remain unknown");
  }
});

Deno.test("low confidence does not produce a price", () => {
  const result = reconcileSecondhandValue(
    primary("£50–£80", "exact", 0.4),
    verification("£45–£70"),
    false,
  );
  if (result.estimate !== "Unknown (GBP)") {
    throw new Error("Expected low-confidence identity to withhold price");
  }
});
