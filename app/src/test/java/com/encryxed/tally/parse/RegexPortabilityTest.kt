package com.encryxed.tally.parse

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * These tests run on the JVM, but the app runs on Android — and the two use
 * different regex engines. Android delegates to ICU, which rejects several
 * constructs the JDK accepts.
 *
 * That difference is invisible to every other test in this suite: a pattern
 * using a JDK-only feature compiles happily here and throws
 * PatternSyntaxException the moment a real device touches it. It shipped
 * exactly that way once — `(?U)` in the total-matching pattern crashed the app
 * on every capture while all 57 tests stayed green.
 *
 * So this file checks the patterns for portability rather than behaviour.
 */
class RegexPortabilityTest {

    /** Inline flags the JDK supports and Android's ICU engine does not. */
    private val jvmOnlyFlags = listOf("(?U)", "(?u)")

    private fun patternsUnderTest(): List<Pair<String, String>> {
        val all = LanguagePack.of(ReceiptLanguage.entries)
        val single = LanguagePack.of(listOf(ReceiptLanguage.FINNISH))
        return listOf(
            "all languages" to all.totalPattern.pattern,
            "single language" to single.totalPattern.pattern,
        )
    }

    @Test
    fun `generated patterns avoid JDK-only inline flags`() {
        patternsUnderTest().forEach { (name, pattern) ->
            jvmOnlyFlags.forEach { flag ->
                assertFalse(
                    "$name pattern uses $flag, which Android's ICU regex rejects: $pattern",
                    pattern.contains(flag),
                )
            }
        }
    }

    @Test
    fun `word boundaries still separate net from netto`() {
        val pack = LanguagePack.of(listOf(ReceiptLanguage.ENGLISH, ReceiptLanguage.DUTCH))
        assertTrue(pack.hasTotalKeyword("net 12,50"))
        // Dutch NETTO is the pre-VAT subtotal, not the total.
        assertFalse(pack.hasTotalKeyword("netto 11,01"))
    }

    @Test
    fun `accented keywords match at their boundaries`() {
        // The whole reason the unsupported flag was there in the first place:
        // a plain ASCII \b does not close a word that ends in a letter like ä.
        assertTrue(LanguagePack.of(listOf(ReceiptLanguage.FINNISH)).hasTotalKeyword("yhteensä 12,50"))
        assertTrue(LanguagePack.of(listOf(ReceiptLanguage.TURKISH)).hasTotalKeyword("ödenecek 40,00"))
        assertTrue(LanguagePack.of(listOf(ReceiptLanguage.HUNGARIAN)).hasTotalKeyword("fizetendő 900"))
        assertTrue(LanguagePack.of(listOf(ReceiptLanguage.POLISH)).hasTotalKeyword("do zapłaty 45,00"))
    }

    @Test
    fun `a keyword glued inside a longer word does not match`() {
        val pack = LanguagePack.of(listOf(ReceiptLanguage.ENGLISH))
        assertFalse(pack.hasTotalKeyword("subtotalling"))
        assertFalse(pack.hasTotalKeyword("totally different"))
    }
}
