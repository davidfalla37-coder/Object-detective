import { hasConflictingModelIdentifiers, manufacturersConflict, normalizeManufacturerName } from "./identity-evidence.ts";

Deno.test("different model codes cannot be treated as an exact agreement", () => {
  if (!hasConflictingModelIdentifiers("Samsung Galaxy Buds3 Pro SM-R630", "Samsung Galaxy Buds2 Pro SM-R510")) {
    throw new Error("Expected distinct model identifiers to conflict");
  }
});

Deno.test("a shared code among several identifiers confirms identifier overlap", () => {
  if (hasConflictingModelIdentifiers("SM-R630 EP-OR630", "Samsung SM-R630")) {
    throw new Error("Expected a shared model code to prevent a false conflict");
  }
});

Deno.test("different code formatting is normalized", () => {
  if (hasConflictingModelIdentifiers("SM-R630", "sm r630")) {
    throw new Error("Expected punctuation and case differences to normalize");
  }
});

Deno.test("generic names and missing identifiers do not create a false conflict", () => {
  if (hasConflictingModelIdentifiers("wireless earbuds", "Samsung earbuds")) {
    throw new Error("Expected generic names to remain eligible for broad identity comparison");
  }
  if (hasConflictingModelIdentifiers("SM-R630", "Unknown")) {
    throw new Error("Expected one missing identifier to leave the result partial, not conflicting");
  }
});


Deno.test("Casio and G-SHOCK are one manufacturer, not an identity conflict", () => {
  if (normalizeManufacturerName("CASIO") !== normalizeManufacturerName("G-SHOCK")) {
    throw new Error("Expected the Casio maker mark and G-SHOCK product-line mark to agree");
  }
  if (manufacturersConflict("Casio", "G-SHOCK")) {
    throw new Error("Expected Casio/G-SHOCK photos to avoid the unknown-identity fallback");
  }
});

Deno.test("genuinely different manufacturers still trigger a conflict", () => {
  if (!manufacturersConflict("Casio", "Samsung")) {
    throw new Error("Expected unrelated manufacturer marks to remain a conflict");
  }
});
