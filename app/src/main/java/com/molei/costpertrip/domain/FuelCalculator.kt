package com.molei.costpertrip.domain

enum class FuelPriceMode { PerTank, PerLitre }

enum class TripMode { Normal, Eco }

/** All four calculated values for a trip, at full precision (round only at display). */
data class TripCalculation(
    val pricePerLitre: Double,
    val consumptionRate: Double,
    val fuelUsedLitres: Double,
    val cost: Double,
)

/**
 * The spec's calculation formulas, verbatim. Pure Kotlin — no Android dependencies —
 * so the validation gate in FuelCalculatorTest runs as a plain JVM unit test.
 */
object FuelCalculator {

    /** 1. pricePerLitre = fuelPricePerTank / tankCapacityLitres (skipped in PerLitre mode). */
    fun pricePerLitre(
        mode: FuelPriceMode,
        fuelPricePerTank: Double,
        tankCapacityLitres: Double,
        fuelPricePerLitre: Double,
    ): Double = when (mode) {
        FuelPriceMode.PerLitre -> fuelPricePerLitre
        FuelPriceMode.PerTank -> fuelPricePerTank / tankCapacityLitres
    }

    /** 2. consumptionRate = (mode == Eco) ? consumptionEco : consumptionNormal */
    fun consumptionRate(mode: TripMode, consumptionNormal: Double, consumptionEco: Double): Double =
        if (mode == TripMode.Eco) consumptionEco else consumptionNormal

    /** 3. fuelUsedLitres = distanceKm * consumptionRate / 100 */
    fun fuelUsedLitres(distanceKm: Double, consumptionRate: Double): Double =
        distanceKm * consumptionRate / 100

    /** 4. cost = fuelUsedLitres * pricePerLitre */
    fun tripCost(fuelUsedLitres: Double, pricePerLitre: Double): Double =
        fuelUsedLitres * pricePerLitre

    /** 5. consumptionEco = consumptionNormal * (1 - ecoSavingPercent / 100) */
    fun deriveEcoConsumption(consumptionNormal: Double, ecoSavingPercent: Double): Double =
        consumptionNormal * (1 - ecoSavingPercent / 100)

    fun calculate(
        mode: TripMode,
        distanceKm: Double,
        consumptionNormal: Double,
        consumptionEco: Double,
        priceMode: FuelPriceMode,
        fuelPricePerTank: Double,
        tankCapacityLitres: Double,
        fuelPricePerLitre: Double,
    ): TripCalculation {
        val price = pricePerLitre(priceMode, fuelPricePerTank, tankCapacityLitres, fuelPricePerLitre)
        val rate = consumptionRate(mode, consumptionNormal, consumptionEco)
        val fuel = fuelUsedLitres(distanceKm, rate)
        return TripCalculation(
            pricePerLitre = price,
            consumptionRate = rate,
            fuelUsedLitres = fuel,
            cost = tripCost(fuel, price),
        )
    }
}
