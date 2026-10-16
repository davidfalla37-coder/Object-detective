import {
  conservativeFallback,
  handleRequest,
  getVerifiedSupabaseUser,
  isValidImageDataUrl,
  normalizeResult,
  normalizeVerification,
  readLimitedJson,
  OPENAI_NO_STORE,
  reconcileVerification,
} from "./index.ts";

function assert(condition: unknown, message: string): asserts condition {
  if (!condition) throw new Error(message);
}

function assertEquals(actual: unknown, expected: unknown, message: string): void {
  if (actual !== expected) {
    throw new Error(`${message}: expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
  }
}

function assertIncludes(actual: string, expected: string, message: string): void {
  if (!actual.includes(expected)) throw new Error(`${message}: missing ${JSON.stringify(expected)}`);
}

async function assertErrorCode(response: Response, status: number, code: string): Promise<void> {
  assertEquals(response.status, status, "response status");
  const body = await response.json();
  assertEquals(body.error?.code, code, "error code");
}

function primaryResult(overrides: Record<string, unknown> = {}) {
  return normalizeResult({
    object: "complete photographed device",
    category: "electronics",
    confidence: 0.91,
    exact_match_status: "likely",
    exact_match_confidence: 0.82,
    condition: "Used; appears functional",
    condition_grade: "used",
    condition_evidence: "Overall intact and appears functional",
    ...overrides,
  });
}

function verification(overrides: Record<string, unknown> = {}) {
  return normalizeVerification({
    verdict: "agree",
    object: "complete photographed device",
    brand: "Acme",
    model: "Model 4",
    model_number: "A4",
    variant: "standard",
    value_estimate: "£40–£60",
    confidence: 0.8,
    exact_match_status: "exact",
    exact_match_confidence: 0.75,
    evidence: "A4 is visible on the label.",
    next_photo: "No additional photo needed.",
    ...overrides,
  });
}

Deno.test("all Responses API calls disable stored response state", async () => {
  assertEquals(OPENAI_NO_STORE.store, false, "shared Responses API storage setting");
  const source = await Deno.readTextFile(new URL("./index.ts", import.meta.url));
  assertEquals(
    (source.match(/\.\.\.OPENAI_NO_STORE/g) ?? []).length,
    3,
    "every Responses API request must include the no-store setting",
  );
});

Deno.test("image data URL validation accepts supported image types and rejects malformed URLs", () => {
  assert(isValidImageDataUrl("data:image/jpeg;base64,Zm9v"), "JPEG should be accepted");
  assert(isValidImageDataUrl("data:image/PNG;base64,Zm9v"), "PNG MIME matching should be case-insensitive");
  assert(isValidImageDataUrl("data:image/webp;base64,Zm9v"), "WEBP should be accepted");
  assert(!isValidImageDataUrl("data:image/svg+xml;base64,PHN2Zz4="), "SVG must be rejected");
  assert(!isValidImageDataUrl("data:image/jpeg,Zm9v"), "base64 marker is required");
  assert(!isValidImageDataUrl("data:image/jpeg;base64,"), "empty image data must be rejected");
  assert(!isValidImageDataUrl("data:image/jpeg;base64,Zm9v!"), "invalid base64 characters must be rejected");
});

Deno.test("request handler returns predictable validation errors without calling upstream services", async () => {
  const preflight = await handleRequest(new Request("https://edge.test/", { method: "OPTIONS" }));
  assertEquals(preflight.status, 204, "OPTIONS status");

  await assertErrorCode(
    await handleRequest(new Request("https://edge.test/", { method: "GET" })),
    405,
    "method_not_allowed",
  );
  await assertErrorCode(
    await handleRequest(new Request("https://edge.test/", { method: "POST", body: "{" })),
    400,
    "invalid_json",
  );
  await assertErrorCode(
    await handleRequest(new Request("https://edge.test/", { method: "POST", body: "{}" })),
    400,
    "missing_image",
  );
  await assertErrorCode(
    await handleRequest(new Request("https://edge.test/", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ image: "data:image/svg+xml;base64,PHN2Zz4=" }),
    })),
    400,
    "invalid_image",
  );
});

Deno.test("valid analysis and chat requests require a Supabase user session", async () => {
  const analysis = await handleRequest(new Request("https://edge.test/", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ image: "data:image/jpeg;base64,Zm9v" }),
  }));
  await assertErrorCode(analysis, 401, "authentication_required");

  const chat = await handleRequest(new Request("https://edge.test/", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ mode: "chat", question: "What is this?" }),
  }));
  await assertErrorCode(chat, 401, "authentication_required");
});

Deno.test("session verification accepts only a user returned by Supabase Auth", async () => {
  const request = new Request("https://edge.test/", {
    method: "POST",
    headers: {
      "apikey": "test-publishable-key",
      "authorization": "Bearer test-user-token",
    },
    body: "{}",
  });
  let authRequestSeen = false;
  const user = await getVerifiedSupabaseUser(request, {
    supabaseUrl: "https://project.supabase.co",
    fetcher: async (input, init) => {
      authRequestSeen = String(input) === "https://project.supabase.co/auth/v1/user";
      assertEquals((init?.headers as Record<string, string>).Authorization, "Bearer test-user-token", "bearer token forwarded for Auth validation");
      return Response.json({ id: "123e4567-e89b-12d3-a456-426614174000", is_anonymous: true });
    },
  });
  assert(authRequestSeen, "Auth service is queried");
  assertEquals(user?.id, "123e4567-e89b-12d3-a456-426614174000", "verified user id");
  assertEquals(user?.is_anonymous, true, "anonymous account type");

  const rejected = await getVerifiedSupabaseUser(request, {
    supabaseUrl: "https://project.supabase.co",
    fetcher: async () => new Response("{}", { status: 401 }),
  });
  assertEquals(rejected, null, "invalid session is rejected");
});

Deno.test("request JSON parsing enforces a streaming size limit", async () => {
  const oversized = await readLimitedJson(
    new Request("https://edge.test/", { method: "POST", body: '{"value":"123456"}' }),
    8,
  );
  assertEquals(oversized.ok, false, "oversized body is rejected");
  if (!oversized.ok) assertEquals(oversized.tooLarge, true, "oversized body has size error");

  const valid = await readLimitedJson(
    new Request("https://edge.test/", { method: "POST", body: '{"ok":true}' }),
    32,
  );
  assertEquals(valid.ok, true, "small JSON body is parsed");
  if (valid.ok) assertEquals((valid.value as { ok: boolean }).ok, true, "parsed JSON value");
});

Deno.test("result normalization clamps confidence and rejects unsupported labels", () => {
  const result = primaryResult({
    confidence: 1.7,
    exact_match_confidence: -0.2,
    exact_match_status: "confirmed",
    condition_grade: "excellent",
  });
  assertEquals(result.confidence, 1, "confidence upper bound");
  assertEquals(result.exact_match_confidence, 0, "exact confidence lower bound");
  assertEquals(result.exact_match_status, "not_confirmed", "unsupported exact-match status");
  assertEquals(result.condition_grade, "cannot_tell", "unsupported condition grade");
  assertIncludes(result.condition, "functionality not verified", "condition safety wording");
  assertIncludes(result.condition_evidence, "functionality not verified", "condition evidence safety wording");
});

Deno.test("result normalization keeps the complete photographed object when model fields name a component", () => {
  const result = primaryResult({
    object: "complete photographed device",
    brand: "Acme",
    model: "Tank 2",
  });
  assertEquals(result.object, "complete photographed device", "whole-item description");
});

Deno.test("verification normalization clamps numeric fields and treats unknown verdicts conservatively", () => {
  const result = verification({
    verdict: "unexpected",
    confidence: 2,
    exact_match_confidence: -1,
    evidence: "Proposed value is £100. The visible A4 label supports the model.",
  });
  assertEquals(result.verdict, "insufficient", "unsupported verdict");
  assertEquals(result.confidence, 1, "verification confidence upper bound");
  assertEquals(result.exact_match_confidence, 0, "verification exact confidence lower bound");
  assert(!result.evidence.includes("Proposed value"), "proposed price commentary must be removed");
});

Deno.test("disagreement lowers confidence, removes exact-match claims, and adds a warning", () => {
  const result = reconcileVerification(
    primaryResult({ brand: "Acme", model_number: "A4" }),
    verification({
      verdict: "disagree",
      object: "different device",
      confidence: 0.99,
      exact_match_status: "exact",
      exact_match_confidence: 0.99,
    }),
  );
  assertEquals(result.object, "different device", "corrected object");
  assertEquals(result.exact_match_status, "not_confirmed", "disagreement exact status");
  assert(result.confidence <= 0.68, "disagreement confidence must be capped");
  assert(result.exact_match_confidence <= 0.55, "disagreement exact confidence must be capped");
  assertIncludes(result.warning, "Independent verification disagreed", "disagreement warning");
});

Deno.test("an exact status requires a strong identifier and uses the lower confidence", () => {
  const exact = reconcileVerification(
    primaryResult({ brand: "Acme", model_number: "A4", exact_match_confidence: 0.82 }),
    verification({ exact_match_confidence: 0.75 }),
  );
  assertEquals(exact.exact_match_status, "exact", "identified model may remain exact");
  assertEquals(exact.exact_match_confidence, 0.75, "exact confidence uses the lower check");

  const unconfirmed = reconcileVerification(
    primaryResult({ brand: "Unknown", model_number: "Unknown" }),
    verification({ brand: "Unknown", model: "Unknown", model_number: "Unknown", exact_match_confidence: 0.75 }),
  );
  assertEquals(unconfirmed.exact_match_status, "likely", "exact status downgrades without a strong identifier");
});

Deno.test("partial and unavailable verification remain cautious", () => {
  const partial = reconcileVerification(
    primaryResult({ confidence: 0.95, exact_match_confidence: 0.95 }),
    verification({
      verdict: "partial",
      exact_match_status: "not_confirmed",
      confidence: 0.9,
      exact_match_confidence: 0.9,
    }),
  );
  assertEquals(partial.exact_match_status, "not_confirmed", "partial result exact status");
  assert(partial.exact_match_confidence <= 0.74, "partial exact confidence must be capped");

  const fallback = conservativeFallback(primaryResult({
    confidence: 0.99,
    exact_match_status: "exact",
    exact_match_confidence: 0.99,
  }));
  assertEquals(fallback.exact_match_status, "likely", "fallback downgrades exact status");
  assert(fallback.confidence <= 0.79, "fallback confidence must be capped");
  assert(fallback.exact_match_confidence <= 0.69, "fallback exact confidence must be capped");
  assertIncludes(fallback.warning, "second verification pass was unavailable", "fallback warning");
});
