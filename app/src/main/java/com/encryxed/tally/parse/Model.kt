package com.encryxed.tally.parse

import java.time.LocalDate

/**
 * One line of text the OCR engine found, plus where it sat on the page.
 *
 * Position matters as much as the text does: a shop name is big and near the
 * top, a total is near the bottom. The parser leans on both.
 */
data class OcrLine(
    val text: String,
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
) {
    val height: Int get() = (bottom - top).coerceAtLeast(1)
    val centerY: Int get() = (top + bottom) / 2
}

/** How much the parser trusts a single field it filled in. */
enum class Confidence {
    /** Matched a known shop, or an explicit "TOTAL" label. Safe to accept. */
    HIGH,

    /** Inferred from layout and shape. Usually right, worth a glance. */
    MEDIUM,

    /** A guess from a weak signal. Shown flagged for correction. */
    LOW,

    /** Nothing found. */
    NONE,
    ;

    /** Worth flagging in the UI so the user gives it a second look. */
    val isWeak: Boolean get() = this == LOW || this == NONE
}

enum class Category(val label: String, val emoji: String) {
    GROCERIES("Groceries", "🧺"),
    DINING("Food & drink", "🍔"),
    FUEL("Fuel", "⛽"),
    TRANSPORT("Transport", "🚆"),
    HEALTH("Health & care", "💊"),
    HOME("Home & DIY", "🔨"),
    ELECTRONICS("Electronics", "💻"),
    CLOTHING("Clothing", "👕"),
    ENTERTAINMENT("Entertainment", "🎬"),
    OTHER("Other", "🧾"),
}

/** Everything the parser managed to pull off one receipt photo. */
data class ParsedReceipt(
    val merchant: String?,
    val merchantConfidence: Confidence,
    val total: Double?,
    val totalConfidence: Confidence,
    val date: LocalDate?,
    val dateConfidence: Confidence,
    val currency: String,
    val category: Category,
    val rawText: String,
) {
    /** True when every field the user cares about came out filled. */
    val isComplete: Boolean
        get() = merchant != null && total != null && date != null

    /**
     * How well-formed this parse looks, used to choose between competing
     * readings of the same photo — most importantly the four rotations of it.
     *
     * Recognised text volume carries most of the weight because it is the
     * clearest orientation signal: OCR reads far more characters off a page
     * that is the right way up than off one lying on its side. The field
     * confidences then break ties between plausible orientations.
     */
    val qualityScore: Int
        get() {
            fun points(confidence: Confidence) = when (confidence) {
                Confidence.HIGH -> 6
                Confidence.MEDIUM -> 3
                Confidence.LOW -> 1
                Confidence.NONE -> 0
            }
            return rawText.count { it.isLetterOrDigit() } / 20 +
                points(merchantConfidence) +
                points(totalConfidence) +
                points(dateConfidence)
        }

    /** Fields worth asking the user to double-check. */
    val uncertainFields: List<String>
        get() = buildList {
            if (merchantConfidence.isWeak) add("store")
            if (totalConfidence.isWeak) add("total")
            if (dateConfidence.isWeak) add("date")
        }
}
