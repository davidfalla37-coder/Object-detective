# Google Play Data safety draft

Review against the exact production services before submission.

Potential data processed:
- Photos/images: collected when a user submits an image; may be shared with the configured AI processor.
- Investigation history: collected only if cloud history/account features are enabled.
- Diagnostics: only if analytics/crash reporting is added.

The first release should avoid precise location, contacts, SMS, phone logs, health data and advertising identifiers. Final Play Console answers must match the actual app and third-party SDKs.
