# Google Play Data safety facts for Object Detective

Use these verified implementation facts when completing Play Console. Final answers must reflect the current app bundle, all SDKs, and the providers' current terms.

## App behaviour
- The app has no account creation, sign-in, or cloud investigation history.
- When a user runs an investigation, selected photos are transmitted from the phone to the Supabase Edge Function over HTTPS. The function sends image and prompt content to OpenAI over HTTPS to generate the result. The function does not write photo or chat content to an app-owned database; the OpenAI request sets `store: false`.
- Follow-up chat questions and related investigation context are sent through the same HTTPS path when the user chooses chat.
- The app stores investigation history and settings locally on the device. The Recent investigations Clear control removes that local history.
- The Android Gradle file does not configure analytics or crash-reporting SDKs. Confirm no other SDK or service has been added before submitting the form.
- Google Play may treat data processed ephemerally differently in the user-facing Data safety label; answer the form based on Google's current definitions and include processing that meets its criteria.

## Deletion and retention
- Users can delete on-device history from Recent investigations > Clear, or clear app storage/uninstall.
- There is no account or cloud history to delete through an account control. For a deletion/privacy request, users can contact the developer through the contact details on the Google Play listing.
- Service providers may retain limited request or technical data under their own security, abuse-prevention, and legal retention rules. Do not promise immediate deletion from provider systems.

## Encryption in transit
- The Android app sends investigations to the Supabase HTTPS endpoint.
- The Supabase function calls OpenAI using HTTPS.
- The app manifest does not enable cleartext traffic; the app's external marketplace links also use HTTPS.
- The Play Console answer for encryption in transit should be Yes, provided the configured endpoint and provider requests remain HTTPS.
