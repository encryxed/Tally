package com.encryxed.tally.data

import android.content.Context
import com.encryxed.tally.parse.ReceiptLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Currency
import java.util.Locale

/** What to assume when a receipt writes a date like 03/04 and nothing settles it. */
enum class DateOrder { AUTO, DAY_FIRST, MONTH_FIRST }

/**
 * Languages Tally's own interface is available in.
 *
 * Deliberately wider than [ReceiptLanguage]: the OCR model only reads Latin
 * script, but there is no reason someone reading Latin-script receipts can't
 * have the app itself in Russian or Ukrainian.
 */
enum class AppLanguage(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    DUTCH("nl", "Nederlands"),
    GERMAN("de", "Deutsch"),
    FRENCH("fr", "Français"),
    SPANISH("es", "Español"),
    ITALIAN("it", "Italiano"),
    PORTUGUESE("pt", "Português"),
    POLISH("pl", "Polski"),
    TURKISH("tr", "Türkçe"),
    SWEDISH("sv", "Svenska"),
    DANISH("da", "Dansk"),
    NORWEGIAN("nb", "Norsk"),
    FINNISH("fi", "Suomi"),
    CZECH("cs", "Čeština"),
    SLOVAK("sk", "Slovenčina"),
    ROMANIAN("ro", "Română"),
    HUNGARIAN("hu", "Magyar"),
    INDONESIAN("id", "Bahasa Indonesia"),
    VIETNAMESE("vi", "Tiếng Việt"),
    RUSSIAN("ru", "Русский"),
    UKRAINIAN("uk", "Українська"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }
    }
}

data class TallySettings(
    /** null means "follow the system language". */
    val uiLanguageTag: String?,
    val defaultCurrency: String,
    val receiptLanguages: Set<ReceiptLanguage>,
    val dateOrder: DateOrder,
)

/**
 * Plain SharedPreferences rather than DataStore: these are four small values
 * that must also be readable synchronously from `attachBaseContext`, before
 * any coroutine scope exists.
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<TallySettings> = _settings.asStateFlow()

    fun setUiLanguage(tag: String?) {
        prefs.edit().apply {
            if (tag == null) remove(KEY_UI_LANGUAGE) else putString(KEY_UI_LANGUAGE, tag)
        }.apply()
        _settings.value = read()
    }

    fun setDefaultCurrency(code: String) {
        prefs.edit().putString(KEY_CURRENCY, code).apply()
        _settings.value = read()
    }

    fun setReceiptLanguages(languages: Set<ReceiptLanguage>) {
        prefs.edit().putStringSet(KEY_RECEIPT_LANGUAGES, languages.map { it.tag }.toSet()).apply()
        _settings.value = read()
    }

    fun setDateOrder(order: DateOrder) {
        prefs.edit().putString(KEY_DATE_ORDER, order.name).apply()
        _settings.value = read()
    }

    private fun read(): TallySettings = TallySettings(
        uiLanguageTag = prefs.getString(KEY_UI_LANGUAGE, null),
        defaultCurrency = prefs.getString(KEY_CURRENCY, null) ?: deviceCurrency(),
        receiptLanguages = prefs.getStringSet(KEY_RECEIPT_LANGUAGES, null)
            ?.mapNotNull(ReceiptLanguage::fromTag)
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?: defaultReceiptLanguages(),
        dateOrder = prefs.getString(KEY_DATE_ORDER, null)
            ?.let { runCatching { DateOrder.valueOf(it) }.getOrNull() }
            ?: DateOrder.AUTO,
    )

    companion object {
        private const val PREFS = "tally_settings"
        private const val KEY_UI_LANGUAGE = "ui_language"
        private const val KEY_CURRENCY = "default_currency"
        private const val KEY_RECEIPT_LANGUAGES = "receipt_languages"
        private const val KEY_DATE_ORDER = "date_order"

        /** Readable before the app has a ViewModel, for the locale override. */
        fun uiLanguageTag(context: Context): String? = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_UI_LANGUAGE, null)

        private fun deviceCurrency(): String =
            runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }
                .getOrDefault("EUR")

        /**
         * Start with the phone's own language plus English, rather than all
         * twenty. Fewer languages means fewer words to collide with, and most
         * people's receipts are in one or two languages.
         */
        private fun defaultReceiptLanguages(): Set<ReceiptLanguage> {
            val device = ReceiptLanguage.fromTag(Locale.getDefault().language)
            return setOfNotNull(device, ReceiptLanguage.ENGLISH)
        }
    }
}
