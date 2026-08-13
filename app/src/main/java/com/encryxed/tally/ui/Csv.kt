package com.encryxed.tally.ui

import com.encryxed.tally.data.Receipt

/**
 * Builds a spreadsheet-ready export.
 *
 * Values are quoted and embedded quotes doubled, so a shop called
 * O"Brien's, Ltd doesn't shift every following column.
 */
fun buildCsv(receipts: List<Receipt>): String = buildString {
    appendLine("date,merchant,category,total,currency,note")
    receipts
        .sortedWith(compareByDescending<Receipt> { it.date }.thenByDescending { it.id })
        .forEach { r ->
            appendLine(
                listOf(
                    r.date.toString(),
                    r.merchant,
                    r.category.label,
                    String.format(java.util.Locale.ROOT, "%.2f", r.total),
                    r.currency,
                    r.note,
                ).joinToString(",") { field -> "\"" + field.replace("\"", "\"\"") + "\"" }
            )
        }
}
