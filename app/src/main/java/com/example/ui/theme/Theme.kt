package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentTeal,
    onPrimary = DeepNavy,
    primaryContainer = SlateNavy,
    onPrimaryContainer = PureWhite,
    secondary = AccentTeal,
    onSecondary = DeepNavy,
    error = DangerRed,
    background = DeepNavy,
    surface = SlateNavy,
    onBackground = PureWhite,
    onSurface = PureWhite
)

private val LightColorScheme = lightColorScheme(
    primary = DeepNavy,
    onPrimary = PureWhite,
    primaryContainer = SlateNavy,
    onPrimaryContainer = PureWhite,
    secondary = SlateNavy,
    onSecondary = PureWhite,
    error = DangerRed,
    background = SurfaceLight,
    surface = PureWhite,
    surfaceVariant = SurfaceVariantLight,
    onBackground = NeutralDark,
    onSurface = NeutralDark,
    onSurfaceVariant = NeutralMedium
)

@Composable
fun JagrukTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    JagrukTheme(darkTheme = darkTheme, content = content)
}
