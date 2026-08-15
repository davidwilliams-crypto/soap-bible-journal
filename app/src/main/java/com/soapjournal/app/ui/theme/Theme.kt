package com.soapjournal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nocturne surfaces beyond the M3 [MaterialTheme.colorScheme] — background/divider/accent-300
 * and the app-specific words-of-Christ red, which isn't a Nocturne token.
 */
data class JournalSurfaces(
    val paper: Color,
    val paperDeep: Color,
    val rule: Color,
    val margin: Color,
    val ink: Color,
    val jesusRed: Color,
    val accent300: Color
)

val LocalJournalSurfaces = staticCompositionLocalOf {
    JournalSurfaces(
        paper = NocturneBg,
        paperDeep = NocturneSurface,
        rule = NocturneDivider,
        margin = NocturneMuted,
        ink = NocturneText,
        jesusRed = NocturneJesusRed,
        accent300 = NocturneAccent300
    )
}

/** Nocturne's compact spacing scale — space-1 (2.8dp) … space-8 (22.4dp). */
data class NocturneSpacing(val unit: Dp = 2.8.dp) {
    val space1 get() = unit * 1
    val space2 get() = unit * 2
    val space3 get() = unit * 3
    val space4 get() = unit * 4
    val space5 get() = unit * 5
    val space6 get() = unit * 6
    val space7 get() = unit * 7
    val space8 get() = unit * 8
}

data class NocturneRadius(
    val sm: Dp = 4.dp,
    val md: Dp = 8.dp,
    val lg: Dp = 14.dp
)

val LocalNocturneSpacing = staticCompositionLocalOf { NocturneSpacing() }
val LocalNocturneRadius = staticCompositionLocalOf { NocturneRadius() }

private val DarkColors = darkColorScheme(
    primary = NocturneAccent,
    onPrimary = NocturneBg,
    primaryContainer = NocturneAccent.copy(alpha = 0.16f),
    onPrimaryContainer = NocturneAccent300,
    secondary = NocturneAccent300,
    onSecondary = NocturneBg,
    background = NocturneBg,
    onBackground = NocturneText,
    surface = NocturneSurface,
    onSurface = NocturneText,
    surfaceVariant = NocturneSurface,
    onSurfaceVariant = NocturneText.copy(alpha = 0.6f),
    outline = NocturneDivider,
    error = NocturneDanger
)

private val LightColors = lightColorScheme(
    primary = PaperAccent,
    onPrimary = Color.White,
    primaryContainer = PaperAccent.copy(alpha = 0.14f),
    onPrimaryContainer = PaperAccent300,
    secondary = PaperAccent300,
    onSecondary = Color.White,
    background = PaperBg,
    onBackground = PaperText,
    surface = PaperSurface,
    onSurface = PaperText,
    surfaceVariant = PaperSurface,
    onSurfaceVariant = PaperText.copy(alpha = 0.6f),
    outline = PaperDivider,
    error = PaperDanger
)

private val DarkJournal = JournalSurfaces(
    paper = NocturneBg,
    paperDeep = NocturneSurface,
    rule = NocturneDivider,
    margin = NocturneMuted,
    ink = NocturneText,
    jesusRed = NocturneJesusRed,
    accent300 = NocturneAccent300
)

private val LightJournal = JournalSurfaces(
    paper = PaperBg,
    paperDeep = PaperSurface,
    rule = PaperDivider,
    margin = PaperMuted,
    ink = PaperText,
    jesusRed = PaperJesusRed,
    accent300 = PaperAccent300
)

@Composable
fun SOAPBibleJournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalJournalSurfaces provides if (darkTheme) DarkJournal else LightJournal,
        LocalNocturneSpacing provides NocturneSpacing(),
        LocalNocturneRadius provides NocturneRadius()
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            content = content
        )
    }
}
