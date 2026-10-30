package com.enonymous1.receiptfolder

import com.enonymous1.receiptfolder.domain.ReceiptValidation
import com.enonymous1.receiptfolder.domain.formatCents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyAndValidationTest {
    @Test
    fun formatCentsFormatsDollarsAndCents() {
        assertEquals("$0.00", formatCents(0))
        assertEquals("$12.50", formatCents(1250))
        assertEquals("$1,000.00", formatCents(100000))
    }

    @Test
    fun blankMerchantIsInvalid() {
        assertTrue(ReceiptValidation.validate("", 1250).isNotEmpty())
        assertTrue(ReceiptValidation.validate("   ", 1250).isNotEmpty())
    }

    @Test
    fun negativeTotalIsInvalid() {
        assertTrue(ReceiptValidation.validate("Corner Market", -1).isNotEmpty())
    }

    @Test
    fun validReceiptHasNoErrors() {
        assertTrue(ReceiptValidation.validate("Corner Market", 1250).isEmpty())
        assertTrue(ReceiptValidation.validate("Corner Market", null).isEmpty())
    }
}
