package com.ascend.mavlab.feature.onboarding

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.ascend.mavlab.core.ui.theme.MAVLabTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule val compose = createComposeRule()
    private var completions = 0

    private fun content(swipeEnabled: Boolean = true) {
        compose.setContent { MAVLabTheme { OnboardingScreen({ completions++ }, swipeEnabled = swipeEnabled) } }
    }

    private fun next() { compose.onNodeWithTag("onboarding-next").performClick(); compose.waitForIdle() }
    private fun step(number: Int) { compose.onNodeWithText("$number / 9").assertIsDisplayed() }

    @Test fun nextBackAndAndroidBackShareTheSamePage() {
        content()
        step(1)
        compose.onNodeWithTag("onboarding-back").assertDoesNotExist()
        next(); step(2)
        compose.onNodeWithTag("onboarding-back").performClick(); step(1)
        next(); step(2)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        step(1)
        assertEquals(0, completions)
    }

    @Test fun contentMovesDuringSwipeAndSettlesOnNextPage() {
        content()
        val before = compose.onNodeWithText("What is MAVLab?").getUnclippedBoundsInRoot().left.value
        compose.onNodeWithTag("onboarding-pager").performTouchInput {
            down(center)
            moveBy(Offset(-width * .08f, 0f), 16)
            moveBy(Offset(-width * .35f, 0f), 100)
        }
        val held = compose.onNodeWithText("What is MAVLab?").getUnclippedBoundsInRoot().left.value
        assertTrue("Content must follow the finger before release", held < before - 20f)
        step(1)
        compose.onNodeWithTag("onboarding-pager").performTouchInput { up() }
        step(2)
        // Begin inside the app, not its deliberately protected Android gesture edge.
        compose.onNodeWithTag("onboarding-pager").performTouchInput {
            swipe(center, center + Offset(width * .25f, 0f), 180)
        }
        step(1)
    }

    @Test fun endpointsDoNotWrapOrCompleteAndFinalActionCompletesOnce() {
        content()
        compose.onNodeWithTag("onboarding-pager").performTouchInput {
            swipe(center, center + Offset(width * .25f, 0f), 180)
        }
        step(1)
        repeat(8) { next() }
        step(9)
        compose.onNodeWithTag("onboarding-pager").performTouchInput {
            swipe(center, center + Offset(-width * .25f, 0f), 180)
        }
        step(9)
        assertEquals(0, completions)
        compose.onNodeWithText("Start flying").performClick()
        assertEquals(1, completions)
    }

    @Test fun skipCompletesWithoutWalkingThroughTheTour() {
        content()
        compose.onNodeWithTag("onboarding-skip").performClick()
        assertEquals(1, completions)
    }

    @Test fun recordingPreviewUsesTheLiveChartLayoutWithoutClaimingLiveData() {
        content()
        repeat(8) { next() }
        compose.onNodeWithText("Example recording · not live telemetry · last 10 seconds")
            .performScrollTo().assertIsDisplayed()
        listOf("Altitude AGL", "Roll", "Pitch", "Yaw", "Altitude (m)", "Attitude (°)", "10 s ago", "5 s", "Now")
            .forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Start flying").assertIsDisplayed()
        assertEquals(0, completions)
    }

    @Test fun selectedPageSurvivesStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MAVLabTheme { OnboardingScreen({ completions++ }) } }
        next(); next(); step(3)
        restoration.emulateSavedInstanceStateRestore()
        step(3)
        compose.onNodeWithText("Four surfaces, one drone").assertIsDisplayed()
    }

    @Test fun demoSliderKeepsItsGestureInsteadOfChangingPages() {
        content()
        repeat(4) { next() }
        val slider = compose.onNodeWithTag("onboarding-throttle")
        // Drag the current (50%) thumb rather than starting at the screen edge.
        slider.performScrollTo().performTouchInput {
            swipe(center, Offset(width * .9f, center.y), 300)
        }
        step(5)
        assertTrue(slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current > .8f)
        assertEquals(0, completions)
    }

    @Test fun accessibilityCanDisableSwipeWhileButtonsStillNavigate() {
        content(swipeEnabled = false)
        compose.onNodeWithTag("onboarding-pager").performTouchInput {
            swipe(center, center + Offset(-width * .25f, 0f), 180)
        }
        step(1)
        next(); step(2)
    }

    @Test fun shortDisplayAndLargeTextKeepActionsVisibleAndBodyScrollable() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.8f)) {
                MAVLabTheme { Box(Modifier.size(320.dp, 500.dp)) { OnboardingScreen({ completions++ }) } }
            }
        }
        compose.onNodeWithTag("onboarding-next").assertIsDisplayed()
        compose.onNodeWithTag("onboarding-skip").assertIsDisplayed()
        compose.onNodeWithText("Simulation only · no real aircraft").performScrollTo().assertIsDisplayed()
        next(); step(2)
        compose.onNodeWithTag("onboarding-back").assertIsDisplayed()
        compose.onNodeWithTag("onboarding-next").assertIsDisplayed()
    }
}
