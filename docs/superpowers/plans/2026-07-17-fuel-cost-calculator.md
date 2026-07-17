# Fuel Cost Calculator (Android) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Native Android app that calculates per-trip fuel cost from a vehicle's consumption rate, trip distance, and fuel price, with vehicle profiles, trip history, and optional Directions-API distance lookup.

**Architecture:** Single-module Compose app. A pure-Kotlin `FuelCalculator` object holds all formulas (unit-testable with zero Android deps). Room persists vehicles/trips; DataStore holds settings (API key, last-used vehicle). ViewModels expose StateFlow; result cards recompute live via `combine`/`derivedStateOf`. Retrofit calls Google Directions API only when an API key exists in settings; otherwise manual distance entry only (no silent haversine fallback).

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose (BOM 2024.12.01, Material 3), Room 2.6.1 (KSP), DataStore Preferences 1.1.1, Retrofit 2.11.0 + Gson, Navigation Compose, AGP 8.7.3, Gradle 8.9, minSdk 26 / target 35.

## Global Constraints

- Formulas must be used exactly as specced (see Task 2); all four calculated values recompute live as the user edits inputs.
- Validation gate: Honda Civic case (normal 6.7, eco 6.1, tank 47 L, $100/tank; trip 21 km eco) must display fuelUsed **1.28 L** and cost **$2.73**.
- Full precision internally; round to 2 dp (HALF_UP) only at display.
- No hardcoded API key anywhere; key lives in DataStore, entered in Settings.
- No straight-line/haversine distance substitute. No key → manual entry only.
- Currency symbol is per-vehicle, default `$`.
- Nice-to-have features are gated on the core flow being verified end-to-end on a device; they are OUT of scope for this plan.

## File Structure

```
FuelCostCalculator/
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradle/libs.versions.toml
├── gradle/wrapper/gradle-wrapper.properties (+ gradlew scripts; jar fetched by Studio/gradle)
├── app/build.gradle.kts, app/proguard-rules.pro
├── app/src/main/AndroidManifest.xml
├── app/src/main/java/com/molei/fuelcost/
│   ├── MainActivity.kt            — activity + NavHost + bottom nav
│   ├── FuelCostApp.kt             — Application; lazy singletons (db, repos)
│   ├── domain/FuelCalculator.kt   — ALL formulas, pure Kotlin (Task 2)
│   ├── data/Models.kt             — Vehicle, Trip entities + enums + converters
│   ├── data/Daos.kt               — VehicleDao, TripDao
│   ├── data/AppDatabase.kt        — Room database
│   ├── data/SettingsRepository.kt — DataStore: apiKey, lastUsedVehicleId
│   ├── data/DirectionsClient.kt   — Retrofit iface + DTOs + distanceKm extraction
│   └── ui/
│       ├── theme/Theme.kt
│       ├── Format.kt              — money/number display rounding (2 dp HALF_UP)
│       ├── vehicle/VehicleSetupScreen.kt / VehicleListScreen.kt / VehicleViewModel.kt
│       ├── trip/NewTripScreen.kt / NewTripViewModel.kt
│       ├── history/TripHistoryScreen.kt / TripHistoryViewModel.kt
│       ├── detail/TripDetailScreen.kt / TripDetailViewModel.kt
│       └── settings/SettingsScreen.kt / SettingsViewModel.kt
└── app/src/test/java/com/molei/fuelcost/
    ├── FuelCalculatorTest.kt      — validation gate lives here
    └── DirectionsParsingTest.kt   — meters→km, legs summing, error statuses
```

---

### Task 1: Project scaffold

**Files:** Create all Gradle files, version catalog, wrapper properties + scripts, manifest, `Theme.kt`, `MainActivity.kt` (placeholder screen), `.gitignore`.

- [ ] Step 1: Write Gradle root/app build files, `libs.versions.toml`, `settings.gradle.kts`, `gradle.properties`, wrapper properties (distribution 8.9-bin), `.gitignore` (standard Android).
- [ ] Step 2: Manifest with `INTERNET` permission, app label "Fuel Cost", Material3 theme, single activity.
- [ ] Step 3: Commit `chore: scaffold Android project`.

