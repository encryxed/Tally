package com.encryxed.tally.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * A poor photo often recognises a receipt's *words* while losing its column of
 * amounts entirely — the labels are large and bold, the figures small.
 *
 * The danger is that the parser then reaches for whatever other number it can
 * see and presents it as the total. That number can come from anywhere: a
 * second receipt in the same frame, a price list behind it, a barcode. A wrong
 * figure recorded confidently is worse than an admitted gap, because the user
 * has no reason to re-check it.
 */
class UnreadableTotalTest {

    private val today = LocalDate.of(2026, 8, 20)

    private fun page(vararg lines: String): List<OcrLine> =
        lines.mapIndexed { i, text ->
            OcrLine(text = text, left = 0, top = i * 35, right = 400, bottom = i * 35 + 30)
        }

    @Test
    fun `a labelled total with no readable figure yields nothing, not a guess`() {
        val parsed = ReceiptParser.parse(
            page(
                "CORNER MARKET",
                "SUBTOTAL",          // amount column lost to the photo
                "TAX",
                "TOTAL",
                "CARD PAYMENT",
                // An unrelated amount lower down the frame — a second receipt,
                // a leaflet, anything. It must not be adopted as the total.
                "GREEN GRAPE 085055000271 F",
                "4.22 N",
            ),
            preferDayFirst = false,
            today = today,
        )

        assertNull("must not adopt an unrelated number", parsed.total)
        assertEquals(Confidence.NONE, parsed.totalConfidence)
        assertTrue(parsed.uncertainFields.contains("total"))
    }

    @Test
    fun `a receipt that never labels a total may still be guessed at`() {
        // The original fallback stays available where its premise holds: no
        // total was named anywhere, so the largest amount low on the page is
        // a reasonable, clearly-flagged guess.
        val parsed = ReceiptParser.parse(
            page(
                "CORNER SHOP",
                "Water 1,10",
                "Snack 2,40",
                "3,50",
            ),
            preferDayFirst = true,
            today = today,
        )

        assertEquals(3.50, parsed.total!!, 0.001)
        assertEquals(Confidence.LOW, parsed.totalConfidence)
    }

    @Test
    fun `a readable labelled total is unaffected`() {
        val parsed = ReceiptParser.parse(
            page(
                "CORNER MARKET",
                "SUBTOTAL 93.62",
                "TAX 4.59",
                "TOTAL 98.21",
                "4.22",
            ),
            preferDayFirst = false,
            today = today,
        )

        assertEquals(98.21, parsed.total!!, 0.001)
        assertEquals(Confidence.HIGH, parsed.totalConfidence)
    }
}
