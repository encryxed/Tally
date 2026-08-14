package com.encryxed.tally.parse

/**
 * The vocabulary Tally needs to read a receipt in a given language: the words
 * that mark a total, the words that rule a line out, and the month names.
 *
 * This is separate from the language the *app* is shown in. Someone can run
 * Tally in English and still shop where the tills print Polish.
 *
 * Only Latin-script languages appear here, because the bundled OCR model reads
 * Latin script only — there is no point knowing the Greek for "total" if the
 * recogniser cannot see the letters.
 */
enum class ReceiptLanguage(
    val tag: String,
    val englishName: String,
    val nativeName: String,
    /** What an ambiguous date like 03/04 means where this language is spoken. */
    val dayFirst: Boolean,
    val totalWords: List<String>,
    val vetoWords: List<String>,
    /** Lower-cased three-letter month prefixes, as printed on receipts. */
    val monthPrefixes: Map<String, Int>,
) {
    ENGLISH(
        "en", "English", "English", dayFirst = false,
        totalWords = listOf(
            "grand total", "total amount", "amount due", "balance due",
            "total due", "total", "to pay", "sum", "net",
        ),
        vetoWords = listOf(
            "subtotal", "sub total", "total vat", "vat total", "tax", "vat",
            "discount", "savings", "change", "cash", "tip", "items", "qty",
            "quantity", "return", "refund", "points", "balance forward",
        ),
        monthPrefixes = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
            "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
        ),
    ),
    DUTCH(
        "nl", "Dutch", "Nederlands", dayFirst = true,
        totalWords = listOf(
            "totaal te voldoen", "te betalen", "totaalbedrag", "totaal", "bedrag",
        ),
        vetoWords = listOf(
            "subtotaal", "totaal btw", "btw totaal", "btw", "totaal korting",
            "korting", "besparing", "voordeel", "wisselgeld", "terug", "contant",
            "kontant", "fooi", "aantal", "artikelen", "stuks", "retour",
            "spaarpunten", "netto",
        ),
        monthPrefixes = mapOf(
            "jan" to 1, "feb" to 2, "mrt" to 3, "maa" to 3, "apr" to 4, "mei" to 5,
            "jun" to 6, "jul" to 7, "aug" to 8, "sep" to 9, "okt" to 10,
            "nov" to 11, "dec" to 12,
        ),
    ),
    GERMAN(
        "de", "German", "Deutsch", dayFirst = true,
        totalWords = listOf(
            "gesamtbetrag", "gesamtsumme", "endbetrag", "zu zahlen", "gesamt",
            "summe", "betrag",
        ),
        vetoWords = listOf(
            "zwischensumme", "mwst", "ust", "rabatt", "ersparnis", "rückgeld",
            "trinkgeld", "anzahl", "artikel", "retoure", "bar",
        ),
        monthPrefixes = mapOf(
            "jan" to 1, "feb" to 2, "mär" to 3, "mrz" to 3, "apr" to 4, "mai" to 5,
            "jun" to 6, "jul" to 7, "aug" to 8, "sep" to 9, "okt" to 10,
            "nov" to 11, "dez" to 12,
        ),
    ),
    FRENCH(
        "fr", "French", "Français", dayFirst = true,
        totalWords = listOf(
            "montant total", "net à payer", "à payer", "montant dû", "total ttc",
            "total", "montant",
        ),
        vetoWords = listOf(
            "sous-total", "sous total", "tva", "remise", "économie", "monnaie",
            "espèces", "pourboire", "quantité", "articles", "retour", "rendu",
        ),
        monthPrefixes = mapOf(
            "jan" to 1, "fév" to 2, "fev" to 2, "mar" to 3, "avr" to 4, "mai" to 5,
            "jui" to 6, "jul" to 7, "aoû" to 8, "aou" to 8, "sep" to 9,
            "oct" to 10, "nov" to 11, "déc" to 12, "dec" to 12,
        ),
    ),
    SPANISH(
        "es", "Spanish", "Español", dayFirst = true,
        totalWords = listOf(
            "importe total", "total a pagar", "a pagar", "total", "importe",
        ),
        vetoWords = listOf(
            "subtotal", "iva", "descuento", "ahorro", "cambio", "efectivo",
            "propina", "cantidad", "artículos", "devolución",
        ),
        monthPrefixes = mapOf(
            "ene" to 1, "feb" to 2, "mar" to 3, "abr" to 4, "may" to 5, "jun" to 6,
            "jul" to 7, "ago" to 8, "sep" to 9, "set" to 9, "oct" to 10,
            "nov" to 11, "dic" to 12,
        ),
    ),
    ITALIAN(
        "it", "Italian", "Italiano", dayFirst = true,
        totalWords = listOf(
            "totale complessivo", "importo totale", "da pagare", "totale", "importo",
        ),
        vetoWords = listOf(
            "subtotale", "iva", "sconto", "risparmio", "resto", "contanti",
            "mancia", "quantità", "articoli", "reso",
        ),
        monthPrefixes = mapOf(
            "gen" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "mag" to 5, "giu" to 6,
            "lug" to 7, "ago" to 8, "set" to 9, "ott" to 10, "nov" to 11, "dic" to 12,
        ),
    ),
    PORTUGUESE(
        "pt", "Portuguese", "Português", dayFirst = true,
        totalWords = listOf(
            "valor total", "total a pagar", "a pagar", "total", "valor",
        ),
        vetoWords = listOf(
            "subtotal", "iva", "desconto", "poupança", "troco", "dinheiro",
            "gorjeta", "quantidade", "artigos", "devolução",
        ),
        monthPrefixes = mapOf(
            "jan" to 1, "fev" to 2, "mar" to 3, "abr" to 4, "mai" to 5, "jun" to 6,
            "jul" to 7, "ago" to 8, "set" to 9, "out" to 10, "nov" to 11, "dez" to 12,
        ),
    ),
    POLISH(
        "pl", "Polish", "Polski", dayFirst = true,
        totalWords = listOf("do zapłaty", "razem", "łącznie", "suma"),
        vetoWords = listOf(
            "podsuma", "vat", "ptu", "rabat", "oszczędność", "reszta", "gotówka",
            "napiwek", "ilość", "zwrot",
        ),
        monthPrefixes = mapOf(
            "sty" to 1, "lut" to 2, "mar" to 3, "kwi" to 4, "maj" to 5, "cze" to 6,
            "lip" to 7, "sie" to 8, "wrz" to 9, "paź" to 10, "paz" to 10,
            "lis" to 11, "gru" to 12,
        ),
    ),
    TURKISH(
        "tr", "Turkish", "Türkçe", dayFirst = true,
        totalWords = listOf("genel toplam", "ödenecek", "toplam", "tutar"),
        vetoWords = listOf(
            "ara toplam", "kdv", "indirim", "para üstü", "nakit", "bahşiş",
            "adet", "iade",
        ),
        monthPrefixes = mapOf(
            "oca" to 1, "şub" to 2, "sub" to 2, "mar" to 3, "nis" to 4, "may" to 5,
            "haz" to 6, "tem" to 7, "ağu" to 8, "agu" to 8, "eyl" to 9,
            "eki" to 10, "kas" to 11, "ara" to 12,
        ),
    ),
    SWEDISH(
        "sv", "Swedish", "Svenska", dayFirst = true,
        totalWords = listOf("att betala", "totalt", "total", "summa"),
        vetoWords = listOf(
            "delsumma", "moms", "rabatt", "växel", "kontant", "dricks",
            "antal", "retur",
        ),
        monthPrefixes = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "maj" to 5, "jun" to 6,
            "jul" to 7, "aug" to 8, "sep" to 9, "okt" to 10, "nov" to 11, "dec" to 12,
        ),
    ),
    DANISH(
        "da", "Danish", "Dansk", dayFirst = true,
        totalWords = listOf("at betale", "i alt", "total", "beløb"),
        vetoWords = listOf(
            "subtotal", "moms", "rabat", "byttepenge", "kontant", "drikkepenge",
            "antal", "retur",
        ),
        monthPrefixes = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "maj" to 5, "jun" to 6,
            "jul" to 7, "aug" to 8, "sep" to 9, "okt" to 10, "nov" to 11, "dec" to 12,
        ),
    ),
    NORWEGIAN(
        "nb", "Norwegian", "Norsk", dayFirst = true,
        totalWords = listOf("å betale", "totalt", "total", "sum", "beløp"),
        vetoWords = listOf(
            "delsum", "mva", "rabatt", "vekslepenger", "kontant", "driks",
            "antall", "retur",
        ),
        monthPrefixes = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "mai" to 5, "jun" to 6,
            "jul" to 7, "aug" to 8, "sep" to 9, "okt" to 10, "nov" to 11, "des" to 12,
        ),
    ),
    FINNISH(
        "fi", "Finnish", "Suomi", dayFirst = true,
        totalWords = listOf("loppusumma", "maksettava", "yhteensä", "summa"),
        vetoWords = listOf(
            "välisumma", "alv", "alennus", "vaihtoraha", "käteinen", "juomaraha",
            "määrä", "palautus",
        ),
        monthPrefixes = mapOf(
            "tam" to 1, "hel" to 2, "maa" to 3, "huh" to 4, "tou" to 5, "kes" to 6,
            "hei" to 7, "elo" to 8, "syy" to 9, "lok" to 10, "mar" to 11, "jou" to 12,
        ),
    ),
    CZECH(
        "cs", "Czech", "Čeština", dayFirst = true,
        totalWords = listOf("k úhradě", "celková částka", "celkem", "částka"),
        vetoWords = listOf(
            "mezisoučet", "dph", "sleva", "vráceno", "hotovost", "spropitné",
            "počet", "vratka",
        ),
        monthPrefixes = mapOf(
            "led" to 1, "úno" to 2, "uno" to 2, "bře" to 3, "bre" to 3, "dub" to 4,
            "kvě" to 5, "kve" to 5, "čer" to 6, "cer" to 6, "srp" to 8,
            "zář" to 9, "zar" to 9, "říj" to 10, "rij" to 10, "lis" to 11, "pro" to 12,
        ),
    ),
    SLOVAK(
        "sk", "Slovak", "Slovenčina", dayFirst = true,
        totalWords = listOf("k úhrade", "celkom", "spolu", "suma"),
        vetoWords = listOf(
            "medzisúčet", "dph", "zľava", "výdavok", "hotovosť", "prepitné",
            "počet", "vrátenie",
        ),
        monthPrefixes = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "máj" to 5, "maj" to 5,
            "jún" to 6, "jun" to 6, "júl" to 7, "jul" to 7, "aug" to 8, "sep" to 9,
            "okt" to 10, "nov" to 11, "dec" to 12,
        ),
    ),
    ROMANIAN(
        "ro", "Romanian", "Română", dayFirst = true,
        totalWords = listOf("total de plată", "de plată", "total", "suma"),
        vetoWords = listOf(
            "subtotal", "tva", "reducere", "rest", "numerar", "bacșiș",
            "cantitate", "retur",
        ),
        monthPrefixes = mapOf(
            "ian" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "mai" to 5, "iun" to 6,
            "iul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "noi" to 11, "dec" to 12,
        ),
    ),
    HUNGARIAN(
        "hu", "Hungarian", "Magyar", dayFirst = true,
        totalWords = listOf("fizetendő", "végösszeg", "összesen", "összeg"),
        vetoWords = listOf(
            "részösszeg", "áfa", "kedvezmény", "visszajáró", "készpénz",
            "borravaló", "mennyiség", "visszáru",
        ),
        monthPrefixes = mapOf(
            "jan" to 1, "feb" to 2, "már" to 3, "mar" to 3, "ápr" to 4, "apr" to 4,
            "máj" to 5, "maj" to 5, "jún" to 6, "jun" to 6, "júl" to 7, "jul" to 7,
            "aug" to 8, "sze" to 9, "okt" to 10, "nov" to 11, "dec" to 12,
        ),
    ),
    INDONESIAN(
        "id", "Indonesian", "Bahasa Indonesia", dayFirst = true,
        totalWords = listOf("total bayar", "harus dibayar", "total", "jumlah"),
        vetoWords = listOf(
            "subtotal", "ppn", "diskon", "kembali", "tunai", "kembalian", "retur",
        ),
        monthPrefixes = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "mei" to 5, "jun" to 6,
            "jul" to 7, "agu" to 8, "sep" to 9, "okt" to 10, "nov" to 11, "des" to 12,
        ),
    ),
    VIETNAMESE(
        "vi", "Vietnamese", "Tiếng Việt", dayFirst = true,
        totalWords = listOf("tổng cộng", "tổng tiền", "thành tiền", "tổng"),
        vetoWords = listOf(
            "tạm tính", "thuế", "giảm giá", "tiền thừa", "tiền mặt",
            "số lượng", "trả lại",
        ),
        // Vietnamese receipts print months numerically, so there is nothing
        // useful to match here.
        monthPrefixes = emptyMap(),
    ),
    CATALAN(
        "ca", "Catalan", "Català", dayFirst = true,
        totalWords = listOf("import total", "a pagar", "total", "import"),
        vetoWords = listOf(
            "subtotal", "iva", "descompte", "canvi", "efectiu", "propina",
            "quantitat", "devolució",
        ),
        monthPrefixes = mapOf(
            "gen" to 1, "feb" to 2, "mar" to 3, "abr" to 4, "mai" to 5, "jun" to 6,
            "jul" to 7, "ago" to 8, "set" to 9, "oct" to 10, "nov" to 11, "des" to 12,
        ),
    ),
    ;

    companion object {
        fun fromTag(tag: String): ReceiptLanguage? = entries.firstOrNull { it.tag == tag }
    }
}

