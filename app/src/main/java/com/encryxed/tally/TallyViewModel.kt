package com.encryxed.tally

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.encryxed.tally.data.Budget
import com.encryxed.tally.data.BudgetPeriod
import com.encryxed.tally.data.MerchantAlias
import com.encryxed.tally.data.Receipt
import com.encryxed.tally.data.DateOrder
import com.encryxed.tally.data.SettingsStore
import com.encryxed.tally.data.TallyDatabase
import com.encryxed.tally.data.TallySettings
import com.encryxed.tally.parse.LanguagePack
import com.encryxed.tally.parse.ReceiptLanguage
import com.encryxed.tally.parse.ReceiptParser
import com.encryxed.tally.parse.receiptSignature
import com.encryxed.tally.scan.Ocr
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.Currency
import java.util.Locale

/** What the scan flow is currently doing. */
sealed interface ScanState {
    data object Idle : ScanState
    data object Reading : ScanState

    /**
     * The receipt is already in the database. There is no confirmation step —
     * the shutter is the confirmation, and anything wrong gets fixed later
     * from the list.
     */
    data class Saved(
        val receiptId: Long,
        val merchant: String,
        val total: Double,
        val currency: String,
        val needsReview: Boolean,
    ) : ScanState

    data class Failed(val message: String) : ScanState
}

class TallyViewModel(app: Application) : AndroidViewModel(app) {

    private val database = TallyDatabase.get(app)
    private val dao = database.receiptDao()
    private val aliasDao = database.merchantAliasDao()
    private val prefs = app.getSharedPreferences("tally", Context.MODE_PRIVATE)
    private val settingsStore = SettingsStore(app)

    val settings: StateFlow<TallySettings> = settingsStore.settings

    val receipts: StateFlow<List<Receipt>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var scanState by mutableStateOf<ScanState>(ScanState.Idle)
        private set

    var budget by mutableStateOf(loadBudget())
        private set

    fun setUiLanguage(tag: String?) = settingsStore.setUiLanguage(tag)

    fun setDefaultCurrency(code: String) = settingsStore.setDefaultCurrency(code)

    fun setReceiptLanguages(languages: Set<ReceiptLanguage>) =
        settingsStore.setReceiptLanguages(languages)

    fun setDateOrder(order: DateOrder) = settingsStore.setDateOrder(order)

    /**
     * Which way round an ambiguous date like 03/04 is read. On AUTO this
     * follows the phone's region, which is right far more often than not.
     */
    private fun preferDayFirst(): Boolean = when (settingsStore.settings.value.dateOrder) {
        DateOrder.DAY_FIRST -> true
        DateOrder.MONTH_FIRST -> false
        DateOrder.AUTO -> Locale.getDefault().country !in setOf("US", "PH", "FM", "MH", "PW")
    }

