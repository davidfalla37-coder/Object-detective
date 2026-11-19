import { stripIdentityClaims } from "./identity_text.ts";

function assertEquals(actual: string, expected: string): void {
  if (actual !== expected) {
    throw new Error("Expected " + JSON.stringify(expected) + " but received " + JSON.stringify(actual));
  }
}

Deno.test("brand conflict keeps the broad earbuds identity", () => {
  assertEquals(
    stripIdentityClaims("Samsung wireless earbuds with charging case", ["Samsung", "Galaxy Buds"]),
    "wireless earbuds with charging case",
  );
});

Deno.test("brand conflict keeps the broad television remote identity", () => {
  assertEquals(
    stripIdentityClaims("TCL television remote control", ["TCL", "RC902V"]),
    "television remote control",
  );
});

Deno.test("brand conflict keeps the broad air freshener identity", () => {
  assertEquals(
    stripIdentityClaims("Febreze wall-plug air freshener diffuser", ["Febreze"]),
    "wall-plug air freshener diffuser",
  );
});

Deno.test("identity stripping does not erase unrelated words", () => {
  assertEquals(
    stripIdentityClaims("Renault Megane RS265 front bumper", ["Renault", "Megane RS265"]),
    "front bumper",
  );
});

Deno.test("identity claims containing regular-expression characters are safe", () => {
  assertEquals(
    stripIdentityClaims("Acme (Model X) cordless drill", ["Acme", "(Model X)"]),
    "cordless drill",
  );
});

Deno.test("empty or unknown identity claims leave the description intact", () => {
  assertEquals(stripIdentityClaims("watch strap", ["", "Unknown"]), "watch strap");
});

Deno.test("an empty result is reported as Unknown rather than an invented category", () => {
  assertEquals(stripIdentityClaims("Samsung", ["Samsung"]), "Unknown");
});
