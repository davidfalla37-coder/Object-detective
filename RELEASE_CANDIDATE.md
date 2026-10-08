# Object Detective — next update candidate

**Status:** Consolidated draft for the next tester update. Keep the current 1.9 tester build unchanged until the candidate is reviewed and signed off. Nothing here is merged to `main`, deployed to Supabase, published to Google Play, or uploaded to a tester track.

## Included work

| Area | Prepared change | Original draft |
| --- | --- | --- |
| Identification quality | Compare the cautious estimate with recent completed eBay listings using the object's known/manual search terms; clearly explain that searches are not verified sales evidence. | [#4](https://github.com/davidfalla37-coder/Object-detective/pull/4) |
| Photo capture and storage | Choose from gallery or camera and submit up to three angles; gallery conversions stay in temporary app-private cache instead of public Pictures. | [#5](https://github.com/davidfalla37-coder/Object-detective/pull/5) |
| Accuracy process | Repeatable manual test plan for identification, confidence, value, safety, and photo selection. | [#6](https://github.com/davidfalla37-coder/Object-detective/pull/6) |
| Play store materials | Listing text, screenshot capture plan, candidate launcher icon, and 1024 × 500 feature graphic. Screenshots still need to come from the finished app. | [#7](https://github.com/davidfalla37-coder/Object-detective/pull/7) |
| Backend reliability | Automated Edge Function validation/safety regression tests and CI execution. | [#8](https://github.com/davidfalla37-coder/Object-detective/pull/8) |
| Privacy access | In-app link to the public privacy policy. | [#9](https://github.com/davidfalla37-coder/Object-detective/pull/9) |
| Privacy and retention | Named provider data flows, Supabase invocation-log disclosure, OpenAI default-retention disclosure, and `store:false` on all three Responses API requests. | [#10](https://github.com/davidfalla37-coder/Object-detective/pull/10) |
| Android WebView safety | Serve bundled app files through AndroidX WebViewAssetLoader; deny file-URL access, block unapproved network requests, restrict navigation, deny web media permissions, and disable Android backup. | Security review |
| API abuse protection | Anonymous Supabase Auth sessions, JWT-required function configuration, per-account daily quotas, and account deletion controls. Requires migration and Supabase dashboard setup before deployment. | Security review |

## Security review — release blockers

A source review and CI build cannot establish that any app is “100% safe.” They can reduce known risks; real-device testing, accurate privacy disclosures, and protection of the live service are still required.

- [x] Latest candidate CI run [#64](https://github.com/davidfalla37-coder/Object-detective/actions/runs/37828312841) passed the Android debug build, Edge Function tests, and Edge Function type-check.
- [x] WebView code no longer loads the packaged app through `file://`; local files and unapproved network origins are blocked in the candidate.
- [x] Android manifest disables app backup and cleartext traffic; WebView media-permission requests are denied.
- [x] Candidate code now validates Supabase user JWTs against the Auth service, checks per-account daily quotas before calling OpenAI (30 investigations and 120 chat messages), and includes an in-app anonymous-session deletion flow. A SQL migration creates the quota table and RPC.
- [ ] **Before rollout, apply the quota migration, enable Supabase anonymous sign-ins, and deploy with `verify_jwt=false` exactly as reviewed: the handler itself validates each bearer token with Supabase Auth `/user` and fails closed. Do not disable that handler check.** The currently deployed function remains public and unchanged, so this draft code does not protect the live service yet.
- [ ] Add suitable bot protection for anonymous sign-up. Supabase recommends CAPTCHA because anonymous accounts can otherwise be created automatically; the candidate does not include a CAPTCHA challenge yet. Test quotas, rejected requests, token refresh, and account deletion before rollout.
- [ ] Confirm the Edge Function's Supabase service-role secret is available for deletion, and verify the public account-deletion page and privacy policy after they are published.
- [ ] On the owner's Android phone, verify camera capture, gallery selection, multiple photos up to three, cancellation, permission denial, oversized selections, external HTTPS links, blocked HTTP links, and investigation retry/error states.
- [ ] Confirm gallery photo conversion creates no copies in the public Pictures folder, and temporary converted files are cleared when the app closes.
- [ ] Run the accuracy plan on representative ordinary objects, branded/model-labelled objects, incomplete items, vehicle parts, and unclear photos; record the results.
- [ ] Verify that sold-item searches open with useful terms, and that the user can still manually improve the shopping query.
- [ ] Verify the privacy link opens the correct public HTTPS policy and that the contact address is correct.
- [ ] Review the merged Android manifest and packaged SDKs against the Play Data Safety draft.
- [ ] Confirm the active OpenAI organization/project data-retention setting in OpenAI Platform. Do not claim ZDR/MAM unless the setting is visible and enabled.
- [ ] Recheck Supabase plan/log retention and align the public privacy policy before deployment.
- [ ] Capture genuine Play screenshots from the integrated release build and review the candidate icon and feature graphic at their required sizes.
- [ ] Freeze code, assign a new unique version code, then build and inspect a newly signed AAB. The already-built 1.9/code 10 AAB is from the old build and must not be reused for this candidate.
- [ ] Upload only after the planned release review; keep current testers on their existing build until then.

## Release ordering

1. Add and test anti-automation protection; configure anonymous Auth; apply the quota migration; confirm OpenAI retention settings.
2. Deploy with `verify_jwt=false` exactly as reviewed; the handler validates each request with Supabase Auth `/user`. Publish the matching privacy policy and account-deletion page together.
3. Build the signed AAB from the reviewed candidate and install it on the owner's device first.
4. Roll the same approved AAB out to the existing test group when ready; do not change Play Console listing or track settings until the release review.

## Still pending

- CI run #64 passed the Android debug build and Edge Function tests/type-check. Hands-on device testing remains outstanding.
- Auth, quota, and deletion changes are code-only at this stage; the live function, database, and tester app have not been changed.
- CAPTCHA/bot protection is not implemented yet. Supabase Auth settings and the quota migration also remain to be configured before rollout.
- Hands-on Android testing of sign-up, token refresh, quotas, and account deletion has not run yet.
- Store screenshots are a plan only; the feature graphic and icon remain candidates for visual review.
- The OpenAI data-retention control is account-level and was not available through repository/Supabase access.
- The candidate does not include a new version number or signed AAB yet.