### Task 2: Calculation engine (TDD — the validation gate)

**Files:** Create `domain/FuelCalculator.kt`, `ui/Format.kt`, test `FuelCalculatorTest.kt`.

**Interfaces (produced, used by every ViewModel):**

```kotlin
enum class FuelPriceMode { PerTank, PerLitre }
enum class TripMode { Normal, Eco }

data class TripCalculation(
    val pricePerLitre: Double,
    val consumptionRate: Double,
    val fuelUsedLitres: Double,
    val cost: Double,
)

object FuelCalculator {
    // 1. pricePerLitre = fuelPricePerTank / tankCapacityLitres (PerTank mode only)
    fun pricePerLitre(mode: FuelPriceMode, perTank: Double, tankCapacityLitres: Double, perLitre: Double): Double
    // 5. consumptionEco = consumptionNormal * (1 - ecoSavingPercent / 100)
    fun deriveEcoConsumption(consumptionNormal: Double, ecoSavingPercent: Double): Double
    // 2. rate = mode == Eco ? consumptionEco : consumptionNormal
    fun consumptionRate(mode: TripMode, consumptionNormal: Double, consumptionEco: Double): Double
    // 3. fuelUsedLitres = distanceKm * consumptionRate / 100
    fun fuelUsedLitres(distanceKm: Double, consumptionRate: Double): Double
    // 4. cost = fuelUsedLitres * pricePerLitre
    fun tripCost(fuelUsedLitres: Double, pricePerLitre: Double): Double
    fun calculate(mode: TripMode, distanceKm: Double, consumptionNormal: Double,
                  consumptionEco: Double, priceMode: FuelPriceMode, perTank: Double,
                  tankCapacityLitres: Double, perLitre: Double): TripCalculation
}

// Format.kt
fun Double.display2dp(): String   // BigDecimal HALF_UP, 2 dp, Locale.US
fun formatMoney(symbol: String, value: Double): String  // "$" + display2dp
```

- [ ] Step 1: Write `FuelCalculatorTest.kt` first — Civic case asserts `fuelUsedLitres.display2dp() == "1.28"` and `formatMoney("$", cost) == "$2.73"`, plus raw-value asserts (1.281 and 128.1/47 within 1e-9), PerLitre passthrough, eco derive 6.7→6.097 at 9%, Normal-mode rate selection.
- [ ] Step 2: Implement `FuelCalculator` + `Format.kt`.
- [ ] Step 3: No local JVM available — verify arithmetic by mirroring formulas + HALF_UP rounding in a Python script; expected output `1.28` / `2.73`. JUnit test remains the in-project gate (`gradlew :app:testDebugUnitTest`).
- [ ] Step 4: Commit `feat: calculation engine with validation-gate tests`.

### Task 3: Data layer (Room + DataStore)

**Files:** Create `data/Models.kt`, `data/Daos.kt`, `data/AppDatabase.kt`, `data/SettingsRepository.kt`, `FuelCostApp.kt`.

**Interfaces (produced):**

```kotlin
@Entity data class Vehicle(id: Long = 0, name: String, consumptionNormal: Double,
    consumptionEco: Double, ecoSavingPercent: Double = 9.0, ecoAutoDerived: Boolean = true,
    tankCapacityLitres: Double, fuelPriceMode: FuelPriceMode, fuelPricePerTank: Double,
    fuelPricePerLitre: Double, currencySymbol: String = "$")
@Entity data class Trip(id: Long = 0, vehicleId: Long, dateEpochMillis: Long,
    startLocation: String?, endLocation: String?, distanceKm: Double,
    distanceSource: DistanceSource, mode: TripMode, fuelUsedLitres: Double, cost: Double)
enum class DistanceSource { Manual, DirectionsAPI }

VehicleDao: observeAll(): Flow<List<Vehicle>>, byId(id): Flow<Vehicle?>, upsert(v): Long, delete(v)
TripDao: observeAllNewestFirst(): Flow<List<Trip>>, byId(id): Flow<Trip?>, insert(t): Long, delete(t)
SettingsRepository: apiKey: Flow<String>, lastVehicleId: Flow<Long?>, setApiKey(s), setLastVehicleId(id)
FuelCostApp exposes: database, settingsRepository
```

