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
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        // ── Success badge ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = ScanGreen,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Receipt scanned successfully",
                style = MaterialTheme.typography.bodyMedium,
                color = ScanGreen,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // ── Receipt paper card ────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = ReceiptCream,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // Store name header
                Text(
                    text = receipt.storeName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(4.dp))

                // Address & date
                receipt.storeAddress?.let {
                    ReceiptMetaRow(icon = Icons.Outlined.LocationOn, text = it)
                }
                receipt.date?.let {
                    ReceiptMetaRow(icon = Icons.Outlined.DateRange, text = it)
                }
                receipt.paymentMethod?.let {
                    ReceiptMetaRow(icon = Icons.Outlined.Payment, text = it)
                }

                Spacer(Modifier.height(16.dp))
                ReceiptDivider(label = "ITEMS")
                Spacer(Modifier.height(8.dp))

                // Items list
                if (receipt.items.isNotEmpty()) {
                    receipt.items.forEach { item ->
                        ReceiptItemRow(item = item)
                    }
                } else {
                    Text(
                        text = "No line items detected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))
                ReceiptDivider(label = "SUMMARY")
                Spacer(Modifier.height(8.dp))

                // Totals
                receipt.subtotal?.let { TotalRow(label = "Subtotal", amount = it, isBold = false) }
                receipt.tax?.let { TotalRow(label = "Tax", amount = it, isBold = false) }
                receipt.total?.let {
                    Spacer(Modifier.height(4.dp))
                    TotalRow(label = "TOTAL", amount = it, isBold = true, isHighlighted = true)
                }

                Spacer(Modifier.height(16.dp))

                // Perforated bottom
                ReceiptDivider(label = "")
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Actions ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = onToggleRawText,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Code,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(if (showRawText) "Hide Raw Text" else "View Raw Text")
            }

            FilledTonalButton(
                onClick = onReset,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("Scan New")
            }
        }

        // ── Raw text panel ────────────────────────────────────────────────
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
                Text(
                    text = rawText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun ReceiptMetaRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
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
        Divider(
            modifier = Modifier.weight(1f),
            color = DividerColor,
            thickness = 1.dp,
        )
    }
}

@Composable
private fun ReceiptItemRow(item: ReceiptItem) {
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
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (item.quantity != null && item.unitPrice != null) {
                Text(
                    text = "${item.quantity} × ${item.unitPrice}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        item.totalPrice?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun TotalRow(
    label: String,
    amount: String,
    isBold: Boolean,
    isHighlighted: Boolean = false,
) {
    val bgModifier = if (isHighlighted) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    }

    Row(
        modifier = bgModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = amount,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
        )
    }
}