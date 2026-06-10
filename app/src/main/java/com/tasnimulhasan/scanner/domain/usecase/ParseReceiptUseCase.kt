package com.tasnimulhasan.scanner.domain.usecase

import com.tasnimulhasan.scanner.domain.model.OcrResult
import com.tasnimulhasan.scanner.domain.model.Receipt
import com.tasnimulhasan.scanner.domain.model.ReceiptItem
import java.util.UUID
import javax.inject.Inject

/**
 * Use Case: Parses raw OCR text into a structured Receipt aggregate.
 * This is pure domain logic — no framework dependencies.
 */
class ParseReceiptUseCase @Inject constructor() {

    operator fun invoke(ocrResult: OcrResult): Receipt {
        val lines = ocrResult.rawText
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return Receipt(
            id = UUID.randomUUID().toString(),
            storeName = extractStoreName(lines),
            storeAddress = extractAddress(lines),
            date = extractDate(lines),
            items = extractItems(lines),
            subtotal = extractAmountByLabel(lines, listOf("subtotal", "sub total", "sub-total")),
            tax = extractAmountByLabel(lines, listOf("tax", "vat", "gst", "hst")),
            total = extractAmountByLabel(lines, listOf("total", "grand total", "amount due", "balance due")),
            paymentMethod = extractPaymentMethod(lines),
        )
    }

    private fun extractStoreName(lines: List<String>): String {
        // Typically the first non-empty, non-numeric line is the store name
        return lines.firstOrNull { line ->
            line.length > 3 && !line.matches(Regex(".*\\d{2}/\\d{2}/\\d{2,4}.*"))
        } ?: "Unknown Store"
    }

    private fun extractAddress(lines: List<String>): String? {
        val addressPatterns = listOf(
            Regex(".*\\d+.*\\b(st|street|ave|avenue|blvd|boulevard|rd|road|dr|drive|ln|lane)\\b.*", RegexOption.IGNORE_CASE),
            Regex(".*\\d{5}(-\\d{4})?.*"), // ZIP code
        )
        return lines.firstOrNull { line -> addressPatterns.any { it.containsMatchIn(line) } }
    }

    private fun extractDate(lines: List<String>): String? {
        val dateRegex = Regex(
            """(\d{1,2}[/\-\.]\d{1,2}[/\-\.]\d{2,4})|(\d{4}[/\-\.]\d{1,2}[/\-\.]\d{1,2})"""
        )
        return lines.firstNotNullOfOrNull { line ->
            dateRegex.find(line)?.value
        }
    }

    private fun extractItems(lines: List<String>): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()
        // Pattern: "Item Name   $X.XX" or "Item Name   X.XX"
        val pricePattern = Regex("""(.*?)\s+\$?([\d,]+\.\d{2})\s*$""")
        val skipKeywords = listOf(
            "subtotal", "sub total", "tax", "total", "vat", "gst", "hst",
            "cash", "card", "change", "visa", "mastercard", "thank", "receipt",
            "balance", "amount", "payment",
        )

        for (line in lines) {
            val lower = line.lowercase()
            if (skipKeywords.any { lower.contains(it) }) continue

            val match = pricePattern.find(line) ?: continue
            val itemName = match.groupValues[1].trim()
            val price = match.groupValues[2]

            if (itemName.length > 1) {
                items.add(
                    ReceiptItem(
                        name = itemName,
                        quantity = null,
                        unitPrice = null,
                        totalPrice = "$$price",
                    )
                )
            }
        }
        return items
    }

    private fun extractAmountByLabel(lines: List<String>, labels: List<String>): String? {
        val pricePattern = Regex("""\$?([\d,]+\.\d{2})""")
        return lines.firstOrNull { line ->
            val lower = line.lowercase()
            labels.any { lower.contains(it) }
        }?.let { line ->
            pricePattern.find(line)?.value?.let { "\$$it".replace("$$", "$") }
        }
    }

    private fun extractPaymentMethod(lines: List<String>): String? {
        val methods = mapOf(
            "visa" to "Visa",
            "mastercard" to "Mastercard",
            "amex" to "American Express",
            "cash" to "Cash",
            "debit" to "Debit Card",
            "credit" to "Credit Card",
            "apple pay" to "Apple Pay",
            "google pay" to "Google Pay",
        )
        val text = lines.joinToString(" ").lowercase()
        return methods.entries.firstOrNull { (key, _) -> text.contains(key) }?.value
    }
}