    /**
     * Reads the photo and files the receipt immediately.
     *
     * Whatever the parser couldn't work out is filled with a sensible
     * placeholder and the row is flagged for review, rather than blocking the
     * user behind a form. The only case that doesn't save is a photo with no
     * readable text at all, where there would be nothing to file.
     */
    fun captureAndSave(imageUri: Uri, imagePath: String?) {
        scanState = ScanState.Reading
        // Read once per scan so the settings can't shift mid-parse.
        val current = settingsStore.settings.value
        val pack = LanguagePack.of(current.receiptLanguages)
        val dayFirst = preferDayFirst()

        viewModelScope.launch {
            scanState = runCatching {
                // The photo is read at all four right-angle rotations and the
                // best-scoring parse wins. A receipt lying sideways in frame
                // otherwise defeats every spatial rule the parser relies on.
                val reads = Ocr.readAllOrientations(getApplication(), imageUri)
                if (reads.isEmpty()) {
                    discardImage(imagePath)
                    return@runCatching ScanState.Failed(
                        "Couldn't read anything. Try again with more light and the whole receipt in frame."
                    )
                }

                val parsed = reads
                    .map { read ->
                        ReceiptParser.parse(
                            lines = read.lines,
                            preferDayFirst = dayFirst,
                            defaultCurrency = current.defaultCurrency,
                            pack = pack,
                        )
                    }
                    .maxBy { it.qualityScore }
                val signature = receiptSignature(parsed.merchant, parsed.rawText)
                val learned = signature.takeIf { it.isNotEmpty() }?.let { aliasDao.forSignature(it) }

                val merchant = learned?.merchant
                    ?: parsed.merchant?.takeIf { it.isNotBlank() }
                    ?: Receipt.UNKNOWN_MERCHANT
                val category = learned?.category ?: parsed.category
                val needsReview = learned == null &&
                    (parsed.merchant.isNullOrBlank() || parsed.total == null || parsed.uncertainFields.isNotEmpty())

                val receipt = Receipt(
                    merchant = merchant,
                    total = parsed.total ?: 0.0,
                    currency = parsed.currency.ifEmpty { current.defaultCurrency },
                    date = parsed.date ?: LocalDate.now(),
                    category = category,
                    imagePath = imagePath,
                    rawText = parsed.rawText,
                    needsReview = needsReview,
                    signature = signature,
                )
                val id = dao.insert(receipt)

                ScanState.Saved(
                    receiptId = id,
                    merchant = merchant,
                    total = receipt.total,
                    currency = receipt.currency,
                    needsReview = needsReview,
                )
            }.getOrElse { error ->
                ScanState.Failed(error.message ?: "Could not read that image.")
            }
        }
    }

    fun clearScan() {
        scanState = ScanState.Idle
    }

    /**
     * Applies a hand edit. Correcting the shop teaches the parser: the same
     * till produces the same signature next time, so the fix sticks.
     */
    fun update(receipt: Receipt) {
        viewModelScope.launch {
            dao.update(receipt.copy(needsReview = false))

            val signature = receipt.signature
            if (signature.isNotEmpty() &&
                receipt.merchant.isNotBlank() &&
                receipt.merchant != Receipt.UNKNOWN_MERCHANT
            ) {
                aliasDao.upsert(
                    MerchantAlias(
                        signature = signature,
                        merchant = receipt.merchant,
                        category = receipt.category,
                    )
                )
            }
        }
    }

    fun delete(receipt: Receipt) {
        viewModelScope.launch {
            dao.delete(receipt)
            receipt.imagePath?.let { path -> runCatching { File(path).delete() } }
        }
    }

    /** Throws away a photo the user decided not to keep. */
    fun discardImage(path: String?) {
        if (path != null) runCatching { File(path).delete() }
    }

    fun fallbackCurrency(): String = settingsStore.settings.value.defaultCurrency

    // ------------------------------------------------------------- budget

    // Not named setBudget: that clashes with the setter generated for the
    // `budget` property above.
    fun updateBudget(newBudget: Budget?) {
        budget = newBudget
        prefs.edit().apply {
            if (newBudget == null) {
                remove(KEY_BUDGET_AMOUNT)
                remove(KEY_BUDGET_PERIOD)
            } else {
                putFloat(KEY_BUDGET_AMOUNT, newBudget.amount.toFloat())
                putString(KEY_BUDGET_PERIOD, newBudget.period.name)
            }
        }.apply()
    }

    private fun loadBudget(): Budget? {
        val amount = prefs.getFloat(KEY_BUDGET_AMOUNT, -1f)
        if (amount <= 0f) return null
        val period = runCatching {
            BudgetPeriod.valueOf(prefs.getString(KEY_BUDGET_PERIOD, null) ?: BudgetPeriod.MONTHLY.name)
        }.getOrDefault(BudgetPeriod.MONTHLY)
        return Budget(amount.toDouble(), period)
    }

    private companion object {
        const val KEY_BUDGET_AMOUNT = "budget_amount"
        const val KEY_BUDGET_PERIOD = "budget_period"
    }
}
