package com.molei.fuelcost

import com.molei.fuelcost.domain.FuelCalculator
import com.molei.fuelcost.domain.FuelPriceMode
import com.molei.fuelcost.domain.TripMode
import com.molei.fuelcost.ui.display2dp
import com.molei.fuelcost.ui.formatMoney
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Validation gate from the spec: the Honda Civic 1.6 VTi CVT case must
 * reproduce fuelUsed = 1.28 L and cost = $2.73 exactly (at 2 dp display).
 */
class FuelCalculatorTest {

    // Honda Civic 1.6 VTi CVT
    private val consumptionNormal = 6.7
    private val consumptionEco = 6.1
    private val tankCapacityLitres = 47.0
    private val fuelPricePerTank = 100.0

    @Test
    fun `civic sample trip - 21 km eco - reproduces spec values exactly`() {
        val result = FuelCalculator.calculate(
            mode = TripMode.Eco,
            distanceKm = 21.0,
            consumptionNormal = consumptionNormal,
            consumptionEco = consumptionEco,
            priceMode = FuelPriceMode.PerTank,
            fuelPricePerTank = fuelPricePerTank,
            tankCapacityLitres = tankCapacityLitres,
            fuelPricePerLitre = 0.0,
        )

        // Raw values at full precision
        assertEquals(21.0 * 6.1 / 100, result.fuelUsedLitres, 1e-9)          // 1.281
        assertEquals(100.0 / 47.0, result.pricePerLitre, 1e-9)               // 2.1276595...
        assertEquals(1.281 * (100.0 / 47.0), result.cost, 1e-9)              // 2.7255319...

        // Spec's exact displayed outputs
        assertEquals("1.28", result.fuelUsedLitres.display2dp())
        assertEquals("$2.73", formatMoney("$", result.cost))
        assertEquals("2.13", result.pricePerLitre.display2dp())
    }

    @Test
    fun `per-litre price mode uses fuelPricePerLitre directly`() {
        val result = FuelCalculator.calculate(
            mode = TripMode.Normal,
            distanceKm = 100.0,
            consumptionNormal = consumptionNormal,
            consumptionEco = consumptionEco,
            priceMode = FuelPriceMode.PerLitre,
            fuelPricePerTank = 999.0, // must be ignored
            tankCapacityLitres = tankCapacityLitres,
            fuelPricePerLitre = 2.50,
        )
        assertEquals(2.50, result.pricePerLitre, 1e-9)
        assertEquals(6.7, result.fuelUsedLitres, 1e-9) // 100 km at 6.7 L/100km
        assertEquals(6.7 * 2.50, result.cost, 1e-9)
    }

    @Test
    fun `normal mode selects consumptionNormal, eco selects consumptionEco`() {
        assertEquals(6.7, FuelCalculator.consumptionRate(TripMode.Normal, 6.7, 6.1), 1e-9)
        assertEquals(6.1, FuelCalculator.consumptionRate(TripMode.Eco, 6.7, 6.1), 1e-9)
    }

    @Test
    fun `eco consumption auto-derives from eco saving percent`() {
        // 6.7 * (1 - 9/100) = 6.097
        assertEquals(6.097, FuelCalculator.deriveEcoConsumption(6.7, 9.0), 1e-9)
        // 10 * (1 - 15/100) = 8.5
        assertEquals(8.5, FuelCalculator.deriveEcoConsumption(10.0, 15.0), 1e-9)
    }

    @Test
    fun `display rounding is half-up at 2 dp`() {
        assertEquals("1.28", 1.281.display2dp())
        assertEquals("2.73", 2.725531914893617.display2dp())
        assertEquals("2.13", 2.127659574468085.display2dp())
        assertEquals("1.29", 1.285.display2dp()) // half rounds up
        assertEquals("$0.00", formatMoney("$", 0.0))
        assertEquals("€3.50", formatMoney("€", 3.5))
    }
}
