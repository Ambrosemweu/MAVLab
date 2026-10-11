package com.ascend.mavlab.feature.controller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ascend.mavlab.core.common.ControllerInputMode
import com.ascend.mavlab.core.ui.theme.MAVLabTheme
import com.ascend.mavlab.feature.dashboard.CockpitDataFreshness
import com.ascend.mavlab.simulation.engine.DroneState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ControllerLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val clicks = mutableListOf<ControllerAction>()
    private val ui = ControllerUiState(
        CockpitDataFreshness.LIVE, "Preview", null, null, false,
        false, true, true, true,
        ControllerActionSpec(ControllerAction.ARM, "Arm"),
        ControllerActionSpec(ControllerAction.CALIBRATE, "Calibrate"),
        "Calibrate neutral before arming. Arming and takeoff are separate.", true,
    )

    private fun content(width: Int, height: Int, scale: Float, state: ControllerUiState = ui) {
        compose.setContent {
            // Small physical test viewport so both orientations fit the portrait host.
            CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                MAVLabTheme {
                    Column(Modifier.size(width.dp, height.dp).testTag("frame")) {
                        ControllerContentLayout(
                            modifier = Modifier.weight(1f),
                            status = { compact -> ControllerStatus(DroneState(), state, ControllerInputMode.PHONE_SENSORS, false, 0, compact) },
                            actions = { modifier, stacked -> ControllerActionDock(state, 10f, { clicks.add(it) }, modifier, stacked) },
                            controls = { modifier ->
                                Column(modifier.verticalScroll(rememberScrollState())) {
                                    repeat(20) { Text("Control row $it", Modifier.padding(12.dp)) }
                                }
                            },
                        )
                        Box(Modifier.fillMaxWidth().height(48.dp).testTag("navigation")) { Text("Cockpit · Control · SIM · Ops") }
                    }
                }
            }
        }
    }

    private fun assertWholeActionAboveNavigation(label: String) {
        val action = compose.onNodeWithText(label)
        action.assertIsDisplayed()
        val full = action.getUnclippedBoundsInRoot()
        val clipped = action.getBoundsInRoot()
        val nav = compose.onNodeWithTag("navigation").getUnclippedBoundsInRoot()
        assertEquals((full.bottom - full.top).value, (clipped.bottom - clipped.top).value, .5f)
        assertTrue("$label must not be covered by navigation", full.bottom <= nav.top)
        action.performClick()
    }

    @Test fun largeTextLandscapeKeepsBothActionsFullyVisibleAndClickable() {
        content(720, 320, 1.5f)
        assertWholeActionAboveNavigation("Arm")
        assertWholeActionAboveNavigation("Calibrate")
        assertEquals(listOf(ControllerAction.ARM, ControllerAction.CALIBRATE), clicks)
    }

    @Test fun shortLandscapeUsesTheBottomDockEvenAtNormalTextSize() {
        content(720, 250, 1f)
        assertWholeActionAboveNavigation("Arm")
        assertWholeActionAboveNavigation("Calibrate")
    }

    @Test fun regularLandscapeStillSupportsTheSideDock() {
        content(720, 360, .8f)
        assertWholeActionAboveNavigation("Arm")
        assertWholeActionAboveNavigation("Calibrate")
    }

    @Test fun largePortraitDoesNotSplitTheVehicleStatusWord() {
        content(360, 650, 1.5f)
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Disarmed").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1, layouts.single().lineCount)
        assertWholeActionAboveNavigation("Arm")
        assertWholeActionAboveNavigation("Calibrate")
    }

    @Test fun largeTextWarningAndLongActionRemainAccessible() {
        content(720, 360, 1.5f, ui.copy(
            warning = "GCS peer absent · authority has not automatically changed",
            primaryAction = ControllerActionSpec(ControllerAction.TAKE_CONTROL_AND_LAND, "Take control & land"),
            secondaryAction = ControllerActionSpec(ControllerAction.OPEN_OPS, "Open Ops"),
            actionHint = "Local landing will replace GCS control. Confirm before taking over.",
        ))
        assertWholeActionAboveNavigation("Take control & land")
        assertWholeActionAboveNavigation("Open Ops")
    }
}
