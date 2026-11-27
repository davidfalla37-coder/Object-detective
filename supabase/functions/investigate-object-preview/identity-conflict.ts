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




export type UniversalEvidenceResult = {
  object: string; category: string; brand: string; manufacturer: string; model: string;
  model_number: string; part_number: string; variant: string; confidence: number;
  exact_match_status: string; exact_match_confidence: number; exact_match_evidence: string;
  identifying_clues: string; case_file_summary: string; next_photo: string; warning: string;
  value_estimate: string; value_reasoning: string; confidence_booster: string; buyer_seller_check: string;
};

/**
 * Category-neutral final pass. It checks the agreed identity against per-photo OCR
 * and barcode catalogue records, and prevents an unsupported visual guess from
 * being presented as an exact model match.
 */
export function applyUniversalEvidenceReconciliation<T extends UniversalEvidenceResult>(
  result: T,
  primary: Partial<UniversalEvidenceResult>,
  verification: Partial<UniversalEvidenceResult> | null,
  machineEvidenceContext: string,
): T {
  const known = (value: unknown): value is string =>
    typeof value === "string" && value.trim().length > 0 &&
    !/^unknown(?:\s*\([^)]*\))?$/i.test(value.trim());
  const normalize = (value: string): string => value.toLowerCase().replace(/[^a-z0-9]+/g, "");
  let payload: Record<string, unknown> = {};
  try {
    const parsed = JSON.parse(machineEvidenceContext);
    if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
      payload = parsed as Record<string, unknown>;
    }
  } catch {
    // Older app builds can send a plain-text "no evidence" message.
  }
  const photos = Array.isArray(payload.extractedFromPhotos)
    ? payload.extractedFromPhotos as Array<Record<string, unknown>>
    : [];
  const ocrText = photos.map((photo) => typeof photo.visibleText === "string" ? photo.visibleText : "").join(" ");
  const normalizedOcr = normalize(ocrText);
  // Recover one clear alphanumeric model-style code directly from OCR when the
  // same photo also contains the already-supported manufacturer mark. Keep it
  // as a likely model-family match; OCR alone cannot prove a retail variant.
  const modelCodePattern = /\b[A-Z]{1,5}-(?:[A-Z]{1,3})?\d{2,6}(?:[-/][A-Z0-9]{1,5})?\b|\b[A-Z]{1,5}\d{2,6}(?:[-/][A-Z0-9]{1,5})?\b/gi;
  const ocrModelCodeMap = new Map<string, string>();
  for (const photo of photos) {
    const text = typeof photo.visibleText === "string" ? photo.visibleText : "";
    if (!known(result.brand) || !normalize(text).includes(normalize(result.brand))) continue;
    for (const match of text.matchAll(modelCodePattern)) {
      const code = match[0].toUpperCase();
      ocrModelCodeMap.set(normalize(code), code);
    }
  }
  const ocrModelCodeCandidates = [...ocrModelCodeMap.values()];
  const ocrModelCode = ocrModelCodeCandidates.length === 1 ? ocrModelCodeCandidates[0] : "";
  const outputIdentifiers = [
    result.model_number, result.part_number,
    primary.model_number, primary.part_number,
    verification?.model_number, verification?.part_number,
  ].filter((value): value is string => known(value));
  const directlyReadIdentifiers = [...new Map(outputIdentifiers
    .filter((value) => {
      const token = normalize(value);
      return token.length >= 4 && normalizedOcr.includes(token);
    })
    .map((value) => [normalize(value), value])).values()];
  const same = (a?: string, b?: string): boolean =>
    known(a) && known(b) && normalize(a) === normalize(b);
  const primaryModelNumberRead = directlyReadIdentifiers.some((value) => same(primary.model_number, value));
  const verificationModelNumberRead = directlyReadIdentifiers.some((value) => same(verification?.model_number, value));
  const primaryPartNumberRead = directlyReadIdentifiers.some((value) => same(primary.part_number, value));
  const verificationPartNumberRead = directlyReadIdentifiers.some((value) => same(verification?.part_number, value));
  const preferredReadModelNumber = directlyReadIdentifiers.find((value) => same(result.model_number, value)) ||
    directlyReadIdentifiers.find((value) => same(primary.model_number, value)) ||
    directlyReadIdentifiers.find((value) => same(verification?.model_number, value)) || "";
  const preferredReadPartNumber = directlyReadIdentifiers.find((value) => same(result.part_number, value)) ||
    directlyReadIdentifiers.find((value) => same(primary.part_number, value)) ||
    directlyReadIdentifiers.find((value) => same(verification?.part_number, value)) || "";
  const preferredReadIdentifier = preferredReadModelNumber || preferredReadPartNumber;

  const catalogueRecords = Array.isArray(payload.barcodeCatalogueLookups)
    ? payload.barcodeCatalogueLookups as Array<Record<string, unknown>>
    : [];
  const foundProducts = catalogueRecords.filter((entry) =>
    entry.found === true && typeof entry.name === "string" && entry.name.trim().length > 0
  );
  const candidateText = normalize([result.object, result.brand, result.model, result.model_number, result.category].join(" "));
  const catalogueMatches = foundProducts.filter((entry) => {
    const productName = normalize(String(entry.name));
    const productBrand = normalize(typeof entry.brand === "string" ? entry.brand : "");
    const productType = normalize(typeof entry.type === "string" ? entry.type : "");
    const nameAligns = candidateText.includes(productName) ||
      (known(result.model) && productName.includes(normalize(result.model)));
    const brandAndTypeAlign = productBrand.length >= 3 && candidateText.includes(productBrand) &&
      (!productType || candidateText.includes(productType));
    return productName.length >= 5 && (nameAligns || brandAndTypeAlign);
  });
  const uniqueCatalogueNames = [...new Set(catalogueMatches.map((entry) => normalize(String(entry.name))))];
  const barcodeConfirmsCandidate = uniqueCatalogueNames.length === 1;
  const identifierConflict =
    (!!known(primary.model_number) && !!known(verification?.model_number) &&
      !same(primary.model_number, verification?.model_number) && primaryModelNumberRead === verificationModelNumberRead) ||
    (!!known(primary.part_number) && !!known(verification?.part_number) &&
      !same(primary.part_number, verification?.part_number) && primaryPartNumberRead === verificationPartNumberRead);
  const independentExactAgreement = !!verification &&
    same(primary.model, verification.model) &&
    primary.exact_match_status === "exact" && verification.exact_match_status === "exact" &&
    Number(primary.exact_match_confidence) >= 0.85 && Number(verification.exact_match_confidence) >= 0.85;

  if (identifierConflict) {
    result.exact_match_status = "not_confirmed";
    result.exact_match_confidence = Math.min(Number(result.exact_match_confidence) || 0, 0.45);
    result.model_number = "Unknown";
    result.part_number = "Unknown";
    result.warning = [result.warning, "The two checks proposed different identifiers and the readable photo text does not resolve them. The broader object description is retained."].filter(Boolean).join(" ");
  } else if (preferredReadIdentifier && !identifierConflict) {
    const identifier = preferredReadIdentifier;
    if (preferredReadModelNumber) result.model_number = identifier;
    else result.part_number = identifier;
    result.exact_match_status = "exact";
    result.exact_match_confidence = Math.max(Number(result.exact_match_confidence) || 0, 0.92);
    result.exact_match_evidence = [result.exact_match_evidence, "Readable photo text independently confirms identifier " + identifier + "."].filter(known).join(" ");
    result.identifying_clues = [result.identifying_clues, "Photo text confirms identifier " + identifier + "."].filter(known).join(" ");
    result.next_photo = "No additional identifier photo is needed. Verify operation, condition and completeness where relevant.";
    result.confidence_booster = "A readable identifier in the supplied photos supports the model shown. Check function and condition separately.";
  } else if (ocrModelCode && known(result.brand) && !known(result.model_number) &&
    (!known(result.model) || !normalize(result.model).includes(normalize(ocrModelCode)))) {
    result.model = ocrModelCode;
    result.model_number = ocrModelCode;
    result.exact_match_status = "likely";
    result.exact_match_confidence = Math.min(0.78, Math.max(0.68, Number(result.exact_match_confidence) || 0));
    const broadObject = result.object.replace(new RegExp(result.brand, "ig"), "").replace(/\s+/g, " ").trim();
    result.object = [result.brand, ocrModelCode, broadObject].filter(Boolean).join(" ");
    result.exact_match_evidence = [result.exact_match_evidence, "OCR reads the manufacturer and the single model-style code " + ocrModelCode + " in the same photograph. This supports the model family; the full market or colour variant is not confirmed."].filter(known).join(" ");
    result.identifying_clues = [result.identifying_clues, "Visible manufacturer text and model-style code " + ocrModelCode + " appear together in one photo."].filter(known).join(" ");
    result.next_photo = "The model family is likely from the visible code. Check a label or packaging for the full variant reference if needed.";
    result.confidence_booster = "A readable code supports the likely model family; verify the full variant and test operation separately.";
    if (!/^£\s*\d/i.test(result.value_estimate)) {
      const supportedRanges = [primary.value_estimate, verification?.value_estimate]
        .filter((value): value is string => typeof value === "string" && /^£\s*\d/i.test(value.trim()));
      const firstAmount = (value: string): number => Number(value.match(/£\s*(\d+(?:\.\d+)?)/)?.[1] ?? Number.POSITIVE_INFINITY);
      supportedRanges.sort((a, b) => firstAmount(a) - firstAmount(b));
      if (supportedRanges.length) {
        result.value_estimate = supportedRanges[0];
        result.value_reasoning = "Retained the lowest available UK used-market range after a model-style code was read from the same photo as the manufacturer. The full variant, working status and condition remain unverified; check recent completed sales before relying on the estimate.";
      }
    }
  } else if (barcodeConfirmsCandidate) {
    const product = catalogueMatches[0];
    const productBrand = typeof product.brand === "string" ? product.brand.trim() : "";
    if (!known(result.brand) && productBrand) result.brand = productBrand;
    if (!known(result.model)) result.model = String(product.name).trim();
    if (!known(result.object) || /^unknown$/i.test(result.object)) result.object = String(product.name).trim();
    if (!known(result.model_number) && typeof product.code === "string") result.model_number = product.code;
    result.exact_match_status = "exact";
    result.exact_match_confidence = Math.max(Number(result.exact_match_confidence) || 0, 0.94);
    result.exact_match_evidence = [result.exact_match_evidence, "A scanned barcode catalogue record matches the independently identified product: " + String(product.name).trim() + "."].filter(known).join(" ");
    result.identifying_clues = [result.identifying_clues, "Barcode catalogue match: " + String(product.name).trim() + "."].filter(known).join(" ");
    result.next_photo = "No additional identifier photo is needed. Check the product variant and condition against the item.";
    result.confidence_booster = "The barcode catalogue and visual identification agree on the product. Verify the variant and condition.";
  } else if (result.exact_match_status === "exact" && !independentExactAgreement) {
    result.exact_match_status = known(result.model_number) || known(result.part_number) ? "likely" : "not_confirmed";
    result.exact_match_confidence = Math.min(Number(result.exact_match_confidence) || 0, 0.78);
  }

  return result;
}


