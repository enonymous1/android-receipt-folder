package com.enonymous1.receiptfolder.domain

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What OCR produced, after the first useful pass over the raw text. */
data class ParsedReceipt(
    val merchant: String,
    val dateMillis: Long? = null,
    val totalCents: Long? = null,
    val rawText: String = "",
)

/**
 * Parses ML Kit OCR text into receipt fields. This is the fix for v1, which
 * dumped the entire recognised block into the merchant field and never
 * filled in date, total, or image at all.
 *
 * Rules, in order of preference:
 * - Merchant: the first non-empty line that is not itself a date or a money
 *   amount. Receipts almost always print the store name first.
 * - Date: the first parseable date, tried as MM/DD/YYYY, YYYY-MM-DD, and
 *   "Jan 12, 2024" style month names. Two-digit years are read as 20xx.
 * - Total: the amount on the last line containing the word "total"
 *   (grand total beats subtotal); if no such line exists, the largest
 *   money amount in the text. Amounts need cents, so a bare "12" is not
 *   mistaken for $12.00.
 * Everything is best-effort: the capture form always lets the user correct
 * a field before saving, and a null here means "leave it blank", not zero.
 */
object ReceiptParser {
    private val moneyRegex = Regex("""\$?\s*(\d{1,3}(?:,\d{3})*|\d+)\.(\d{2})""")
    private val slashDateRegex = Regex("""(\d{1,2})/(\d{1,2})/(\d{2,4})""")
    private val isoDateRegex = Regex("""(\d{4})-(\d{1,2})-(\d{1,2})""")
    private val monthNameDateRegex =
        Regex("""(?i)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+(\d{1,2}),?\s+(\d{4})""")
    private val monthNames = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
    )

    fun parse(rawText: String): ParsedReceipt {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return ParsedReceipt(merchant = "", rawText = rawText)
        val merchant = lines.firstOrNull { line ->
            slashDateRegex.find(line) == null &&
                isoDateRegex.find(line) == null &&
                monthNameDateRegex.find(line) == null &&
                moneyRegex.find(line) == null
        } ?: lines.first()
        val dateMillis = lines.firstNotNullOfOrNull { parseDate(it) }
        val totalCents = parseTotal(lines)
        return ParsedReceipt(
            merchant = merchant.take(100),
            dateMillis = dateMillis,
            totalCents = totalCents,
            rawText = rawText,
        )
    }

    /** Parses one date string, or null. Public so the edit form can reuse it. */
    fun parseDate(text: String): Long? {
        slashDateRegex.find(text)?.let { match ->
            val month = match.groupValues[1].toIntOrNull() ?: return@let null
            val day = match.groupValues[2].toIntOrNull() ?: return@let null
            var year = match.groupValues[3].toIntOrNull() ?: return@let null
            if (year < 100) year += 2000
            return toMillis(year, month, day)
        }
        isoDateRegex.find(text)?.let { match ->
            val year = match.groupValues[1].toIntOrNull() ?: return@let null
            val month = match.groupValues[2].toIntOrNull() ?: return@let null
            val day = match.groupValues[3].toIntOrNull() ?: return@let null
            return toMillis(year, month, day)
        }
        monthNameDateRegex.find(text)?.let { match ->
            val month = monthNames[match.groupValues[1].lowercase(Locale.US)] ?: return@let null
            val day = match.groupValues[2].toIntOrNull() ?: return@let null
            val year = match.groupValues[3].toIntOrNull() ?: return@let null
            return toMillis(year, month, day)
        }
        return null
    }

    /** Formats epoch millis as YYYY-MM-DD for the edit form. */
    fun formatDate(dateMillis: Long?): String {
        if (dateMillis == null) return ""
        return LocalDate.ofEpochDay(dateMillis / 86_400_000L)
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /** Parses a dollars string such as "12.50" into cents, or null. */
    fun parseDollarsToCents(text: String): Long? {
        val cleaned = text.trim().removePrefix("$").replace(",", "")
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value < 0) return null
        return (value * 100).toLong()
    }

    private fun toMillis(year: Int, month: Int, day: Int): Long? {
        if (year !in 2000..2100 || month !in 1..12 || day !in 1..31) return null
        return try {
            LocalDate.of(year, month, day)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTotal(lines: List<String>): Long? {
        val totalLines = lines.filter { it.contains("total", ignoreCase = true) }
        val lineToUse = totalLines.lastOrNull()
        if (lineToUse != null) {
            moneyRegex.findAll(lineToUse).lastOrNull()?.let { return moneyToCents(it) }
        }
        return lines.flatMap { moneyRegex.findAll(it).toList() }
            .maxOfOrNull { moneyToCents(it) }
    }

    private fun moneyToCents(match: MatchResult): Long {
        val whole = match.groupValues[1].replace(",", "").toLongOrNull() ?: 0L
        val cents = match.groupValues[2].toLongOrNull() ?: 0L
        return whole * 100 + cents
    }
}

/** Validation for the edit form. A blank merchant is the only hard failure. */
object ReceiptValidation {
    fun validate(merchant: String, totalCents: Long?): List<String> {
        val errors = mutableListOf<String>()
        if (merchant.isBlank()) errors += "Merchant is required."
        if (totalCents != null && totalCents < 0) errors += "Total cannot be negative."
        return errors
    }
}
