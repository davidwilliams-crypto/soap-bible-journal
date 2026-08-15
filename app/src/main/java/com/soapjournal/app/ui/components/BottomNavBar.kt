package com.soapjournal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.soapjournal.app.ui.theme.LocalJournalSurfaces

enum class BottomTab(val label: String) {
    HOME("Home"),
    BIBLE("Bible"),
    JOURNAL("Journal"),
    PLAN("Plan"),
    MORE("More")
}

/** Persistent bottom tab bar across the five top-level Nocturne destinations. */
@Composable
fun NocturneBottomBar(
    selected: BottomTab,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaces = LocalJournalSurfaces.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaces.paperDeep)
            .navigationBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomTab.entries.forEach { tab ->
            BottomTabItem(
                tab = tab,
                active = tab == selected,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RowScope.BottomTabItem(
    tab: BottomTab,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val (filled, outline) = iconsFor(tab)
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (active) filled else outline,
            contentDescription = tab.label,
            tint = color,
            modifier = Modifier.padding(bottom = 3.dp)
        )
        Text(
            tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

private fun iconsFor(tab: BottomTab): Pair<ImageVector, ImageVector> = when (tab) {
    BottomTab.HOME -> Icons.Filled.Home to Icons.Outlined.Home
    BottomTab.BIBLE -> Icons.Filled.MenuBook to Icons.Outlined.MenuBook
    BottomTab.JOURNAL -> Icons.Filled.Notes to Icons.Outlined.Notes
    BottomTab.PLAN -> Icons.Filled.CalendarMonth to Icons.Outlined.CalendarMonth
    BottomTab.MORE -> Icons.Filled.MoreHoriz to Icons.Outlined.MoreHoriz
}
