package com.encryxed.tally.parse

/** A money value spotted inside a line of text. */
data class MoneyMatch(
    val value: Double,
    val raw: String,
    val start: Int,
    /** True when the token carried a real fractional part, not just an integer. */
    val hasDecimals: Boolean,
)

/**
 * Ordered alternation, widest pattern first:
 *  1. grouped thousands with decimals  -> 1.234,56 / 1,234.56 / 1 234,567
 *  2. plain decimals                   -> 12,50 / 12.50 / 28.668
 *  3. bare integer                     -> 12
 *
 * Two *or three* fractional digits: Jordanian dinar, Kuwaiti dinar, Bahraini
 * dinar and friends are quoted to three (28.668 JD), and assuming two turned
 * every amount on such a receipt into a wrong integer.
 */
private val MONEY = Regex(
    """(?<![\d\p{L}])(-?\d{1,3}(?:[ .,]\d{3})+[.,]\d{2,3}|-?\d+[.,]\d{2,3}|-?\d{1,6})(?![\d])"""
)

private val TWO_DECIMALS = Regex("""(?<!\d)\d+[.,]\d{2}(?!\d)""")
private val THREE_DECIMALS = Regex("""(?<!\d)\d+[.,]\d{3}(?!\d)""")

/**
 * Works out whether this receipt quotes money to two or three decimals.
 *
 * `1.234` is genuinely ambiguous — 1234 euros or 1.234 dinars — so it can't be
 * decided per-token. Across a whole receipt it's obvious: a three-decimal till
 * prints dozens of `0.428`-shaped amounts and almost no two-decimal ones.
 */
fun detectDecimalDigits(text: String): Int {
    val three = THREE_DECIMALS.findAll(text).count()
    val two = TWO_DECIMALS.findAll(text).count()
    return if (three >= 3 && three > two) 3 else 2
}

/** Every money-shaped token in a line, left to right. */
fun findMoney(line: String, decimalDigits: Int = 2): List<MoneyMatch> =
    MONEY.findAll(line).mapNotNull { m ->
        val raw = m.groupValues[1]
        val (value, wasDecimal) = interpret(raw, decimalDigits) ?: return@mapNotNull null
        MoneyMatch(value = value, raw = raw, start = m.range.first, hasDecimals = wasDecimal)
    }.toList()

/**
 * Turns a money token into a number without knowing the locale up front.
 *
 * Whichever of `.` or `,` appears *last* is the decimal separator, provided
 * the digits after it match what this receipt uses. Anything else is
 * thousands grouping and gets stripped — which is how 1.234,56 and 1,234.56
 * are handled by the same code.
 */
fun parseMoneyToken(token: String, decimalDigits: Int = 2): Double? =
    interpret(token, decimalDigits)?.first

/** Returns the value plus whether a real decimal separator was present. */
private fun interpret(token: String, decimalDigits: Int): Pair<Double, Boolean>? {
    val negative = token.trimStart().startsWith("-")
    val s = token.replace(" ", "").removePrefix("-")
    if (s.isEmpty() || s.none { it.isDigit() }) return null

    val sepIndex = maxOf(s.lastIndexOf('.'), s.lastIndexOf(','))

    var isDecimal = false
    val normalized = if (sepIndex < 0) {
        s
    } else {
        val digitsAfter = s.length - sepIndex - 1
        // Three digits after the separator is grouping on a 2dp receipt, but
        // the fractional part on a 3dp one.
        val grouping = digitsAfter == 3 && decimalDigits == 2
        if (!grouping && digitsAfter in 1..3) {
            isDecimal = true
            val intPart = s.substring(0, sepIndex).replace(".", "").replace(",", "")
            val fracPart = s.substring(sepIndex + 1)
            "$intPart.$fracPart"
        } else {
            s.replace(".", "").replace(",", "")
        }
    }

    val value = normalized.toDoubleOrNull() ?: return null
    return (if (negative) -value else value) to isDecimal
}

/**
 * ISO codes first, then the local abbreviations tills actually print (JD, KD).
 * All matched on word boundaries so a stray "JD" inside a word can't rewrite
 * the whole receipt.
 */
private val CURRENCY_CODES: List<Pair<Regex, String>> = listOf(
    // Three-decimal currencies — the ones that break naive parsers.
    "JOD" to "JOD", "KWD" to "KWD", "BHD" to "BHD",
    "OMR" to "OMR", "TND" to "TND", "LYD" to "LYD", "IQD" to "IQD",
    // Zero-decimal currencies.
    "JPY" to "JPY", "KRW" to "KRW", "ISK" to "ISK", "CLP" to "CLP",
    "VND" to "VND", "HUF" to "HUF", "PYG" to "PYG", "RWF" to "RWF",
    // Everything else.
    "EUR" to "EUR", "GBP" to "GBP", "USD" to "USD", "CHF" to "CHF",
    "SEK" to "SEK", "NOK" to "NOK", "DKK" to "DKK", "PLN" to "PLN",
    "CZK" to "CZK", "RON" to "RON", "BGN" to "BGN", "UAH" to "UAH",
    "RUB" to "RUB", "TRY" to "TRY", "ILS" to "ILS", "AED" to "AED",
    "SAR" to "SAR", "QAR" to "QAR", "EGP" to "EGP", "MAD" to "MAD",
    "DZD" to "DZD", "LBP" to "LBP", "ZAR" to "ZAR", "NGN" to "NGN",
    "KES" to "KES", "INR" to "INR", "PKR" to "PKR", "BDT" to "BDT",
    "THB" to "THB", "PHP" to "PHP", "IDR" to "IDR", "MYR" to "MYR",
    "SGD" to "SGD", "HKD" to "HKD", "TWD" to "TWD", "CNY" to "CNY",
    "AUD" to "AUD", "NZD" to "NZD", "CAD" to "CAD", "MXN" to "MXN",
    "BRL" to "BRL", "ARS" to "ARS", "COP" to "COP", "PEN" to "PEN",
    // Local shorthands.
    "JD" to "JOD", "KD" to "KWD", "BD" to "BHD", "SR" to "SAR",
    "RM" to "MYR", "RP" to "IDR",
).map { (code, iso) -> Regex("""\b$code\b""") to iso }

