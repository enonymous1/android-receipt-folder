package com.enonymous1.receiptfolder.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * One activity, five destinations. v1 used seven activities, one per
 * fragment, and passed receipts between them as intents and bundles,
 * which is where the captured image got lost; here every screen reads
 * the same ViewModel and the same Room database.
 */
@Composable
fun ReceiptApp(viewModel: ReceiptViewModel, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier,
    ) {
        composable("home") {
            val receipts by viewModel.receipts.collectAsState()
            HomeScreen(
                receiptCount = receipts.size,
                onReceiptsClick = { navController.navigate("receipts") },
                onCaptureClick = { navController.navigate("capture") },
                onReportsClick = { navController.navigate("reports") },
            )
        }
        composable("receipts") {
            val receipts by viewModel.receipts.collectAsState()
            ReceiptListScreen(
                receipts = receipts,
                onBack = { navController.popBackStack() },
                onReceiptClick = { receipt -> navController.navigate("detail/${receipt.id}") },
                onAddClick = { navController.navigate("capture") },
            )
        }
        composable(
            route = "detail/{receiptId}",
            arguments = listOf(navArgument("receiptId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val receiptId = backStackEntry.arguments?.getLong("receiptId") ?: 0L
            val receipt by viewModel.receipt(receiptId).collectAsState(initial = null)
            DetailScreen(
                receipt = receipt,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("form/$receiptId") },
                onDelete = { deleted ->
                    viewModel.delete(deleted) { navController.popBackStack() }
                },
            )
        }
        composable(
            route = "form/{receiptId}",
            arguments = listOf(navArgument("receiptId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val receiptId = backStackEntry.arguments?.getLong("receiptId") ?: 0L
            val existing by viewModel.receipt(receiptId).collectAsState(initial = null)
            val pending by viewModel.pendingCapture.collectAsState()
            ReceiptFormScreen(
                existing = if (receiptId == 0L) null else existing,
                pending = if (receiptId == 0L) pending else null,
                onSaved = {
                    viewModel.clearPendingCapture()
                    navController.navigate("receipts") {
                        popUpTo("home")
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
                onSave = { receipt -> viewModel.save(receipt) },
            )
        }
        composable("capture") {
            CaptureScreen(
                viewModel = viewModel,
                onCaptured = { navController.navigate("form/0") },
                onBack = { navController.popBackStack() },
            )
        }
        composable("reports") {
            val receipts by viewModel.receipts.collectAsState()
            ReportsScreen(
                receipts = receipts,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
