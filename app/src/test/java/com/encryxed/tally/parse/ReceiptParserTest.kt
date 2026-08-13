package com.encryxed.tally.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The detection engine is the whole point of the app, so it gets tested
 * against realistic receipt layouts rather than only happy-path snippets.
 */
class ReceiptParserTest {

    private val today = LocalDate.of(2026, 8, 20)

    /** Stacks plain text into synthetic OCR geometry: 30px lines, 5px gaps. */
    private fun ocr(text: String): List<OcrLine> =
        text.trimIndent().lines()
            .filter { it.isNotBlank() }
            .mapIndexed { i, line ->
                OcrLine(text = line, left = 0, top = i * 35, right = 400, bottom = i * 35 + 30)
            }

    // ------------------------------------------------------------ full receipts

    private val albertHeijn = """
        ALBERT HEIJN 1043
        Nieuwezijds Voorburgwal 226
        1012 RR AMSTERDAM
        Tel: 020-4210111
        VOLLE MELK 1L          1,29
        BROOD HEEL             2,49
        KIPFILET               5,99
        KOFFIE BONEN           7,49
        STATIEGELD             0,25
        SUBTOTAAL             17,51
        KORTING BONUS         -1,20
        TOTAAL                16,31
        BTW 9%                 1,35
        PINNEN                16,31
        13-08-2026 14:32
    """

    @Test
    fun `reads a dutch supermarket receipt end to end`() {
        val r = ReceiptParser.parse(ocr(albertHeijn), preferDayFirst = true, today = today)

        assertEquals("Albert Heijn", r.merchant)
        assertEquals(Confidence.HIGH, r.merchantConfidence)
        assertEquals(16.31, r.total!!, 0.001)
        assertEquals(Confidence.HIGH, r.totalConfidence)
        assertEquals(LocalDate.of(2026, 8, 13), r.date)
        assertEquals(Category.GROCERIES, r.category)
        assertTrue(r.isComplete)
    }

    @Test
    fun `prefers TOTAAL over SUBTOTAAL and the VAT line`() {
        val r = ReceiptParser.parse(ocr(albertHeijn), preferDayFirst = true, today = today)
        // 17,51 is the subtotal and 1,35 is VAT — neither may win.
        assertEquals(16.31, r.total!!, 0.001)
    }

    private val walmart = """
        Walmart
        Save money. Live better.
        1234 Main St
        Bentonville AR 72716
        GV MILK 1GAL           3.48
        BREAD WHEAT            1.98
        EGGS LARGE             2.97
        SUBTOTAL               8.43
        TAX 1                  0.69
        TOTAL                 ${'$'}9.12
        DEBIT TEND            10.00
        CHANGE DUE             0.88
        08/09/2026 19:45
    """

    @Test
    fun `reads a US receipt with month-first dates and dollars`() {
        val r = ReceiptParser.parse(ocr(walmart), preferDayFirst = false, today = today)

        assertEquals("Walmart", r.merchant)
        assertEquals(9.12, r.total!!, 0.001)
        assertEquals("USD", r.currency)
        assertEquals(LocalDate.of(2026, 8, 9), r.date)
        assertEquals(Category.GROCERIES, r.category)
    }

    @Test
    fun `ignores tax, tendered cash and change lines`() {
        val r = ReceiptParser.parse(ocr(walmart), preferDayFirst = false, today = today)
        assertTrue("10.00 tendered must not be read as the total", r.total != 10.00)
        assertTrue("0.88 change must not be read as the total", r.total != 0.88)
    }

    @Test
    fun `falls back to layout for an unknown shop`() {
        val r = ReceiptParser.parse(
            ocr(
                """
                DE KLEINE BAKKERIJ
                Dorpsstraat 12
                Koffie                 2,80
                Appeltaart             3,50
                TOTAAL                 6,30
                02-03-2026
                """
            ),
            preferDayFirst = true,
            today = today,
        )

        assertEquals("De Kleine Bakkerij", r.merchant)
        assertEquals(Confidence.MEDIUM, r.merchantConfidence)
        assertEquals(6.30, r.total!!, 0.001)
        assertEquals(LocalDate.of(2026, 3, 2), r.date)
    }

