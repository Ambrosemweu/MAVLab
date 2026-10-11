package com.ascend.mavlab.feature.navigation

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.ascend.mavlab.core.ui.navigation.LocalScreenSwipeAreas
import com.ascend.mavlab.core.ui.navigation.ScreenSwipeAreas
import com.ascend.mavlab.core.ui.navigation.ScreenSwipePager
import com.ascend.mavlab.core.ui.navigation.screenSwipeNavigation
import com.ascend.mavlab.core.ui.navigation.rememberScreenSwipeMotion
import com.ascend.mavlab.feature.controller.ControllerScreen
import com.ascend.mavlab.feature.dashboard.CockpitScreen
import com.ascend.mavlab.feature.drone3d.Drone3DScreen
import com.ascend.mavlab.feature.onboarding.OnboardingScreen
import com.ascend.mavlab.feature.settings.SettingsScreen

internal enum class MavLabTab(
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
fun MavLabAppShell(fullScreen: Boolean = false, onFullScreenChange: (Boolean) -> Unit = {}) {
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
    val screenStates = rememberSaveableStateHolder()
    val swipeAreas = remember { ScreenSwipeAreas() }
    val density = LocalDensity.current
    val accessibility = remember(context) { context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager }
    var touchExploration by remember(accessibility) { mutableStateOf(accessibility.isTouchExplorationEnabled) }
    DisposableEffect(accessibility) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { touchExploration = it }
        accessibility.addTouchExplorationStateChangeListener(listener)
        onDispose { accessibility.removeTouchExplorationStateChangeListener(listener) }
    }
    val keyboardVisible = WindowInsets.ime.getBottom(density) > 0
    val systemGestures = WindowInsets.systemGestures
    val direction = LocalLayoutDirection.current
    val edgeGuard = maxOf(systemGestures.getLeft(density, direction), systemGestures.getRight(density, direction), with(density) { 24.dp.toPx().toInt() }).toFloat()
    val swipeMotion = rememberScreenSwipeMotion(selectedTab.ordinal, MavLabTab.entries.size)
    fun openTab(tab: MavLabTab, mission: Boolean = false, route: Boolean = false) {
        openMissionDetails = mission
        openMissionRoute = route
        if (tab != selectedTab) {
            swipeMotion.select(tab.ordinal)
            selectedTab = tab
        }
    }

    if (!onboardingComplete) {
        OnboardingScreen(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
            onComplete = {
                preferences.edit().putBoolean("onboarding_complete", true).apply()
                onboardingComplete = true
            },
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        bottomBar = {
            CompactNavigationBar(selectedTab) { if (it != selectedTab) openTab(it) }
        },
    ) { innerPadding ->
        CompositionLocalProvider(LocalScreenSwipeAreas provides swipeAreas) {
            Box(Modifier.padding(innerPadding).consumeWindowInsets(innerPadding).fillMaxSize().clipToBounds()
                .screenSwipeNavigation(swipeAreas, !touchExploration && !keyboardVisible,
                    selectedTab.ordinal, MavLabTab.entries.size, edgeGuard,
                    onDrag = swipeMotion::drag,
                    onDragEnd = swipeMotion::finish,
                ) { openTab(MavLabTab.entries[it]) }) {
              // The pager moves both visible shells continuously; it never owns flight input.
              ScreenSwipePager(swipeMotion) { page, active, settled ->
                val tab = MavLabTab.entries[page]
                screenStates.SaveableStateProvider(tab.name) {
                    when (tab) {
                        MavLabTab.Cockpit -> CockpitScreen(
                            onOpenController = { openTab(MavLabTab.Control) },
                            onOpenSim = { openTab(MavLabTab.Sim) },
                            onOpenMission = { openTab(MavLabTab.Control, mission = true) },
                            onOpenOps = { openTab(MavLabTab.Ops) },
                        )
                        MavLabTab.Control -> ControllerScreen(
                            missionInitiallyExpanded = active && openMissionDetails,
                            onMissionDetailsShown = { openMissionDetails = false },
                            onOpenSim = { openTab(MavLabTab.Sim, route = true) },
                            onOpenOps = { openTab(MavLabTab.Ops) },
                        )
                        MavLabTab.Sim -> Drone3DScreen(
                            routeInitiallyOpen = active && openMissionRoute,
                            onRouteShown = { openMissionRoute = false },
                            allowRendererStart = settled,
                            rendererVisible = swipeMotion.pager.layoutInfo.visiblePagesInfo.any { it.index == page },
                        )
                        MavLabTab.Ops -> SettingsScreen(
                            fullScreen = fullScreen,
                            onFullScreenChange = onFullScreenChange,
                            onOpenController = { openTab(MavLabTab.Control) },
                            onReplayOnboarding = {
                                preferences.edit().putBoolean("onboarding_complete", false).apply()
                                onboardingComplete = false
                            },
                        )
                    }
                }
              }
            }
        }
    }
}
