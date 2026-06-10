package com.tasnimulhasan.scanner.domain.usecase

import com.tasnimulhasan.scanner.domain.model.OcrLine
import com.tasnimulhasan.scanner.domain.model.OcrResult
import com.tasnimulhasan.scanner.domain.model.ParseConfidence
import com.tasnimulhasan.scanner.domain.model.Receipt
import com.tasnimulhasan.scanner.domain.model.ReceiptItem
import java.util.UUID
import javax.inject.Inject
import kotlin.collections.first

/**
 * ParseReceiptUseCase — production-grade receipt parser.
 *
 * STRATEGY:
 *  Receipts fall into two physical layouts:
 *
 *  A) SINGLE-COLUMN  — name and price on the same OCR line, separated by spaces:
 *       "Whole Milk 1L          2.49"
 *
 *  B) TWO-COLUMN — name in left block, price in right block (detected separately
 *     by ML Kit as different TextBlocks at different X positions):
 *       Block 1: "Whole Milk 1L"  (left=0.05, right=0.60)
 *       Block 2: "2.49"           (left=0.70, right=0.95)
 *
 *  We handle both by:
 *  1. Collecting ALL OcrLines sorted by vertical position (top).
 *  2. Grouping lines whose Y centres are within ROW_Y_TOLERANCE of each other
 *     into logical rows (handles two-column layout).
 *  3. Within each row, separating "label" tokens (left side) from "price" tokens
 *     (right side, contains a numeric amount).
 *  4. Classifying each row as: HEADER, ITEM, DISCOUNT, SUBTOTAL, TAX,
 *     SERVICE, TOTAL, PAYMENT, SEPARATOR, or UNKNOWN.
 *  5. Assembling the Receipt aggregate from classified rows.
 */
class ParseReceiptUseCase @Inject constructor() {

