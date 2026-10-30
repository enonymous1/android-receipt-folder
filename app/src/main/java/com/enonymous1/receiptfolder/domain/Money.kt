package com.enonymous1.receiptfolder.domain

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formats integer cents as US currency: 1250 -> "$12.50". */
fun formatCents(totalCents: Long): String =
    NumberFormat.getCurrencyInstance(Locale.US).format(totalCents / 100.0)

/** Formats epoch millis as "Jan 12, 2024", or "No date" when null. */
fun formatDateDisplay(dateMillis: Long?): String {
    if (dateMillis == null) return "No date"
    return Instant.ofEpochMilli(dateMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
}
