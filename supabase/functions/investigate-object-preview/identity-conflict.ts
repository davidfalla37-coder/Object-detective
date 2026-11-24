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