    companion object {
        /** Lines whose Y-centres are within this fraction of image height are the same row */
        private const val ROW_Y_TOLERANCE = 0.018f

        /** Lines whose left edge is beyond this fraction are considered "right column" */
        private const val RIGHT_COLUMN_THRESHOLD = 0.55f

        // ── Regex patterns ──────────────────────────────────────────────────

        /** Matches any currency amount, with or without symbol, comma thousands sep */
        val PRICE_RE = Regex("""[\$€£₹৳¥₩]?\s*(\d{1,3}(?:[,.\s]\d{3})*(?:[.,]\d{1,2})|\d+[.,]\d{2})""")

        /** Matches a standalone price token (entire string is a price) */
        val PRICE_ONLY_RE = Regex("""^\s*[\$€£₹৳¥₩]?\s*-?\s*\d+[.,]\d{1,2}\s*$""")

        /** Matches quantity patterns: "2x", "x2", "2 @", "Qty: 3", "3 pcs" etc. */
        val QTY_RE = Regex("""(?:^|\s)(?:x\s*(\d+)|(\d+)\s*x|(\d+)\s*@|qty[:\s]*(\d+)|(\d+)\s*(?:pcs?|units?|nos?))""", RegexOption.IGNORE_CASE)

        /** Separator lines: dashes, equals, asterisks */
        val SEPARATOR_RE = Regex("""^[-=*_.]{3,}$""")

        /** Date: DD/MM/YYYY, MM-DD-YY, YYYY.MM.DD etc. */
        val DATE_RE = Regex("""(\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4})|(\d{4}[/\-.]\d{1,2}[/\-.]\d{1,2})""")

        /** Time: HH:MM or HH:MM:SS */
        val TIME_RE = Regex("""\b(\d{1,2}:\d{2}(?::\d{2})?(?:\s*[AaPp][Mm])?)\b""")

        /** Receipt / invoice / order number */
        val RECEIPT_NUM_RE = Regex("""(?:receipt|invoice|order|trans|txn|bill|no\.?|#)[^\d]*(\d{4,})""", RegexOption.IGNORE_CASE)

        /** Keywords that mean a line is a TOTAL */
        val TOTAL_KEYWORDS = setOf(
            "total", "grand total", "amount due", "balance due", "amount payable",
            "net amount", "net total", "total amount", "to pay", "payable",
        )

        /** Keywords that mean a line is a SUBTOTAL */
        val SUBTOTAL_KEYWORDS = setOf(
            "subtotal", "sub total", "sub-total", "net", "before tax",
        )

        /** Keywords that mean a line is TAX */
        val TAX_KEYWORDS = setOf(
            "tax", "vat", "gst", "hst", "pst", "sales tax", "service tax",
        )

        /** Keywords that mean a line is a SERVICE FEE */
        val SERVICE_KEYWORDS = setOf(
            "service", "service charge", "service fee", "delivery", "delivery fee",
            "tip", "gratuity", "surcharge",
        )

        /** Keywords that mean a line is a DISCOUNT */
        val DISCOUNT_KEYWORDS = setOf(
            "discount", "coupon", "promo", "offer", "saving", "savings",
            "member discount", "loyalty", "points redeemed",
        )

        /** Lines with these keywords are definitively NOT item lines */
        val NON_ITEM_KEYWORDS = TOTAL_KEYWORDS + SUBTOTAL_KEYWORDS + TAX_KEYWORDS +
                SERVICE_KEYWORDS + DISCOUNT_KEYWORDS +
                setOf(
                    "payment", "cash", "change", "visa", "mastercard", "amex",
                    "debit", "credit", "card", "apple pay", "google pay", "bkash",
                    "nagad", "thank", "welcome", "please", "receipt", "invoice",
                    "tel", "phone", "fax", "email", "www", "http", "store",
                    "cashier", "reg", "terminal", "operator", "served by",
                )

        /** Payment method keywords */
        val PAYMENT_MAP = linkedMapOf(
            "bkash" to "bKash",
            "nagad" to "Nagad",
            "rocket" to "Rocket",
            "apple pay" to "Apple Pay",
            "google pay" to "Google Pay",
            "samsung pay" to "Samsung Pay",
            "visa" to "Visa",
            "mastercard" to "Mastercard",
            "amex" to "American Express",
            "american express" to "American Express",
            "discover" to "Discover",
            "debit" to "Debit Card",
            "credit" to "Credit Card",
            "cash" to "Cash",
            "cheque" to "Cheque",
            "check" to "Cheque",
        )

        /** Currency symbol detection */
        fun detectCurrency(text: String): String = when {
            text.contains('৳') || text.contains("BDT", ignoreCase = true) -> "৳"
            text.contains('€') || text.contains("EUR", ignoreCase = true) -> "€"
            text.contains('£') || text.contains("GBP", ignoreCase = true) -> "£"
            text.contains('₹') || text.contains("INR", ignoreCase = true) -> "₹"
            text.contains('¥') || text.contains("JPY", ignoreCase = true) ||
                    text.contains("CNY", ignoreCase = true) -> "¥"
            text.contains('₩') || text.contains("KRW", ignoreCase = true) -> "₩"
            else -> "$"
        }
    }

    // ── Public entry point ────────────────────────────────────────────────────

