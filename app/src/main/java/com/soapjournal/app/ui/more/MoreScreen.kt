package com.soapjournal.app.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.soapjournal.app.ui.theme.LocalJournalSurfaces
import com.soapjournal.app.ui.theme.LocalNocturneRadius

private data class MoreRow(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val onClick: () -> Unit
)

/** Nocturne "More" hub — consolidates Insights, Memorize and Settings under one tab. */
@Composable
fun MoreScreen(
    onOpenInsights: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val surfaces = LocalJournalSurfaces.current
    val rows = listOf(
        MoreRow(
            icon = Icons.Outlined.TrendingUp,
            title = "Insights",
            description = "Streaks, rhythm and reflection stats",
            onClick = onOpenInsights
        ),
        MoreRow(
            icon = Icons.Outlined.Psychology,
            title = "Memorize",
            description = "Verses you've saved for practice",
            onClick = onOpenMemory
        ),
        MoreRow(
            icon = Icons.Outlined.Settings,
            title = "Settings",
            description = "Reading, reminders and data",
            onClick = onOpenSettings
        )
    )

    Scaffold(containerColor = surfaces.paper) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                "More",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
            rows.forEach { row ->
                MoreHubRow(row)
            }
        }
    }
}

@Composable
private fun MoreHubRow(row: MoreRow) {
    val radius = LocalNocturneRadius.current
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = row.onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(accent.copy(alpha = 0.16f), RoundedCornerShape(radius.md)),
            contentAlignment = Alignment.Center
        ) {
            Icon(row.icon, contentDescription = null, tint = accent)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(row.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                row.description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
