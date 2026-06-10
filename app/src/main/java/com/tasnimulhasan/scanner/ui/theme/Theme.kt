package com.tasnimulhasan.scanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = InkBlue800,
    onPrimary = SurfaceWhite,
    primaryContainer = InkBlue700,
    onPrimaryContainer = ReceiptCream,
    secondary = AccentAmber,
    onSecondary = InkBlue900,
    secondaryContainer = AccentAmberLight,
    onSecondaryContainer = InkBlue900,
    background = ReceiptCream,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = ReceiptCreamDark,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = SurfaceWhite,
    outline = DividerColor,
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentAmber,
    onPrimary = InkBlue900,
    primaryContainer = InkBlue800,
    onPrimaryContainer = ReceiptCream,
    secondary = AccentAmberLight,
    onSecondary = InkBlue900,
    background = InkBlue900,
    onBackground = ReceiptCream,
    surface = InkBlue800,
    onSurface = ReceiptCream,
    surfaceVariant = InkBlue700,
    onSurfaceVariant = TextMuted,
    error = ErrorRed,
    outline = InkBlue700,
)

@Composable
fun ScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}