# Phase B handoff — build the iOS app (run this on the Mac)

Context for a Claude Code session on the Mac. Read the design spec first:
`docs/superpowers/specs/2026-07-17-kmp-ios-design.md`.

## What already exists (Phase A, done)

- `:shared` KMP module (androidTarget, iosArm64, iosSimulatorArm64) containing ALL
  formulas (`domain/FuelCalculator.kt`, `domain/Format.kt`), Room 2.7 KMP database
  (`data/AppDatabase.kt` + platform factories), DataStore settings, and the Ktor
  Directions client. Framework name: **Shared** (static).
- iOS entry points (in `shared/src/iosMain/.../Factories.ios.kt`):
  `createAppDatabase()`, `createSettingsRepository()` — both store under the app's
  Documents directory. `DirectionsClient.lookupDistanceKm(origin, destination, apiKey)`.
- 15 common tests, run on iOS by CI (`:shared:iosSimulatorArm64Test`, macOS job).
- Validation gate (must hold on iOS too): Civic 21 km eco → "1.28" L, "$2.73", "2.13"/L.

## Phase B task list

1. **Verify toolchain:** Xcode 16+, `./gradlew :shared:iosSimulatorArm64Test` passes locally.
2. **Add SKIE** (Touchlab) to `shared/build.gradle.kts` so Kotlin `Flow`s surface as Swift
   `AsyncSequence` — the DAOs and `SettingsRepository` expose `Flow`s that drive live UI.
3. **Create `iosApp/`** Xcode project (SwiftUI, iOS 16+, bundle id `com.molei.costpertrip`),
   with the standard KMP framework-embedding build phase:
   `./gradlew :shared:embedAndSignAppleFrameworkForXcode`.
4. **SwiftUI screens**, mirroring Android (`app/src/main/java/.../ui/` is the reference):
   TabView — New Trip (vehicle picker defaulting to last-used, Normal/Eco segmented
   control, distance field OR address pair + Calculate button [disabled without API key,
   never a straight-line fallback], live result card), History (month total header),
   Vehicles (list + editor with auto-eco toggle), Settings (API key). Trip detail sheet
   with the formula breakdown. All arithmetic via `FuelCalculator`/`Format` — never
   reimplement in Swift.
5. **Swift view models:** `@MainActor ObservableObject` per screen wrapping the shared
   DAOs/repositories; collect Flows via SKIE `for await`.
6. **Run on Simulator + iPhone** (personal team signing is fine pre-enrollment).
   Re-validate the Civic case end-to-end on device.
7. **CI:** extend the macOS job to build the iOS app
   (`xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator build`).

## Phase C checklist (after Apple Developer enrollment — user does the enrollment)

- App Store Connect app record (bundle id above), App Store Connect API key in GitHub
  secrets, fastlane or xcodebuild archive + upload in CI, TestFlight to the user's iPhone.
- Privacy: "Data Not Collected" nutrition label (everything is on-device; the only network
  call is the user's own Google Directions key to Google), privacy policy URL, App Store
  screenshots (reuse the Android redaction rule: no real addresses).
