package com.enonymous1.receiptfolder

import com.enonymous1.receiptfolder.domain.ReceiptParser
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptParserTest {
    private fun epoch(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun merchantIsFirstLineNotDateOrMoney() {
        val parsed = ReceiptParser.parse("Corner Market\n123 Main St\n01/12/2024\nTotal \$12.50")
        assertEquals("Corner Market", parsed.merchant)
    }

    @Test
    fun slashDateIsParsed() {
        val parsed = ReceiptParser.parse("Corner Market\n01/12/2024\nTotal \$12.50")
        assertEquals(epoch(2024, 1, 12), parsed.dateMillis)
    }

    @Test
    fun isoDateIsParsed() {
        val parsed = ReceiptParser.parse("Corner Market\n2024-03-05\nTotal \$12.50")
        assertEquals(epoch(2024, 3, 5), parsed.dateMillis)
    }

    @Test
    fun monthNameDateIsParsed() {
        val parsed = ReceiptParser.parse("Corner Market\nJan 12, 2024\nTotal \$12.50")
        assertEquals(epoch(2024, 1, 12), parsed.dateMillis)
    }

    @Test
    fun totalLineBeatsSubtotal() {
        val parsed = ReceiptParser.parse("Corner Market\nSubtotal \$10.00\nTotal \$12.50")
        assertEquals(1250L, parsed.totalCents)
    }

    @Test
    fun largestAmountIsFallbackWithoutTotalLine() {
        val parsed = ReceiptParser.parse("Corner Market\nMilk \$3.25\nBread \$4.75")
        assertEquals(475L, parsed.totalCents)
    }

    @Test
    fun bareNumbersWithoutCentsAreNotMoney() {
        val parsed = ReceiptParser.parse("Corner Market\nItems 12\nTotal \$12.50")
        assertEquals(1250L, parsed.totalCents)
    }

    @Test
    fun emptyTextHasNoFields() {
        val parsed = ReceiptParser.parse("")
        assertEquals("", parsed.merchant)
        assertNull(parsed.dateMillis)
        assertNull(parsed.totalCents)
    }

    @Test
    fun invalidDateIsIgnored() {
        val parsed = ReceiptParser.parse("Corner Market\n99/99/2024\nTotal \$12.50")
        assertNull(parsed.dateMillis)
        assertEquals(1250L, parsed.totalCents)
    }

    @Test
    fun dollarsToCentsParsesAndRejectsNegative() {
        assertEquals(1250L, ReceiptParser.parseDollarsToCents("12.50"))
        assertEquals(1250L, ReceiptParser.parseDollarsToCents("\$12.50"))
        assertNull(ReceiptParser.parseDollarsToCents("-5.00"))
        assertNull(ReceiptParser.parseDollarsToCents("abc"))
    }

    @Test
    fun dateRoundTripsThroughFormat() {
        val millis = epoch(2024, 1, 12)
        assertEquals("2024-01-12", ReceiptParser.formatDate(millis))
        assertEquals(millis, ReceiptParser.parseDate("2024-01-12"))
    }
}
