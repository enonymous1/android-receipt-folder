package com.enonymous1.receiptfolder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enonymous1.receiptfolder.data.Receipt
import com.enonymous1.receiptfolder.data.ReceiptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Single source of truth for every screen. v1 kept a separate list per
 * fragment and re-added every document on each snapshot event, which is
 * why the list duplicated itself; here the Room Flow is shared, keyed by
 * receipt id in the list, and never manually appended to.
 */
class ReceiptViewModel(private val repository: ReceiptRepository) : ViewModel {
    val receipts: StateFlow<List<Receipt>> = repository.receipts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Prefilled values from a just-captured photo, consumed once by the form. */
    data class PendingCapture(
        val imagePath: String?,
        val merchant: String,
        val dateMillis: Long?,
        val totalCents: Long?,
    )

    private val _pendingCapture = MutableStateFlow<PendingCapture?>(null)
    val pendingCapture: StateFlow<PendingCapture?> = _pendingCapture.asStateFlow()

    fun setPendingCapture(pending: PendingCapture) {
        _pendingCapture.value = pending
    }

    fun clearPendingCapture() {
        _pendingCapture.value = null
    }

    fun save(receipt: Receipt, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.save(receipt)
            onSaved(id)
        }
    }

    fun delete(receipt: Receipt, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.delete(receipt)
            onDeleted()
        }
    }

    fun receipt(id: Long) = repository.observeReceipt(id)
}
