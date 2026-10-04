# Google Play Data safety draft

Review against the exact production services before submission.

Potential data processed:
- Photos/images: collected when a user submits an image; sent through the Supabase Edge Function to the configured AI processor for analysis.
- User-generated content: typed follow-up questions and the investigation fields needed to answer them are sent through the Supabase Edge Function to the configured AI processor. The app keeps the chat transcript only for the current screen session; it does not currently save chat transcripts to an account or cloud history.
- Investigation history: saved locally on the device. No cloud history or account feature is currently enabled.
- Diagnostics: only if analytics/crash reporting is added.

The first release should avoid precise location, contacts, SMS, phone logs, health data and advertising identifiers. Final Play Console answers must match the actual production services, retention settings and third-party SDKs.