    operator fun invoke(ocrResult: OcrResult): Receipt {
        // Step 1: collect all lines with spatial data, sorted top→bottom
        val allLines = ocrResult.blocks
            .flatMap { block -> block.lines }
            .sortedBy { it.top }

        // Raw lines verbatim (for UI display — user always sees exactly what OCR read)
        val rawLines = allLines.map { it.text }

        // Detect currency from full text
        val currency = detectCurrency(ocrResult.rawText)

        // Step 2: group into logical rows (handles two-column layout)
        val rows: List<LogicalRow> = groupIntoRows(allLines)

        // Step 3: classify each row
        val classified: List<ClassifiedRow> = rows.map { classify(it, currency) }

        // Step 4: extract fields
        val headerLines = classified.filter { it.type == RowType.HEADER }
        val itemRows    = classified.filter { it.type == RowType.ITEM }
        val discountRows = classified.filter { it.type == RowType.DISCOUNT }
        val subtotalRow = classified.firstOrNull { it.type == RowType.SUBTOTAL }
        val taxRows     = classified.filter { it.type == RowType.TAX }
        val serviceRows = classified.filter { it.type == RowType.SERVICE }
        val totalRow    = classified.lastOrNull { it.type == RowType.TOTAL }
        val paymentRow  = classified.firstOrNull { it.type == RowType.PAYMENT }

        val fullText = ocrResult.rawText

        // Confidence scoring
        val confidence = when {
            totalRow != null && itemRows.size >= 3 -> ParseConfidence.HIGH
            totalRow != null || itemRows.size >= 2  -> ParseConfidence.MEDIUM
            else                                    -> ParseConfidence.LOW
        }

        return Receipt(
            id              = UUID.randomUUID().toString(),
            storeName       = extractStoreName(headerLines, classified),
            storeAddress    = extractAddress(classified),
            date            = DATE_RE.find(fullText)?.value,
            time            = TIME_RE.find(fullText)?.groupValues?.get(1),
            receiptNumber   = RECEIPT_NUM_RE.find(fullText)?.groupValues?.get(1)?.let { "#$it" },
            items           = itemRows.map { buildReceiptItem(it, currency) } +
                    discountRows.map { buildReceiptItem(it, currency, isDiscount = true) },
            subtotal        = subtotalRow?.priceText,
            discount        = discountRows.firstOrNull()?.priceText,
            tax             = taxRows.firstOrNull()?.priceText?.let {
                if (taxRows.size > 1)
                    taxRows.sumAmounts(currency)
                else it
            },
            serviceFee      = serviceRows.firstOrNull()?.priceText?.let {
                if (serviceRows.size > 1)
                    serviceRows.sumAmounts(currency)
                else it
            },
            total           = totalRow?.priceText,
            paymentMethod   = paymentRow?.labelText ?: detectPayment(fullText),
            currency        = currency,
            parseConfidence = confidence,
            rawLines        = rawLines,
        )
    }

    // ── Step 2: group lines into logical rows ─────────────────────────────────

    /**
     * Lines from separate text blocks (left column / right column) that share
     * approximately the same vertical position are merged into one LogicalRow.
     */
    private fun groupIntoRows(lines: List<OcrLine>): List<LogicalRow> {
        if (lines.isEmpty()) return emptyList()

        val rows = mutableListOf<LogicalRow>()
        var currentGroup = mutableListOf(lines.first())

        for (i in 1 until lines.size) {
            val prev = currentGroup.last()
            val curr = lines[i]
            val prevCentre = (prev.top + prev.bottom) / 2f
            val currCentre = (curr.top + curr.bottom) / 2f

            if (kotlin.math.abs(currCentre - prevCentre) <= ROW_Y_TOLERANCE) {
                currentGroup.add(curr)
            } else {
                rows.add(toLogicalRow(currentGroup))
                currentGroup = mutableListOf(curr)
            }
        }
        if (currentGroup.isNotEmpty()) rows.add(toLogicalRow(currentGroup))
        return rows
    }

    private fun toLogicalRow(lines: List<OcrLine>): LogicalRow {
        // Sort left→right within a row
        val sorted = lines.sortedBy { it.left }
        val fullText = sorted.joinToString("  ") { it.text }

        // Find price tokens on the right side
        val rightTokens = sorted.filter { line ->
            line.left > RIGHT_COLUMN_THRESHOLD || PRICE_ONLY_RE.matches(line.text)
        }
        val leftTokens = sorted - rightTokens.toSet()

        val priceText = rightTokens.joinToString(" ") { it.text }.trim()
            .ifEmpty { extractPriceFromEnd(fullText) }

        val labelText = leftTokens.joinToString(" ") { it.text }.trim()
            .ifEmpty { fullText.substringBefore(priceText).trim() }

        return LogicalRow(
            fullText  = fullText,
            labelText = labelText.ifEmpty { fullText },
            priceText = priceText,
            topY      = lines.minOf { it.top },
        )
    }

