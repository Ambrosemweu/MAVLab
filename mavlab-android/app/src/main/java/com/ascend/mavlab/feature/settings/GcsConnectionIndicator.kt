package com.ascend.mavlab.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Link presence only: validated GCS heartbeats do not grant flight authority. */
@Composable
internal fun GcsConnectionIndicator(connected: Boolean, modifier: Modifier = Modifier) {
    val color = if (connected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
    Row(
        modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape).testTag("gcs-connection-dot"))
        Text(
            if (connected) "GCS connected" else "GCS disconnected",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