    @Test
    fun `handles a total printed on the line below its label`() {
        val r = ReceiptParser.parse(
            ocr(
                """
                SHELL STATION
                Euro 95        42,10 L
                TOTAAL
                87,45
                11-08-2026 08:15
                """
            ),
            preferDayFirst = true,
            today = today,
        )

        assertEquals("Shell", r.merchant)
        assertEquals(87.45, r.total!!, 0.001)
        assertEquals(Category.FUEL, r.category)
    }

    @Test
    fun `flags a guess when no total label exists`() {
        val r = ReceiptParser.parse(
            ocr(
                """
                CORNER SHOP
                Water                  1,10
                Snack                  2,40
                3,50
                """
            ),
            preferDayFirst = true,
            today = today,
        )
        assertEquals(3.50, r.total!!, 0.001)
        assertEquals(Confidence.LOW, r.totalConfidence)
        assertTrue(r.uncertainFields.contains("total"))
    }

    @Test
    fun `does not mistake a NETTO line for the shop name`() {
        val r = ReceiptParser.parse(
            ocr(
                """
                BISTRO CENTRAAL
                Lunch                 12,00
                NETTO                 11,01
                BTW 9%                 0,99
                TOTAAL                12,00
                05-08-2026
                """
            ),
            preferDayFirst = true,
            today = today,
        )
        assertEquals("Bistro Centraal", r.merchant)
        assertEquals(12.00, r.total!!, 0.001)
    }

    @Test
    fun `returns empty result for a blank scan rather than crashing`() {
        val r = ReceiptParser.parse(emptyList())
        assertEquals(null, r.merchant)
        assertEquals(null, r.total)
        assertEquals(null, r.date)
        assertTrue(!r.isComplete)
    }

    @Test
    fun `does not read receipt boilerplate as the shop name`() {
        val r = ReceiptParser.parse(
            ocr(
                """
                KASSABON
                ==================
                CAFE DE ZWAAN
                Koffie                 2,80
                TOTAAL                 2,80
                04-08-2026
                """
            ),
            preferDayFirst = true,
            today = today,
        )
        assertEquals("Cafe De Zwaan", r.merchant)
    }

    @Test
    fun `looks below the header band when the top is all noise`() {
        val r = ReceiptParser.parse(
            ocr(
                """
                *** KLANTTICKET ***
                Transactie 88213
                Terminal 4471
                SUPERMARKT DE HOEK
                Brood                  2,10
                TOTAAL                 2,10
                04-08-2026
                """
            ),
            preferDayFirst = true,
            today = today,
        )
        assertEquals("Supermarkt De Hoek", r.merchant)
    }

    @Test
    fun `keeps a corrected shop reachable by a stable signature`() {
        val first = ReceiptParser.parse(ocr(albertHeijn), preferDayFirst = true, today = today)
        val second = ReceiptParser.parse(ocr(albertHeijn), preferDayFirst = true, today = today)

        val a = receiptSignature(first.merchant, first.rawText)
        val b = receiptSignature(second.merchant, second.rawText)
        assertTrue("signature must not be blank", a.isNotEmpty())
        assertEquals("same till must produce the same signature", a, b)
    }

    @Test
    fun `builds a signature even when no shop was detected`() {
        val signature = receiptSignature(null, "SOME UNKNOWN SHOP\nItem 1,00")
        assertTrue(signature.isNotEmpty())
    }

    // ----------------------------------------- three-decimal currencies

    /**
     * A real COZMO receipt from Jordan. Dinars are quoted to three decimals,
     * the total is labelled "Net:", and OCR turned "Welcome" into "Nelcome"
     * and the O in COZMO into a zero.
     */
    private val cozmo = """
        Nelcome To C0ZMO
        Shift # : 10387        POS # : 31
        Cashier : 340          Date : 13/08/2026
        Rcpt No.: 13347        Time : 16:36
        1. TOMATO KG (21)      0.725      0.428
        2. CUCUMBER (22)       0.459      0.271
        3. LURPAK BUTTER UNSA  3.000      3.600
        Special Offer                    -0.630
        4. ALRAEDA MUSHROOMS   1.000      1.750
        7. ALMARAI FRESH MILK  1.000      1.500
        16. BLUE MILL CUMIN GR 1.000      1.000
        Net:                            28.668
        Cash Paid:                      28.668
        Return:                          0.000
        As a THE Card Holder you just saved
        0.630 JD on this purchase.
    """

