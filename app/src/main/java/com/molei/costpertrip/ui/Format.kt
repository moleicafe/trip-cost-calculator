package com.molei.costpertrip.ui

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Display rounding: 2 decimal places, HALF_UP. Calculations keep full precision;
 * rounding happens only here, at the display boundary.
 */
fun Double.display2dp(): String =
    BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP).toPlainString()

fun formatMoney(currencySymbol: String, value: Double): String =
    currencySymbol + value.display2dp()
