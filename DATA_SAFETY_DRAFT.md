# Google Play Data Safety — release review draft

This is an internal worksheet for the next release, not a submitted Play Console declaration. It reflects the app source and Supabase project configuration reviewed on 8 October 2026. Confirm the OpenAI organization retention setting and final merged Android manifest before submission.

## Data flow verified in source

- The user selects or photographs an object image. The app sends it as a base64 request body to the Supabase Edge Function.
- The Edge Function sends the image and investigation prompt to OpenAI for first-pass analysis and a separate verification request.
- Follow-up chat sends the user's typed question, current chat messages, and investigation context to Supabase, then OpenAI.
- The proposed backend change sets `store: false` on all three OpenAI Responses API requests. The app manually provides conversation context and does not rely on stored API response state.
- The app saves investigation results and a preview image in local WebView storage. It provides per-item deletion and a Clear button. It has no user accounts or cloud investigation history. Follow-up chat is held in memory for the current screen session.
- No advertising or analytics SDKs were found in the app Gradle dependencies reviewed. Confirm against the final merged manifest/AAB before submission.

## Potential Play data types and handling

| Data type to assess | What the app processes | Purpose | Recipient and retention |
| --- | --- | --- | --- |
| Photos and videos — photos | Object image selected by the user | App functionality | Sent to Supabase and OpenAI. Supabase invocation records can include request and response bodies. The current Supabase Free plan normally makes project logs available for one day; availability can fall to one hour if the log-query allowance is exceeded. |
| Other user-generated content | Typed follow-up question and relevant investigation/chat context | App functionality | Sent to Supabase and OpenAI. OpenAI default abuse-monitoring logs may include prompts and responses for up to 30 days. Approved organization-level retention controls can change this. |

Supabase invocation records also expose request/response metadata such as headers, status, and duration. Confirm how Play Console's current categories apply to this operational logging before selecting any diagnostics or app-performance category. Transfers to providers should be assessed against Play's current service-provider and sharing definitions; do not treat this worksheet as the final checkbox answer.

No user accounts, contacts, precise location, SMS, phone logs, health data, payment information, or advertising identifiers are used by the app source reviewed.

## Provider details

- Supabase project `gboyflbwcobhzbvpdtat`: region `eu-central-1`; organization plan Free. Supabase documents Edge Function invocation records as including request/response headers and bodies. Its pricing page currently lists one-day Free-plan log availability; its Logs Query guide says availability can shrink to one hour in a degraded state.
- OpenAI: Images, prompts, and follow-up context are sent to the Responses API. The proposed release sets `store:false` for each request. OpenAI documents default abuse-monitoring log retention of up to 30 days, with approved Zero Data Retention or Modified Abuse Monitoring controls available to eligible organizations.
- The OpenAI account's active project/organization data-retention setting is not exposed by the available repository or Supabase access. Confirm it in OpenAI Platform → Settings → Organization → Data controls before finalizing retention wording and Play submission.

## User deletion and retention

- Investigation history is stored on-device and can be deleted one item at a time or cleared in the app; clearing Android app data or uninstalling removes the local copy.
- The app has no account or cloud history, so it has no in-app account-deletion workflow.
- Supabase request/response logs and OpenAI abuse-monitoring logs are provider operational logs, not user history. Users cannot delete an individual provider log through the app; provider retention controls and legal/security exceptions apply.

## Play Console checks before the next release

1. Recheck the final merged Android manifest and all included SDKs for data collection or sharing not visible in source dependencies.
2. Confirm OpenAI project/organization data controls. Do not claim Zero Data Retention or Modified Abuse Monitoring without verifying the active setting.
3. Recheck Supabase plan and log availability if the plan or usage state changes.
4. Ensure the public privacy policy is published and the in-app privacy link points to it.
5. Update the live privacy policy only when the `store:false` backend change is deployed, and submit Data Safety answers only when they match production behavior.

## Primary references

- [Supabase Edge Function logging](https://supabase.com/docs/guides/functions/logging)
- [Supabase logs and retention](https://supabase.com/docs/guides/observability/logs)
- [Supabase pricing](https://supabase.com/pricing)
- [OpenAI API data controls](https://developers.openai.com/api/docs/guides/your-data)
