# Object Detective Pro — implementation plan

## Goal
Prepare an optional paid Pro tier without changing or restricting the existing Free experience. Pro remains unavailable to customers until implementation, testing, Play Console product setup, and policy checks are complete.

## Non-negotiable product rules
- Free investigations continue to work exactly as they do today.
- Never start a paid subscription automatically or convert a trial into a paid plan without a fresh, explicit purchase action.
- Show the full price, billing period, renewal terms, and cancellation route before purchase.
- Provide a clear route to manage/cancel a subscription.
- Do not claim an investigation is more accurate merely because it is paid. Pro value must come from real, implemented capabilities.
- Do not add paid API calls or other recurring costs until their cost and limits are understood.
- No purchase UI should imply that payment is live while billing is disabled.

## Proposed offer (not yet activated)
- Pro monthly: proposed £4.99/month.
- Pro annual: proposed £39.99/year.
These are working proposals only. Confirm affordability, Google Play pricing, tax, service costs, and customer value before creating live products.

## Proposed Free / Pro boundary
### Free
- Existing identification and result experience.
- Existing five-photo workflow where currently supported.
- Existing saved investigations, selling tools, and marketplace links remain available as they are today.
- A transparent usage allowance may be introduced only after usage/cost data is available and existing users are not unexpectedly locked out.

### Pro candidates
Prioritise features that can be delivered and tested reliably:
1. Expanded investigation allowance, with clear limits shown before use.
2. More detailed research and comparison of matching listings.
3. Enhanced valuation evidence, with source/date and uncertainty clearly shown.
4. Exportable/shareable investigation reports and organised collections.
5. Advanced seller tools and saved listing drafts.
6. Additional guided checks for vehicle and engineering parts, with safety/compatibility warnings retained.

Do not promise exact-match identification, guaranteed valuations, 3D analysis, or unlimited use unless those capabilities are actually implemented and validated.

## Technical architecture — required before billing
1. Inspect the current Android WebView app, investigation endpoint, hosting/backend, privacy/data flows, and existing Play Console configuration.
2. Add a single entitlement model (Free / Pro / expired / pending) with a safe Free fallback if entitlement cannot be verified.
3. Use Google Play Billing for Android purchases. Verify purchase tokens securely on a trusted backend; never trust a JavaScript flag or local storage as proof of payment.
4. Acknowledge purchases only after verification; handle pending, cancelled, refunded, expired, grace-period, and account/device restore states.
5. Keep secrets and service-account credentials out of the Android app, web assets, and repository.
6. Add test-only entitlement fixtures so Free and Pro can be tested without real charges.
7. Add analytics only where privacy-compliant and necessary to measure usage, conversion, refunds, and service cost.

## Delivery stages
- Stage 0: repository/backend/Play configuration audit and feature specification.
- Stage 1: implement Free/Pro entitlement boundary and Pro preview, with billing disabled.
- Stage 2: implement and test Pro-only capabilities.
- Stage 3: integrate Google Play Billing and backend purchase verification using licence testers and test products.
- Stage 4: regression-test Free, Pro, restore/cancel/refund flows, accessibility, privacy, and costs.
- Stage 5: create live products and launch only after explicit approval.

## Release safety
- Work on a dedicated feature branch.
- Do not merge to main, change the current release version, trigger a public release, or enable live billing as part of this preparatory work.
- Every stage must build and pass regression checks before it is considered complete.

## Open technical questions
- Which backend currently serves the identify/chat requests, and where is it hosted?
- Is the app already configured for Google Play Billing or only the Play in-app update API?
- What are the real per-investigation model/search costs and current usage?
- Which proposed Pro features can be delivered without new paid third-party services?
