package com.enonymous1.receiptfolder.ui

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.enonymous1.receiptfolder.data.Receipt
import com.enonymous1.receiptfolder.domain.formatCents
import com.enonymous1.receiptfolder.domain.formatDateDisplay
import java.io.File
import java.io.FileOutputStream

/**
 * A real report, not v1's 100x100 page containing only the word "RECEIPT".
 * One line per receipt (merchant, date, total), a count, and a grand total,
 * paginated at 40 lines per page, saved to the app's private files and
 * shared through FileProvider so no storage permission is needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(receipts: List<Receipt>, onBack: () -> Unit) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf("") }
    val grandTotal = receipts.sumOf { it.totalCents }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val file = createReceiptReport(context, receipts)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file,
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share report"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share report")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${receipts.size} ${if (receipts.size == 1) "receipt" else "receipts"}")
            Text("Grand total: ${formatCents(grandTotal)}", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    val file = createReceiptReport(context, receipts)
                    statusMessage = "Report saved: ${file.name}"
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Create PDF report")
            }
            if (statusMessage.isNotEmpty()) Text(statusMessage)
        }
    }
}

fun createReceiptReport(context: android.content.Context, receipts: List<Receipt>): File {
    val lines = mutableListOf<String>()
    lines += "Receipt Folder report"
    lines += "${receipts.size} receipts"
    lines += ""
    receipts.forEach { receipt ->
        lines += "${receipt.merchant} - ${formatDateDisplay(receipt.dateMillis)} - ${formatCents(receipt.totalCents)}"
    }
    lines += ""
    lines += "Grand total: ${formatCents(receipts.sumOf { it.totalCents })}"

    val document = PdfDocument()
    val paint = Paint().apply { textSize = 14f }
    var pageNumber = 1
    var index = 0
    while (index < lines.size || pageNumber == 1) {
        val pageInfo = PageInfo.Builder(612, 792, pageNumber).create()
        val page = document.startPage(pageInfo)
        var y = 40f
        var drawn = 0
        while (index < lines.size && drawn < 40) {
            page.canvas.drawText(lines[index], 40f, y, paint)
            y += 24f
            index++
            drawn++
        }
        document.finishPage(page)
        pageNumber++
        if (lines.isEmpty()) break
    }
    val file = File(context.filesDir, "pdfReceiptReport.pdf")
    FileOutputStream(file).use { output -> document.writeTo(output) }
    document.close()
    return file
}
