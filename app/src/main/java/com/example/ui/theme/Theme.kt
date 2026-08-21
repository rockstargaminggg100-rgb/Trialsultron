package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val UltronColorScheme =
  darkColorScheme(
    primary = SignalOrange,
    onPrimary = UltronBackground,
    secondary = SalmonAccent,
    onSecondary = UltronBackground,
    background = UltronBackground,
    onBackground = TextPrimary,
    surface = UltronSurface,
    onSurface = TextPrimary,
    surfaceVariant = UltronSurfaceHighlight,
    onSurfaceVariant = TextSecondary,
    outline = UltronBorder,
    outlineVariant = UltronBorderSubtle,
  )

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = UltronColorScheme, typography = Typography, content = content)
}

