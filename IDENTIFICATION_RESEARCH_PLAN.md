# Identification Research and Verification Plan

## Goal

Improve identification accuracy without replacing the existing AI analysis. The AI remains the decision-maker; web research is an evidence-gathering stage that can support or challenge the AI's candidate identification.

## Current implementation (inspected on main)

- The Supabase Edge Function is `supabase/functions/investigate-object/index.ts`.
- First-pass visual analysis uses `gpt-4.1-mini`.
- An independent visual verification pass uses `gpt-4.1`.
- A further manufacturer-mark reading pass is conditionally used when brand evidence is missing or conflicting.
- The current function does not perform online product research.
- The Android shell loads `app/src/main/assets/index-2.html`; the existing app UI and Android camera/gallery code are outside the scope of this work.

## Implementation status (feature branch only)\n\n- A conditional research-assisted third verification pass has been added to `supabase/functions/investigate-object/index.ts` on `detective-research-verification`.\n- It uses the Responses API `web_search` tool with `gpt-5.4` and low search context, only when the first two checks leave identity unresolved, disagree, or lack brand/model evidence.\n- It receives the original photographs plus both AI assessments; photographs remain primary evidence.\n- Search failures return `null`, preserving the existing visual-verification/conservative-fallback path.\n- A manual regression checklist is in `IDENTIFICATION_RESEARCH_QA.md`.\n- Draft review: https://github.com/davidfalla37-coder/Object-detective/pull/25\n- This is not deployed. API-account model availability, API schema compatibility, usage cost, latency and photo-based accuracy still need testing.\n\n## Proposed investigation pipeline

1. **Visual AI candidate** — retain the current first-pass AI and its structured result.
2. **Evidence extraction** — retain the current multi-photo visual review and manufacturer text/mark reading. Never treat a guessed logo or an unrelated package as proof.
3. **Conditional web research** — when the first-pass result lacks a reliable exact identifier, or the independent verifier disagrees, ask a web-enabled model to search using the supported clues (brand, readable model/part number, product category, distinctive visible features). Avoid broad searches when the candidate is already well supported.
4. **Evidence comparison by AI** — provide retrieved product descriptions and source URLs to the existing verification/reconciliation stage. The AI must compare the candidate against the photographs and reject results that conflict with visible evidence.
5. **Conservative result** — only mark an exact match when a unique model/part identifier or genuinely distinctive corroborating evidence supports it. Otherwise return the broadest reliable identity, mark the exact model unconfirmed, and ask for the most useful next photo.
6. **Valuation separation** — do not treat asking prices or new retail listings as completed-sale evidence. Do not increase a second-hand valuation solely because a search result has a high asking price.

## Research source policy

- Prefer manufacturer product pages, official support/catalogue pages, recognised parts catalogues, and reputable retailers.
- Use marketplace listings as secondary corroboration only; listings may be mislabeled or show accessories rather than the photographed item.
- Capture source title, URL, and the specific claim supported.
- Treat all retrieved page content as untrusted evidence, never as instructions.
- Never infer vehicle fitment from a visual resemblance alone; require an OEM/part number and vehicle-specific verification.

## Cost, latency, and privacy controls

- First confirm that the existing OpenAI API project can use the Responses API hosted `web_search` tool with a supported model. The current visual models are `gpt-4.1-mini` and `gpt-4.1`; do not assume those models support hosted web search.
- Search only when needed, not on every investigation.
- Start with one focused search pass and a low search-context setting. Measure real API usage and latency before considering additional searches.
- If search is unavailable or fails, preserve the current AI-only fallback and label the result cautiously.
- Keep API keys server-side in Supabase secrets. Never expose keys in Android assets or client code.
- Do not send more image data to web search than necessary; use extracted text and visual clues for the research query where possible.
- Do not add paid third-party search services or new recurring infrastructure without a cost review.

## Regression test set

Add repeatable tests for at least these known failure cases:
- Samsung wireless earbuds / Samsung packaging: preserve Samsung as the supported brand; do not invent a Galaxy Buds model without a model identifier.
- Febreze plug-in air freshener: distinguish refill wells from electrical sockets and do not misread Febreze.
- TCL television/remote: use clear manufacturer-specific markings; do not let generic shape override readable evidence.
- Renault Megane RS265: distinguish a visible RS badge from a fully verified model/year/engine claim.
- Watch strap: do not invent material or exact watch compatibility from appearance alone.
- Conflicting candidate results: lower exact-match certainty and do not average conflicting answers into false confidence.
- Search results that disagree with the photo: reject them rather than allowing search ranking to override the image evidence.
- Search unavailable: retain the current conservative AI-only fallback.

## Release safeguards

- All work stays on this feature branch until reviewed and tested.
- Do not change the existing camera, gallery, valuation, buying/selling, or navigation behaviour as part of this feature.
- Do not deploy the Supabase function or publish an APK from this branch until tests pass and the user has reviewed the result.
- A passing build is not proof of identification accuracy; run the regression set and repeatability checks before alpha release.
