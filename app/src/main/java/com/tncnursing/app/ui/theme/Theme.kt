package com.tncnursing.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TncTealAccent,
    onPrimary = TncNavyDark,
    primaryContainer = TncNavyPrimary,
    onPrimaryContainer = TncBlueContainer,
    secondary = TncAmberSecondary,
    onSecondary = TncNavyDark,
    background = SurfaceDark,
    surface = CardDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = TncRedError
)

private val LightColorScheme = lightColorScheme(
    primary = TncNavyPrimary,
    onPrimary = CardLight,
    primaryContainer = TncBlueContainer,
    onPrimaryContainer = TncNavyPrimary,
    secondary = TncAmberSecondary,
    onSecondary = TncNavyDark,
    background = SurfaceLight,
    surface = CardLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = TncRedError
)

@Composable
fun TncNursingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
