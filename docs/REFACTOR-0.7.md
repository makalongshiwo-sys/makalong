# 0.7 implementation and verification

Core screens remain native Android Views/Canvas. The shell is an XML layout usable in Android Studio Split/Design. Preview content uses tools attributes and is removed when MainActivity renders the live page.

SpatialLesson and the interactive lesson asset are removed. Nine short explanation cards replace the toy UI. The legacy domain calculation helpers remain covered by regression tests.

Quote retrieval has its own single-thread executor. Lifecycle and request tokens reject replies from old screens or canceled sessions. The foreground loop targets three seconds, without claiming streaming latency. Candle interpretation remains tied to closed bars and preserves stale-cache rejection.

ETF history parsing requires a dated table, known ticker headers, exact column counts, finite numbers, unique dates, and consistent complete totals. Missing disclosure is null. Foreground disclosure checks run at five-minute intervals. Date range filtering, pagination and native bars display all reachable records. Complete-day cumulative totals exclude partial days. Read-only public-source probes are recorded separately from synthetic tests.

Background jobs remain system scheduled. Enabling reminders or granting notification permission requests an immediate one-off check. System notification and application battery settings are accessible from the reminder page. No FCM/OEM backend or device delivery claim is made.

CI rebuilds both application and instrumentation from current source using one temporary verification key, checks the resulting source/APK hashes and runs the emulator. A successful CI build does not produce a signed upgrade for the existing private test identity. Existing checked-in APKs and evidence remain historical. CI artifacts are the only evidence for this branch until private-signature release preparation.