    /** Last-resort: pull price from the end of a single-column line */
    private fun extractPriceFromEnd(text: String): String {
        return PRICE_RE.findAll(text).lastOrNull()?.value?.trim() ?: ""
    }

    // ── Step 3: classify rows ─────────────────────────────────────────────────

    private fun classify(row: LogicalRow, currency: String): ClassifiedRow {
        val lower = row.fullText.lowercase().trim()
        val label = row.labelText.lowercase().trim()

        // Separator lines
        if (SEPARATOR_RE.matches(row.fullText.trim())) {
            return ClassifiedRow(row, RowType.SEPARATOR, row.fullText, "")
        }

        // Lines with no price and no alphanumeric content — skip
        if (row.fullText.isBlank()) return ClassifiedRow(row, RowType.UNKNOWN, "", "")

        val hasPrice = row.priceText.isNotEmpty() && PRICE_RE.containsMatchIn(row.priceText)

        // TOTAL — check before subtotal because "grand total" contains "total"
        if (TOTAL_KEYWORDS.any { lower.contains(it) } && hasPrice) {
            return ClassifiedRow(row, RowType.TOTAL, row.labelText, row.priceText)
        }

        // SUBTOTAL
        if (SUBTOTAL_KEYWORDS.any { label.contains(it) } && hasPrice) {
            return ClassifiedRow(row, RowType.SUBTOTAL, row.labelText, row.priceText)
        }

        // TAX
        if (TAX_KEYWORDS.any { label.contains(it) }) {
            return ClassifiedRow(row, RowType.TAX, row.labelText, row.priceText)
        }

        // SERVICE / DELIVERY
        if (SERVICE_KEYWORDS.any { label.contains(it) } && hasPrice) {
            return ClassifiedRow(row, RowType.SERVICE, row.labelText, row.priceText)
        }

        // DISCOUNT — negative prices or discount keywords
        val isNegative = row.priceText.contains('-') || lower.contains('-')
        if (DISCOUNT_KEYWORDS.any { lower.contains(it) } || (isNegative && hasPrice)) {
            return ClassifiedRow(row, RowType.DISCOUNT, row.labelText, row.priceText)
        }

        // PAYMENT
        if (PAYMENT_MAP.keys.any { lower.contains(it) } && !hasPrice) {
            val method = PAYMENT_MAP.entries.firstOrNull { (k, _) -> lower.contains(k) }?.value
                ?: row.fullText.trim()
            return ClassifiedRow(row, RowType.PAYMENT, method, "")
        }

        // ITEM — has a price and doesn't match non-item keywords
        if (hasPrice) {
            val isNonItem = NON_ITEM_KEYWORDS.any { label.contains(it) }
            if (!isNonItem && row.labelText.length > 1) {
                return ClassifiedRow(row, RowType.ITEM, row.labelText, row.priceText)
            }
        }

        // HEADER — short lines near the top with no price: store name / address
        return ClassifiedRow(row, RowType.HEADER, row.fullText, "")
    }

    // ── Step 4: field extractors ──────────────────────────────────────────────

    private fun extractStoreName(
        headers: List<ClassifiedRow>,
        all: List<ClassifiedRow>,
    ): String {
        // Skip lines that look like separators, addresses, phone numbers, or pure dates
        val skipRe = listOf(
            SEPARATOR_RE,
            Regex("""^\d[\d\s\-\+\(\)]{6,}$"""),           // phone-only line
            DATE_RE,
            Regex("""^(receipt|invoice|order|bill|tax invoice)""", RegexOption.IGNORE_CASE),
            Regex("""^(tel|phone|fax|email|www|http)""",    RegexOption.IGNORE_CASE),
        )

        // First non-skipped header line, preferring longer text (more likely a store name)
        return headers
            .map { it.labelText.trim() }
            .filter { line ->
                line.length >= 3 && skipRe.none { it.containsMatchIn(line) }
            }
            .maxByOrNull { it.length }
            ?: all.firstOrNull { it.type != RowType.SEPARATOR }?.labelText?.trim()
            ?: "Unknown Store"
    }

