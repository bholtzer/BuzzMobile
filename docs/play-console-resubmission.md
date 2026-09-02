# Google Play resubmission — Mango Guardian 1.0.5 (11)

## Changes in this release

- Compile and target SDK remain 36 (Android 16); the previous source already targeted 36. Upload the new bundle rather than resubmitting an older artifact. Version code 11 must exceed every version previously uploaded to Play; increase it if necessary.
- All in-app entry points to Accessibility settings now use one separate, scrollable disclosure with affirmative consent and a Not now action, in English, Hebrew, Spanish and French.
- Consent is saved locally through the settings repository. The hardware trigger refuses key events and delayed triggers without consent, including for existing installations that previously enabled the service.
- Accessibility can be skipped during onboarding; manual SOS remains available after setup. Declining or dismissing the disclosure does not open Android settings or grant consent.
- Service metadata explicitly declares `isAccessibilityTool=false`, disables screen retrieval and gestures, and links to the correct settings activity.

## Text to add to the full Play Store description

### Accessibility API use

Mango Guardian uses the Android AccessibilityService API for an optional volume-button SOS shortcut. With your consent and the service enabled, it detects volume-button presses and their duration, including while the app is in the background or the device is locked. Holding Volume Up and Volume Down together for 2 seconds starts your configured SOS actions.

Raw key events are processed on your device and are not saved or shared. An SOS report can record that volume buttons triggered the emergency and send that fact to Google Firebase with the emergency report. The accessibility service does not read screen content, typed text, contacts, messages, or activity in other apps, perform gestures, or press controls in other apps. SMS and WhatsApp messages require you to review them and press Send.

You can decline accessibility access and continue setup for manual SOS. Without accessibility access, the volume-button shortcut will not work. You can disable the service at any time in Android Accessibility settings.

Use equivalent wording in every localized full listing; do not rely on the short description, privacy policy, or Android service description alone. This block describes accessibility use, not all app data practices. Keep the rest of the listing accurate about location, calls, media, Firebase reports, and user-sent messages. Avoid promises of guaranteed emergency delivery or automatic message sending.

## Play Console actions still required

1. Upload the signed release AAB for this release, with a version code higher than all existing uploads. Confirm API 36 and the correct signing certificate in App Bundle Explorer. Review any active tracks cited in the rejection.
2. Add the accessibility text above to the full store listing and translate it for localized listings.
3. Complete or update App content → Accessibility API declaration. This is a general SOS app, not a dedicated accessibility tool. Explain the API use as app functionality: detecting the user's explicit hardware-button SOS action. Declare the derived trigger-source information sent in emergency reports accurately; do not claim that no accessibility-related information leaves the device. Review the exact current form categories against the report schema.
4. Provide a reviewer-accessible video showing app launch, normal onboarding, the complete disclosure (scroll slowly), Not now, continuing without hardware SOS, reopening the disclosure from setup, Agree and continue, Android service enablement, and the volume-button feature. Use only consenting test contacts; SOS can place a real call.
5. Publish the updated privacy-policy HTML at the URL configured in Play Console and the app. Updating this repository alone does not update the hosted page. Align Data safety with actual Firebase reports, media, identifiers, location, contacts and optional analytics; the accessibility disclosure does not replace separate disclosures for other sensitive data.
6. Review the detailed rejection notice. “Not adhering to Google Play Developer Program policies” is a broad heading and cannot establish which other policy problems exist. Check any specifically cited permissions, foreground services, data handling or misleading claims before resubmission.

## Device verification before submission

- Fresh install: decline the disclosure and dismiss it with Back/outside tap. No settings screen opens, no hardware SOS starts, and setup can continue using “Continue without volume-button SOS”.
- Accept: verify consent precedes Android settings. Enable the service, return and verify the shortcut. Stop SOS immediately after the controlled test.
- Enable the service directly in Android settings on a fresh install: hardware SOS must remain blocked until in-app consent. Reopen the app and accept via onboarding/setup.
- Upgrade an existing installation: hardware SOS must require the new disclosure; the setup warning provides the acceptance path. Verify this explicitly because the change intentionally stops previously enabled hardware detection until consent.
- Test every entry point: onboarding, setup warning, permissions card and shortcut warning. Test rotation/process recreation and all four languages, with large text; every disclosure must scroll and both actions remain reachable.
- Disable the service and verify hardware detection stops while manual SOS remains available.
- Test Android 16 on a device/emulator, including locked/background triggers, calls, message composers, notification stop action and permissions denied. A successful compile does not verify device behavior or guarantee Play approval.

## Official references

- [AccessibilityService API policy and required review video](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en)
- [Prominent disclosure and consent](https://support.google.com/googleplay/android-developer/answer/11150561?hl=en)
- [Target API level requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)

These changes and documents prepare a resubmission; they do not submit a release, update Play Console, publish the privacy policy or guarantee approval.
