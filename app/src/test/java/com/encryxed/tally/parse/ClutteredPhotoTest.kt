package com.encryxed.tally.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * A photographed receipt rarely fills the frame on its own. There is usually
 * something else in shot — a screen behind it, a poster, a second receipt on
 * the table — and that clutter is often recognised before the receipt itself
 * in reading order.
 *
 * These tests pin the general rule that resolves it: the shop's own name is
 * printed *prominently* at the head of the receipt, so prominence must beat
 * reading order. No test here depends on any particular shop.
 */
class ClutteredPhotoTest {

    private val today = LocalDate.of(2026, 8, 20)

    /**
     * Builds a page where each line carries its own text height, so the tests
     * can express "this line is large" and "this one is incidental".
     */
    private fun page(vararg lines: Pair<String, Int>): List<OcrLine> {
        var y = 0
        return lines.map { (text, height) ->
            val top = y
            y += height + 6
            OcrLine(text = text, left = 0, top = top, right = 400, bottom = top + height)
        }
    }

    @Test
    fun `small background text does not outrank the large shop header`() {
        val parsed = ReceiptParser.parse(
            page(
                // Incidental clutter caught at the edge of the photo, in tiny
                // text, and recognised first.
                "amazon.com/orders" to 8,
                "youtube.com" to 8,
                // The receipt's own header, printed large.
                "WALMART" to 40,
                "Save money. Live better." to 12,
                "GREAT VALUE" to 12,
                "TOTAL 98.21" to 14,
                "07/28/2026" to 12,
            ),
            preferDayFirst = false,
            today = today,
        )

        assertEquals("Walmart", parsed.merchant)
        assertNotEquals("Amazon", parsed.merchant)
    }

    @Test
    fun `prominence wins regardless of which chain appears first`() {
        // Same page, opposite chains, to prove the rule is about size and not
        // about any particular shop being preferred.
        val parsed = ReceiptParser.parse(
            page(
                "walmart.com/help" to 8,
                "AMAZON FRESH" to 40,
                "TOTAL 12.00" to 14,
                "07/28/2026" to 12,
            ),
            preferDayFirst = false,
            today = today,
        )

        assertEquals("Amazon", parsed.merchant)
    }

    @Test
    fun `a labelled total beats a larger line item elsewhere on the page`() {
        val parsed = ReceiptParser.parse(
            page(
                "CORNER MARKET" to 36,
                "BIG TICKET ITEM 99.99" to 12,
                "SUBTOTAL 93.62" to 12,
                "TAX 1 6.750 % 4.59" to 12,
                "TOTAL 98.21" to 12,
                "VISA TEND 98.21" to 12,
                "CHANGE DUE 0.00" to 12,
                "07/28/2026" to 12,
            ),
            preferDayFirst = false,
            today = today,
        )

        assertEquals(98.21, parsed.total!!, 0.001)
        assertEquals(Confidence.HIGH, parsed.totalConfidence)
    }

    @Test
    fun `quality score prefers the reading with more text and firmer fields`() {
        // Stands in for the same photo read at two rotations: one upright,
        // one sideways where OCR recovers only fragments.
        val upright = ReceiptParser.parse(
            page(
                "CORNER MARKET" to 36,
                "BREAD 2.49" to 12,
                "MILK 1.29" to 12,
                "TOTAL 3.78" to 12,
                "07/28/2026" to 12,
            ),
            preferDayFirst = false,
            today = today,
        )
        val sideways = ReceiptParser.parse(
            page("RKET" to 12, "2.4" to 12),
            preferDayFirst = false,
            today = today,
        )

        assertTrue(
            "upright=${upright.qualityScore} sideways=${sideways.qualityScore}",
            upright.qualityScore > sideways.qualityScore,
        )
    }

    @Test
    fun `an all-clutter page still refuses to invent a total`() {
        val parsed = ReceiptParser.parse(
            page("youtube.com" to 8, "New Tab" to 8, "Instagram" to 8),
            preferDayFirst = false,
            today = today,
        )
        assertEquals(null, parsed.total)
        assertTrue(!parsed.isComplete)
    }
}
