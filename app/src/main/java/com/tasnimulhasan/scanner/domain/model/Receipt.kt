package com.tasnimulhasan.scanner.domain.model

import java.time.LocalDateTime

/**
 * DDD Aggregate Root — Receipt
 *
 * Extended for expense-tracker use: time, receiptNumber, discount,
 * currency, and confidence score are now first-class fields.
 * rawLines preserved so the UI can always show exactly what was scanned.
 */
data class Receipt(
    val id: String,
    val storeName: String,
    val storeAddress: String?,
    val date: String?,
    val time: String?,                  // e.g. "14:32" — useful for expense logging
    val receiptNumber: String?,         // e.g. "#00847291"
    val items: List<ReceiptItem>,
    val subtotal: String?,
    val discount: String?,              // any discount/coupon line
    val tax: String?,
    val serviceFee: String?,            // service charge, delivery fee, etc.
    val total: String?,
    val paymentMethod: String?,
    val currency: String,               // e.g. "$", "৳", "€", "£"
    val parseConfidence: ParseConfidence,
    val rawLines: List<String>,         // every OCR line verbatim — shown in UI
    val scannedAt: LocalDateTime = LocalDateTime.now(),
)

/**
 * Value Object — a single line item on the receipt.
 * priceRaw stores the exact string from OCR so nothing is lost.
 */
data class ReceiptItem(
    val name: String,
    val quantity: String?,
    val unitPrice: String?,
    val totalPrice: String?,
    val priceRaw: String,               // exact text from OCR, e.g. "3.75" or "৳ 45"
    val isDiscount: Boolean = false,    // true for "-2.00" style lines
)

/**
 * Value Object — how confident the parser is about its output.
 * Shown in the UI so users know when to double-check.
 */
enum class ParseConfidence {
    HIGH,    // store name + total both found, ≥3 items
    MEDIUM,  // total found but few items, or items found but no total
    LOW,     // only raw lines available, minimal structure detected
}