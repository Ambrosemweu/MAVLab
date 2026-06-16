package com.ascend.mavlab.feature.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.ascend.mavlab.feature.controller.ControllerScreen
import com.ascend.mavlab.feature.dashboard.DashboardScreen
import com.ascend.mavlab.feature.drone3d.Drone3DScreen
import com.ascend.mavlab.feature.mission.MissionScreen
import com.ascend.mavlab.feature.onboarding.OnboardingScreen
import com.ascend.mavlab.feature.settings.SettingsScreen

private enum class MavLabTab(
    val label: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
) {
    Cockpit(
        label = "Cockpit",
        title = "Live Operations Cockpit",
        description = "Telemetry, safety state, mission awareness",
        icon = Icons.Filled.Analytics,
    ),
    Controller(
        label = "Controller",
        title = "Local Manual Control",
        description = "Phone sensors, manual input, quick test controls",
        icon = Icons.Filled.ControlCamera,
    ),
    Mission(
        label = "Mission",
        title = "Autonomous Mission Execution",
        description = "QGC upload, waypoint progress, AUTO control",
        icon = Icons.Filled.Route,
    ),
    Sim(
        label = "SIM",
        title = "Physical Behavior Visualization",
        description = "3D attitude, altitude, motors, mission context",
        icon = Icons.Filled.Height,
    ),
    Ops(
        label = "Ops",
        title = "Diagnostics, Logs, Export",
        description = "MAVLink status, QGC setup, flight review",
        icon = Icons.Filled.Settings,
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MavLabAppShell() {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("mavlab_app", android.content.Context.MODE_PRIVATE)
    }
    var onboardingComplete by remember {
        mutableStateOf(preferences.getBoolean("onboarding_complete", false))
    }
    var selectedTab by remember { mutableStateOf(MavLabTab.Cockpit) }

    if (!onboardingComplete) {
        OnboardingScreen(
            onComplete = {
                preferences.edit().putBoolean("onboarding_complete", true).apply()
                onboardingComplete = true
            },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MAVLab by Ascend Labs", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${selectedTab.title} - ${selectedTab.description}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    MavLabTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab.label) },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            MavLabTab.Cockpit -> DashboardScreen(modifier)
            MavLabTab.Controller -> ControllerScreen(modifier)
            MavLabTab.Mission -> MissionScreen(modifier)
            MavLabTab.Sim -> Drone3DScreen(modifier)
            MavLabTab.Ops -> SettingsScreen(
                modifier = modifier,
                onReplayOnboarding = {
                    preferences.edit().putBoolean("onboarding_complete", false).apply()
                    onboardingComplete = false
                },
            )
        }
    }
}
