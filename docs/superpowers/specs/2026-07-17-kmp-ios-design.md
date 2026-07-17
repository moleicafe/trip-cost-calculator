# Cost Per Trip — iOS via Kotlin Multiplatform: Design

**Decision record (user-approved 2026-07-17):** KMP for shared logic, **native SwiftUI**
for the iOS UI. User has a Mac (Phase B happens there), an iPhone for TestFlight, and will
enroll in the Apple Developer Program later (Phase C). Internal identity is rebranded:
`applicationId`/packages move from `com.molei.fuelcost` to `com.molei.costpertrip`
(pre-publication, so this is the last cheap moment).

## Goals

1. One implementation of every formula, model, and data access path, shared by both apps.
2. Android app behavior stays pixel-identical; existing validation gate keeps passing.
3. The shared code provably compiles for iOS **before** any Swift is written (CI macOS job).
4. EV support (later) lands once in `shared` and reaches both platforms.

## Module structure

```
shared/   KMP: androidTarget, iosArm64, iosSimulatorArm64 — framework "Shared" (static)
  commonMain/  domain/FuelCalculator.kt (moved verbatim)
               domain/Format.kt         (NEW pure-Kotlin 2dp HALF_UP; replaces BigDecimal)
               data/Models.kt, Daos.kt, AppDatabase.kt   (Room 2.7 KMP, @ConstructedBy)
               data/SettingsRepository.kt                (DataStore core, takes DataStore param)
               data/DirectionsClient.kt                  (Ktor + kotlinx.serialization)
  androidMain/ database builder (Context), settings DataStore path helper
  iosMain/     database builder (NSHomeDirectory), settings path helper, Darwin engine
  commonTest/  FuelCalculatorTest, DirectionsParsingTest, FormatTest — kotlin-test,
               camelCase names (Kotlin/Native forbids spaces in test names)
app/      Android UI (Compose), ViewModels, CostPerTripApp — depends on :shared
iosApp/   Phase B (Mac): SwiftUI TabView mirroring the four tabs; Swift ObservableObjects
          over shared repositories; SKIE for Flow→AsyncSequence interop
```

## Library and toolchain migration (Phase A)

| From | To | Reason |
|---|---|---|
| Kotlin 2.0.21 / AGP 8.7.3 / Gradle 8.9 | Kotlin 2.1.21 / AGP 8.9.2 / Gradle 8.11.1 | Room-KMP + KSP2 + iOS targets |
| Room 2.6.1 (Android) | Room 2.7.1 + sqlite-bundled 2.5.1, BundledSQLiteDriver both platforms | KMP support |
| Retrofit 2.11 + Gson | Ktor client 3.1.3 (okhttp/darwin engines) + kotlinx.serialization 1.8.1 | No Retrofit/Gson on iOS |
| `java.math.BigDecimal` rounding | Pure-Kotlin string-based HALF_UP in commonMain | No BigDecimal on Kotlin/Native |
| DataStore 1.1.1 (Context) | datastore-preferences-core 1.1.7, `createWithPath` (okio) | KMP support |

**Format.kt correctness requirement:** `display2dp()` must reproduce
`BigDecimal.valueOf(d).setScale(2, HALF_UP).toPlainString()` exactly for all app-range
values. Both operate on the double's shortest round-trip decimal string; the new
implementation rounds that string: 3rd fractional digit ≥ 5 rounds up (away from zero),
with carry propagation ("9.995" → "10.00") and sign handling. The old test vectors
(1.281→"1.28", 2.7255…→"2.73", 1.285→"1.29", 3.5→"3.50", 0.0→"0.00") are the contract.

**Identity rename:** packages, `namespace`, `applicationId` → `com.molei.costpertrip`;
`FuelCostApp`→`CostPerTripApp`, `FuelCostTheme`→`CostPerTripTheme`, `Theme.FuelCost`→
`Theme.CostPerTrip`, db file → `costpertrip.db`. The already-installed debug app becomes a
separate app on the phone (different id) — uninstall the old one manually.

## Data layer patterns (Room/DataStore KMP)

- `AppDatabase` in commonMain with `@ConstructedBy(AppDatabaseConstructor::class)`;
  `expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>` (KSP
  generates per-target actuals). KSP configs: `kspAndroid`, `kspIosArm64`,
  `kspIosSimulatorArm64`.
- Platform `databaseBuilder()` helpers; common `buildAppDatabase(builder)` applies
  `BundledSQLiteDriver` + `Dispatchers.IO` query context.
- `SettingsRepository(dataStore: DataStore<Preferences>)`; platform helpers produce the
  okio path (Android `context.filesDir`, iOS documents directory).
- DAOs keep `Flow` return types (supported in Room KMP; SKIE exposes them to Swift later).

## Windows/Mac split

Kotlin/Native cannot compile iOS targets on Windows. Phase A is verified on this PC via
Android unit tests (common tests run in them) + `:app:assembleDebug`; iOS compilation is
verified by a new **CI macOS job** running `:shared:iosSimulatorArm64Test`. Phase B/C run
on the Mac (Claude Code there, same repo; handoff doc in `docs/ios/PHASE-B-HANDOFF.md`).

## Phases

- **A (now, this PC):** toolchain bump → identity rename → extract `:shared` → CI macOS job
  + handoff doc. Done when: Android tests green locally, ubuntu + macos CI jobs green.
- **B (Mac):** iosApp Xcode project, SwiftUI screens, SKIE, Simulator + iPhone via Xcode.
- **C (Mac + user):** Apple Developer enrollment (user, personally), signing, TestFlight,
  privacy label ("no data collected"), privacy policy URL, App Store submission.

## Out of scope for Phase A

SwiftUI code, SKIE, EV support, CSV export/charts, Play Store work.
