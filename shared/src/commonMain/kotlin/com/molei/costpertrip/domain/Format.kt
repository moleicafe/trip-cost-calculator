package com.molei.costpertrip.domain

import kotlin.math.abs

/**
 * Display rounding: 2 decimal places, HALF_UP, full precision upstream.
 *
 * Multiplatform replacement for `BigDecimal.valueOf(d).setScale(2, HALF_UP).toPlainString()`.
 * Both operate on the double's shortest round-trip decimal string (Double.toString), so the
 * results are identical — the contract vectors live in FormatTest.
 */
fun Double.display2dp(): String {
    if (isNaN() || isInfinite()) return toString()
    val negative = this < 0.0
    val plain = expandScientific(abs(this).toString())

    val dot = plain.indexOf('.')
    var intDigits = if (dot >= 0) plain.substring(0, dot) else plain
    val frac = (if (dot >= 0) plain.substring(dot + 1) else "") + "000"
    var keep = frac.substring(0, 2)

    // HALF_UP: round away from zero when the 3rd fractional digit is >= 5
    if (frac[2] >= '5') {
        val digits = (intDigits + keep).toCharArray()
        var i = digits.size - 1
        var carry = true
        while (i >= 0 && carry) {
            if (digits[i] == '9') digits[i] = '0' else { digits[i] = digits[i] + 1; carry = false }
            i--
        }
        val summed = (if (carry) "1" else "") + digits.concatToString()
        intDigits = summed.substring(0, summed.length - 2)
        keep = summed.substring(summed.length - 2)
    }

    val isZero = intDigits.all { it == '0' } && keep.all { it == '0' }
    val sign = if (negative && !isZero) "-" else ""
    return "$sign${intDigits.trimStart('0').ifEmpty { "0" }}.$keep"
}

fun formatMoney(currencySymbol: String, value: Double): String =
    currencySymbol + value.display2dp()

/** "1.0E8" -> "100000000", "1.2E-5" -> "0.000012"; plain strings pass through. */
private fun expandScientific(s: String): String {
    val e = s.indexOfFirst { it == 'e' || it == 'E' }
    if (e < 0) return s
    val mantissa = s.substring(0, e)
    val exp = s.substring(e + 1).toInt()
    val dot = mantissa.indexOf('.')
    val digits = mantissa.replace(".", "")
    val pointPos = (if (dot >= 0) dot else mantissa.length) + exp
    return when {
        pointPos <= 0 -> "0." + "0".repeat(-pointPos) + digits
        pointPos >= digits.length -> digits + "0".repeat(pointPos - digits.length)
        else -> digits.substring(0, pointPos) + "." + digits.substring(pointPos)
    }
}
