package com.ascend.mavlab.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MavLabColorScheme = darkColorScheme(
    primary = Color(0xFF66D9C7),
    secondary = Color(0xFF9DB7FF),
    tertiary = Color(0xFFF1C27D),
    background = Color(0xFF101418),
    surface = Color(0xFF161B20),
    onPrimary = Color(0xFF07342D),
    onSecondary = Color(0xFF17224B),
    onBackground = Color(0xFFE8EEF2),
    onSurface = Color(0xFFE8EEF2),
)

@Composable
fun MAVLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MavLabColorScheme,
        content = content,
    )
}
