package com.encryxed.tally.parse

import java.time.LocalDate

/** Month names across the languages most likely to show up on a till receipt. */
private val MONTH_NAMES: Map<String, Int> = mapOf(
    "jan" to 1,
    "feb" to 2, "fev" to 2,
    "mar" to 3, "mrt" to 3, "maa" to 3, "mär" to 3,
    "apr" to 4, "avr" to 4,
    "may" to 5, "mei" to 5, "mai" to 5, "mag" to 5,
    "jun" to 6, "giu" to 6,
    "jul" to 7, "lug" to 7,
    "aug" to 8, "ago" to 8, "aou" to 8,
    "sep" to 9, "set" to 9,
    "oct" to 10, "okt" to 10, "ott" to 10,
    "nov" to 11,
    "dec" to 12, "dez" to 12, "dic" to 12,
)

// yyyy-mm-dd — unambiguous by construction.
private val ISO = Regex("""(?<!\d)(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})(?!\d)""")

// dd-mm-yyyy or mm-dd-yyyy — order decided below.
private val NUMERIC_LONG_YEAR = Regex("""(?<!\d)(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})(?!\d)""")

// dd-mm-yy or mm-dd-yy.
private val NUMERIC_SHORT_YEAR = Regex("""(?<!\d)(\d{1,2})[-/.](\d{1,2})[-/.](\d{2})(?!\d)""")

// 12 aug 2026 / 12-AUG-26
private val DAY_MONTH_NAME = Regex("""(?<!\d)(\d{1,2})[ .\-]+([\p{L}]{3,9})\.?[ ,.\-]+(\d{2,4})(?!\d)""")

// Aug 12, 2026
private val MONTH_NAME_DAY = Regex("""([\p{L}]{3,9})\.?[ ,.\-]+(\d{1,2})[ ,.\-]+(\d{2,4})(?!\d)""")

// A clock time is a strong hint that the date beside it is the purchase date.
// Deliberately only ':' — using '.' too would match prices like 12.50.
private val TIME = Regex("""(?<!\d)([01]?\d|2[0-3]):([0-5]\d)(?!\d)""")

private data class Candidate(val date: LocalDate, val confidence: Confidence, val lineIndex: Int)

/**
 * Finds the purchase date on a receipt.
 *
 * Day/month order is genuinely ambiguous for things like 03/04/2026, so the
 * parser only falls back to [preferDayFirst] when neither number settles it,
 * and reports that guess as MEDIUM rather than HIGH.
 */
fun findDate(
    lines: List<String>,
    preferDayFirst: Boolean,
    today: LocalDate,
): Pair<LocalDate?, Confidence> {
    val candidates = mutableListOf<Candidate>()

    lines.forEachIndexed { index, line ->
        val hasTime = TIME.containsMatchIn(line)

        fun add(date: LocalDate?, base: Confidence) {
            if (date == null || !isPlausible(date, today)) return
            // A time on the same line promotes a merely-plausible date.
            val confidence = if (hasTime && base == Confidence.MEDIUM) Confidence.HIGH else base
            candidates += Candidate(date, confidence, index)
        }

        ISO.findAll(line).forEach { m ->
            val (y, mo, d) = m.destructured
            add(safeDate(y.toInt(), mo.toInt(), d.toInt()), Confidence.HIGH)
        }

        NUMERIC_LONG_YEAR.findAll(line).forEach { m ->
            val (a, b, y) = m.destructured
            add(resolveOrder(a.toInt(), b.toInt(), y.toInt(), preferDayFirst), orderConfidence(a.toInt(), b.toInt()))
        }

        NUMERIC_SHORT_YEAR.findAll(line).forEach { m ->
            val (a, b, y) = m.destructured
            val year = expandYear(y.toInt(), today)
            val base = orderConfidence(a.toInt(), b.toInt())
            // Two-digit years are that bit less trustworthy.
            val downgraded = if (base == Confidence.HIGH) Confidence.MEDIUM else Confidence.LOW
            add(resolveOrder(a.toInt(), b.toInt(), year, preferDayFirst), downgraded)
        }

        DAY_MONTH_NAME.findAll(line).forEach { m ->
            val (d, name, y) = m.destructured
            val month = monthFrom(name) ?: return@forEach
            add(safeDate(expandYear(y.toInt(), today), month, d.toInt()), Confidence.HIGH)
        }

        MONTH_NAME_DAY.findAll(line).forEach { m ->
            val (name, d, y) = m.destructured
            val month = monthFrom(name) ?: return@forEach
            add(safeDate(expandYear(y.toInt(), today), month, d.toInt()), Confidence.HIGH)
        }
    }

    if (candidates.isEmpty()) return null to Confidence.NONE

    val best = candidates
        .sortedWith(compareBy({ it.confidence.ordinal }, { it.lineIndex }))
        .first()
    return best.date to best.confidence
}

/** HIGH when one of the two numbers can only be a day. */
private fun orderConfidence(a: Int, b: Int): Confidence =
    if (a > 12 || b > 12) Confidence.HIGH else Confidence.MEDIUM

private fun resolveOrder(a: Int, b: Int, year: Int, preferDayFirst: Boolean): LocalDate? = when {
    a > 12 && b <= 12 -> safeDate(year, b, a)   // a must be the day
    b > 12 && a <= 12 -> safeDate(year, a, b)   // b must be the day
    preferDayFirst -> safeDate(year, b, a)
    else -> safeDate(year, a, b)
}

private fun monthFrom(name: String): Int? =
    MONTH_NAMES[name.lowercase().take(3)]

private fun expandYear(year: Int, today: LocalDate): Int = when {
    year >= 100 -> year
    // "26" -> 2026, but a year slightly ahead of now rolls back a century.
    2000 + year <= today.year + 1 -> 2000 + year
    else -> 1900 + year
}

private fun safeDate(year: Int, month: Int, day: Int): LocalDate? =
    runCatching { LocalDate.of(year, month, day) }.getOrNull()

/** Rejects card expiry dates, printed copyright years and OCR noise. */
private fun isPlausible(date: LocalDate, today: LocalDate): Boolean =
    !date.isAfter(today.plusDays(1)) && date.isAfter(today.minusYears(15))
