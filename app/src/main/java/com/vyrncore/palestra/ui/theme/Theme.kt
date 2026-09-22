package com.vyrncore.palestra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.vyrncore.palestra.data.repository.ThemeMode

// Visible-on-white warm gray for text field borders and dividers in light mode - the pale
// brand orange would wash out there.
private val WarmGray40 = Color(0xFF8A7566)

private val DarkColors = darkColorScheme(
    primary = Orange50,
    onPrimary = Neutral10,
    primaryContainer = OrangeDeep,
    onPrimaryContainer = Orange95,
    secondary = Orange60,
    onSecondary = Neutral10,
    secondaryContainer = Violet30,
    onSecondaryContainer = Orange80,
    tertiary = Lime50,
    onTertiary = Neutral10,
    tertiaryContainer = Violet30,
    onTertiaryContainer = Lime80,
    background = Violet10,
    onBackground = Neutral90,
    surface = Violet20,
    onSurface = Neutral90,
    surfaceVariant = Violet30,
    onSurfaceVariant = Violet40,
    outline = Violet40,
    error = Coral50,
)

private val LightColors = lightColorScheme(
    primary = OrangeDeep,
    onPrimary = Neutral99,
    primaryContainer = Orange95,
    onPrimaryContainer = Neutral10,
    secondary = Orange50,
    onSecondary = Neutral99,
    secondaryContainer = Orange80,
    onSecondaryContainer = Neutral10,
    tertiary = Lime40,
    onTertiary = Neutral10,
    tertiaryContainer = Lime80,
    onTertiaryContainer = Neutral10,
    background = Neutral95,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Orange95,
    onSurfaceVariant = Neutral20,
    outline = WarmGray40,
    error = Coral50,
)

@Composable
fun PalestraTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PalestraTypography,
        shapes = PalestraShapes,
        content = content,
    )
}
