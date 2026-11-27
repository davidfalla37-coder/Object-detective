import { applyBrandConflictFallback, applyUniversalEvidenceReconciliation } from "./identity-conflict.ts";

function fixture() {
  return {
    object: "Wrong label",
    category: "Watches",
    purpose: "Tells the time",
    brand: "Maker A",
    model: "Model A",
    model_number: "123",
    variant: "Variant A",
    exact_match_evidence: "Old evidence",
    case_file_summary: "Old summary",
    identifying_clues: "Visible watch face and strap",
    confidence: 0.96,
    exact_match_confidence: 0.95,
    exact_match_status: "exact",
    next_photo: "Old instruction",
    warning: "",
  };
}

Deno.test("manufacturer disagreement preserves a watch, not wireless earbuds", () => {
  const result = fixture();
  applyBrandConflictFallback(
    result, "Wristwatch", "Round dial and attached strap",
    0.96, 0.72, 0.95, 0.4, true,
  );
  if (result.object !== "Wristwatch") throw new Error("Broad object identity was overwritten.");
  if (result.category !== "Watches") throw new Error("Original category was changed.");
  if (!result.case_file_summary.includes("Wristwatch")) throw new Error("Case file lost the broad object.");
  if (/earbuds/i.test(result.object + result.case_file_summary + result.next_photo)) {
    throw new Error("Unrelated earbuds fallback remains.");
  }
  if (result.brand !== "Unknown" || result.exact_match_status !== "not_confirmed") {
    throw new Error("Uncertain manufacturer/model was not marked cautiously.");
  }
});

Deno.test("the original broad object is retained for other categories too", () => {
  const result = fixture();
  applyBrandConflictFallback(
    result, "Cordless drill", "Chuck, trigger and battery pack",
    0.91, 0.65, 0.88, 0.3, true,
  );
  if (result.object !== "Cordless drill") throw new Error("Non-watch object was overwritten.");
  if (/earbuds/i.test(result.object + result.case_file_summary)) {
    throw new Error("Unrelated earbuds fallback remains.");
  }
});

Deno.test("an actual earbuds result remains earbuds when that is the primary finding", () => {
  const result = fixture();
  applyBrandConflictFallback(
    result, "Wireless earbuds with charging case", "Two earbuds and charging case",
    0.91, 0.65, 0.88, 0.3, true,
  );
  if (result.object !== "Wireless earbuds with charging case") {
    throw new Error("The real primary object was not preserved.");
  }
});

function universalFixture() {
  return {
    object: "Casio wristwatch", category: "wearable electronic device", purpose: "timekeeping",
    brand: "Casio", manufacturer: "Casio", model: "Unknown", model_number: "Unknown",
    part_number: "Unknown", variant: "Unknown", confidence: 0.9,
    exact_match_status: "not_confirmed", exact_match_confidence: 0.45,
    exact_match_evidence: "Unknown", identifying_clues: "Unknown", case_file_summary: "Unknown",
    next_photo: "Unknown", warning: "", value_estimate: "Unknown (GBP)",
    value_reasoning: "Withheld", confidence_booster: "Unknown", buyer_seller_check: "Unknown",
  };
}

Deno.test("one OCR model code with matching maker text upgrades a generic result to likely", () => {
  const result = universalFixture();
  applyUniversalEvidenceReconciliation(
    result,
    { value_estimate: "£40–£60 (GBP)" },
    { value_estimate: "£35–£55 (GBP)" },
    JSON.stringify({ extractedFromPhotos: [{ photoIndex: 0, visibleText: "CASIO 5590 GA-2000 WATER RESIST 20BAR" }] }),
  );
  if (result.model_number !== "GA-2000" || result.model !== "GA-2000") throw new Error("OCR model code was not carried into the result.");
  if (result.exact_match_status !== "likely") throw new Error("OCR alone must not claim an exact retail variant.");
  if (!result.object.includes("GA-2000")) throw new Error("Likely object summary did not include the readable model family.");
  if (result.value_estimate !== "£35–£55 (GBP)") throw new Error("The lowest supported value range was not restored.");
});

Deno.test("ambiguous OCR codes do not become a model guess", () => {
  const result = universalFixture();
  applyUniversalEvidenceReconciliation(
    result,
    {},
    null,
    JSON.stringify({ extractedFromPhotos: [{ photoIndex: 0, visibleText: "CASIO GA-2000 A1586" }] }),
  );
  if (result.model_number !== "Unknown" || result.exact_match_status !== "not_confirmed") {
    throw new Error("Multiple OCR codes were incorrectly promoted to a model.");
  }
});
