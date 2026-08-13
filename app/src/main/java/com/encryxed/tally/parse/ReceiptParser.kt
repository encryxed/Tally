package com.encryxed.tally.parse

import java.time.LocalDate

/**
 * Turns raw OCR output into a filled-in receipt.
 *
 * Pure Kotlin on purpose — no Android types anywhere in here — so the whole
 * detection engine can be exercised by fast JVM unit tests instead of only
 * ever being testable by pointing a phone at a shop receipt.
 */
object ReceiptParser {

    fun parse(
        lines: List<OcrLine>,
        preferDayFirst: Boolean = true,
        defaultCurrency: String = "EUR",
        today: LocalDate = LocalDate.now(),
    ): ParsedReceipt {
        val clean = lines
            .map { it.copy(text = normalizeDigits(it.text).trim().replace(Regex("\\s+"), " ")) }
            .filter { it.text.isNotEmpty() }

        val rawText = clean.joinToString("\n") { it.text }

        if (clean.isEmpty()) {
            return ParsedReceipt(
                merchant = null, merchantConfidence = Confidence.NONE,
                total = null, totalConfidence = Confidence.NONE,
                date = null, dateConfidence = Confidence.NONE,
                currency = defaultCurrency, category = Category.OTHER, rawText = "",
            )
        }

        // Decided once for the whole receipt: precision changes how every
        // amount on it parses. A currency printed on the receipt is
        // authoritative; otherwise fall back to reading the shape of the
        // numbers themselves.
        val currency = detectCurrencyDetailed(rawText, defaultCurrency)
        val decimals = when {
            currency.explicit -> fractionDigitsFor(currency.code) ?: detectDecimalDigits(rawText)
            fractionDigitsFor(currency.code) == 0 -> 0
            else -> detectDecimalDigits(rawText)
        }

        val (merchant, merchantConfidence, knownCategory) = findMerchant(clean, decimals)
        val (total, totalConfidence) = findTotal(clean, decimals)
        val (date, dateConfidence) = findDate(clean.map { it.text }, preferDayFirst, today)

        return ParsedReceipt(
            merchant = merchant,
            merchantConfidence = merchantConfidence,
            total = total,
            totalConfidence = totalConfidence,
            date = date,
            dateConfidence = dateConfidence,
            currency = currency.code,
            category = knownCategory ?: categoryFromKeywords(rawText),
            rawText = rawText,
        )
    }

    // ---------------------------------------------------------------- total

    /**
     * Words that mark the line carrying the amount actually paid.
     *
     * Matched on word boundaries, which matters more than it looks: "net" is
     * the total on many Middle-Eastern tills, while the Dutch "NETTO" is the
     * pre-VAT subtotal and must *not* match. \bnet\b separates them cleanly.
     */
    private val TOTAL_POSITIVE = Regex(
        """\b(?:grand total|total amount|amount due|balance due|total due""" +
            """|te betalen|totaal te voldoen|totaal|total|totale""" +
            """|gesamtbetrag|gesamt|summe|montant|importe|bedrag""" +
            """|to pay|sum|net)\b"""
    )

    private fun hasTotalKeyword(lower: String) = TOTAL_POSITIVE.containsMatchIn(lower)

    /** Lines that are never the total, whatever else they say. */
    private val TOTAL_VETO = listOf(
        "subtotal", "sub total", "subtotaal", "zwischensumme", "sous-total",
        "totaal btw", "total vat", "btw totaal", "vat total", "totaal korting",
        "korting", "discount", "savings", "besparing", "voordeel",
        "wisselgeld", "change", "terug", "cash", "contant", "kontant",
        "tip", "fooi", "aantal", "artikelen", "items", "qty", "stuks",
        "retour", "return", "spaarpunten", "points",
    )

