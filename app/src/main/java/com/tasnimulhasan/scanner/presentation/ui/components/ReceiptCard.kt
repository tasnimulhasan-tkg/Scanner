package com.tasnimulhasan.scanner.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasnimulhasan.scanner.domain.model.ParseConfidence
import com.tasnimulhasan.scanner.domain.model.Receipt
import com.tasnimulhasan.scanner.domain.model.ReceiptItem
import com.tasnimulhasan.scanner.ui.theme.DividerColor
import com.tasnimulhasan.scanner.ui.theme.ReceiptCream
import com.tasnimulhasan.scanner.ui.theme.ScanGreen

@Composable
fun ReceiptCard(
    receipt: Receipt,
    rawText: String,
    showRawText: Boolean,
    onToggleRawText: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {

        // ── Confidence badge ───────────────────────────────────────────────
        ConfidenceBadge(receipt.parseConfidence)

        Spacer(Modifier.height(12.dp))

        // ── Receipt paper card ─────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ReceiptCream),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // Store name
                Text(
                    text = receipt.storeName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(4.dp))

                // Meta rows
                receipt.storeAddress?.let {
                    ReceiptMetaRow(Icons.Outlined.LocationOn, it)
                }
                // Date + time on one row
                val dateTime = listOfNotNull(receipt.date, receipt.time).joinToString("  ")
                if (dateTime.isNotEmpty()) {
                    ReceiptMetaRow(Icons.Outlined.DateRange, dateTime)
                }
                receipt.receiptNumber?.let {
                    ReceiptMetaRow(Icons.Outlined.Receipt, it)
                }
                receipt.paymentMethod?.let {
                    ReceiptMetaRow(Icons.Outlined.Payment, it)
                }

                Spacer(Modifier.height(16.dp))

                // ── Items ──────────────────────────────────────────────────
                ReceiptDivider("ITEMS")
                Spacer(Modifier.height(8.dp))

                val regularItems = receipt.items.filter { !it.isDiscount }
                val discountItems = receipt.items.filter { it.isDiscount }

                if (regularItems.isNotEmpty()) {
                    regularItems.forEach { ReceiptItemRow(it, receipt.currency) }
                } else {
                    Text(
                        text = "No line items detected — check Raw Text below",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }

                // Discounts as separate section
                if (discountItems.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    ReceiptDivider("DISCOUNTS")
                    Spacer(Modifier.height(4.dp))
                    discountItems.forEach { ReceiptItemRow(it, receipt.currency) }
                }

                // ── Summary ────────────────────────────────────────────────
                Spacer(Modifier.height(8.dp))
                ReceiptDivider("SUMMARY")
                Spacer(Modifier.height(8.dp))

                receipt.subtotal?.let   { SummaryRow("Subtotal",    it) }
                receipt.discount?.let   { SummaryRow("Discount",    it, isDiscount = true) }
                receipt.tax?.let        { SummaryRow("Tax",         it) }
                receipt.serviceFee?.let { SummaryRow("Service Fee", it) }

                receipt.total?.let {
                    Spacer(Modifier.height(4.dp))
                    TotalHighlightRow("TOTAL", it)
                } ?: run {
                    // If no total found, show a note
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Total not detected",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(16.dp))
                ReceiptDivider("")
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Action buttons ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = onToggleRawText,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Code, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (showRawText) "Hide Raw OCR" else "View Raw OCR")
            }
            FilledTonalButton(
                onClick = onReset,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Refresh, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Scan New")
            }
        }

        // ── Raw OCR lines panel ────────────────────────────────────────────
        AnimatedVisibility(
            visible = showRawText,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RAW OCR OUTPUT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    // Show each line numbered so it's easy to cross-reference
                    receipt.rawLines.forEachIndexed { i, line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                        ) {
                            Text(
                                text = "%2d".format(i + 1),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.width(26.dp),
                            )
                            Text(
                                text = line,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun ConfidenceBadge(confidence: ParseConfidence) {
    val (icon, color, label, detail) = when (confidence) {
        ParseConfidence.HIGH -> Quadruple(
            Icons.Outlined.CheckCircle, ScanGreen,
            "Receipt parsed successfully",
            "All fields detected with high confidence",
        )
        ParseConfidence.MEDIUM -> Quadruple(
            Icons.Outlined.WarningAmber, Color(0xFFF59E0B),
            "Partial scan — please review",
            "Some fields may be missing or inaccurate",
        )
        ParseConfidence.LOW -> Quadruple(
            Icons.Outlined.ErrorOutline, MaterialTheme.colorScheme.error,
            "Low confidence — manual check needed",
            "Receipt format not fully recognised. Use Raw OCR to verify.",
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.10f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = color)
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
                Text(detail, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.75f))
            }
        }
    }
}

@Composable
private fun ReceiptMetaRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ReceiptDivider(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp,
            )
        }
        Divider(Modifier.weight(1f), color = DividerColor, thickness = 1.dp)
    }
}

@Composable
private fun ReceiptItemRow(item: ReceiptItem, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.isDiscount) Color(0xFF16A34A) else MaterialTheme.colorScheme.onBackground,
                textDecoration = if (item.isDiscount) TextDecoration.None else null,
            )
            // Show qty × unit price if available
            if (item.quantity != null) {
                val qtyLine = buildString {
                    append("qty: ${item.quantity}")
                    item.unitPrice?.let { append("  ×  $it each") }
                }
                Text(
                    text = qtyLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        item.totalPrice?.let {
            Text(
                text = if (item.isDiscount && !it.startsWith("-")) "-$it" else it,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (item.isDiscount) Color(0xFF16A34A) else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: String, isDiscount: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDiscount) Color(0xFF16A34A) else MaterialTheme.colorScheme.onBackground,
        )
        Text(
            if (isDiscount && !amount.startsWith("-")) "-$amount" else amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isDiscount) Color(0xFF16A34A) else MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun TotalHighlightRow(label: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
        Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
    }
}

/** Utility to destructure four values from a when expression */
private data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private operator fun <A, B, C, D> Quadruple<A, B, C, D>.component1() = a
private operator fun <A, B, C, D> Quadruple<A, B, C, D>.component2() = b
private operator fun <A, B, C, D> Quadruple<A, B, C, D>.component3() = c
private operator fun <A, B, C, D> Quadruple<A, B, C, D>.component4() = d