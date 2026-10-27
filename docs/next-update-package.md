# Object Detective — next update package

**Prepared:** 27 October 2026  
**Working branch:** `feature/vehicle-registration-context`  
**Release status:** prepared separately; not merged, deployed, built for release, or sent to testers.

This document consolidates tonight's product decisions and implementation notes so the next update can be continued from one place.

## Decisions made

### Photo limits and Pro

- Keep the free investigation at **up to 3 photos**.
- Consider **up to 5 photos in Pro** for difficult objects and extra angles.
- Keep a useful basic investigation in Free.
- Proposed Pro benefits: more investigations, stronger/exact-match searches, richer valuations, deeper marketplace searches, saved collections, fuller follow-up assistant, seller tools, extra photos, and no ads.
- Earlier price ideas were **£4.99/month or £39.99/year**. Treat these only as starting points to test; no price is locked in.

### Repair & Engineering Guide

The product direction is: **identify it, explain what it does, help the user understand the next step, then help them find or sell a suitable part.**

The planned guide should show:
- Likely identification and the visual evidence behind it
- Confidence and any missing details
- What the part does
- Compatibility status and what still needs checking
- Tools and materials
- Repair steps only when they are supported by a trustworthy source
- Safety notes and when to stop or ask a qualified person
- Source links and relevant retailer options

Start with common, lower-risk automotive and home DIY jobs. Add engineering categories only as trusted source coverage and safety rules are ready.

### Vehicle details

- In the first version, users manually enter make, model, year and engine/trim.
- The registration field is for the user's reference only: the current branch does not send the plate for lookup, include it in AI analysis, or save it to investigation history.
- Manual vehicle details are sent with the investigation as user-provided context.
- Do not state that a part fits unless an exact part number or trusted fitment source confirms it.
- Automatic registration lookup is deferred.

### Affiliate revenue

Keep data licensing and retailer commission as separate enquiries:
- Ask data providers about commercial catalogue access and permitted use.
- Ask retailers or affiliate networks whether they accept Android apps, support part-specific tracking links, and pay commission on completed qualifying orders.
- Include a clear affiliate disclosure if commission links are used.

## What the current branch contains

The branch is `feature/vehicle-registration-context`. It is separate from `main`, so these edits have not changed the tester app.

- `app/src/main/assets/index-2.html`: manual vehicle reference field and vehicle detail inputs, sent as context; plate itself is not sent.
- `supabase/functions/investigate-object/index.ts`: receives manual vehicle details, treats them as unverified context, and avoids unsupported fitment claims.
- `privacy-policy.html`: describes manual vehicle data processing and the local-only plate field.
- `docs/repair-guide-prototype.html`: interactive sample UI for a wiper blade and a loose cabinet handle. Sample steps/results are illustrative only; there is no live repair-data retrieval.
- `docs/repair-guide-release-readiness.md`: feature scope and release gates.

The repository already has a photo investigation flow: photos go from the app to the Supabase Edge Function, which calls OpenAI's Responses API for image analysis and a second identification check. The new repair feature needs trusted repair and fitment information after visual identification; a custom image model is not the first requirement.

## Commercial data leads

Ask for a small UK consumer-app pilot, a demo or sandbox, and a written quote before committing to a provider.

- [TecDoc Web Service](https://www.tecalliance.net/products/cards/tecdoc-web-service) — vehicle, product and linkage/fitment data.
- [TecRMI](https://www.tecalliance.net/solutions/tecrmi) — repair, maintenance and diagnostic information.
- [HaynesPro](https://www.haynespro.com/) — alternative automotive technical repair data provider.

For each, ask about UK vehicle coverage, supported parts, licensing for Android consumer apps, showing source-linked summaries, caching/retention, per-lookup costs, minimum contracts and whether fitment and repair data can be bundled.

For affiliate links, candidate programmes to check include:
- [Euro Car Parts on Awin](https://ui.awin.com/merchant-profile/3997)
- [GSF Car Parts on Awin](https://ui.awin.com/merchant-profile/12707)
- [AUTODOC UK on Awin](https://ui.awin.com/merchant-profile-terms/12639)

Confirm that each programme accepts app traffic and part-specific deep links before integrating it. Rates and terms can change.

## Remaining work before a live release

1. Pick the first categories and define which jobs the app will not guide.
2. Get provider demos and quotes; confirm commercial rights for the intended in-app use.
3. Build a source retrieval step that matches a guide to the photographed product, model and part number.
4. Add structured guide results with source links, evidence, fitment status and safety limits.
5. Add retailer deep links with approved affiliate tracking and disclosure.
6. Update privacy and Play Data safety details if the final provider/data flow differs from the current manual-only branch.
7. Review the complete flow, then decide when to merge and build the next testing release.

Until those items are complete, the Repair Guide screen remains a prototype and its example steps must not be presented as live, verified advice. Leave the current Play test release unchanged while the agreed 14-day test period is running.
