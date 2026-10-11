package com.ascend.mavlab.feature.navigation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CompactNavigationBar(selected: MavLabTab, onSelect: (MavLabTab) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(Modifier.fillMaxWidth().windowInsetsPadding(
            WindowInsets.safeDrawing.union(WindowInsets.mandatorySystemGestures)
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        )) {
            val dockWidth = maxOf(maxWidth, 192.dp)
            Row(
                Modifier.horizontalScroll(rememberScrollState()).width(dockWidth).selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MavLabTab.entries.forEach { tab ->
                    val active = selected == tab
                    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        Modifier.weight(1f).heightIn(min = 56.dp)
                            .selectable(selected = active, role = Role.Tab, onClick = { onSelect(tab) })
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    ) {
                        Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                        Text(tab.label, color = color, fontSize = 12.sp, lineHeight = 14.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
