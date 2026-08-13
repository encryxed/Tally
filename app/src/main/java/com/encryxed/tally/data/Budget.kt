package com.encryxed.tally.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class BudgetPeriod(val label: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
}

/** A spending cap for the current week or month. */
data class Budget(
    val amount: Double,
    val period: BudgetPeriod,
) {
    fun startOf(today: LocalDate): LocalDate = when (period) {
        BudgetPeriod.WEEKLY -> today.with(DayOfWeek.MONDAY)
        BudgetPeriod.MONTHLY -> today.withDayOfMonth(1)
    }

    fun endOf(today: LocalDate): LocalDate = when (period) {
        BudgetPeriod.WEEKLY -> startOf(today).plusDays(6)
        BudgetPeriod.MONTHLY -> today.withDayOfMonth(today.lengthOfMonth())
    }

    /** Days remaining including today, so the last day still reads "1 day left". */
    fun daysLeft(today: LocalDate): Long =
        ChronoUnit.DAYS.between(today, endOf(today)) + 1
}

/** Spend measured against a budget for the period containing [today]. */
data class BudgetProgress(
    val budget: Budget,
    val spent: Double,
    val today: LocalDate,
) {
    val remaining: Double get() = budget.amount - spent

    val fraction: Float
        get() = if (budget.amount <= 0) 0f else (spent / budget.amount).toFloat()

    val isOver: Boolean get() = spent > budget.amount

    /** What's left to spend per remaining day without blowing the budget. */
    val perDayLeft: Double
        get() {
            val days = budget.daysLeft(today).coerceAtLeast(1)
            return (remaining / days).coerceAtLeast(0.0)
        }
}
