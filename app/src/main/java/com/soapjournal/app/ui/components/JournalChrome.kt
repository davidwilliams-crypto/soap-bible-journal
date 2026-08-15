package com.soapjournal.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.soapjournal.app.ui.theme.LocalJournalSurfaces
import kotlinx.coroutines.delay

/**
 * Full-bleed warm paper wash — the visual plane for ritual screens.
 * Calm design: atmosphere without chrome cards.
 */
@Composable
fun JournalAtmosphere(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val surfaces = LocalJournalSurfaces.current
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        surfaces.paper,
                        scheme.background,
                        surfaces.paperDeep.copy(alpha = 0.85f)
                    )
                )
            )
    ) {
        content()
    }
}

/**
 * Subtle entrance for the first composition (presence, not noise).
 */
@Composable
fun RitualEnter(
    content: @Composable ColumnScope.() -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(40)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(520)) + slideInVertically(
            animationSpec = tween(520),
            initialOffsetY = { it / 28 }
        ),
        exit = fadeOut(tween(200))
    ) {
        Column(content = content)
    }
}

/** Quiet scripture quotation block — accent rule, outline eyebrow tag, no card chrome. */
@Composable
fun ScriptureQuotation(
    eyebrow: String,
    reference: String? = null,
    modifier: Modifier = Modifier,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (eyebrow.isNotBlank()) {
            Tag(eyebrow, style = TagStyle.Outline)
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(10.dp)
            )
        }
        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, end = 14.dp)
                    .width(2.dp)
                    .height(56.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, Color.Transparent)
                        )
                    )
            )
            Column(modifier = Modifier.weight(1f)) {
                if (!reference.isNullOrBlank()) {
                    Text(
                        reference,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.height(6.dp)
                    )
                }
                body()
                footer?.invoke(this)
            }
        }
    }
}

@Composable
fun ConfirmActionDialog(
    title: String,
    body: String,
    confirmLabel: String = "Confirm",
    destructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/** Celebrates reaching a streak milestone (7/30/100/365 days). */
@Composable
fun MilestoneCelebrationDialog(days: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("$days days in the Word", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(
                "You've kept a $days-day SOAP rhythm. “Let us not grow weary of doing good, " +
                    "for in due season we will reap, if we do not give up.” — Galatians 6:9",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep going", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}
