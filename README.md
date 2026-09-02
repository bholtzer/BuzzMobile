# MangoSos

MangoSos is an Android SOS app built with Kotlin, Jetpack Compose, and an MVVM-style structure.

## Current Features

- Volume-button SOS trigger via an accessibility service
- Loud siren/alarm playback
- Flashlight blinking during SOS
- Direct call to the first configured emergency contact
- Prefilled SMS draft with the user's last known location; the user reviews it and presses Send
- Compose-based onboarding, setup, and active-SOS screens
- DataStore-backed local settings

## Emergency Contact Behavior

- Enter one or more phone numbers in the setup screen
- Separate contacts with commas, semicolons, or new lines
- The first contact is used for direct calling
- An SMS draft is prepared for the listed contacts; delivery requires the user to press Send

## Permissions

The app requests these Android permissions as part of setup:

- `CALL_PHONE`
- `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`
- `CAMERA`
- `POST_NOTIFICATIONS` on newer Android versions

It also requires the app's accessibility service to be enabled for background hardware-key monitoring.

## Build Notes

- Compile SDK and target SDK: Android 16 (API 36).
- Run `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:bundleRelease` with JDK 21.
- Play resubmission text and verification steps: [docs/play-console-resubmission.md](docs/play-console-resubmission.md).
