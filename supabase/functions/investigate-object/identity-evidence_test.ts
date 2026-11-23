import { deriveVerificationVerdict } from "./identity-evidence.ts";

function assertEqual(actual: unknown, expected: unknown, label: string): void {
  if (actual !== expected) {
    throw new Error(label + ": expected " + expected + ", received " + actual);
  }
}

Deno.test("different readable manufacturers are a disagreement", () => {
  assertEqual(
    deriveVerificationVerdict(
      { object: "wireless earbuds", category: "audio", brand: "Samsung" },
      { object: "wireless earbuds", category: "audio", brand: "SoundPEATS", verdict: "agree" },
    ),
    "disagree",
    "brand conflict",
  );
});

Deno.test("one shared generic word cannot produce agreement", () => {
  assertEqual(
    deriveVerificationVerdict(
      { object: "wall plug fragrance warmer", category: "home fragrance", brand: "Febreze" },
      { object: "electrical plug adapter", category: "power accessory", brand: "Febreze", verdict: "agree" },
    ),
    "partial",
    "single shared term",
  );
});

Deno.test("multiple matching descriptive terms can agree when brands match", () => {
  assertEqual(
    deriveVerificationVerdict(
      { object: "Samsung wireless earbuds charging case", category: "audio", brand: "Samsung" },
      { object: "Samsung wireless earbuds with charging case", category: "audio", brand: "Samsung", verdict: "agree" },
    ),
    "agree",
    "matching identity",
  );
});

Deno.test("same brand but no object overlap remains partial", () => {
  assertEqual(
    deriveVerificationVerdict(
      { object: "Samsung television", category: "electronics", brand: "Samsung" },
      { object: "Samsung refrigerator", category: "appliance", brand: "Samsung", verdict: "agree" },
    ),
    "partial",
    "brand alone",
  );
});

Deno.test("empty descriptions remain insufficient", () => {
  assertEqual(
    deriveVerificationVerdict(
      { object: "Unknown", category: "Unknown", brand: "Unknown" },
      { object: "Unknown", category: "Unknown", brand: "Unknown", verdict: "agree" },
    ),
    "insufficient",
    "empty evidence",
  );
});
