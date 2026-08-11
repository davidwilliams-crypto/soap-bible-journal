package com.soapjournal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class JournalSurfaces(
    val paper: Color,
    val paperDeep: Color,
    val rule: Color,
    val margin: Color,
    val ink: Color,
    val jesusRed: Color
)

val LocalJournalSurfaces = staticCompositionLocalOf {
    JournalSurfaces(
        paper = Paper,
        paperDeep = PaperDeep,
        rule = PaperDark,
        margin = PaperMargin,
        ink = InkBrown,
        jesusRed = JesusRed
    )
}

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
    surfaceVariant = PaperDeep,
    onSurfaceVariant = InkBrown.copy(alpha = 0.72f),
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

private val LightJournal = JournalSurfaces(
    paper = Paper,
    paperDeep = PaperDeep,
    rule = PaperDark,
    margin = PaperMargin,
    ink = InkBrown,
    jesusRed = JesusRed
)

private val DarkJournal = JournalSurfaces(
    paper = NightPaper,
    paperDeep = NightSurface,
    rule = NightPaperRule,
    margin = NightMist,
    ink = NightInk,
    jesusRed = NightJesusRed
)

@Composable
fun SOAPBibleJournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalJournalSurfaces provides if (darkTheme) DarkJournal else LightJournal
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            content = content
        )
    }
}
