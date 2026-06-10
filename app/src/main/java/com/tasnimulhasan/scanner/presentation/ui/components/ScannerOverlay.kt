package com.tasnimulhasan.scanner.presentation.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.tasnimulhasan.scanner.ui.theme.ScanGreen

@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan_line",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            // Scan line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .graphicsLayer {
                        translationY = (scanPosition - 0.5f) * 160.dp.toPx()
                        alpha = pulseAlpha
                    }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ScanGreen.copy(alpha = 0f),
                                ScanGreen,
                                ScanGreen,
                                ScanGreen.copy(alpha = 0f),
                            )
                        )
                    )
            )

            // Center icon
            Icon(
                imageVector = Icons.Outlined.DocumentScanner,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { alpha = 0.25f },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Scanning receipt…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}