    private fun findTotal(lines: List<OcrLine>, decimals: Int): Pair<Double?, Confidence> {
        data class Hit(val value: Double, val confidence: Confidence, val index: Int)

        val hits = mutableListOf<Hit>()

        lines.forEachIndexed { index, line ->
            val lower = line.text.lowercase()
            if (TOTAL_VETO.any { lower.contains(it) }) return@forEachIndexed

            // A tax-only line like "BTW 21%  5,94" never gets here: it carries
            // no total word. A line that says both ("TOTAAL INCL. BTW") does,
            // which is exactly what we want.
            if (!hasTotalKeyword(lower)) return@forEachIndexed

            // Prefer a decimal amount on the same line.
            val onLine = findMoney(line.text, decimals).filter { (it.hasDecimals || decimals == 0) && it.value > 0 }
            if (onLine.isNotEmpty()) {
                hits += Hit(onLine.maxOf { it.value }, Confidence.HIGH, index)
                return@forEachIndexed
            }

            // Big-font totals often sit on the line below their label.
            for (offset in 1..2) {
                val next = lines.getOrNull(index + offset) ?: break
                val nextLower = next.text.lowercase()
                if (TOTAL_VETO.any { nextLower.contains(it) }) continue
                val below = findMoney(next.text, decimals).filter { (it.hasDecimals || decimals == 0) && it.value > 0 }
                if (below.isNotEmpty()) {
                    hits += Hit(below.maxOf { it.value }, Confidence.MEDIUM, index)
                    break
                }
            }
        }

        if (hits.isNotEmpty()) {
            // Later lines win: the final total is printed after any running ones.
            val best = hits
                .sortedWith(compareBy({ it.confidence.ordinal }, { -it.index }))
                .first()
            return best.value to best.confidence
        }

        // Nothing labelled. The largest decimal amount in the lower part of the
        // receipt is the least-bad guess, but flag it as a guess.
        val topY = lines.minOf { it.top }
        val bottomY = lines.maxOf { it.bottom }
        val cutoff = topY + (bottomY - topY) * 0.45
        val fallback = lines
            .filter { it.centerY >= cutoff }
            .flatMap { findMoney(it.text, decimals) }
            .filter { (it.hasDecimals || decimals == 0) && it.value > 0 }
            .maxByOrNull { it.value }

        return if (fallback != null) fallback.value to Confidence.LOW
        else null to Confidence.NONE
    }

    // ------------------------------------------------------------- merchant

    /**
     * "Welcome to COZMO" is a greeting wrapped around a shop name, not noise
     * to discard. The leading W is optional because OCR routinely reads it as
     * N or V on thermal paper.
     */
    private val GREETING = Regex(
        """^[*\s]*(?:[wnv]elcome\s+to|welkom\s+(?:bij|in)|bienvenue\s+(?:chez|à|a)""" +
            """|bienvenido\s+a|willkommen\s+(?:bei|in)|benvenuto\s+(?:a|da))\s+""",
        RegexOption.IGNORE_CASE,
    )

    private fun stripGreeting(text: String): String = GREETING.replace(text, "").trim()

    /**
     * Puts back letters the OCR read as digits, but only mid-word between two
     * letters — so C0ZMO becomes COZMO while Q8 and 7-Eleven are left alone.
     */
    private fun repairOcrDigits(text: String): String {
        if (text.length < 3) return text
        val chars = text.toCharArray()
        for (i in 1 until chars.size - 1) {
            if (!chars[i - 1].isLetter() || !chars[i + 1].isLetter()) continue
            chars[i] = when (chars[i]) {
                '0' -> 'O'
                '1' -> 'I'
                '5' -> 'S'
                '8' -> 'B'
                else -> chars[i]
            }
        }
        return String(chars)
    }

    private fun findMerchant(lines: List<OcrLine>, decimals: Int): Triple<String?, Confidence, Category?> {
        val topY = lines.minOf { it.top }
        val bottomY = lines.maxOf { it.bottom }
        val pageHeight = (bottomY - topY).coerceAtLeast(1)

        // Shop names live at the top. Search generously for known chains,
        // narrowly for the layout-based guess.
        val knownZone = lines.filter { (it.centerY - topY) <= pageHeight * 0.60 }
        val headerZone = lines.filter { (it.centerY - topY) <= pageHeight * 0.35 }

        for (line in knownZone) {
            val lower = line.text.lowercase()
            // Skip the totals block: a "NET 12,50" line must not become a shop.
            if (hasTotalKeyword(lower)) continue
            if (findMoney(line.text, decimals).any { it.hasDecimals }) continue

            val norm = normalizeForMatch(repairOcrDigits(stripGreeting(line.text)))
            if (norm.isEmpty()) continue

            SHORT_MERCHANTS[norm]?.let {
                return Triple(it.display, Confidence.HIGH, it.category)
            }
            // Longest key first so "ALBERTHEIJN" beats a shorter accidental hit.
            val match = KNOWN_MERCHANTS.entries
                .filter { norm.contains(it.key) }
                .maxByOrNull { it.key.length }
            if (match != null) {
                return Triple(match.value.display, Confidence.HIGH, match.value.category)
            }
        }

        val medianHeight = lines.map { it.height }.sorted()[lines.size / 2].coerceAtLeast(1)

        // Try the tight header band first. If a logo, a slogan or a run of
        // boilerplate pushed the real name further down, widen the search
        // before giving up entirely.
        val best = bestHeaderCandidate(headerZone, medianHeight, decimals)
            ?: bestHeaderCandidate(knownZone, medianHeight, decimals)
            ?: return Triple(null, Confidence.NONE, null)

        return Triple(cleanMerchantName(best.text), Confidence.MEDIUM, null)
    }