/**
 * The merged vocabulary actually used for one scan, built from whichever
 * languages the user has switched on.
 */
class LanguagePack(
    val totalPattern: Regex,
    val vetoWords: List<String>,
    val monthPrefixes: Map<String, Int>,
    val dayFirst: Boolean,
) {
    fun hasTotalKeyword(lowerLine: String) = totalPattern.containsMatchIn(lowerLine)

    fun isVetoed(lowerLine: String) = vetoWords.any { lowerLine.contains(it) }

    companion object {
        /**
         * Word boundaries matter more than they look. "net" is the total on
         * many tills, while the Dutch "netto" is the pre-VAT subtotal and must
         * not match — a boundary separates them.
         *
         * They are spelled out as Unicode lookarounds rather than `\b` because
         * `\b` is ASCII-only, so "yhteensä" would fail to match at its final
         * letter. The JDK offers a `(?U)` flag for exactly this, but **Android
         * uses ICU for regex and rejects that flag outright**, throwing at
         * pattern-compile time. Unit tests run on the JVM and so never see it:
         * this shipped as a crash on every device with a fully green suite.
         * These lookarounds behave identically on both engines.
         */
        private const val BEFORE = "(?<![\\p{L}\\p{N}])"
        private const val AFTER = "(?![\\p{L}\\p{N}])"

        fun of(languages: Collection<ReceiptLanguage>): LanguagePack {
            val languageSet = languages.ifEmpty { listOf(ReceiptLanguage.ENGLISH) }

            // Longest first, so "grand total" is tried before "total".
            val totals = languageSet
                .flatMap { it.totalWords }
                .distinct()
                .sortedByDescending { it.length }
                .joinToString("|") { Regex.escape(it) }

            return LanguagePack(
                totalPattern = Regex("$BEFORE(?:$totals)$AFTER"),
                vetoWords = languageSet.flatMap { it.vetoWords }.distinct(),
                // Earlier languages win a prefix clash, so the order the user
                // enabled them in is respected.
                monthPrefixes = buildMap {
                    languageSet.forEach { language ->
                        language.monthPrefixes.forEach { (prefix, month) ->
                            putIfAbsent(prefix, month)
                        }
                    }
                },
                dayFirst = languageSet.first().dayFirst,
            )
        }

        val ALL: LanguagePack by lazy { of(ReceiptLanguage.entries) }
    }
}