    private fun extractAddress(all: List<ClassifiedRow>): String? {
        val addressRe = listOf(
            // "123 Main Street" or "Block 5, Road 12"
            Regex("""^\d+[\w\s,.-]+\b(st|street|ave|avenue|blvd|rd|road|dr|lane|ln|block|sector|area|zone)\b""", RegexOption.IGNORE_CASE),
            // Postal / ZIP code
            Regex("""\b\d{4,6}\b"""),
            // Contains comma — usually "City, State"
            Regex("""^[A-Za-z\s]+,\s*[A-Za-z\s]+"""),
        )
        return all
            .filter { it.type == RowType.HEADER }
            .map { it.labelText.trim() }
            .firstOrNull { line -> addressRe.any { it.containsMatchIn(line) } }
    }

    private fun detectPayment(text: String): String? {
        val lower = text.lowercase()
        return PAYMENT_MAP.entries.firstOrNull { (k, _) -> lower.contains(k) }?.value
    }

    // ── Item builder ──────────────────────────────────────────────────────────

    private fun buildReceiptItem(
        row: ClassifiedRow,
        currency: String,
        isDiscount: Boolean = false,
    ): ReceiptItem {
        val label = row.labelText

        // Try to extract quantity from label, e.g. "2 x Milk" or "Milk x3"
        val qtyMatch = QTY_RE.find(label)
        val qty = qtyMatch?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }

        // Clean name: remove quantity fragment from label
        val name = if (qtyMatch != null)
            label.replace(qtyMatch.value, "").trim().trimStart('-', '@', 'x', 'X').trim()
        else
            label.trim()

        // Parse numeric value to separate unit price (if qty known)
        val priceNumeric = PRICE_RE.find(row.priceText)?.value?.replace(Regex("[^0-9.,]"), "")
            ?.replace(',', '.')?.toDoubleOrNull()
        val qtyInt = qty?.toIntOrNull()
        val unitPrice = if (qtyInt != null && qtyInt > 0 && priceNumeric != null) {
            val unit = priceNumeric / qtyInt
            "$currency%.2f".format(unit)
        } else null

        return ReceiptItem(
            name       = name.ifEmpty { row.row.fullText },
            quantity   = qty,
            unitPrice  = unitPrice,
            totalPrice = row.priceText.ifEmpty { null }?.let { formatPrice(it, currency) },
            priceRaw   = row.priceText,
            isDiscount = isDiscount,
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun formatPrice(raw: String, currency: String): String {
        // If the raw string already starts with a currency symbol, return as-is
        if (raw.any { it in "$€£₹৳¥₩" }) return raw.trim()
        return "$currency${raw.trim()}"
    }

    /** Sum amounts from multiple rows (e.g. GST + PST) */
    private fun List<ClassifiedRow>.sumAmounts(currency: String): String {
        val total = sumOf { row ->
            PRICE_RE.find(row.priceText)?.value
                ?.replace(Regex("[^0-9.,]"), "")
                ?.replace(',', '.')
                ?.toDoubleOrNull() ?: 0.0
        }
        return "$currency%.2f".format(total)
    }

    // ── Internal data types ───────────────────────────────────────────────────

    private data class LogicalRow(
        val fullText: String,
        val labelText: String,
        val priceText: String,
        val topY: Float,
    )

    private enum class RowType {
        HEADER, ITEM, DISCOUNT, SUBTOTAL, TAX, SERVICE, TOTAL, PAYMENT, SEPARATOR, UNKNOWN
    }

    private data class ClassifiedRow(
        val row: LogicalRow,
        val type: RowType,
        val labelText: String,
        val priceText: String,
    )
}