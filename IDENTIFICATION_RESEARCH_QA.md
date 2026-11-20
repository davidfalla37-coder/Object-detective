# Identification Research Regression Checklist

Run this checklist against the deployed test function and the same photo sets before any production deployment.

## Functional gates

- [ ] A clearly identified, model-numbered item searches for its exact identifier and returns a match only when the source and photo details agree.
- [ ] An unbranded or generic item does not receive an invented brand or model.
- [ ] A readable brand on matching packaging supports the brand, but does not invent an exact model.
- [ ] Search results that conflict with the photos are rejected.
- [ ] Search failure, unsupported model, timeout, or rate limit falls back to the existing conservative visual result.
- [ ] Research output includes source titles and URLs in its evidence text.
- [ ] Research output does not override the photographed object's identity merely because a marketplace title ranks highly.
- [ ] Research does not upgrade an exact match without unique model/part evidence.
- [ ] Research does not change second-hand values using asking prices or new-retail prices.
- [ ] A brand disagreement on a non-earbud category (for example a TV, air freshener, watch, or vehicle part) retains a generic description of that photographed category and is never rewritten as wireless earbuds.
- [ ] Vehicle fitment remains unconfirmed without OEM/part-number and vehicle-specific evidence.
- [ ] The chat, camera, gallery, five-photo upload, buying/selling, navigation, and appearance settings remain unchanged.

## Known failure-case set

1. **Samsung wireless earbuds** — Samsung text on matching packaging should support Samsung wireless earbuds; do not invent Galaxy Buds model without a model identifier.
2. **Febreze plug-in air freshener** — identify the air-freshener warmer/refill device; refill wells must not be called electrical sockets or a power adapter.
3. **TCL television/remote** — a clearly readable TCL-specific mark supports TCL; do not infer the exact television model from a remote.
4. **Renault Megane RS265** — visible RS badge is supporting evidence, but do not claim engine/year/variant details unless separately established.
5. **Watch strap** — do not infer material or exact watch compatibility from appearance alone.
6. **Misleading marketplace listing** — reject a result whose shape, ports, label, or other visible features do not match the photo.
7. **Conflicting AI checks** — research may resolve the broad identity only when reliable sources and visible evidence agree; otherwise keep uncertainty.
8. **No search availability** — the existing conservative AI result remains available and no user-facing hard failure is introduced.

## Repeatability and cost

- [ ] Repeat each case at least three times with the same photos.
- [ ] Record broad-object accuracy, brand accuracy, exact-model accuracy, false exact-match rate, latency, and OpenAI usage cost separately.
- [ ] Compare the same cases against the current main-branch behaviour.
- [ ] Do not claim near-100% accuracy from a small test set.
- [ ] Do not merge or deploy until the regression results and API account availability/cost have been reviewed.

## Deployment boundary

This checklist is a test plan, not evidence that the live service has been changed. The research implementation remains on the feature branch until it passes the checks above.
