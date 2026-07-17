# Phase A: KMP Restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract all domain/data code into a `:shared` KMP module (android + ios targets) with the identity renamed to `com.molei.costpertrip`, keeping the Android app green and proving iOS compilation via CI.

**Architecture:** per `docs/superpowers/specs/2026-07-17-kmp-ios-design.md`.

**Tech Stack:** Kotlin 2.1.21, KSP 2.1.21-2.0.1, AGP 8.9.2, Gradle 8.11.1, Room 2.7.1 + sqlite-bundled 2.5.1, DataStore core 1.1.7, Ktor 3.1.3, kotlinx-serialization 1.8.1, kotlinx-coroutines 1.10.2.

## Global Constraints

- Validation gate must keep passing: 1.28 L / $2.73 / $2.13 displays for the Civic case.
- Formulas move verbatim — no arithmetic changes anywhere.
- commonTest uses kotlin-test with camelCase names (Kotlin/Native forbids backtick-space names).
- Run builds with `JAVA_HOME = Android Studio JBR` (Gradle can't run on the installed JDK 26).
- Verify with local `gradlew` after every task; push only when green.

### Task 1: Toolchain bump in place

**Files:** `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties` (8.11.1), root `build.gradle.kts` (add serialization/multiplatform/android-library plugin aliases `apply false`).

- [ ] Bump: agp 8.9.2, kotlin 2.1.21, ksp 2.1.21-2.0.1, room 2.7.1; add sqliteBundled 2.5.1, datastore 1.1.7 (core artifact), ktor 3.1.3, kotlinxSerialization 1.8.1, kotlinxCoroutines 1.10.2; keep Compose BOM/nav/lifecycle as-is; drop nothing yet (Retrofit removal happens in Task 3).
- [ ] Run `gradlew :app:testDebugUnitTest` → 10/10 pass (Room 2.7.1 is a drop-in on Android).
- [ ] Commit `build: bump to Kotlin 2.1.21 / AGP 8.9.2 / Room 2.7.1 for KMP`.

### Task 2: Identity rename

**Files:** every Kotlin file (package/import lines), `app/build.gradle.kts` (namespace + applicationId), `AndroidManifest.xml`, directory moves `com/molei/fuelcost` → `com/molei/costpertrip`.

- [ ] `git mv` source dirs (main + test); sed package/import prefixes `com.molei.fuelcost` → `com.molei.costpertrip`; rename classes `FuelCostApp`→`CostPerTripApp`, `FuelCostTheme`→`CostPerTripTheme`, style `Theme.FuelCost`→`Theme.CostPerTrip` (themes.xml + manifest); db name stays until Task 3.
- [ ] Run `gradlew :app:testDebugUnitTest` → 10/10. Commit `refactor: rename identity to com.molei.costpertrip`.

### Task 3: Extract :shared KMP module

**Files:** Create `shared/build.gradle.kts`; move `domain/*`, `data/*` → `shared/src/commonMain/kotlin/com/molei/costpertrip/`; create `shared/src/{androidMain,iosMain}` platform helpers; move+convert tests → `shared/src/commonTest`; new `domain/Format.kt`; rewrite `DirectionsClient.kt` on Ktor; update `settings.gradle.kts` (`include(":shared")`), `app/build.gradle.kts` (drop room/ksp/retrofit, add `project(":shared")`), `CostPerTripApp.kt`, `ui/Format.kt` deleted (function moves to `com.molei.costpertrip.domain` — update the 6 UI-file imports), proguard gson rules deleted.

**Interfaces (produced, consumed by app UI and later by Swift):**

```kotlin
// domain/Format.kt (commonMain) — contract vectors in spec §Format.kt
fun Double.display2dp(): String
fun formatMoney(currencySymbol: String, value: Double): String

// data/AppDatabase.kt (commonMain)
@Database(entities = [Vehicle::class, Trip::class], version = 1, exportSchema = false)
@TypeConverters(EnumConverters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() { /* vehicleDao(), tripDao() */ }
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
fun buildAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase   // BundledSQLiteDriver + Dispatchers.IO
// androidMain: fun databaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase>  // name "costpertrip.db"
// iosMain:     fun databaseBuilder(): RoomDatabase.Builder<AppDatabase>                  // NSHomeDirectory()/Documents

// data/SettingsRepository.kt (commonMain)
class SettingsRepository(dataStore: DataStore<Preferences>)   // same public API as today
fun createSettingsDataStore(producePath: () -> String): DataStore<Preferences>
// androidMain: fun settingsPath(context: Context): String; iosMain: fun settingsPath(): String

// data/DirectionsClient.kt (commonMain) — same public API as today, Ktor inside,
// DTOs @Serializable with @SerialName("error_message")
```

- [ ] Write `Format.kt` + `FormatTest.kt` (kotlin-test) first with the spec's contract vectors, then the string-rounding implementation.
- [ ] Move the rest; convert the two existing test classes to kotlin-test/camelCase.
- [ ] Run `gradlew :shared:testDebugUnitTest :app:assembleDebug` → all green. Commit `refactor: extract shared KMP module (android+ios targets)`.

### Task 4: CI + handoff doc

**Files:** `.github/workflows/ci.yml`, `docs/ios/PHASE-B-HANDOFF.md`, README architecture section.

- [ ] ubuntu job: `./gradlew :shared:testDebugUnitTest :app:assembleDebug`; new macos job: `./gradlew :shared:iosSimulatorArm64Test`.
- [ ] Handoff doc: what exists, how to consume the framework, SKIE plan, SwiftUI screen list, Phase C checklist.
- [ ] Push; watch both CI jobs green. Commit `ci: verify shared module on iOS simulator; Phase B handoff`.

## Self-review

Spec coverage: module structure (T3), library swaps (T1/T3), identity rename incl. applicationId (T2), Format contract (T3), CI macOS proof (T4), handoff (T4) — all covered. Type consistency: `display2dp`/`formatMoney` names preserved everywhere; `SettingsRepository` public API unchanged so ViewModels don't change. No placeholders.
