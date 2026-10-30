package com.enonymous1.receiptfolder

import com.enonymous1.receiptfolder.data.Receipt
import com.enonymous1.receiptfolder.data.ReceiptRepository
import com.enonymous1.receiptfolder.ui.ReceiptViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** In-memory fake, so ViewModel behaviour is tested without Room or a device. */
private class FakeReceiptRepository : ReceiptRepository {
    private val state = MutableStateFlow<List<Receipt>>(emptyList())
    private var nextId = 1L

    override val receipts: Flow<List<Receipt>> = state

    override fun observeReceipt(id: Long): Flow<Receipt?> =
        state.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun save(receipt: Receipt): Long {
        val saved = if (receipt.id == 0L) receipt.copy(id = nextId++) else receipt
        state.value = state.value.filter { it.id != saved.id } + saved
        return saved.id
    }

    override suspend fun delete(receipt: Receipt) {
        state.value = state.value.filter { it.id != receipt.id }
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReceiptViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveAddsReceiptToList() = runTest {
        val viewModel = ReceiptViewModel(FakeReceiptRepository())
        viewModel.save(Receipt(merchant = "Corner Market", totalCents = 1250))
        advanceUntilIdle()
        assertEquals(1, viewModel.receipts.value.size)
        assertEquals("Corner Market", viewModel.receipts.value.first().merchant)
    }

    @Test
    fun deleteRemovesReceiptFromList() = runTest {
        val viewModel = ReceiptViewModel(FakeReceiptRepository())
        viewModel.save(Receipt(merchant = "Corner Market", totalCents = 1250))
        advanceUntilIdle()
        val saved = viewModel.receipts.value.first()
        viewModel.delete(saved)
        advanceUntilIdle()
        assertTrue(viewModel.receipts.value.isEmpty())
    }

    @Test
    fun pendingCaptureIsSetAndCleared() = runTest {
        val viewModel = ReceiptViewModel(FakeReceiptRepository())
        viewModel.setPendingCapture(
            ReceiptViewModel.PendingCapture(
                imagePath = "/tmp/receipt.jpg",
                merchant = "Corner Market",
                dateMillis = null,
                totalCents = 1250,
            ),
        )
        assertEquals("Corner Market", viewModel.pendingCapture.value?.merchant)
        viewModel.clearPendingCapture()
        assertEquals(null, viewModel.pendingCapture.value)
    }
}
