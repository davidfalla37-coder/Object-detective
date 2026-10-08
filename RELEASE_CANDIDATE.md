# Object Detective — next update candidate

**Status:** Consolidated draft for the next tester update. Keep the current 1.9 tester build unchanged until the candidate is reviewed and signed off. Nothing here is merged to `main`, deployed to Supabase, published to Google Play, or uploaded to a tester track.

## Included work

| Area | Prepared change | Original draft |
| --- | --- | --- |
| Identification quality | Compare the cautious estimate with recent completed eBay listings using the object's known/manual search terms; clearly explain that searches are not verified sales evidence. | [#4](https://github.com/davidfalla37-coder/Object-detective/pull/4) |
| Photo capture | Choose from gallery or camera and submit up to three angles in one investigation. | [#5](https://github.com/davidfalla37-coder/Object-detective/pull/5) |
| Accuracy process | Repeatable manual test plan for identification, confidence, value, safety, and photo selection. | [#6](https://github.com/davidfalla37-coder/Object-detective/pull/6) |
| Play store materials | Listing text, screenshot capture plan, candidate launcher icon, and 1024 × 500 feature graphic. Screenshots still need to come from the finished app. | [#7](https://github.com/davidfalla37-coder/Object-detective/pull/7) |
| Backend reliability | Automated Edge Function validation/safety regression tests and CI execution. | [#8](https://github.com/davidfalla37-coder/Object-detective/pull/8) |
| Privacy access | In-app link to the public privacy policy. | [#9](https://github.com/davidfalla37-coder/Object-detective/pull/9) |
| Privacy and retention | Named provider data flows, Supabase invocation-log disclosure, OpenAI default-retention disclosure, and `store:false` on all three Responses API requests. | [#10](https://github.com/davidfalla37-coder/Object-detective/pull/10) |

## Checks to finish before producing the release AAB

- [x] Consolidated Android debug build, Edge Function tests, and Edge Function type-check passed in CI run [#25](https://github.com/davidfalla37-coder/Object-detective/actions/runs/37823894430).
- [ ] Run the accuracy plan on representative ordinary objects, branded/model-labelled objects, incomplete items, vehicle parts, and unclear photos; record the results.
- [ ] On an Android phone, verify camera capture, gallery selection, multiple photos up to three, cancellation, permission denial, oversized selections, and investigation retry/error states.
- [ ] Confirm gallery photo conversion creates no copies in the public Pictures folder, and temporary converted files are cleared when the app closes.
- [ ] Verify that sold-item searches open with useful terms, and that the user can still manually improve the shopping query.
- [ ] Verify the privacy link opens the correct public HTTPS policy and that the contact address is correct.
- [ ] Review the merged Android manifest and packaged SDKs against the Play Data Safety draft.
- [ ] Confirm the active OpenAI organization/project data-retention setting in OpenAI Platform. Do not claim ZDR/MAM unless the setting is visible and enabled.
- [ ] Recheck Supabase plan/log retention and align the public privacy policy before deployment.
- [ ] Capture genuine Play screenshots from the integrated release build and review the candidate icon and feature graphic at their required sizes.
- [ ] Freeze code, assign a new unique version code, then build and inspect a newly signed AAB. The already-built 1.9/code 10 AAB is from the old build and must not be reused for this candidate.
- [ ] Upload only after the planned release review; keep current testers on their existing build until then.

## Release ordering

1. Finish the checks above and resolve the OpenAI retention-setting item.
2. Deploy the matching Edge Function and publish the matching privacy policy together.
3. Build the signed AAB from the reviewed candidate and install it on your own device first.
4. Roll the same approved AAB out to the existing test group when ready; do not change Play Console listing or track settings until the release review.

## Still pending

- Combined CI passed in run #25; hands-on Android testing has not run yet.
- Store screenshots are a plan only; the feature graphic and icon remain candidates for visual review.
- The OpenAI data-retention control is account-level and was not available through repository/Supabase access.
- The candidate does not include a new version number or signed AAB yet.
