# Object Detective — S25 build package

This package has been checked and corrected:
- Added the supplied Object Detective feature image to the Android assets.
- Added the missing PWA icon.svg.
- Removed the AndroidX Activity Result dependency from MainActivity so the project has fewer external requirements.
- Added a GitHub Actions workflow for building the APK from a phone using GitHub.

## Build on a Samsung S25
The simplest phone-only route is:
1. Create/sign into GitHub in Chrome.
2. Create a new repository.
3. Upload the contents of this package, keeping the `final/` and `.github/` folders.
4. In GitHub open Actions → Build Object Detective APK → Run workflow.
5. Download the `object-detective-debug-apk` artifact from the completed workflow.
6. Open the APK on the S25 and install it.

## Important
The app currently runs in Demo Mode until `FUNCTION_URL` in `final/android/app/src/main/assets/index.html` points to your deployed Supabase Edge Function. The backend source is `final/investigate-object.ts`.

A Google Play release also needs a signed AAB and a public privacy-policy URL plus Play Console declarations.
