# Native appearance and OriginOS reminders

Native Views/Canvas remain responsible for all core screens. System light/dark configuration selects the palette and Material widget theme. Navigation has five equal touch targets with original drawn icons, short labels and selected-state accessibility. Prices scale within one line. Long candle values and analysis are expandable.

Validation targets a 1080×2400 Android emulator viewport, with normal light, normal dark and 1.3 font-scale light scenarios. This matches a common phone resolution; it does not emulate OriginOS firmware or certify any physical device. Native checks cover minimum navigation touch size, label fit, body/selected-control contrast and system appearance. Real screenshots are exported as labeled fixture previews.

On devices whose manufacturer reports vivo or iQOO, the reminders page explains notification permission, self-start search and background battery controls. Only public Android notification/application settings intents are launched, with an unavailable-settings fallback. Unverified private OEM components are not hardcoded.

The shipped implementation still uses periodic on-device checks. A system notification test is distinct from closed-app cloud delivery. Enabling self-start or changing battery restrictions does not register the app with a push service.

To implement vivo vendor delivery, register the application's package and production/test certificate with the vendor's open platform, obtain the current official Android SDK and project configuration, and implement server-side event delivery. Keep server credentials out of this public repository and APK. Bind device registrations to event subscriptions, deduplicate messages and record actual device delivery. The existing test signing identity must be available before issuing an in-place upgrade.

Vendor SDK initialization, remote send credentials and physical closed-app delivery remain pending until the push project and existing signing configuration are available. No enabled vendor-push state, fake registration identifier or stub success is shipped.