    private fun bestHeaderCandidate(
        zone: List<OcrLine>,
        medianHeight: Int,
        decimals: Int,
    ): OcrLine? =
        zone.mapIndexed { index, line -> line to scoreAsMerchant(line, index, medianHeight, decimals) }
            .filter { it.second > 1.0 }
            .maxByOrNull { it.second }
            ?.first

    private fun scoreAsMerchant(
        line: OcrLine,
        index: Int,
        medianHeight: Int,
        decimals: Int,
    ): Double {
        // Score the name inside the greeting, not the greeting itself.
        val text = stripGreeting(line.text)
        if (text.length < 3) return -10.0

        val letters = text.count { it.isLetter() }
        val digits = text.count { it.isDigit() }
        // A line with barely any letters is a separator or a barcode, not a name.
        if (letters < 3) return -10.0

        val lower = text.lowercase()
        var score = 0.0

        // Header text is printed larger than the body.
        score += (line.height.toDouble() / medianHeight) * 2.0
        score += (letters.toDouble() / text.length) * 2.0
        if (text == text.uppercase() && letters >= 3) score += 0.8
        if (digits > letters) score -= 3.0
        if (text.length > 40) score -= 2.0
        if (looksLikeContactInfo(text)) score -= 4.0
        if (findMoney(text, decimals).any { it.hasDecimals }) score -= 3.0

        // "KASSABON" and friends sit exactly where the shop name should be.
        if (HEADER_NOISE.any { lower.contains(it) }) score -= 5.0

        // A line that is only a date or a time is never the name.
        if (TIME_ONLY.matches(text.trim())) score -= 5.0

        // Slight preference for the very first lines.
        score -= index * 0.15
        return score
    }

    private val TIME_ONLY = Regex("""[\d\s:./\-]+""")

    private val POSTCODE = Regex("""\b\d{4}\s?[A-Z]{2}\b""")
    private val LONG_DIGIT_RUN = Regex("""\d{6,}""")

    private val CONTACT_HINTS = listOf(
        "tel", "www.", "http", "@", ".nl", ".com", ".de", ".be", ".co.uk",
        "btw", "kvk", "vat no", "iban", "straat", "laan", "plein", "postbus",
        "street", "road", "avenue", "filiaal", "winkel nr", "store #",
    )

    private fun looksLikeContactInfo(text: String): Boolean {
        val lower = text.lowercase()
        if (CONTACT_HINTS.any { lower.contains(it) }) return true
        if (POSTCODE.containsMatchIn(text)) return true
        if (LONG_DIGIT_RUN.containsMatchIn(text)) return true
        return false
    }

    private val CORPORATE_SUFFIX = Regex(
        """[\s,]+(b\.?v\.?|n\.?v\.?|ltd\.?|gmbh|inc\.?|llc|s\.?a\.?|plc)\.?$""",
        RegexOption.IGNORE_CASE,
    )
    private val TRAILING_STORE_NUMBER = Regex("""\s+#?\d{2,6}$""")

    private fun cleanMerchantName(raw: String): String {
        var name = repairOcrDigits(stripGreeting(raw))
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', '*', ':', ',', '.')

        name = CORPORATE_SUFFIX.replace(name, "")
        name = TRAILING_STORE_NUMBER.replace(name, "")
        name = name.trim()

        // SHOUTED receipt headers read better title-cased. Short all-consonant
        // words (KFC, BBQ, NS) are acronyms and stay shouting.
        if (name == name.uppercase() && name.any { it.isLetter() }) {
            name = name.split(" ").joinToString(" ") { word ->
                val isAcronym = word.length <= 4 && word.none { it.uppercaseChar() in "AEIOUY" }
                if (isAcronym) word
                else word.lowercase().replaceFirstChar { it.uppercase() }
            }
        }
        return name.ifEmpty { raw.trim() }
    }
}
