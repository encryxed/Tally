package com.encryxed.tally.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.encryxed.tally.R
import com.encryxed.tally.data.Budget
import com.encryxed.tally.data.BudgetPeriod

@Composable
fun BudgetDialog(
    current: Budget?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Budget?) -> Unit,
) {
    var amountText by remember {
        mutableStateOf(current?.amount?.let { String.format(java.util.Locale.ROOT, "%.0f", it) }.orEmpty())
    }
    var period by remember { mutableStateOf(current?.period ?: BudgetPeriod.MONTHLY) }

    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val canSave = amount != null && amount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.budget_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.budget_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BudgetPeriod.entries.forEach { option ->
                        FilterChip(
                            selected = option == period,
                            onClick = { period = option },
                            label = { Text(option.localizedLabel()) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text(stringResource(R.string.budget_amount, currency)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(Budget(amount ?: 0.0, period)) },
                enabled = canSave,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = { onSave(null) }) { Text(stringResource(R.string.remove)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
