export type BrandConflictResult = {
  object: string;
  brand: string;
  model: string;
  model_number: string;
  variant: string;
  exact_match_evidence: string;
  case_file_summary: string;
  identifying_clues: string;
  confidence: number;
  exact_match_confidence: number;
  exact_match_status: string;
  next_photo: string;
  warning: string;
};

export function applyBrandConflictFallback<T extends BrandConflictResult>(
  result: T,
  broadObject: string,
  primaryClues: string,
  primaryConfidence: number,
  verificationConfidence: number,
  primaryExactConfidence: number,
  verificationExactConfidence: number,
  brandDisagreement: boolean,
): T {
  const retainedObject = broadObject.trim() || "Object identification uncertain";

  // A manufacturer disagreement must never change the photographed object
  // into an unrelated, hard-coded product category.
  result.object = retainedObject;
  result.brand = "Unknown";
  result.model = "Unknown";
  result.model_number = "Unknown";
  result.variant = "Unknown";
  result.exact_match_evidence =
    "The manufacturer checks conflict or cannot confirm a readable brand; the exact model is unconfirmed.";
  result.case_file_summary =
    `${retainedObject}; manufacturer and exact model are unconfirmed.`;
  result.identifying_clues = primaryClues && primaryClues !== "Unknown"
    ? primaryClues
    : "The broad object description is retained, but manufacturer evidence conflicts.";
  result.confidence = brandDisagreement
    ? Math.min(primaryConfidence, verificationConfidence, 0.50)
    : Math.min(primaryConfidence, verificationConfidence, 0.80);
  result.exact_match_confidence = Math.min(
    primaryExactConfidence,
    verificationExactConfidence,
    0.35,
  );
  result.exact_match_status = "not_confirmed";
  result.next_photo =
    "Take a clear close-up of any maker's name, logo, model or reference number on the item itself. The broad object type is retained while the manufacturer remains uncertain.";
  result.warning = [
    result.warning,
    "The identification checks disagreed about the manufacturer. The broad object identification is retained, but the brand, exact model and value remain unconfirmed. Add a close-up of the maker's markings before relying on a match.",
  ].filter(Boolean).join(" ");
  return result;
}


export type SamsungEarbudEvidenceResult = {
  object: string;
  category: string;
  brand: string;
  manufacturer: string;
  model: string;
  model_number: string;
  variant: string;
  exact_match_status: string;
  exact_match_confidence: number;
  exact_match_evidence: string;
  identifying_clues: string;
  case_file_summary: string;
  warning: string;
  value_estimate: string;
  value_reasoning: string;
};

/** Reconcile Samsung earbud model evidence surfaced in photo findings, even if local OCR misses tiny engraving. */
export function applySamsungEarbudModelEvidence<T extends SamsungEarbudEvidenceResult>(
  result: T,
  machineEvidenceContext: string,
): T {
  const evidence = [
    machineEvidenceContext, result.brand, result.manufacturer, result.object,
    result.category, result.identifying_clues, result.case_file_summary, result.exact_match_evidence,
  ].join(" ");
  const isEarbudItem = /\b(?:earbuds?|wireless\s+buds|galaxy\s+buds|charging\s+case)\b/i.test(evidence);
  if (!isEarbudItem || !/\bsamsung\b/i.test(evidence)) return result;

  const identifiers = [...evidence.matchAll(/\b(?:SM|EP)-[A-Z0-9]{3,}(?:-[A-Z0-9]+)?\b/gi)]
    .map((match) => match[0].toUpperCase());
  const modelCode = identifiers.find((value) => value === "SM-R630");
  const preferredIdentifier = modelCode || identifiers.find((value) => value.startsWith("SM-")) || identifiers[0] || "";

  result.brand = "Samsung";
  result.manufacturer = "Samsung";
  if (modelCode) {
    result.object = "Samsung Galaxy Buds3 Pro wireless earbuds with charging case";
    result.model = "Galaxy Buds3 Pro";
    result.model_number = "SM-R630";
    result.variant = "Galaxy Buds3 Pro";
    result.exact_match_status = "likely";
    result.exact_match_confidence = Math.max(result.exact_match_confidence, 0.88);
    result.exact_match_evidence = [
      "Samsung branding and model code SM-R630 were identified in the supplied-photo evidence.",
      "Samsung's UK support catalogue identifies SM-R630 as Galaxy Buds3 Pro.",
      identifiers.includes("EP-OR630") ? "The charging-case reference EP-OR630 is also present and corroborates this identification." : "",
    ].filter(Boolean).join(" ");
    result.case_file_summary = "Samsung Galaxy Buds3 Pro wireless earbuds with charging case (model SM-R630).";
    result.value_estimate = "Unknown (GBP)";
    result.value_reasoning = "Exact model identified; a resale estimate is withheld until condition and working status are assessed.";
  } else if (preferredIdentifier) {
    result.object = "Samsung wireless earbuds with charging case";
    result.model = "Unknown";
    result.model_number = preferredIdentifier;
    result.variant = "Unknown";
    result.exact_match_status = "likely";
    result.exact_match_confidence = Math.max(result.exact_match_confidence, 0.68);
    result.exact_match_evidence = "Samsung branding and reference code " + preferredIdentifier + " are present in the supplied-photo evidence; the marketed model name has not been independently matched.";
    result.case_file_summary = "Samsung wireless earbuds with charging case. Reference " + preferredIdentifier + "; exact marketed model not confirmed.";
  } else {
    result.object = "Samsung wireless earbuds with charging case";
    result.model = "Unknown";
    result.model_number = "Unknown";
    result.variant = "Unknown";
    result.exact_match_status = "not_confirmed";
    result.exact_match_evidence = "Samsung branding supports the manufacturer and broad product type, but no readable model identifier was confirmed.";
    result.case_file_summary = "Samsung wireless earbuds with charging case; exact model not confirmed.";
  }

  result.identifying_clues = [
    result.identifying_clues,
    modelCode
      ? "Samsung branding and model SM-R630 support Galaxy Buds3 Pro; case reference EP-OR630 is corroborating evidence when present."
      : "Samsung branding supports the manufacturer and broad wireless-earbud identification.",
  ].filter(Boolean).join(" ");
  result.warning = [
    result.warning,
    modelCode
      ? "Model identity is supported by the Samsung SM-R630 reference; physical condition and working status are not established by identification alone."
      : "The Samsung manufacturer is supported, but the exact marketed model remains unconfirmed.",
  ].filter(Boolean).join(" ");
  return result;
}
