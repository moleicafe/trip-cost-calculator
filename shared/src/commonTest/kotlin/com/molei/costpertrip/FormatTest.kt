package com.molei.costpertrip

import com.molei.costpertrip.domain.display2dp
import com.molei.costpertrip.domain.formatMoney
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract: display2dp() must match the old JVM implementation,
 * BigDecimal.valueOf(d).setScale(2, HALF_UP).toPlainString(), for all app-range values.
 */
class FormatTest {

    @Test
    fun roundsHalfUpAt2dp() {
        assertEquals("1.28", 1.281.display2dp())
        assertEquals("2.73", 2.725531914893617.display2dp())
        assertEquals("2.13", 2.127659574468085.display2dp())
        assertEquals("1.29", 1.285.display2dp()) // half rounds up
        assertEquals("1.28", 1.2849999.display2dp())
    }

    @Test
    fun padsTrailingZeros() {
        assertEquals("0.00", 0.0.display2dp())
        assertEquals("3.50", 3.5.display2dp())
        assertEquals("5.00", 5.0.display2dp())
        assertEquals("21.00", 21.0.display2dp())
    }

    @Test
    fun carryPropagatesThroughNines() {
        assertEquals("10.00", 9.995.display2dp())
        assertEquals("100.00", 99.999.display2dp())
        assertEquals("1.00", 0.999.display2dp())
    }

    @Test
    fun negativesRoundAwayFromZeroAndZeroHasNoSign() {
        assertEquals("-1.29", (-1.285).display2dp())
        assertEquals("-1.28", (-1.281).display2dp())
        assertEquals("0.00", (-0.001).display2dp())
    }

    @Test
    fun largeAndSmallMagnitudes() {
        assertEquals("100000000.00", 1.0E8.display2dp())
        assertEquals("0.00", 1.2E-5.display2dp())
        assertEquals("1234567.89", 1234567.89.display2dp())
    }

    @Test
    fun moneyPrependsSymbol() {
        assertEquals("$0.00", formatMoney("$", 0.0))
        assertEquals("€3.50", formatMoney("€", 3.5))
        assertEquals("S$2.73", formatMoney("S$", 2.725531914893617))
    }
}
