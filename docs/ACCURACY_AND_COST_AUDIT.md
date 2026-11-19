# Object Detective — accuracy and investigation-cost audit

Date: 19 November 2026
Branch: `feature/pro-foundation`
Status: inspection only; no live service or app behavior changed.

## What the current investigation pipeline does

The deployed Supabase Edge Function is `investigate-object`, currently active at version 41. It accepts image investigations and follow-up chat requests. Its deployed configuration has `verify_jwt = false`.

For an image investigation, the current handler:
1. Accepts between one and five image data URLs.
2. Starts an independent verification request using `gpt-4.1`.
3. Starts the main structured identification request using `gpt-4.1-mini`.
4. Reconciles the two model outputs.
5. May make a further `gpt-4.1` manufacturer-mark reading request when the two outputs conflict, or brand evidence is missing while the exact identity is unclear.

The two main model calls run concurrently. Therefore, an investigation normally causes at least two OpenAI requests, and some cases can cause a third. Follow-up chat causes another request. There is no per-user identity, quota ledger, or server-side usage budget in the inspected handler.

The handler already contains substantial instructions about reading labels, not guessing exact models, distinguishing a Febreze air-freshener device from a plug adapter, conservative used-value estimates, and separating object confidence from exact-model confidence. This means prompt wording alone is not a sufficient fix for the reported errors; the complete pipeline needs repeatable evidence-based tests.

## Reported regression cases to include in the benchmark

These cases were reported during real app testing and should be retained as regression fixtures. Use the original user-supplied photos when available; do not fabricate expected model numbers or prices.

| Fixture | Known evidence / expected behavior | Failure to prevent |
|---|---|---|
| Samsung wireless earbuds | Matching packaging visibly reads Samsung wireless earbuds. Identify Samsung as the supported brand and wireless earbuds as the broad item; do not invent a Galaxy Buds model without a readable model identifier. | Calling them SoundPEATS or another brand; changing identity on repeated runs. |
| Febreze air freshener | Brand and refill openings / device shape should be read in context. Distinguish an air-freshener device from an electrical multi-socket adapter. | Misclassifying it as a plug adapter or misreading Febreze as another brand. |
| Renault Megane RS265 | The visible RS marking supports the RS line; exact year/trim/model should only be stated when the photos support it. | Ignoring a readable RS mark or inventing unsupported vehicle details. |
| Watch strap | Identify the object as a watch strap where supported; material, brand and exact model remain unknown unless directly evidenced. | Treating an uncertain material or brand as fact. |
| TCL television / remote | Read manufacturer-specific marks literally; streaming-service shortcut buttons alone do not identify the remote maker or TV model. | Incorrectly naming another TV brand from appearance alone. |

## Acceptance rules for identification and valuation

- Report the whole photographed item, not just one replaceable component.
- Read visible text literally. If text is ambiguous, say it is unreadable rather than choosing a visually similar brand.
- Separate broad object confidence from exact model confidence.
- A brand on matching packaging may support the brand and broad product type, but does not prove the exact model.
- Only use `exact` when a clear model/reference/part identifier uniquely supports it; otherwise use `likely` or `not_confirmed`.
- Do not let repeated model calls create contradictory identity fields. Reconciliation must preserve clear direct evidence and surface unresolved conflicts.
- Do not invent sold-price evidence. Keep UK second-hand value estimates cautious, state uncertainty, and use an unknown value when evidence is inadequate.
- A photo cannot establish that an electronic or mechanical item works.
- Repeating the same photo set should not produce materially different brands, models, or value ranges without explaining what evidence changed.

## Required benchmark process before accuracy claims

1. Build a fixed, versioned photo fixture set from genuine user test cases, with the correct facts and the evidence visible in each photo recorded separately.
2. Run every fixture against the candidate backend version and save the structured output, model names, prompt/version identifiers, and timestamps. Avoid retaining original photos longer than needed; document consent and retention.
3. Score broad object, brand, model, exact-match status, evidence extraction, condition, and value as separate dimensions.
4. Repeat selected fixtures to measure output consistency.
5. Fail the release gate for a wrong explicit brand, invented exact model, unsupported compatibility/safety claim, or materially inflated valuation.
6. Compare latency and request count as well as accuracy. Do not add more model calls by default as a substitute for better reconciliation.
7. Re-run the same suite after every prompt, model, or reconciliation change.

## Cost and safety gate

- Do not run a benchmark against the live OpenAI-backed endpoint without first agreeing a request budget; each run can incur API cost.
- Prefer local fixtures, mocked model responses, and offline unit tests for deterministic logic.
- Before changing the live endpoint, add authentication/abuse controls and a per-user usage ledger, with a compatibility plan for older app versions.
- Never expose OpenAI keys, Supabase service-role keys, Google Play service-account credentials, or purchase tokens to the Android/WebView client.
- No production deployment, paid service, database migration, or public app release is part of this audit.

## Immediate engineering conclusion

The existing prompt already addresses several reported error types, but the production path is a multi-call model pipeline without user identity or usage controls. The next safe work is to extract and test deterministic reconciliation/validation logic with mocked responses, then design the authenticated endpoint migration so existing app versions keep working. Do not claim accuracy is fixed until the regression suite passes on real fixtures.
