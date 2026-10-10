package com.ascend.mavlab.feature.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.ascend.mavlab.feature.controller.ControllerScreen
import com.ascend.mavlab.feature.dashboard.CockpitScreen
import com.ascend.mavlab.feature.drone3d.Drone3DScreen
import com.ascend.mavlab.feature.onboarding.OnboardingScreen
import com.ascend.mavlab.feature.settings.SettingsScreen

private enum class MavLabTab(
    val label: String,
    val icon: ImageVector,
) {
    Cockpit(
        label = "Cockpit",
        icon = Icons.Filled.Analytics,
    ),
    Control(
        label = "Control",
        icon = Icons.Filled.ControlCamera,
    ),
    Sim(
        label = "SIM",
        icon = Icons.Filled.Height,
    ),
    Ops(
        label = "Ops",
        icon = Icons.Filled.Settings,
    ),
}

@Composable
fun MavLabAppShell() {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("mavlab_app", android.content.Context.MODE_PRIVATE)
    }
    var onboardingComplete by remember {
        mutableStateOf(preferences.getBoolean("onboarding_complete", false))
    }
    var selectedTab by rememberSaveable { mutableStateOf(MavLabTab.Cockpit) }
    var openMissionRoute by rememberSaveable { mutableStateOf(false) }
    var openMissionDetails by rememberSaveable { mutableStateOf(false) }

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
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    MavLabTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { openMissionDetails = false; openMissionRoute = false; selectedTab = tab },
                            label = { Text(tab.label) },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
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
            MavLabTab.Cockpit -> CockpitScreen(
                modifier = modifier,
                onOpenController = { selectedTab = MavLabTab.Control },
                onOpenSim = { openMissionRoute = false; selectedTab = MavLabTab.Sim },
                onOpenMission = { openMissionDetails = true; selectedTab = MavLabTab.Control },
                onOpenOps = { selectedTab = MavLabTab.Ops },
            )
            MavLabTab.Control -> ControllerScreen(
                modifier = modifier,
                missionInitiallyExpanded = openMissionDetails,
                onOpenSim = { openMissionRoute = true; selectedTab = MavLabTab.Sim },
                onOpenOps = { selectedTab = MavLabTab.Ops },
            )
            MavLabTab.Sim -> Drone3DScreen(modifier, routeInitiallyOpen = openMissionRoute)
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
