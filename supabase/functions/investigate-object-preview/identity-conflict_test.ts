import { applyBrandConflictFallback, applySamsungEarbudModelEvidence, reconcileCrossCheckIdentityEvidence } from "./identity-conflict.ts";

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


Deno.test("Samsung SM-R630 evidence resolves conflicting earbud brand and model", () => {
  const result = {
    object: "SoundPEATS wireless earbuds", category: "Electronics",
    brand: "SoundPEATS", manufacturer: "SoundPEATS", model: "Unknown",
    model_number: "Unknown", variant: "Unknown", exact_match_status: "not_confirmed",
    exact_match_confidence: 0.2, exact_match_evidence: "Visual guess",
    identifying_clues: "Samsung branding, model SM-R630, case reference EP-OR630, wireless earbuds and charging case.",
    case_file_summary: "Wireless earbuds.", warning: "",
    value_estimate: "Unknown (GBP)", value_reasoning: "",
  };
  applySamsungEarbudModelEvidence(result, "No local OCR text extracted.");
  if (result.brand !== "Samsung" || result.manufacturer !== "Samsung") throw new Error("Samsung brand conflict was not resolved.");
  if (result.model !== "Galaxy Buds3 Pro" || result.model_number !== "SM-R630") throw new Error("SM-R630 was not resolved to Galaxy Buds3 Pro.");
  if (result.exact_match_status !== "likely") throw new Error("Model confidence must remain cautious.");
});

Deno.test("Samsung model evidence does not alter unrelated categories", () => {
  const result = {
    object: "Cordless drill", category: "Tools", brand: "Unknown", manufacturer: "Unknown",
    model: "Unknown", model_number: "Unknown", variant: "Unknown",
    exact_match_status: "not_confirmed", exact_match_confidence: 0.2,
    exact_match_evidence: "", identifying_clues: "Samsung SM-R630",
    case_file_summary: "", warning: "", value_estimate: "Unknown (GBP)", value_reasoning: "",
  };
  applySamsungEarbudModelEvidence(result, "");
  if (result.object !== "Cordless drill") throw new Error("Unrelated object was changed.");
});


function crossCheckFixture() {
  return {
    object: "Wireless earbuds with charging case", category: "Electronics",
    brand: "Unknown", manufacturer: "Unknown", model: "Unknown", model_number: "Unknown",
    variant: "Unknown", exact_match_status: "not_confirmed", exact_match_confidence: 0.2,
    exact_match_evidence: "", identifying_clues: "Unknown", case_file_summary: "Unknown",
    confidence: 0.7, warning: "",
  };
}

Deno.test("cross-category reconciliation preserves readable maker and model evidence", () => {
  const result = crossCheckFixture();
  reconcileCrossCheckIdentityEvidence(result,
    { object: "SoundPEATS wireless earbuds", brand: "SoundPEATS", model: "Unknown", model_number: "Unknown", variant: "Unknown", identifying_clues: "Case shape resembles SoundPEATS." },
    { object: "Samsung wireless earbuds", brand: "Samsung", model: "Galaxy Buds3 Pro", model_number: "SM-R630", variant: "Pro", identifying_clues: "Samsung branding and SM-R630 are visible." },
    '{"visibleText":"Samsung SM-R630"}');
  if (result.brand !== "Samsung" || result.model_number !== "SM-R630") throw new Error("Direct evidence did not resolve maker/model.");
  if (!result.identifying_clues.includes("SoundPEATS") || !result.identifying_clues.includes("Samsung")) throw new Error("Cross-check clues were discarded.");
});

Deno.test("unresolved manufacturer conflict retains broad object instead of choosing an AI guess", () => {
  const result = crossCheckFixture();
  reconcileCrossCheckIdentityEvidence(result,
    { object: "SoundPEATS wireless earbuds", brand: "SoundPEATS", model: "Air4", model_number: "A4", variant: "Unknown", identifying_clues: "Visual shape resembles SoundPEATS." },
    { object: "Samsung wireless earbuds", brand: "Samsung", model: "Galaxy Buds", model_number: "SM-X", variant: "Unknown", identifying_clues: "Visual shape resembles Samsung." }, "");
  if (result.brand !== "Unknown" || result.model !== "Unknown" || result.model_number !== "Unknown") throw new Error("Unsupported brand/model guess survived.");
  if (!/earbuds/i.test(result.object)) throw new Error("Broad object type was lost.");
});

Deno.test("a specific primary identity survives a broader Unknown verification", () => {
  const result = crossCheckFixture();
  reconcileCrossCheckIdentityEvidence(result,
    { object: "Renault Megane RS", brand: "Renault", model: "Megane RS265", model_number: "Unknown", variant: "RS265", identifying_clues: "RS badge visible." },
    { object: "Renault performance hatchback", brand: "Renault", model: "Unknown", model_number: "Unknown", variant: "Unknown", identifying_clues: "Renault badge visible." },
    '{"visibleText":"Renault RS"}');
  if (result.brand !== "Renault" || result.model !== "Megane RS265") throw new Error("Specific primary identity was discarded.");
});

Deno.test("conflicting model numbers are withheld unless one is directly readable", () => {
  const result = crossCheckFixture();
  reconcileCrossCheckIdentityEvidence(result,
    { object: "Cordless drill", brand: "Makita", model: "DHP486", model_number: "DHP486", variant: "Unknown" },
    { object: "Cordless drill", brand: "Makita", model: "DHP484", model_number: "DHP484", variant: "Unknown" }, "");
  if (result.model !== "Unknown" || result.model_number !== "Unknown") throw new Error("Conflicting model number was guessed.");
});
