package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkAquariumColorScheme = darkColorScheme(
    primary = TurquoisePrimary,
    onPrimary = Color(0xFF00201D),
    primaryContainer = Color(0xFF004F46),
    onPrimaryContainer = TurquoiseGlow,
    secondary = TurquoiseSecondary,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF0D3B47),
    onSecondaryContainer = Color(0xFFA5EDF7),
    tertiary = CoralAccent,
    background = OceanDeepDark,
    onBackground = CleanWhite,
    surface = OceanSurfaceDark,
    onSurface = CleanWhite,
    surfaceVariant = OceanCardDark,
    onSurfaceVariant = MutedAquaText,
    outline = OceanBorderDark
)

private val LightAquariumColorScheme = lightColorScheme(
    primary = OceanTealPrimary,
    onPrimary = CleanWhite,
    primaryContainer = Color(0xFF70F7E2),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF028090),
    onSecondary = CleanWhite,
    background = OceanLightBg,
    onBackground = OceanDarkText,
    surface = OceanLightSurface,
    onSurface = OceanDarkText,
    surfaceVariant = OceanLightCard,
    onSurfaceVariant = Color(0xFF33586C),
    outline = Color(0xFFB5D3E3)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to deep blue aquarium dark theme for stunning immersion
    dynamicColor: Boolean = false, // Keep intentional turquoise/navy palette
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkAquariumColorScheme else LightAquariumColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
