package com.soapjournal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Olive,
    onPrimary = Color.White,
    primaryContainer = Mist,
    onPrimaryContainer = OliveDark,
    secondary = SoftClay,
    onSecondary = Color.White,
    background = Paper,
    onBackground = InkBrown,
    surface = Paper,
    onSurface = InkBrown,
    surfaceVariant = PaperDark,
    onSurfaceVariant = InkBrown.copy(alpha = 0.75f),
    outline = Mist,
    error = Danger
)

private val DarkColors = darkColorScheme(
    primary = NightOlive,
    onPrimary = NightBg,
    primaryContainer = NightMist,
    onPrimaryContainer = NightInk,
    secondary = SoftClay,
    onSecondary = NightInk,
    background = NightBg,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightMist,
    onSurfaceVariant = NightInk.copy(alpha = 0.75f),
    outline = NightMist,
    error = Danger
)

@Composable
fun SOAPBibleJournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