    @Test
    fun `reads a three-decimal dinar receipt`() {
        val r = ReceiptParser.parse(ocr(cozmo), preferDayFirst = true, today = today)

        assertEquals(28.668, r.total!!, 0.0001)
        assertEquals(Confidence.HIGH, r.totalConfidence)
        assertEquals("JOD", r.currency)
        assertEquals(LocalDate.of(2026, 8, 13), r.date)
    }

    @Test
    fun `unwraps a greeting and repairs OCR digits in the shop name`() {
        val r = ReceiptParser.parse(ocr(cozmo), preferDayFirst = true, today = today)
        // "Nelcome To C0ZMO" -> greeting stripped -> zero repaired to O ->
        // title-cased, same as any other shouted receipt header.
        assertEquals("Cozmo", r.merchant)
    }

    @Test
    fun `does not treat cash paid or return as the total`() {
        val r = ReceiptParser.parse(ocr(cozmo), preferDayFirst = true, today = today)
        assertTrue(r.total != 0.0)
    }

    @Test
    fun `detects how many decimals a receipt uses`() {
        assertEquals(3, detectDecimalDigits(cozmo))
        assertEquals(2, detectDecimalDigits(albertHeijn))
    }

    @Test
    fun `three-decimal amounts survive tokenising`() {
        assertEquals(28.668, parseMoneyToken("28.668", 3)!!, 0.0001)
        assertEquals(0.428, parseMoneyToken("0.428", 3)!!, 0.0001)
        // The same token on a two-decimal receipt is thousands grouping.
        assertEquals(28668.0, parseMoneyToken("28.668", 2)!!, 0.0001)
    }

    @Test
    fun `still reads euro receipts correctly after the dinar fix`() {
        val r = ReceiptParser.parse(ocr(albertHeijn), preferDayFirst = true, today = today)
        assertEquals(16.31, r.total!!, 0.001)
        assertEquals("Albert Heijn", r.merchant)
    }

    @Test
    fun `net does not match the dutch NETTO subtotal`() {
        val r = ReceiptParser.parse(
            ocr(
                """
                BISTRO CENTRAAL
                Lunch                 12,00
                NETTO                 11,01
                BTW 9%                 0,99
                TOTAAL                12,00
                05-08-2026
                """
            ),
            preferDayFirst = true,
            today = today,
        )
        assertEquals(12.00, r.total!!, 0.001)
    }

    // -------------------------------------------------- currency handling

    @Test
    fun `knows how many decimals each currency really uses`() {
        assertEquals(3, fractionDigitsFor("JOD"))
        assertEquals(3, fractionDigitsFor("KWD"))
        assertEquals(2, fractionDigitsFor("EUR"))
        assertEquals(2, fractionDigitsFor("USD"))
        assertEquals(0, fractionDigitsFor("JPY"))
        assertEquals(0, fractionDigitsFor("KRW"))
    }

    @Test
    fun `a printed currency code beats guessing from number shapes`() {
        // Only two 3-decimal amounts, so the statistical guess would say 2 —
        // but the receipt says KD, and Kuwaiti dinars use three.
        val r = ReceiptParser.parse(
            ocr(
                """
                CITY CENTRE
                Item one              1.500
                TOTAL                 4.250
                Paid in KD
                07-08-2026
                """
            ),
            preferDayFirst = true,
            today = today,
        )
        assertEquals("KWD", r.currency)
        assertEquals(4.250, r.total!!, 0.0001)
    }

