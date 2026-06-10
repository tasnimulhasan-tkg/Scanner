package com.tasnimulhasan.scanner.domain.model

import java.time.LocalDateTime

/**
 * DDD Aggregate Root — Receipt
 * Encapsulates all value objects belonging to a scanned receipt.
 */
data class Receipt(
    val id: String,
    val storeName: String,
    val storeAddress: String?,
    val date: String?,
    val items: List<ReceiptItem>,
    val subtotal: String?,
    val tax: String?,
    val total: String?,
    val paymentMethod: String?,
    val scannedAt: LocalDateTime = LocalDateTime.now(),
)

/**
 * Value Object — a single line item on the receipt.
 */
data class ReceiptItem(
    val name: String,
    val quantity: String?,
    val unitPrice: String?,
    val totalPrice: String?,
)