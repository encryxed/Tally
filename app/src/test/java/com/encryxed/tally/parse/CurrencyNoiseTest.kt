package com.encryxed.tally.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OCR on a poor photo produces plenty of three-letter fragments. A stray one
 * that happens to spell a currency code must not relabel the whole receipt,
 * because the consequence is silent and total: every amount on the page ends
 * up denominated in a currency the shop never used.
 */
class CurrencyNoiseTest {

    @Test
    fun `a stray code far from any amount is ignored`() {
        val noisy = """
            WALMART
            PLN GARBLED SCAN ARTEFACT
            GREAT VALUE 9.97
            TOTAL 98.21
        """.trimIndent()

        val guess = detectCurrencyDetailed(noisy, fallback = "USD")
        assertEquals("USD", guess.code)
        assertFalse("a floating code is not explicit evidence", guess.explicit)
    }

    @Test
    fun `a code printed beside an amount is believed`() {
        val guess = detectCurrencyDetailed("TOTAL 28.668 JD", fallback = "USD")
        assertEquals("JOD", guess.code)
        assertTrue(guess.explicit)
    }

    @Test
    fun `a printed symbol beats a stray code elsewhere on the page`() {
        val text = """
            SOME PLN NOISE HERE
            TOTAL ${'$'}98.21
        """.trimIndent()

        assertEquals("USD", detectCurrencyDetailed(text, fallback = "EUR").code)
    }

    @Test
    fun `an unmarked receipt falls back to the device currency`() {
        val text = """
            CORNER MARKET
            BREAD 2.49
            TOTAL 2.49
        """.trimIndent()

        val guess = detectCurrencyDetailed(text, fallback = "GBP")
        assertEquals("GBP", guess.code)
        assertFalse(guess.explicit)
    }

    @Test
    fun `euro symbol is still detected normally`() {
        assertEquals("EUR", detectCurrencyDetailed("TOTAAL €16,31", fallback = "USD").code)
    }
}
