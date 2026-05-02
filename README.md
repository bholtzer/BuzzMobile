# MangoSos

MangoSos is an Android SOS app built with Kotlin, Jetpack Compose, and an MVVM-style structure.

## Current Features

- Volume-button SOS trigger via an accessibility service
- Loud siren/alarm playback
- Flashlight blinking during SOS
- Direct call to the first configured emergency contact
- SMS with the user's last known location to all configured emergency contacts
- Compose-based onboarding, setup, and active-SOS screens
- DataStore-backed local settings

## Emergency Contact Behavior

- Enter one or more phone numbers in the setup screen
- Separate contacts with commas, semicolons, or new lines
- The first contact is used for direct calling
- All listed contacts receive the location SMS when SOS starts

## Permissions

The app requests these Android permissions as part of setup:

- `CALL_PHONE`
- `SEND_SMS`
- `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`
- `CAMERA`
- `POST_NOTIFICATIONS` on newer Android versions

It also requires the app's accessibility service to be enabled for background hardware-key monitoring.

## Build Notes

- The repository currently includes Gradle build files and wrapper properties
- `gradle/wrapper/gradle-wrapper.jar` is not present yet, so `./gradlew` or `gradlew.bat` will not run until the wrapper is generated or restored
- Opening the project in Android Studio is the easiest way to finish local setup
