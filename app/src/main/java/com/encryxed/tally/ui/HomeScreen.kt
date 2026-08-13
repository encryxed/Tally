package com.encryxed.tally.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import com.encryxed.tally.data.Budget
import com.encryxed.tally.data.BudgetProgress
import com.encryxed.tally.data.Receipt
import com.encryxed.tally.parse.Category
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    receipts: List<Receipt>,
    budget: Budget?,
    onScan: () -> Unit,
    onExport: () -> Unit,
    onEdit: (Receipt) -> Unit,
    onEditBudget: () -> Unit,
) {
    val needingReview = receipts.count { it.needsReview }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tally", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onEditBudget) {
                        Icon(Icons.Default.Settings, contentDescription = "Set budget")
                    }
                    if (receipts.isNotEmpty()) {
                        IconButton(onClick = onExport) {
                            Icon(Icons.Default.Share, contentDescription = "Export as CSV")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScan,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Scan receipt") },
            )
        },
    ) { padding ->
        if (receipts.isEmpty()) {
            EmptyState(Modifier.padding(padding))
            return@Scaffold
        }

        val byMonth = receipts
            .groupBy { YearMonth.from(it.date) }
            .toList()
            .sortedByDescending { it.first }

        // Everything that adds up is reported in one currency only.
        val currency = primaryCurrency(receipts, "EUR")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (needingReview > 0) {
                item(key = "review-banner") {
                    ReviewBanner(needingReview)
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (budget != null) {
                item(key = "budget") {
                    BudgetCard(
                        progress = BudgetProgress(
                            budget = budget,
                            spent = spentInCurrentPeriod(
                                receipts.filter { it.currency == currency },
                                budget,
                            ),
                            today = LocalDate.now(),
                        ),
                        currency = currency,
                        onClick = onEditBudget,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                MonthSummaryCard(
                    receipts = byMonth.first().second,
                    month = byMonth.first().first,
                    currency = currency,
                )
                Spacer(Modifier.height(8.dp))
            }

            byMonth.forEach { (month, monthReceipts) ->
                item(key = "header-$month") {
                    MonthHeader(month, monthReceipts, currency)
                }
                items(monthReceipts, key = { it.id }) { receipt ->
                    ReceiptRow(receipt = receipt, onClick = { onEdit(receipt) })
                }
            }

            item(key = "watermark") {
                Spacer(Modifier.height(16.dp))
                Watermark()
            }
        }
    }

}

private const val PROJECT_URL = "https://github.com/encryxed/tally"

/**
 * Author credit and source link, at the foot of the list and on the empty state.
 *
 * Handing a URL to the browser is an intent, not a network call, so this works
 * fine in an app that holds no INTERNET permission.
 */
@Composable
private fun Watermark(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = "Fully open source · built by @encryxed",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        textDecoration = TextDecoration.Underline,
        modifier = modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(PROJECT_URL) }
            .padding(vertical = 8.dp),
    )
}

@Composable
private fun ReviewBanner(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Text(
            if (count == 1) "1 receipt needs a quick check — tap the highlighted one below."
            else "$count receipts need a quick check — tap the highlighted ones below.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/** Total spent inside the budget's current week or month. */
private fun spentInCurrentPeriod(receipts: List<Receipt>, budget: Budget): Double {
    val today = LocalDate.now()
    val start = budget.startOf(today)
    val end = budget.endOf(today)
    return receipts
        .filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
        .sumOf { it.total }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetCard(
    progress: BudgetProgress,
    currency: String,
    onClick: () -> Unit,
) {
    val over = progress.isOver
    val nearlyThere = !over && progress.fraction >= 0.8f

    val container = when {
        over -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val onContainer = when {
        over -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${progress.budget.period.label.uppercase()} BUDGET",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    when {
                        over -> "Over budget"
                        nearlyThere -> "Nearly there"
                        else -> "On track"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))

            Text(
                formatMoney(progress.spent, currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "of ${formatMoney(progress.budget.amount, currency)}",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(onContainer.copy(alpha = 0.15f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.fraction.coerceIn(0f, 1f))
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(onContainer.copy(alpha = 0.75f))
                )
            }
            Spacer(Modifier.height(10.dp))

            val days = progress.budget.daysLeft(progress.today)
            Text(
                if (over) {
                    "${formatMoney(-progress.remaining, currency)} over · $days day${if (days == 1L) "" else "s"} left"
                } else {
                    "${formatMoney(progress.remaining, currency)} left · " +
                        "${formatMoney(progress.perDayLeft, currency)}/day for $days day${if (days == 1L) "" else "s"}"
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MonthSummaryCard(receipts: List<Receipt>, month: YearMonth, currency: String) {
    val inCurrency = receipts.filter { it.currency == currency }
    val otherCurrencies = receipts.size - inCurrency.size
    val total = inCurrency.sumOf { it.total }

    val byCategory = inCurrency
        .groupBy { it.category }
        .map { (category, list) -> category to list.sumOf { it.total } }
        .sortedByDescending { it.second }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                formatMonth(month).uppercase(),
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                formatMoney(total, currency),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                buildString {
                    append("${inCurrency.size} receipt${if (inCurrency.size == 1) "" else "s"}")
                    // Never add different currencies together — say so instead.
                    if (otherCurrencies > 0) append(" · $otherCurrencies in other currencies")
                },
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(16.dp))
            byCategory.take(4).forEach { (category, amount) ->
                CategoryBar(
                    category = category,
                    amount = amount,
                    fraction = if (total > 0) (amount / total).toFloat() else 0f,
                    currency = currency,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CategoryBar(
    category: Category,
    amount: Double,
    fraction: Float,
    currency: String,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${category.emoji} ${category.label}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            Text(
                formatMoney(amount, currency),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, receipts: List<Receipt>, currency: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            formatMonth(month),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            formatMoney(receipts.filter { it.currency == currency }.sumOf { it.total }, currency),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptRow(receipt: Receipt, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (receipt.needsReview) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            },
        ),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReceiptThumbnail(receipt)
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    receipt.merchant,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (receipt.needsReview) "${formatDate(receipt.date)} · tap to check"
                    else formatDate(receipt.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (receipt.needsReview) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                if (receipt.total > 0) formatMoney(receipt.total, receipt.currency) else "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** The captured photo, falling back to the category emoji. */
@Composable
private fun ReceiptThumbnail(receipt: Receipt) {
    val file = remember(receipt.imagePath) { receipt.imagePath?.let(::File)?.takeIf { it.exists() } }

    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (file != null) {
            AsyncImage(
                model = file,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(receipt.category.emoji, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🧾", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "No receipts yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Point the camera at a receipt and Tally fills in the shop, date and total for you.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Watermark()
    }
}
