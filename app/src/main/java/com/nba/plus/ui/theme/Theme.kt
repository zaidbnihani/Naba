package com.nba.plus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = PurpleContainer,
    onPrimaryContainer = OnPurpleContainer,
    secondary = PurpleLight,
    onSecondary = Color(0xFF1B1740),
    secondaryContainer = PurpleContainer,
    onSecondaryContainer = OnPurpleContainer,
    tertiary = GoldAccent,
    onTertiary = Color(0xFF2A1F05),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = Purple,
    inverseSurface = Color(0xFFF0F0F5),
    inverseOnSurface = Color(0xFF1A1C2B),
    error = DarkError,
    onError = DarkOnError,
    errorContainer = Color(0xFF3D1220),
    onErrorContainer = Color(0xFFFFB2BB),
    outline = DarkOutline,
    outlineVariant = Color(0xFF2C2F48),
    scrim = DarkScrim,
)

private val LightColors = lightColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E0FF),
    onPrimaryContainer = Color(0xFF1B1259),
    secondary = PurpleDark,
    onSecondary = Color.White,
    tertiary = Color(0xFF8A5A00),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = LightError,
    onError = Color.White,
    outline = LightOutline,
)

@Composable
fun NbaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
