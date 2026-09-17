package com.vyrncore.palestra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.vyrncore.palestra.data.repository.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Magenta60,
    onPrimary = Neutral10,
    primaryContainer = Violet30,
    onPrimaryContainer = Violet90,
    secondary = Orange50,
    onSecondary = Violet10,
    secondaryContainer = Violet30,
    onSecondaryContainer = Orange80,
    tertiary = Lime50,
    onTertiary = Violet10,
    tertiaryContainer = Violet30,
    onTertiaryContainer = Lime80,
    background = Violet10,
    onBackground = Neutral90,
    surface = Violet20,
    onSurface = Neutral90,
    surfaceVariant = Violet30,
    onSurfaceVariant = Violet80,
    outline = Violet40,
    error = Coral50,
)

private val LightColors = lightColorScheme(
    primary = Magenta50,
    onPrimary = Neutral99,
    primaryContainer = Violet95,
    onPrimaryContainer = Violet20,
    secondary = Orange40,
    onSecondary = Neutral99,
    secondaryContainer = Orange80,
    onSecondaryContainer = Violet10,
    tertiary = Lime40,
    onTertiary = Violet10,
    tertiaryContainer = Lime80,
    onTertiaryContainer = Violet10,
    background = Neutral95,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Violet95,
    onSurfaceVariant = Violet30,
    outline = Violet80,
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
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PalestraTypography,
        shapes = PalestraShapes,
        content = content,
    )
}
