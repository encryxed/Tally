package com.encryxed.tally.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.encryxed.tally.R
import com.encryxed.tally.data.AppLanguage
import com.encryxed.tally.data.DateOrder
import com.encryxed.tally.data.TallySettings
import com.encryxed.tally.parse.ReceiptLanguage
import java.util.Currency
import java.util.Locale

/** Currencies offered in the picker, widest coverage without listing all 180. */
private val CURRENCY_CHOICES = listOf(
    "EUR", "USD", "GBP", "CHF", "SEK", "NOK", "DKK", "PLN", "CZK", "HUF",
    "RON", "BGN", "HRK", "TRY", "UAH", "RUB", "ISK", "ILS", "AED", "SAR",
    "QAR", "KWD", "BHD", "OMR", "JOD", "EGP", "MAD", "TND", "DZD", "ZAR",
    "NGN", "KES", "INR", "PKR", "BDT", "LKR", "THB", "VND", "IDR", "MYR",
    "SGD", "PHP", "HKD", "TWD", "CNY", "JPY", "KRW", "AUD", "NZD", "CAD",
    "MXN", "BRL", "ARS", "CLP", "COP", "PEN",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: TallySettings,
    versionName: String,
    onUiLanguage: (String?) -> Unit,
    onDefaultCurrency: (String) -> Unit,
    onReceiptLanguages: (Set<ReceiptLanguage>) -> Unit,
    onDateOrder: (DateOrder) -> Unit,
    onEditBudget: () -> Unit,
    onBack: () -> Unit,
) {
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showReceiptLanguages by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingRow(
                title = stringResource(R.string.budget_title),
                subtitle = stringResource(R.string.set_budget),
                hint = stringResource(R.string.budget_body),
                onClick = onEditBudget,
            )
            HorizontalDivider()

            SettingRow(
                title = stringResource(R.string.settings_app_language),
                subtitle = AppLanguage.fromTag(settings.uiLanguageTag)?.nativeName
                    ?: stringResource(R.string.settings_system_default),
                hint = stringResource(R.string.settings_app_language_hint),
                onClick = { showLanguagePicker = true },
            )
            HorizontalDivider()

            SettingRow(
                title = stringResource(R.string.settings_default_currency),
                subtitle = currencyLabel(settings.defaultCurrency),
                hint = stringResource(R.string.settings_currency_hint),
                onClick = { showCurrencyPicker = true },
            )
            HorizontalDivider()

            SettingRow(
                title = stringResource(R.string.settings_receipt_languages),
                subtitle = settings.receiptLanguages
                    .sortedBy { it.nativeName }
                    .joinToString { it.nativeName }
                    .ifEmpty { stringResource(R.string.settings_receipt_languages_none) },
                hint = stringResource(R.string.settings_receipt_languages_hint),
                onClick = { showReceiptLanguages = true },
            )
            HorizontalDivider()

            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    stringResource(R.string.settings_date_order),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.settings_date_order_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                DateOrder.entries.forEach { order ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onDateOrder(order) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = settings.dateOrder == order,
                            onClick = { onDateOrder(order) },
                        )
                        Text(
                            stringResource(
                                when (order) {
                                    DateOrder.AUTO -> R.string.date_order_auto
                                    DateOrder.DAY_FIRST -> R.string.date_order_day_first
                                    DateOrder.MONTH_FIRST -> R.string.date_order_month_first
                                }
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            HorizontalDivider()

            Text(
                stringResource(R.string.settings_ocr_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
            HorizontalDivider()

            SettingRow(
                title = stringResource(R.string.settings_about),
                subtitle = stringResource(R.string.settings_version, versionName),
                hint = stringResource(R.string.settings_source),
                onClick = { uriHandler.openUri("https://github.com/encryxed/Tally") },
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showLanguagePicker) {
        ChoiceDialog(
            title = stringResource(R.string.settings_app_language),
            onDismiss = { showLanguagePicker = false },
        ) {
            item {
                ChoiceRow(
                    label = stringResource(R.string.settings_system_default),
                    selected = settings.uiLanguageTag == null,
                    onClick = {
                        onUiLanguage(null)
                        showLanguagePicker = false
                    },
                )
            }
            items(AppLanguage.entries) { language ->
                ChoiceRow(
                    label = language.nativeName,
                    selected = settings.uiLanguageTag == language.tag,
                    onClick = {
                        onUiLanguage(language.tag)
                        showLanguagePicker = false
                    },
                )
            }
        }
    }

    if (showCurrencyPicker) {
        ChoiceDialog(
            title = stringResource(R.string.settings_default_currency),
            onDismiss = { showCurrencyPicker = false },
        ) {
            items(CURRENCY_CHOICES) { code ->
                ChoiceRow(
                    label = currencyLabel(code),
                    selected = settings.defaultCurrency == code,
                    onClick = {
                        onDefaultCurrency(code)
                        showCurrencyPicker = false
                    },
                )
            }
        }
    }

    if (showReceiptLanguages) {
        val selected = remember { mutableStateOf(settings.receiptLanguages) }
        AlertDialog(
            onDismissRequest = { showReceiptLanguages = false },
            title = { Text(stringResource(R.string.settings_receipt_languages)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(ReceiptLanguage.entries) { language ->
                        val isOn = language in selected.value
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected.value = if (isOn) {
                                        selected.value - language
                                    } else {
                                        selected.value + language
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = isOn, onCheckedChange = null)
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            Text(language.nativeName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReceiptLanguages(selected.value)
                        showReceiptLanguages = false
                    },
                    enabled = selected.value.isNotEmpty(),
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showReceiptLanguages = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    hint: String,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    onDismiss: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = content,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** "EUR — Euro", falling back to the bare code for anything the JDK lacks. */
private fun currencyLabel(code: String): String =
    runCatching {
        val currency = Currency.getInstance(code)
        val symbol = currency.getSymbol(Locale.getDefault())
        val name = currency.getDisplayName(Locale.getDefault())
        if (symbol == code) "$code — $name" else "$code ($symbol) — $name"
    }.getOrDefault(code)
