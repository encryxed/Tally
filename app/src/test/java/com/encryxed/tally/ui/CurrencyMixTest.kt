package com.encryxed.tally.ui

import com.encryxed.tally.data.Receipt
import com.encryxed.tally.parse.Category
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Adding amounts in different currencies together produces a meaningless
 * number, so the summaries must pick one currency and be honest about the
 * rest.
 */
class CurrencyMixTest {

    private fun receipt(id: Long, currency: String, total: Double) = Receipt(
        id = id,
        merchant = "Shop $id",
        total = total,
        currency = currency,
        date = LocalDate.of(2026, 8, 10),
        category = Category.OTHER,
    )

    @Test
    fun `picks the most common currency`() {
        val receipts = listOf(
            receipt(1, "EUR", 10.0),
            receipt(2, "JOD", 5.0),
            receipt(3, "EUR", 20.0),
            receipt(4, "EUR", 30.0),
        )
        assertEquals("EUR", primaryCurrency(receipts, "USD"))
    }

    @Test
    fun `falls back when there are no receipts at all`() {
        assertEquals("USD", primaryCurrency(emptyList(), "USD"))
    }

    @Test
    fun `a single foreign receipt still defines its own currency`() {
        assertEquals("JOD", primaryCurrency(listOf(receipt(1, "JOD", 28.668)), "EUR"))
    }

    @Test
    fun `csv keeps every receipt in its own currency`() {
        val csv = buildCsv(
            listOf(receipt(1, "EUR", 10.0), receipt(2, "JOD", 28.668))
        )
        assertEquals(true, csv.contains("\"EUR\""))
        assertEquals(true, csv.contains("\"JOD\""))
    }

    @Test
    fun `edit field uses the currency's own precision`() {
        assertEquals("28.668", moneyToEditText(28.668, "JOD"))
        assertEquals("12.50", moneyToEditText(12.5, "EUR"))
        assertEquals("330", moneyToEditText(330.0, "JPY"))
    }
}