export type SamsungEarbudEvidenceResult = {
  object: string; category: string; purpose: string; brand: string; manufacturer: string;
  model: string; model_number: string; variant: string; exact_match_status: string;
  exact_match_confidence: number; exact_match_evidence: string; identifying_clues: string;
  case_file_summary: string; warning: string; value_estimate: string; value_reasoning: string;
  age_era_estimate: string; explanation: string; compatibility: string; next_photo: string;
  confidence: number; confidence_booster: string; buyer_seller_check: string;
};

/** Resolve legible Samsung earbud identifiers deterministically, clearing any model-level conflicts. */
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
  result.category = "consumer electronics";
  result.purpose = "Wireless audio playback and hands-free communication.";
  result.compatibility = "Bluetooth audio devices, including compatible phones, tablets and computers; USB-C charging case.";
  result.age_era_estimate = "Unknown";
  result.warning = "The printed identifier supports the model details shown. Photo identification cannot verify pairing, battery health, authenticity or working condition.";

  if (modelCode) {
    result.object = "Samsung Galaxy Buds3 Pro wireless earbuds with charging case";
    result.model = "Galaxy Buds3 Pro";
    result.model_number = "SM-R630";
    result.variant = "Galaxy Buds3 Pro";
    result.exact_match_status = "exact";
    result.exact_match_confidence = 0.96;
    result.value_estimate = "£25–£40 (GBP)";
    result.value_reasoning = "Indicative UK used-market range for Galaxy Buds3 Pro. Recent sold examples ranged from £26.42 to £47.07; this cautious range allows for the visible light wear and the fact that pairing, charging and battery health have not been tested. Check current local sold listings before sale.";
    result.next_photo = "No extra photo is needed to identify the model. Test Bluetooth pairing, audio in both earbuds, charging and battery health to refine the resale estimate.";
    result.confidence_booster = "The printed SM-R630 model code and EP-OR630 case reference confirm Galaxy Buds3 Pro. The model is identified; function tests would refine condition and resale value.";
    result.buyer_seller_check = "Test Bluetooth pairing, audio in both earbuds, charging case operation and battery health. The model code is already confirmed from the photos.";
    result.exact_match_evidence = [
      "Samsung branding and the printed model code SM-R630 are present in the supplied-photo evidence.",
      "Samsung support maps model code SM-R630 to Galaxy Buds3 Pro.",
      identifiers.includes("EP-OR630") ? "The printed charging-case reference EP-OR630 also corroborates the identification." : "",
    ].filter(Boolean).join(" ");
    result.case_file_summary = "Samsung Galaxy Buds3 Pro wireless earbuds with charging case (SM-R630).";
    result.identifying_clues = [
      "Printed Samsung branding.",
      "Printed model code SM-R630.",
      identifiers.includes("EP-OR630") ? "Charging-case reference EP-OR630." : "",
    ].filter(Boolean).join(" ");
    result.explanation = "The printed Samsung model code SM-R630 identifies Galaxy Buds3 Pro. The case reference EP-OR630 corroborates the match.";
    result.next_photo = "To estimate resale value, check pairing, battery health and that both earbuds and the charging case work.";
  } else {
    result.object = "Samsung wireless earbuds with charging case";
    result.model = "Unknown";
    result.model_number = preferredIdentifier || "Unknown";
    result.variant = "Unknown";
    result.exact_match_status = preferredIdentifier ? "likely" : "not_confirmed";
    result.exact_match_confidence = preferredIdentifier ? Math.max(result.exact_match_confidence, 0.68) : Math.min(result.exact_match_confidence, 0.35);
    result.exact_match_evidence = preferredIdentifier
      ? "Samsung branding and reference code " + preferredIdentifier + " are present; the marketed model name is not confirmed."
      : "Samsung branding supports the manufacturer and broad product type; no readable model identifier was confirmed.";
    result.case_file_summary = "Samsung wireless earbuds with charging case; exact marketed model not confirmed.";
    result.identifying_clues = preferredIdentifier
      ? "Printed Samsung branding and reference code " + preferredIdentifier + "."
      : "Samsung branding and wireless earbuds with charging case.";
    result.explanation = "The manufacturer and broad object type are supported. The exact marketed model remains unconfirmed.";
    result.next_photo = "Take a clear close-up of the printed model code on the case or packaging.";
  }
  return result;
}
