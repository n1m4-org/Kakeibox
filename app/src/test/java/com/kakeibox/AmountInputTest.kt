package com.kakeibox

import com.kakeibox.ui.quickadd.AmountKey
import com.kakeibox.ui.quickadd.MAX_AMOUNT_DIGITS
import com.kakeibox.ui.quickadd.applyKey
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountInputTest {
    private fun type(vararg keys: AmountKey) = keys.fold("") { acc, k -> applyKey(acc, k) }
    private fun d(n: Int) = AmountKey.Digit(n)

    @Test
    fun digitsAppend() = assertEquals("1280", type(d(1), d(2), d(8), d(0)))

    @Test
    fun leadingZeroIgnored() {
        assertEquals("", type(d(0)))
        assertEquals("", type(AmountKey.DoubleZero))
        assertEquals("5", type(d(0), AmountKey.DoubleZero, d(5)))
    }

    @Test
    fun doubleZeroAppendsTwoZeros() = assertEquals("300", type(d(3), AmountKey.DoubleZero))

    @Test
    fun backspaceRemovesLastDigit() {
        assertEquals("12", type(d(1), d(2), d(3), AmountKey.Backspace))
        assertEquals("", type(AmountKey.Backspace))
    }

    @Test
    fun maxDigitsEnforced() {
        val full = "9".repeat(MAX_AMOUNT_DIGITS)
        assertEquals(full, applyKey(full, d(1)))
        val almost = "9".repeat(MAX_AMOUNT_DIGITS - 1)
        assertEquals(almost, applyKey(almost, AmountKey.DoubleZero))
        assertEquals(almost + "0", applyKey(almost, d(0)))
    }
}
