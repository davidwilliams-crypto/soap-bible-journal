package com.soapjournal.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.soapjournal.app.ui.theme.LocalJournalSurfaces
import com.soapjournal.app.ui.theme.LocalNocturneRadius

/**
 * `.btn-primary` — outlined Button with accent border, transparent fill, accent text.
 * The single high-emphasis call-to-action per screen.
 */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val radius = LocalNocturneRadius.current
    val accent = MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(radius.md),
        border = BorderStroke(1.dp, if (enabled) accent else accent.copy(alpha = 0.4f)),
        colors = OutlinedButtonDefaults.outlinedButtonColors(contentColor = accent),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        content = content
    )
}

/**
 * `.btn-secondary` — bordered pill in the neutral divider color. Used for filter/nav
 * pills (book & chapter pickers, quick links) that don't carry primary emphasis.
 */
@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val radius = LocalNocturneRadius.current
    val surfaces = LocalJournalSurfaces.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(radius.md),
        border = BorderStroke(1.dp, surfaces.rule),
        colors = OutlinedButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        content = content
    )
}

enum class TagStyle { Outline, Accent, Neutral }

/** `.tag` — small pill label (status, day counters, frequent-tag chips). */
@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    style: TagStyle = TagStyle.Neutral
) {
    val surfaces = LocalJournalSurfaces.current
    val accent = MaterialTheme.colorScheme.primary
    val (container, content, borderColor) = when (style) {
        TagStyle.Outline -> Triple(Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant, surfaces.rule)
        TagStyle.Accent -> Triple(accent.copy(alpha = 0.16f), accent, null)
        TagStyle.Neutral -> Triple(surfaces.paperDeep, MaterialTheme.colorScheme.onSurfaceVariant, null)
    }
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .background(container, shape)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = content
        )
    }
}

/** `.card` — surface-filled panel, no heavy elevation (edge + ambient only). */
@Composable
fun NocturneCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val radius = LocalNocturneRadius.current
    val surfaces = LocalJournalSurfaces.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(radius.lg),
        colors = CardDefaults.cardColors(containerColor = surfaces.paperDeep),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/** Small uppercase eyebrow label — Nocturne kicker (`TODAY'S READING`, `UP NEXT`, ...). */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier) {
    val surfaces = LocalJournalSurfaces.current
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = surfaces.accent300,
        modifier = modifier
    )
}
