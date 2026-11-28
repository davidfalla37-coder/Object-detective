import { hasConflictingModelIdentifiers } from "./identity-evidence.ts";

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
