package com.enonymous1.receiptfolder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.enonymous1.receiptfolder.data.ReceiptDatabase
import com.enonymous1.receiptfolder.data.RoomReceiptRepository
import com.enonymous1.receiptfolder.ui.ReceiptApp
import com.enonymous1.receiptfolder.ui.ReceiptViewModel
import com.enonymous1.receiptfolder.ui.theme.ReceiptFolderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = Room.databaseBuilder(
            applicationContext,
            ReceiptDatabase::class.java,
            "receipts.db",
        ).build()
        val repository = RoomReceiptRepository(database.receiptDao())
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReceiptViewModel(repository) as T
            }
        }
        setContent {
            ReceiptFolderTheme {
                val receiptViewModel: ReceiptViewModel = viewModel(factory = factory)
                ReceiptApp(viewModel = receiptViewModel)
            }
        }
    }
}
