# Mango Guardian — Google Play listing copy

## Short description

Trigger an SOS alert, call trusted contacts, and share your location quickly.

## Long description

Mango Guardian is a personal emergency-assistance app that helps you alert trusted contacts quickly from your Android device.

Set up a protected person, choose emergency contacts, and configure the SOS actions you want. When SOS is triggered, Mango Guardian can sound a siren, blink the flashlight, place a call to your chosen contact, obtain your last known location, capture an emergency photo and short video, and open prefilled SMS or WhatsApp alerts for you to review and send.

AccessibilityService disclosure

Mango Guardian offers an optional hardware volume-button SOS trigger. To provide this feature, the app uses Android AccessibilityService to detect hardware Volume Up and Volume Down key events while the app is in the background or the device is locked. Holding both volume buttons together for 2 seconds triggers the configured SOS actions.

The AccessibilityService is used only for this volume-button trigger. Mango Guardian does not read or collect screen content, typed text, contacts, messages, or activity in other apps through AccessibilityService. It does not perform gestures, press controls, change settings, or bypass Android privacy controls. Accessibility data is not stored or shared. Users must review a prominent in-app disclosure and explicitly agree before Android Accessibility settings are opened, and they can disable the service at any time.

Mango Guardian is not an accessibility tool and does not use the IsAccessibilityTool flag.

Important details

- SMS and WhatsApp alerts open as drafts. You review them and press Send.
- Mango Guardian is not a replacement for police, ambulance, fire, medical, or other official emergency services.
- Emergency actions can be affected by network coverage, Android settings, battery restrictions, permissions, device-manufacturer behavior, or unavailable third-party apps.
- Use the app only with the consent of the protected person and emergency contacts.

Privacy policy: https://bholtzer.github.io/BuzzMobile/privacy-policy.html

Support: mangosos.support@gmail.com

## Accessibility declaration wording

Mango Guardian is not an accessibility tool. It uses AccessibilityService only to detect the user-configured Volume Up + Volume Down hardware-key SOS trigger while the app is in the background or the device is locked. The service requests key-event filtering and cannot retrieve window content or perform gestures. It does not read screen content, inspect other apps, enter text, click controls, change settings, or collect, store, or share accessibility data.

## Reviewer access instructions

1. Launch Mango Guardian; no account or sign-in is required.
2. Complete the initial name and runtime-permission steps.
3. On the Accessibility step, tap the button that opens the disclosure.
4. Review the disclosure explaining hardware key-event access and tap **Agree and continue**.
5. In Android Accessibility settings, choose **Mango Guardian Trigger Service** and enable it.
6. Return to the app and finish setup with test emergency-contact details.
7. Hold Volume Up and Volume Down together for 2 seconds, then release, to demonstrate the SOS trigger.
8. Stop the SOS using the on-screen **Stop SOS** button.

## Required review video checklist

Upload an unlisted YouTube video and provide its URL with every submission. Record one continuous flow showing:

1. The Play-submitted build/version installed on a device.
2. The Accessibility onboarding step.
3. The entire prominent disclosure, readable on screen.
4. Tapping **Agree and continue** before Android settings open.
5. Enabling Mango Guardian Trigger Service in Android settings.
6. Returning to Mango Guardian.
7. Triggering SOS with both volume buttons.
8. The resulting SOS screen/actions.
9. Disabling the service again in Android Accessibility settings.

Do not use a video from an older build; Google requests an updated video with each new submission.
