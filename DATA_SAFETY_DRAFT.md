# Google Play Data safety review

Verified against the current Android app and deployed Supabase function on 8 October 2026. Recheck if services or SDKs change.

## Current data flow

- Photos and videos: user-selected images are sent from the app to the Supabase Edge Function over HTTPS and forwarded to OpenAI's Responses API over HTTPS for analysis.
- User-generated content: follow-up questions and relevant investigation details are sent through the same path.
- Saved investigation history: stored only in the app's local device storage. Users can delete one item with its × button or all items with **Delete all**. This does not remove original photos from the device's Gallery.
- No user account, cloud history, or Supabase database/storage persistence is implemented.
- The Responses API requests set `store: false`. OpenAI's standard API abuse-monitoring logs may contain customer content and are retained for up to 30 days by default, subject to legal or safety exceptions. This is within Google's 90-day limit for the deletion request mechanism badge.

## Form answers to verify in Play Console

- **Encryption in transit:** Yes. Both app-to-Supabase and Supabase-to-OpenAI requests use HTTPS.
- **Deletion request mechanism:** Yes, if Play Console's current question accepts automatic deletion within 90 days. In-app controls delete local history; provider abuse-monitoring logs are automatically retained for up to 30 days by default. Make sure the form's declaration matches its current wording.
- **Collected data types:** Declare photos/videos and user-generated content if required by the form. Requests are processed off-device and are not ephemeral while the provider's standard abuse-monitoring retention applies.
- **Data shared:** Review the current Play definition for service providers processing data on the developer's behalf. Do not claim “no data shared with third parties” unless the processor relationship and form rules support it.
- **Account deletion:** Not applicable; the app has no account creation or sign-in.

