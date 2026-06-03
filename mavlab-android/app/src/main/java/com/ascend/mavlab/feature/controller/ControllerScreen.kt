package com.ascend.mavlab.feature.controller

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ascend.mavlab.core.ui.components.PlaceholderPanel

@Composable
fun ControllerScreen(modifier: Modifier = Modifier) {
    PlaceholderPanel(
        title = "Controller",
        description = "Phase 0 placeholder. Phase 3 will add tilt control with sensor fallback and on-screen controls.",
        modifier = modifier,
    )
}