    @Test
    fun `reads a zero-decimal yen receipt where totals are whole numbers`() {
        val r = ReceiptParser.parse(
            ocr(
                """
                FAMILYMART
                Onigiri                 150
                Coffee                  180
                合計 TOTAL               330
                JPY
                09-08-2026
                """
            ),
            preferDayFirst = true,
            today = today,
        )
        assertEquals("JPY", r.currency)
        assertEquals(330.0, r.total!!, 0.001)
    }

    @Test
    fun `recognises currency symbols as well as codes`() {
        assertEquals("EUR", detectCurrency("TOTAAL € 12,50", "USD"))
        assertEquals("GBP", detectCurrency("TOTAL £4.20", "USD"))
        assertEquals("ILS", detectCurrency("SUM ₪ 30.00", "USD"))
        assertEquals("JOD", detectCurrency("you saved 0.630 JD today", "EUR"))
    }

    @Test
    fun `does not mistake letters inside words for a currency code`() {
        // "JD" only counts as a standalone word, not inside "JDSPORTS".
        assertEquals("GBP", detectCurrency("JDSPORTS LONDON TOTAL £25.00", "GBP"))
    }

    @Test
    fun `converts arabic-indic digits to something parseable`() {
        assertEquals("28.668", normalizeDigits("٢٨.٦٦٨"))
        assertEquals("12,50", normalizeDigits("١٢٫٥٠"))
        assertEquals("plain 123", normalizeDigits("plain 123"))
    }

    // -------------------------------------------------------------- money

    @Test
    fun `parses european and american number formats`() {
        assertEquals(1234.56, parseMoneyToken("1.234,56")!!, 0.001)
        assertEquals(1234.56, parseMoneyToken("1,234.56")!!, 0.001)
        assertEquals(1234.56, parseMoneyToken("1 234,56")!!, 0.001)
        assertEquals(12.50, parseMoneyToken("12,50")!!, 0.001)
        assertEquals(12.50, parseMoneyToken("12.50")!!, 0.001)
        assertEquals(12.0, parseMoneyToken("12")!!, 0.001)
        assertEquals(-5.0, parseMoneyToken("-5,00")!!, 0.001)
    }

    @Test
    fun `treats a lone three-digit group as thousands`() {
        assertEquals(1234.0, parseMoneyToken("1.234")!!, 0.001)
        assertEquals(1234.0, parseMoneyToken("1,234")!!, 0.001)
    }

    @Test
    fun `finds money inside a noisy line`() {
        val found = findMoney("TOTAAL INCL. BTW      16,31")
        assertEquals(16.31, found.last().value, 0.001)
    }

    // --------------------------------------------------------------- dates

    @Test
    fun `resolves day-month order when one number settles it`() {
        val (date, confidence) = findDate(listOf("25/12/2025"), preferDayFirst = false, today = today)
        // 25 cannot be a month, so day-first wins despite the US preference.
        assertEquals(LocalDate.of(2025, 12, 25), date)
        assertEquals(Confidence.HIGH, confidence)
    }

    @Test
    fun `parses iso and written-out dates`() {
        assertEquals(
            LocalDate.of(2026, 7, 4),
            findDate(listOf("2026-07-04"), preferDayFirst = true, today = today).first,
        )
        assertEquals(
            LocalDate.of(2026, 8, 12),
            findDate(listOf("12 aug 2026"), preferDayFirst = true, today = today).first,
        )
        assertEquals(
            LocalDate.of(2026, 8, 12),
            findDate(listOf("Aug 12, 2026"), preferDayFirst = false, today = today).first,
        )
    }

    @Test
    fun `rejects implausible dates like card expiry`() {
        val (date, _) = findDate(listOf("VALID THRU 12/31"), preferDayFirst = true, today = today)
        // 12/31 has no year and 2031 would be in the future — nothing valid here.
        assertTrue(date == null || !date.isAfter(today))
    }

    @Test
    fun `picks the dated line that carries a clock time`() {
        val (date, confidence) = findDate(
            listOf("KVK 33 12 45 67", "06-08-2026 16:04"),
            preferDayFirst = true,
            today = today,
        )
        assertNotNull(date)
        assertEquals(LocalDate.of(2026, 8, 6), date)
        assertEquals(Confidence.HIGH, confidence)
    }
}
