# Google Play Data Safety — release review draft

This is an internal worksheet for the next release, not a submitted Play Console declaration. It reflects the app source and Supabase project configuration reviewed on 8 October 2026. Confirm the OpenAI organization retention setting, final Android manifest, and live auth/rate-limit configuration before submission.

## Data flow verified in source

- The user selects or photographs an object image. The app sends it as a base64 request body to the Supabase Edge Function.
- The Edge Function sends the image and investigation prompt to OpenAI for first-pass analysis and a separate verification request.
- Follow-up chat sends the user's typed question, current chat messages, and investigation context to Supabase, then OpenAI.
- The candidate creates an anonymous Supabase Auth user on first use. Its random user ID and session tokens authenticate requests. The app stores session tokens locally and provides an in-app control to delete the anonymous user and local investigation history.
- A proposed Postgres migration applies per-user daily limits of 30 investigations and 120 chat messages. Usage counters are deleted after three days. The migration must be applied before the candidate function is deployed.
- The candidate Edge Function validates each bearer token with Supabase Auth `/user` and fails closed if validation is unavailable. Gateway JWT verification is intentionally disabled so the handler can apply that validation and return consistent CORS responses. It checks per-account daily quotas before calling OpenAI.
- The backend sets `store: false` on all three OpenAI Responses API requests. The app manually provides conversation context and does not rely on stored API response state.
- The app saves investigation results and a preview image in local WebView storage. It provides per-item deletion and a Clear button. No advertising or analytics SDKs were found in the app Gradle dependencies reviewed. Confirm against the final merged manifest/AAB before submission.

## Potential Play data types and handling

| Data type to assess | What the app processes | Purpose | Recipient and retention |
| --- | --- | --- | --- |
| Photos and videos — photos | Object image selected by the user | App functionality | Sent to Supabase and OpenAI. Supabase invocation records can include request and response bodies. The current Supabase Free plan normally makes project logs available for one day; availability can fall to one hour if the log-query allowance is exceeded. |
| Other user-generated content | Typed follow-up question and relevant investigation/chat context | App functionality | Sent to Supabase and OpenAI. OpenAI default abuse-monitoring logs may include prompts and responses for up to 30 days. Approved organization-level retention controls can change this. |
| Personal info — user IDs | Random anonymous Supabase Auth user ID used to authenticate app requests and apply daily quotas | App functionality; fraud prevention, security, and compliance | Supabase Auth retains the anonymous account until the user deletes it in the app or requests deletion. The in-app action deletes the account and local app history. |

Google's Data Safety categories include identifiers relating to an account or user. Treat the anonymous Auth ID as a user ID for the final Play Console review. Whether provider processing is declared as collection/sharing must be answered against the current Play definitions and actual provider arrangement; this worksheet is not the final checkbox answer.

Supabase invocation records also expose request/response metadata such as headers, status, and duration. Confirm how Play Console's current categories apply to this operational logging before selecting any diagnostics or app-performance category.

No name, email, password, contacts, precise location, SMS, phone logs, health data, payment information, or advertising identifiers are requested by the app source reviewed.

## Provider details

- Supabase project `gboyflbwcobhzbvpdtat`: region `eu-central-1`; organization plan Free. Supabase documents Edge Function invocation records as including request/response headers and bodies. Its pricing page currently lists one-day Free-plan log availability; its Logs Query guide says availability can shrink to one hour in a degraded state.
- Supabase anonymous sign-up is subject to Supabase Auth's IP rate limit; CAPTCHA is recommended by Supabase to reduce automated account creation. The candidate has per-account quotas but does not yet include CAPTCHA. Add and verify an appropriate bot-control before a public rollout, or document and accept a reviewed residual risk.
- OpenAI: Images, prompts, and follow-up context are sent to the Responses API. The candidate sets `store:false` for each request. OpenAI documents default abuse-monitoring log retention of up to 30 days, with approved Zero Data Retention or Modified Abuse Monitoring controls available to eligible organizations.
- The OpenAI account's active project/organization data-retention setting is not exposed by the available repository or Supabase access. Confirm it in OpenAI Platform → Settings → Organization → Data controls before finalizing retention wording and Play submission.

## User deletion and retention

- Investigation history is stored on-device and can be deleted one item at a time or cleared in the app.
- The anonymous Supabase account and its usage counters can be deleted in-app. An external account-deletion page explains how to request deletion by sending the copied anonymous session ID to the support email.
- The app does not ask for an email, name, or password and has no cloud investigation history. A user ID is still created by Supabase Auth for authenticated requests.
- Supabase request/response logs and OpenAI abuse-monitoring logs are provider operational logs, not user history. Users cannot delete an individual provider log through the app; provider retention controls and legal/security exceptions apply.

## Play Console checks before the next release

1. Recheck the final merged Android manifest and all included SDKs for data collection or sharing not visible in source dependencies.
2. Apply and verify the quota migration; enable anonymous sign-ins and configure suitable anti-automation protection in Supabase Auth.
3. Confirm the deployed function performs server-side Supabase Auth validation and rejects over-quota requests before calling OpenAI.
4. Confirm the account-deletion page is publicly reachable and the in-app deletion flow works.
5. Confirm OpenAI project/organization data controls. Do not claim Zero Data Retention or Modified Abuse Monitoring without verifying the active setting.
6. Recheck Supabase plan and log availability if the plan or usage state changes.
7. Publish the matching privacy policy and account-deletion page before submitting Data Safety answers that match production behavior.

## Primary references

- [Supabase Edge Function logging](https://supabase.com/docs/guides/functions/logging)
- [Supabase anonymous sign-ins and abuse prevention](https://supabase.com/docs/guides/auth/auth-anonymous)
- [Supabase Edge Function authentication](https://supabase.com/docs/guides/functions/auth)
- [Supabase logs and retention](https://supabase.com/docs/guides/observability/logs)
- [Supabase pricing](https://supabase.com/pricing)
- [OpenAI API data controls](https://developers.openai.com/api/docs/guides/your-data)
- [Google Play Data Safety](https://support.google.com/googleplay/android-developer/answer/10787469)
