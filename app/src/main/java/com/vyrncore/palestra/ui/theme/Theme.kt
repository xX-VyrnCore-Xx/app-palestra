package com.vyrncore.palestra.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val PalestraOrange = Color(0xFFFF5722)
private val PalestraOrangeDark = Color(0xFFBF360C)
private val PalestraDark = Color(0xFF121212)

private val LightColors = lightColorScheme(
    primary = PalestraOrange,
    secondary = PalestraOrangeDark,
    background = Color(0xFFFAFAFA),
)

private val DarkColors = darkColorScheme(
    primary = PalestraOrange,
    secondary = PalestraOrangeDark,
    background = PalestraDark,
)

@Composable
fun PalestraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PalestraTypography,
        content = content,
    )
}