- [ ] Step 1: Implement entities + enum TypeConverters + DAOs + database (version 1, `fallbackToDestructiveMigration` off; plain schema, exportSchema=false).
- [ ] Step 2: DataStore-backed `SettingsRepository`.
- [ ] Step 3: Commit `feat: Room entities/DAOs and DataStore settings`.

### Task 4: Directions API client (TDD on parsing)

**Files:** Create `data/DirectionsClient.kt`, test `DirectionsParsingTest.kt`.

**Interfaces (produced):**

```kotlin
sealed interface DistanceLookupResult {
    data class Success(val distanceKm: Double) : DistanceLookupResult
    data class Failure(val message: String) : DistanceLookupResult
}
object DirectionsClient {
    suspend fun lookupDistanceKm(origin: String, destination: String, apiKey: String): DistanceLookupResult
    fun extractDistanceKm(response: DirectionsResponse): DistanceLookupResult  // pure, tested
}
```

- [ ] Step 1: Test first: sums `routes[0].legs[*].distance.value` meters → km; status `ZERO_RESULTS`/`REQUEST_DENIED`/empty routes → Failure with readable message.
- [ ] Step 2: Retrofit interface (`maps.googleapis.com`), Gson DTOs, `lookupDistanceKm` wraps network errors into Failure.
- [ ] Step 3: Commit `feat: Directions API distance lookup`.

### Task 5: ViewModels

**Files:** Create the five ViewModels (vehicle, new-trip, history, detail, settings), each taking DAOs/repos via a factory on `FuelCostApp`.

Key behavior — `NewTripViewModel`: holds `vehicleId/distanceText/mode/start/end` in a `MutableStateFlow` form state; `calculation: StateFlow<TripCalculation?>` recomputed via `combine(form, vehicleFlow)` on every keystroke (live recomputation requirement). `saveTrip()` persists computed values + sets last-used vehicle. `lookupDistance()` reads API key; if blank → emits "no API key" UI event (manual entry stays); on Success fills distance field with source=DirectionsAPI (reset to Manual on manual edit).
`TripHistoryViewModel`: exposes trips + `monthTotal: StateFlow<Double>` (current month via `YearMonth`).
`VehicleViewModel`: form state incl. auto-eco toggle → applies `deriveEcoConsumption` live when toggled on.

- [ ] Step 1: Implement all five + factories. Commit `feat: ViewModels with live recalculation`.

### Task 6: UI screens + navigation

**Files:** Create the six screens + `MainActivity.kt` NavHost with bottom nav (New Trip / History / Vehicles / Settings); detail + edit reached by tap.

- New Trip: vehicle dropdown (defaults last-used), distance field, optional start/end address fields + "Calculate distance" (disabled with explanatory helper text when no API key), Normal/Eco segmented toggle, live result card (rate used, price/L, litres, cost), Save.
- Vehicle Setup: all vehicle fields; eco field disabled+auto-filled when toggle on; per-tank vs per-litre price toggle showing derived price/L live.
- History: newest first, month running total header, rows show date/route-or-"manual entry"/distance/litres/cost.
- Detail: full breakdown listing each formula with the numbers substituted.
- Settings: API key field (password-style), note about manual-only fallback.
- [ ] Step 1: Implement screens + nav. Commit `feat: Compose UI screens and navigation`.

### Task 7: README + final review

- [ ] Step 1: README: open-in-Android-Studio instructions, wrapper note, how to run the unit-test validation gate, API key setup, formulas table.
- [ ] Step 2: Self-review pass (spec coverage, placeholder scan, type consistency across tasks). Commit `docs: README`.
