package com.ascend.mavlab.feature.labs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ascend.mavlab.core.ui.components.PlaceholderPanel

@Composable
fun LabsScreen(modifier: Modifier = Modifier) {
    PlaceholderPanel(
        title = "Labs",
        description = "Phase 0 placeholder. Later phases add MAVLink, PID, sensor, failure, and mission labs.",
        modifier = modifier,
    )
}
