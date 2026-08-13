package com.encryxed.tally.ui

import com.encryxed.tally.data.Receipt
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

private val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
private val monthTitle: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

fun formatMoney(amount: Double, currencyCode: String): String =
    runCatching {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance(currencyCode)
        }.format(amount)
    }.getOrElse { String.format(Locale.getDefault(), "%s %.2f", currencyCode, amount) }

fun formatDate(date: LocalDate): String = date.format(dayMonth)

/**
 * Plain editable text for an amount, using the currency's own precision —
 * dinars need three decimals, euros two, yen none.
 */
fun moneyToEditText(amount: Double, currencyCode: String): String {
    val digits = runCatching { Currency.getInstance(currencyCode).defaultFractionDigits }
        .getOrDefault(2)
        .coerceIn(0, 4)
    return String.format(Locale.ROOT, "%.${digits}f", amount)
}

fun formatMonth(month: YearMonth): String = month.atDay(1).format(monthTitle)

/**
 * The currency most of these receipts are in.
 *
 * Totals must never be summed across currencies — 20 EUR plus 20 JOD is not
 * 40 of anything. Summaries report this one currency and say how many
 * receipts they left out.
 */
fun primaryCurrency(receipts: List<Receipt>, fallback: String): String =
    receipts.groupingBy { it.currency }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?: fallback
