# Fuel Cost Calculator

Native Android app (Kotlin, Jetpack Compose, Material 3) that calculates fuel cost per trip
from a vehicle's consumption rate, trip distance, and fuel price.

- **Vehicle profiles** — consumption (normal + eco, with auto-derive from an eco-saving %),
  tank capacity, fuel price entered per full tank or per litre, per-vehicle currency symbol.
- **New Trip** — pick a vehicle (defaults to last used), enter distance manually or look up
  road distance between two addresses via the Google Maps Directions API, toggle Normal/Eco.
  The result card (rate used, price/L, fuel used, cost) recomputes live as you type.
- **Trip History** — newest first, with a running total for the current month.
- **Trip Detail** — full breakdown showing the formulas with the trip's numbers substituted.

## Formulas (from the spec, used exactly)

| # | Value | Formula |
|---|-------|---------|
| 1 | pricePerLitre | `fuelPricePerTank / tankCapacityLitres` (skipped in per-litre mode) |
| 2 | consumptionRate | `mode == Eco ? consumptionEco : consumptionNormal` |
| 3 | fuelUsedLitres | `distanceKm × consumptionRate / 100` |
| 4 | cost | `fuelUsedLitres × pricePerLitre` |
| 5 | consumptionEco (auto) | `consumptionNormal × (1 − ecoSavingPercent / 100)` |

Calculations keep full double precision; rounding (2 dp, HALF_UP) happens only at display.

## Building

1. Open the project folder in **Android Studio** (Ladybug or newer; JDK 17).
2. The Gradle wrapper **jar** is intentionally not committed. Either let Android Studio
   configure Gradle when it prompts, or run `gradle wrapper --gradle-version 8.9` once from
   any local Gradle install. `gradle/wrapper/gradle-wrapper.properties` already pins 8.9.
3. Sync and run the `app` configuration (minSdk 26).

## Validation gate (run before trusting the numbers)

```
gradlew :app:testDebugUnitTest
```

`FuelCalculatorTest` reproduces the spec's acceptance case — Honda Civic 1.6 VTi CVT
(6.7 / 6.1 L/100km, 47 L tank, $100 per tank), 21 km trip in eco mode — and requires
**1.28 L** fuel used and **$2.73** cost exactly at display precision (price/L = $2.13).
`DirectionsParsingTest` covers the Directions API response parsing.

## Distance lookup and the API key

Address-to-address distance uses the **Google Maps Directions API** (road distance, sum of
the first route's legs). Supply your own key in **Settings** — it is stored on-device in
DataStore and never hardcoded in source or sent anywhere except Google's API.

Without a key, distance entry is **manual only**. The app deliberately has **no
straight-line/haversine fallback**: it under-estimates road distance meaningfully (a tested
Yishun → Jalan Sedap case came out ~7% short: 19.5 km estimated vs 21 km actual road distance).

## Architecture

```
domain/FuelCalculator.kt   pure Kotlin — all five formulas, no Android deps (unit-tested)
data/                      Room (Vehicle, Trip), DataStore settings, Retrofit Directions client
ui/<feature>/              one ViewModel (StateFlow, live recombination) + screen per feature
MainActivity.kt            NavHost + bottom navigation (New Trip · History · Vehicles · Settings)
```

## Deliberately not built yet

The spec gates nice-to-haves (CO₂-based entry, CSV export, spend charts, km/L display,
multi-vehicle comparison) on the core flow being verified end-to-end on a device first.
