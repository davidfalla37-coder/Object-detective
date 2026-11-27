# Object Detective Pro — isolated build plan

## Purpose
Build Free and Pro together from one shared identification engine. Pro adds depth and capacity; it must not be a separate, divergent identification implementation.

## Non-negotiable safeguards
- Work only on the `object-detective-pro-preview` branch and the isolated preview Supabase project.
- Do not merge to the default/release branch, publish to Google Play, alter the live Supabase project, or enable paid billing as part of this work.
- Do not introduce real charges. Purchases and entitlements must be simulated until Google Play Billing is deliberately integrated and tested.
- Preserve existing Free functionality. A Pro feature must never block a Free user from core identification or safety-critical information.
- Do not claim exact matches, photographs, valuations, or confidence beyond the evidence actually obtained.

## Shared improvements for Free and Pro
1. Photo analysis, OCR, barcode/ISBN lookup, and manufacturer/catalogue evidence feed one reconciliation pipeline.
2. Exact model/part-number matches are distinguished from likely and similar matches.
3. Relevant reference images are shown only when their source and match quality are clear; otherwise explain that no verified image was found.
4. Confidence and uncertainty are evidence-based and consistent across tiers.
5. Shared reliability, latency, and cost controls apply to both tiers.

## Pro capabilities to build behind an inactive entitlement gate
- Higher investigation and photo allowances.
- Deeper manufacturer and specialist catalogue searches.
- Additional exact model/part-number cross-checks.
- Enhanced valuation research with comparable-item evidence and date/context.
- Saved collections and richer investigation reports.
- Expanded seller tools and exportable results.

## Free tier principles
- Keep a genuinely useful everyday identification allowance.
- Keep barcode/ISBN and core photo identification available.
- Keep safety notices, uncertainty disclosures, and basic result evidence available.
- Explain limits clearly before a user reaches them; never use deceptive countdowns or forced subscription flows.

## Proposed commercial assumptions — not yet committed
- Previously discussed indicative pricing: £4.99 monthly / £39.99 annually.
- Validate service costs, usage limits, and customer value before finalising prices.
- No auto-enrolment, no paid trial that silently converts, clear renewal terms, and straightforward cancellation.

## Implementation sequence
1. Audit current app flows and existing Pro planning/entitlement work before changing production-facing code.
2. Add a test-only entitlement abstraction with Free as the default and Pro simulation controlled locally in preview.
3. Implement and test Pro capability gates without requiring sign-in, billing, or production database changes.
4. Add premium capabilities incrementally while regression-testing the shared Free experience.
5. Add purchase integration only in a later, explicitly approved stage; verify restore purchases, cancellation state, server-side entitlement validation, and failure handling before any launch.
6. Run functional, security, cost, and S25 device tests before considering a release.

## Acceptance criteria
- Fresh install defaults to Free.
- Preview-only Pro simulation can be enabled/disabled without a payment.
- Free core identification continues to work when Pro is disabled or entitlement checks fail.
- No client-side secret keys or trusted client-supplied entitlement claims.
- Usage limits and cost controls are tested with simulated requests.
- Existing camera, gallery, barcode/QR, history, visual references, marketplace, settings, and navigation regressions pass.
- No changes reach Google Play or the live Supabase project during preview development.
