package com.encryxed.tally.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.encryxed.tally.R
import com.encryxed.tally.data.BudgetPeriod
import com.encryxed.tally.parse.Category

/**
 * Display names for the enums.
 *
 * These live here rather than on the enums themselves because `parse` is kept
 * free of Android types so it can be unit tested on the JVM. The enums keep an
 * English `label` for the CSV export, where a stable machine-readable value
 * matters more than the reader's language.
 */
@Composable
fun Category.localizedLabel(): String = stringResource(
    when (this) {
        Category.GROCERIES -> R.string.cat_groceries
        Category.DINING -> R.string.cat_dining
        Category.FUEL -> R.string.cat_fuel
        Category.TRANSPORT -> R.string.cat_transport
        Category.HEALTH -> R.string.cat_health
        Category.HOME -> R.string.cat_home
        Category.ELECTRONICS -> R.string.cat_electronics
        Category.CLOTHING -> R.string.cat_clothing
        Category.ENTERTAINMENT -> R.string.cat_entertainment
        Category.OTHER -> R.string.cat_other
    }
)

@Composable
fun BudgetPeriod.localizedLabel(): String = stringResource(
    when (this) {
        BudgetPeriod.WEEKLY -> R.string.budget_weekly
        BudgetPeriod.MONTHLY -> R.string.budget_monthly
    }
)
