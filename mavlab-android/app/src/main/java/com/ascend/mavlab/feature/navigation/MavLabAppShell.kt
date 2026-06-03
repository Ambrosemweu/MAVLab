package com.ascend.mavlab.feature.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ascend.mavlab.feature.controller.ControllerScreen
import com.ascend.mavlab.feature.dashboard.DashboardScreen
import com.ascend.mavlab.feature.drone3d.Drone3DScreen
import com.ascend.mavlab.feature.labs.LabsScreen
import com.ascend.mavlab.feature.settings.SettingsScreen

private enum class MavLabTab(val label: String) {
    Dashboard("Dashboard"),
    Controller("Controller"),
    Drone3D("3D View"),
    Labs("Labs"),
    Settings("Settings"),
}

@Composable
fun MavLabAppShell() {
    var selectedTab by remember { mutableStateOf(MavLabTab.Dashboard) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MavLabTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        icon = { Text(tab.label.take(1)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            MavLabTab.Dashboard -> DashboardScreen(modifier)
            MavLabTab.Controller -> ControllerScreen(modifier)
            MavLabTab.Drone3D -> Drone3DScreen(modifier)
            MavLabTab.Labs -> LabsScreen(modifier)
            MavLabTab.Settings -> SettingsScreen(modifier)
        }
    }
}
