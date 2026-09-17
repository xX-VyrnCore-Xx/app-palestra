package com.vyrncore.palestra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Violet50,
    onPrimary = Neutral10,
    primaryContainer = Indigo40,
    onPrimaryContainer = Indigo90,
    secondary = Lime50,
    onSecondary = Indigo10,
    secondaryContainer = Indigo30,
    onSecondaryContainer = Lime80,
    tertiary = Coral50,
    background = Indigo10,
    onBackground = Neutral90,
    surface = Indigo20,
    onSurface = Neutral90,
    surfaceVariant = Indigo30,
    onSurfaceVariant = Indigo80,
    outline = Indigo40,
    error = Coral50,
)

private val LightColors = lightColorScheme(
    primary = Violet40,
    onPrimary = Neutral99,
    primaryContainer = Indigo95,
    onPrimaryContainer = Indigo20,
    secondary = Lime40,
    onSecondary = Indigo10,
    secondaryContainer = Lime80,
    onSecondaryContainer = Indigo10,
    tertiary = Coral50,
    background = Neutral95,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Indigo95,
    onSurfaceVariant = Indigo30,
    outline = Indigo80,
    error = Coral50,
)

@Composable
fun PalestraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PalestraTypography,
        shapes = PalestraShapes,
        content = content,
    )
}
