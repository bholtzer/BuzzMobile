# Mango Guardian — Google Play listing copy

## Short description

Trigger an SOS alert, call trusted contacts, and share your location quickly.

## Long description

Mango Guardian is a personal emergency-assistance app that helps you alert trusted contacts quickly from your Android device.

Set up a protected person, choose emergency contacts, and configure the SOS actions you want. When SOS is triggered, Mango Guardian can sound a siren, blink the flashlight, place a call to your chosen contact, obtain your last known location, capture an emergency photo and short video, and open prefilled SMS or WhatsApp alerts for you to review and send.

AccessibilityService disclosure

Mango Guardian offers an optional hardware volume-button SOS trigger. To provide this feature, the app uses Android AccessibilityService to detect hardware Volume Up and Volume Down key events while the app is in the background or the device is locked. Holding both volume buttons together for 2 seconds triggers the configured SOS actions.

The AccessibilityService is used only for this volume-button trigger. Mango Guardian does not read or collect screen content, typed text, contacts, messages, or activity in other apps through AccessibilityService. It does not perform gestures, press controls, change settings, or bypass Android privacy controls. Raw key events are processed on your device and are not saved or shared. An SOS report may record that the volume buttons triggered the emergency and send that fact to Google Firebase with the emergency report. Users must review a prominent in-app disclosure and explicitly agree before Android Accessibility settings are opened, and they can disable the service at any time.

Mango Guardian is not an accessibility tool and does not use the IsAccessibilityTool flag.

Important details

- SMS and WhatsApp alerts open as drafts. You review them and press Send.
- Mango Guardian is not a replacement for police, ambulance, fire, medical, or other official emergency services.
- Emergency actions can be affected by network coverage, Android settings, battery restrictions, permissions, device-manufacturer behavior, or unavailable third-party apps.
- Use the app only with the consent of the protected person and emergency contacts.

Privacy policy: https://bholtzer.github.io/BuzzMobile/privacy-policy.html

Support: mangosos.support@gmail.com
