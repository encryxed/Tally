package com.encryxed.tally.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetTest {

    // Thursday 13 August 2026.
    private val thursday = LocalDate.of(2026, 8, 13)

    @Test
    fun `weekly period runs monday to sunday`() {
        val budget = Budget(200.0, BudgetPeriod.WEEKLY)
        assertEquals(LocalDate.of(2026, 8, 10), budget.startOf(thursday))
        assertEquals(LocalDate.of(2026, 8, 16), budget.endOf(thursday))
    }

    @Test
    fun `monthly period covers the whole calendar month`() {
        val budget = Budget(800.0, BudgetPeriod.MONTHLY)
        assertEquals(LocalDate.of(2026, 8, 1), budget.startOf(thursday))
        assertEquals(LocalDate.of(2026, 8, 31), budget.endOf(thursday))
    }

    @Test
    fun `days left counts today so the final day reads as one`() {
        val budget = Budget(200.0, BudgetPeriod.WEEKLY)
        assertEquals(4, budget.daysLeft(thursday))          // Thu..Sun
        assertEquals(1, budget.daysLeft(LocalDate.of(2026, 8, 16)))
    }

    @Test
    fun `reports remaining spend and a daily allowance`() {
        val progress = BudgetProgress(
            budget = Budget(200.0, BudgetPeriod.WEEKLY),
            spent = 120.0,
            today = thursday,
        )
        assertEquals(80.0, progress.remaining, 0.001)
        assertEquals(0.6f, progress.fraction, 0.001f)
        assertFalse(progress.isOver)
        assertEquals(20.0, progress.perDayLeft, 0.001)      // 80 over 4 days
    }

    @Test
    fun `flags going over budget and never suggests negative daily spend`() {
        val progress = BudgetProgress(
            budget = Budget(100.0, BudgetPeriod.MONTHLY),
            spent = 145.0,
            today = thursday,
        )
        assertTrue(progress.isOver)
        assertEquals(-45.0, progress.remaining, 0.001)
        assertEquals(0.0, progress.perDayLeft, 0.001)
    }

    @Test
    fun `a zero budget does not blow up the progress bar`() {
        val progress = BudgetProgress(Budget(0.0, BudgetPeriod.MONTHLY), spent = 10.0, today = thursday)
        assertEquals(0f, progress.fraction, 0.001f)
    }
}
