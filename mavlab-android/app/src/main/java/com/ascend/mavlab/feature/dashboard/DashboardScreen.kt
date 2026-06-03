package com.ascend.mavlab.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ascend.mavlab.core.ui.components.PlaceholderPanel

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    PlaceholderPanel(
        title = "Dashboard",
        description = "Phase 0 placeholder. Phase 3 will show telemetry cards and bounded-rate charts.",
        modifier = modifier,
    )
}