/**
 * Only unambiguous symbols. "$" is deliberately last and means USD merely as
 * a default — a receipt printing CAD or AUD says so in the code list above,
 * which is checked first.
 */
private val CURRENCY_SYMBOLS = listOf(
    "€" to "EUR", "£" to "GBP", "₪" to "ILS", "₹" to "INR",
    "₺" to "TRY", "₽" to "RUB", "₩" to "KRW", "฿" to "THB",
    "₱" to "PHP", "₫" to "VND", "¥" to "JPY", "zł" to "PLN",
    "Kč" to "CZK", "$" to "USD",
)

/** A currency guess, plus whether it was actually printed on the receipt. */
data class CurrencyGuess(val code: String, val explicit: Boolean)

/**
 * How near a currency code must sit to an amount before we believe it.
 *
 * Real receipts print the currency beside the money — "28.668 JD",
 * "TOTAL EUR 12,50". Three letters floating anywhere else on a noisy scan are
 * far more likely to be OCR inventing a word out of a smudge, and a single
 * hallucinated code otherwise relabels the entire receipt.
 */
private const val CODE_PROXIMITY = 12

fun detectCurrencyDetailed(text: String, fallback: String): CurrencyGuess {
    // A printed symbol is the strongest evidence available: unambiguous, and
    // OCR seldom invents one out of nothing.
    for (line in text.lineSequence()) {
        for ((symbol, iso) in CURRENCY_SYMBOLS) {
            if (line.contains(symbol, ignoreCase = true)) {
                return CurrencyGuess(iso, explicit = true)
            }
        }
    }

    // Next best: a code printed right beside an amount, as in "28.668 JD".
    for (line in text.lineSequence()) {
        val amounts = findMoney(line)
        if (amounts.isEmpty()) continue
        val upper = line.uppercase()
        for ((pattern, iso) in CURRENCY_CODES) {
            val match = pattern.find(upper) ?: continue
            val adjacent = amounts.any { money ->
                val gap = if (money.start >= match.range.last) {
                    money.start - match.range.last
                } else {
                    match.range.first - (money.start + money.raw.length)
                }
                gap in 0..CODE_PROXIMITY
            }
            if (adjacent) return CurrencyGuess(iso, explicit = true)
        }
    }

    // Weakest: a code standing alone on a short, tidy line — "JPY", or
    // "Paid in KD". Plenty of tills print it that way, so it has to count,
    // but the line must look deliberate. A code buried in a long run of
    // half-recognised characters is far more likely to be OCR noise, and a
    // single invented code silently relabels every amount on the receipt.
    for (line in text.lineSequence()) {
        val words = line.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty() || words.size > 3) continue
        val upper = line.uppercase()
        for ((pattern, iso) in CURRENCY_CODES) {
            if (pattern.containsMatchIn(upper)) return CurrencyGuess(iso, explicit = true)
        }
    }

    return CurrencyGuess(fallback, explicit = false)
}

/** Best-guess currency for a receipt, falling back to the device default. */
fun detectCurrency(text: String, fallback: String): String =
    detectCurrencyDetailed(text, fallback).code

/**
 * How many decimals a currency really uses — 3 for dinars, 2 for euros,
 * 0 for yen. Comes straight from the JDK's currency tables rather than a
 * hand-maintained list. Null for codes the JDK doesn't know.
 */
fun fractionDigitsFor(code: String): Int? =
    runCatching { java.util.Currency.getInstance(code).defaultFractionDigits }
        .getOrNull()
        ?.takeIf { it >= 0 }

/**
 * Maps Arabic-Indic and Persian digits onto ASCII, and their decimal marks
 * onto `,` and `.`, so the rest of the parser sees ordinary numbers.
 */
fun normalizeDigits(text: String): String {
    if (text.none { it in '٠'..'۹' }) return text
    return buildString(text.length) {
        for (ch in text) {
            append(
                when (ch) {
                    in '٠'..'٩' -> '0' + (ch - '٠')  // Arabic-Indic
                    in '۰'..'۹' -> '0' + (ch - '۰')  // Persian
                    '٫' -> ','                                  // Arabic decimal mark
                    '٬' -> '.'                                  // Arabic thousands mark
                    else -> ch
                }
            )
        }
    }
}
