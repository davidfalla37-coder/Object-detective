# Object Detective Pro — implementation and acceptance checklist

This document is the engineering gate for Pro. It deliberately separates preparation from activation.

## Current known blockers
- The app's existing investigation endpoint is callable without a signed-in user.
- There is no per-user usage ledger, so usage caps cannot be enforced safely.
- There is no existing subscription entitlement or Google Play purchase verification.
- The current app is a Kotlin WebView shell; the investigation UI and API calls live in the web asset.
- Existing Free behavior must not be changed as part of Pro preparation.

## Required implementation sequence

### A. Identity (before any purchase)
- Introduce Supabase Auth identity with a user-scoped session in the Android app.
- Anonymous/guest use must continue to be supported if currently supported; guest investigations must not silently become paid or be blocked.
- Never use a device ID, email string, JavaScript flag, or locally stored tier as proof of Pro.
- Validate the signed-in user's access token on the backend for all entitlement and account-specific usage operations.
- Test sign-out, reinstall, restore, and account switching.

### B. Entitlement and usage service
- Apply the reviewed SQL migration only after confirming Auth is enabled and RLS behavior in a disposable/test project.
- Create a backend-only entitlement check. Fail closed to Free when verification is unavailable.
- Create a usage ledger with idempotency keys and server-side limits only after the free allowance and cost budget have been agreed.
- Never trust a client-supplied tier, quota, purchase state, or remaining count.
- Avoid changing the existing investigation function until a compatibility plan covers old installed app versions.

### C. Google Play Billing
- Add the supported Play Billing dependency to the Android project only after checking the current Play Billing Library requirements.
- Launch purchase flow only after the user explicitly chooses a clearly priced plan.
- Send purchase token to the trusted backend over TLS.
- Verify with Google Play Developer API; for subscriptions use subscriptionsv2.get as source of truth.
- Grant access only after a verified PURCHASED state. Pending purchases must not unlock Pro.
- Implement acknowledgement, restore/query purchases, account binding, token replay protection, expiry, cancellation, grace period, account hold, pause, refund/revoke and Real-time Developer Notifications.
- Keep all service-account credentials and token encryption keys in backend secrets.
- Use Play licence testers and test products. No real product activation or live charge during development.

### D. Pro capabilities
Only expose a feature as Pro once its server-side or client-side behavior exists and passes acceptance tests:
- Additional investigation allowance with transparent limits and cost controls.
- Research/comparison evidence with source links and timestamps.
- Valuation ranges with evidence, age, and uncertainty; never promise guaranteed sale prices.
- Saved collections and export/share reports.
- Seller assistance and guided vehicle/engineering checks.
Do not claim "exact match", guaranteed identification, unlimited use, 3D analysis, or better accuracy unless validated by measured results.

### E. Compatibility and regression
- Current Free flow works on Samsung Galaxy S25, camera and gallery, HEIC, multiple photos, back navigation, buy links, selling tools and appearance settings.
- Existing beta users do not unexpectedly lose access or have their data reset.
- Old app versions calling the current investigation endpoint continue to work during rollout.
- Network failures, auth expiry, offline state, duplicate taps, app restarts and concurrent investigations do not create duplicate charges or quota deductions.
- Accessibility, privacy disclosures, data deletion, retention, and Play Data Safety answers are reviewed.

## Acceptance criteria before any public Pro launch
- [ ] No client can self-award Pro by editing local storage, API payloads, or app files.
- [ ] No pending, cancelled, expired, refunded, revoked, or invalid purchase grants Pro.
- [ ] Valid purchase restores on reinstall and a second device after sign-in.
- [ ] Free users retain the current promised experience.
- [ ] Every price, renewal period, trial condition, and cancellation route is clear before purchase.
- [ ] No trial converts to paid without an explicit purchase choice.
- [ ] Cost per investigation and abuse exposure are measured and bounded.
- [ ] Billing test suite passes with Play licence testers.
- [ ] No production release, live product, paid service, or real charge occurs without explicit owner approval.

## Cost guardrails
- Use existing GitHub and Supabase resources only for inspection and local/test development.
- Do not create a paid Supabase branch, upgrade a plan, add paid third-party APIs, create live Play products, or start advertising.
- Before any change that could incur a charge, stop and ask for approval.


### F. Accuracy and cost regression gate

- [ ] Run the fixed photo benchmark described in [ACCURACY_AND_COST_AUDIT.md](ACCURACY_AND_COST_AUDIT.md) using genuine, consented fixtures.
- [ ] Include the reported Samsung earbuds, Febreze air freshener, Renault RS, watch strap, and TCL cases.
- [ ] Score broad object, brand, exact model, evidence, condition, and valuation separately.
- [ ] Repeat selected photo sets to check identity and valuation consistency.
- [ ] Reject wrong explicit brands, invented exact models, unsupported safety/compatibility claims, and materially inflated values.
- [ ] Record model request count and latency; do not add model calls without a measured benefit and approved cost budget.
- [ ] Use mocked/offline tests first. Do not spend OpenAI API credits on live benchmark runs without an agreed request budget.
