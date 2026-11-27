# Object Detective privacy audit — Pro preview

This document records implementation facts found during the privacy review. It is not a substitute for the final privacy policy or the Google Play Data safety questionnaire. Re-check the released APK, all connected services, and current provider terms before publication.

## Product commitments

- Keep investigation history because it is an important app feature. It remains on the user's device and the user controls deletion.
- Do not create user accounts or collect names, email addresses, phone numbers, advertising IDs, or behavioural profiles for ordinary app use.
- Do not upload investigation history or photographs to an Object Detective database or cloud file store.
- Keep identification, barcode lookup, OCR/evidence processing, and the Pro dossier working. Privacy changes must not remove or reduce these capabilities.
- Use Google Play Billing for any future subscription. Object Detective should not collect card or bank details.

## Verified app behaviour

- No account creation, sign-in, analytics SDK, or crash-reporting SDK is configured in the inspected app Gradle file.
- Investigation history metadata is stored in WebView localStorage. Investigation photographs are stored in WebView IndexedDB on the device. The app currently keeps up to 10 history items in Free and up to 50 in the Pro preview.
- Theme and welcome-guide preferences are stored locally.
- Photo correction mappings are stored locally in localStorage using SHA-256 photo fingerprints. These are not sent as a separate analytics or tracking event. **Current deletion gap:** clearing all history deletes history entries and saved history photos, but does not yet clear the separate photo-correction mapping. This must be addressed before making a broad claim that Clear removes all investigation-related local data.
- Individual history deletion removes that entry and its saved photographs. The correction mapping is also currently separate from the history entry.
- The Android manifest currently has `android:allowBackup="true"`. Android system backup behaviour therefore needs an explicit product decision and testing; do not claim local data can never be included in an OS-managed backup.
- Feedback and correction sharing is user-initiated through Android's share sheet or clipboard. The app does not automatically send feedback when the user taps Yes/No. The shared text excludes photos.

## Processing and external services

- When a user runs an investigation, selected photos and extracted text/barcode evidence are sent over HTTPS to the relevant Object Detective Supabase Edge Function.
- The function sends image/prompt content to the OpenAI Responses API over HTTPS to generate the identification. Follow-up chat text and investigation context are sent when the user chooses the chat feature.
- All five OpenAI Responses API requests in the inspected investigation preview function set `store: false`. This disables Responses application-state storage; it does **not** establish Zero Data Retention.
- OpenAI's standard API abuse-monitoring logs may retain customer content for up to 30 days unless the API organisation/project has been approved for Modified Abuse Monitoring or Zero Data Retention. Approval status has not been verified. Never promise zero provider retention until this is confirmed.
- Barcode lookup sends the barcode value and format to the preview lookup function, which queries external product catalogues. The barcode value is not an account identifier, but this external processing should be disclosed.
- The inspected investigation functions contain no Supabase database inserts/upserts or Storage writes. The public schema currently has no tables.
- Supabase Edge Function invocation logs record technical request metadata, not request/response bodies. The platform's log retention duration depends on the Supabase plan and must be checked in the project settings.
- The Pro preview investigation and barcode functions now emit fixed error-category messages rather than provider exception text or error details. This reduces the chance that a provider error could expose request-related information in custom logs.
- The live Free investigation function has not been modified as part of this preview-only change. It still needs the same privacy logging review before a Free release update.

## Payments

- The inspected app Gradle file does not currently include Google Play Billing. Subscription processing is not yet implemented in this code.
- When subscriptions are introduced, Google Play should process payment credentials. The app should retain only the minimum purchase token/status data needed to verify entitlement and prevent replay/fraud. Do not collect or store card/bank details.
- Purchase tokens are still sensitive transaction identifiers and must be protected, never logged, and retained only as long as operational/legal needs require.

## Deletion and outstanding decisions

- The existing Clear control removes local history entries and saved history photographs.
- Before release, decide and implement whether clearing all history also clears saved photo-correction fingerprints, and ensure individual deletion does not leave an undisclosed correction record behind.
- Decide whether Android automatic backup should remain enabled. Disabling it may stop local history from being restored to a replacement phone; leaving it enabled may allow Android's backup system to handle app data under the user's device/account backup settings.
- Publish a clear privacy notice explaining that photos and chat content are sent to Supabase/OpenAI for processing and may be subject to provider retention rules.
- Verify OpenAI ZDR/MAM eligibility and approval, Supabase plan/log retention, all barcode catalogue providers, and the final Android backup behaviour before making any no-retention promise or completing Play Console Data safety.
- Keep the production Free app and production Edge Functions unchanged until the preview privacy work has been reviewed and tested.
