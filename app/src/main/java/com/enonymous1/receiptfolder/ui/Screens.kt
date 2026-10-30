package com.enonymous1.receiptfolder.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.enonymous1.receiptfolder.data.Receipt
import com.enonymous1.receiptfolder.domain.ReceiptParser
import com.enonymous1.receiptfolder.domain.ReceiptValidation
import com.enonymous1.receiptfolder.domain.formatCents
import com.enonymous1.receiptfolder.domain.formatDateDisplay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    receiptCount: Int,
    onReceiptsClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onReportsClick: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Receipt Folder") }) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (receiptCount == 0) {
                    "No receipts yet. Capture one and it stays on this device."
                } else {
                    "$receiptCount ${if (receiptCount == 1) "receipt" else "receipts"} saved on this device."
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onCaptureClick, modifier = Modifier.fillMaxWidth()) {
                Text("Capture a receipt")
            }
            Button(onClick = onReceiptsClick, modifier = Modifier.fillMaxWidth()) {
                Text("My receipts")
            }
            Button(onClick = onReportsClick, modifier = Modifier.fillMaxWidth()) {
                Text("Reports")
            }
            Text(
                text = "Photos are recognised on-device and stored locally. " +
                    "There is no account, no cloud sync, and no shared trial collection in v2.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptListScreen(
    receipts: List<Receipt>,
    onBack: () -> Unit,
    onReceiptClick: (Receipt) -> Unit,
    onAddClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My receipts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add receipt")
            }
        },
    ) { padding ->
        if (receipts.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Nothing here yet. Tap + to capture your first receipt.")
            }
        } else {
            // Keyed by id: the list is a pure function of the database Flow,
            // so re-emissions replace rows instead of appending duplicates.
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(receipts, key = { it.id }) { receipt ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onReceiptClick(receipt) },
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(receipt.merchant, style = MaterialTheme.typography.titleMedium)
                            Text(formatDateDisplay(receipt.dateMillis))
                            Text(formatCents(receipt.totalCents))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    receipt: Receipt?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (Receipt) -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receipt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
    ) { padding ->
        if (receipt == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Receipt not found.")
            }
        } else {
            val currentReceipt = receipt
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReceiptImage(imagePath = currentReceipt.imagePath)
                Text(currentReceipt.merchant, style = MaterialTheme.typography.titleLarge)
                if (currentReceipt.merchantAddress.isNotBlank()) Text(currentReceipt.merchantAddress)
                Text(formatDateDisplay(currentReceipt.dateMillis))
                Text(formatCents(currentReceipt.totalCents), style = MaterialTheme.typography.titleMedium)
                if (currentReceipt.receiptName.isNotBlank()) Text("Name: ${currentReceipt.receiptName}")
            }
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("Delete") },
                    text = { Text("Are you sure you want to delete this receipt?") },
                    confirmButton = {
                        TextButton(onClick = { onDelete(currentReceipt) }) { Text("Yes") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) { Text("No") }
                    },
                )
            }
        }
    }
}

@Composable
fun ReceiptImage(imagePath: String?) {
    if (imagePath == null) return
    val bitmap = remember(imagePath) {
        BitmapFactory.decodeFile(imagePath)?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Receipt photo",
            modifier = Modifier.fillMaxWidth().height(320.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptFormScreen(
    existing: Receipt?,
    pending: ReceiptViewModel.PendingCapture?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    onSave: (Receipt) -> Unit,
) {
    var merchant by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var totalText by remember { mutableStateOf("") }
    var receiptName by remember { mutableStateOf("") }
    var imagePath by remember { mutableStateOf<String?>(null) }
    var prefilled by remember { mutableStateOf(false) }

    // Prefill exactly once, from whichever source arrived: the stored
    // receipt being edited, or the OCR result from a just-captured photo.
    LaunchedEffect(existing, pending) {
        if (!prefilled) {
            if (existing != null) {
                merchant = existing.merchant
                address = existing.merchantAddress
                dateText = ReceiptParser.formatDate(existing.dateMillis)
                totalText = if (existing.totalCents == 0L) "" else "%.2f".format(Locale.US, existing.totalCents / 100.0)
                receiptName = existing.receiptName
                imagePath = existing.imagePath
                prefilled = true
            } else if (pending != null) {
                merchant = pending.merchant
                dateText = ReceiptParser.formatDate(pending.dateMillis)
                totalText = if (pending.totalCents == null) "" else "%.2f".format(Locale.US, pending.totalCents / 100.0)
                imagePath = pending.imagePath
                prefilled = true
            }
        }
    }

    val parsedTotalCents = ReceiptParser.parseDollarsToCents(totalText)
    val totalCents = parsedTotalCents ?: 0L
    val errors = ReceiptValidation.validate(merchant, totalCents) +
        if (totalText.isNotBlank() && parsedTotalCents == null) {
            listOf("Total must be a number like 12.50.")
        } else {
            emptyList()
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New receipt" else "Edit receipt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReceiptImage(imagePath = imagePath)
            OutlinedTextField(
                value = merchant, onValueChange = { merchant = it },
                label = { Text("Merchant") }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = address, onValueChange = { address = it },
                label = { Text("Merchant address (optional)") }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = dateText, onValueChange = { dateText = it },
                label = { Text("Date (YYYY-MM-DD, optional)") }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = totalText, onValueChange = { totalText = it },
                label = { Text("Total (optional)") }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = receiptName, onValueChange = { receiptName = it },
                label = { Text("Receipt name (optional)") }, modifier = Modifier.fillMaxWidth(),
            )
            errors.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    onSave(
                        Receipt(
                            id = existing?.id ?: 0,
                            merchant = merchant.trim(),
                            merchantAddress = address.trim(),
                            dateMillis = ReceiptParser.parseDate(dateText),
                            totalCents = totalCents,
                            receiptName = receiptName.trim(),
                            imagePath = imagePath,
                            createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
                        ),
                    )
                    onSaved()
                },
                enabled = errors.isEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save receipt")
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}
