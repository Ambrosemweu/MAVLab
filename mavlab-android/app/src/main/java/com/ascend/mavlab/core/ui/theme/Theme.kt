package com.ascend.mavlab.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MavLabColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    primaryContainer = Color(0xFF1E3A5F),
    secondary = Color(0xFF34D399),
    secondaryContainer = Color(0xFF1A3B2E),
    tertiary = Color(0xFFFBBF24),
    error = Color(0xFFEF4444),
    background = Color(0xFF0A0E1A),
    surface = Color(0xFF141B2D),
    surfaceVariant = Color(0xFF1E2740),
    onPrimary = Color(0xFF081526),
    onSecondary = Color(0xFF061A12),
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFCBD5E1),
    onSurfaceVariant = Color(0xFF94A3B8),
)

@Composable
fun MAVLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MavLabColorScheme,
        content = content,
    )
}
