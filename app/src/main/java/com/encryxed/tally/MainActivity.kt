package com.encryxed.tally

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.encryxed.tally.ui.BudgetDialog
import com.encryxed.tally.ui.EditScreen
import com.encryxed.tally.ui.HomeScreen
import com.encryxed.tally.ui.SavedSummary
import com.encryxed.tally.ui.ScanScreen
import com.encryxed.tally.ui.buildCsv
import com.encryxed.tally.ui.formatMoney
import com.encryxed.tally.ui.primaryCurrency
import com.encryxed.tally.ui.theme.TallyTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TallyTheme {
                TallyApp()
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val EDIT = "edit/{receiptId}"
    fun edit(id: Long) = "edit/$id"
}

@Composable
fun TallyApp(viewModel: TallyViewModel = viewModel()) {
    val navController = rememberNavController()
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // SAF hands us a destination the user picked; no storage permission needed.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(buildCsv(receipts).toByteArray())
                }
            }
        }
    }

    var showBudgetDialog by rememberSaveable { mutableStateOf(false) }

    if (showBudgetDialog) {
        BudgetDialog(
            current = viewModel.budget,
            currency = primaryCurrency(receipts, viewModel.fallbackCurrency()),
            onDismiss = { showBudgetDialog = false },
            onSave = { newBudget ->
                viewModel.updateBudget(newBudget)
                showBudgetDialog = false
            },
        )
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                receipts = receipts,
                budget = viewModel.budget,
                onScan = { navController.navigate(Routes.SCAN) },
                onExport = { exportLauncher.launch("tally-receipts.csv") },
                onEdit = { receipt -> navController.navigate(Routes.edit(receipt.id)) },
                onEditBudget = { showBudgetDialog = true },
            )
        }

        composable(Routes.SCAN) {
            val state = viewModel.scanState
            val saved = state as? ScanState.Saved

            // The receipt is already filed. Clear the confirmation after a
            // moment so the viewfinder is ready for the next one.
            LaunchedEffect(saved?.receiptId) {
                if (saved != null) {
                    delay(3_000)
                    viewModel.clearScan()
                }
            }

            ScanScreen(
                isReading = state is ScanState.Reading,
                errorMessage = (state as? ScanState.Failed)?.message,
                savedSummary = saved?.let {
                    SavedSummary(
                        receiptId = it.receiptId,
                        merchant = it.merchant,
                        amount = if (it.total > 0) formatMoney(it.total, it.currency) else "no total",
                        needsReview = it.needsReview,
                    )
                },
                onCaptured = { uri, path -> viewModel.captureAndSave(uri, path) },
                onEditSaved = { id ->
                    viewModel.clearScan()
                    navController.navigate(Routes.edit(id))
                },
                onBack = {
                    viewModel.clearScan()
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("receiptId") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("receiptId") ?: -1L
            val receipt = receipts.firstOrNull { it.id == id }

            if (receipt == null) {
                // Deleted, or the list hasn't loaded yet.
                LaunchedEffect(id) { navController.popBackStack(Routes.HOME, inclusive = false) }
            } else {
                EditScreen(
                    receipt = receipt,
                    onSave = {
                        viewModel.update(it)
                        navController.popBackStack()
                    },
                    onDelete = {
                        viewModel.delete(it)
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
