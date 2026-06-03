package com.ascend.mavlab.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ascend.mavlab.core.ui.components.PlaceholderPanel

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderPanel(
        title = "Settings",
        description = "Phase 0 placeholder. Phase 1 will expose MAVLink socket and system ID settings.",
        modifier = modifier,
    )
}
