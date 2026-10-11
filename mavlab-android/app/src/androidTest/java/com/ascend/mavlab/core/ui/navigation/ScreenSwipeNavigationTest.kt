package com.ascend.mavlab.core.ui.navigation

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Real Compose dispatch, including the competing vertical scroll parent. */
class ScreenSwipeNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val page = mutableIntStateOf(0)
    private val enabled = mutableStateOf(true)
    private var scrollOffset = { 0 }
    private var controlDrags = 0
    private var buttonTaps = 0
    private lateinit var motion: ScreenSwipeMotion
    private val pageLifecycles = mutableMapOf<Int, Lifecycle>()

    private fun content() {
        compose.setContent {
            val areas = androidx.compose.runtime.remember { ScreenSwipeAreas() }
            motion = rememberScreenSwipeMotion()
            CompositionLocalProvider(LocalScreenSwipeAreas provides areas) {
                Box(Modifier.size(300.dp, 600.dp).screenSwipeNavigation(
                    areas, enabled.value, page.intValue, 4, 0f,
                    onDrag = motion::drag,
                    onDragEnd = motion::finish,
                ) {
                    motion.enter(if (it > page.intValue) 1 else -1)
                    page.intValue = it
                }) {
                  ScreenSwipePager(motion) { index, active, _ ->
                    pageLifecycles[index] = LocalLifecycleOwner.current.lifecycle
                    val scroll = rememberScrollState()
                    if (active) scrollOffset = { scroll.value }
                    Column(Modifier.verticalScroll(scroll).screenSwipeArea()) {
                        Box(Modifier.fillMaxWidth().height(200.dp).screenSwipeArea().testTag(if (active) "passive" else "preview-$index"))
                        Spacer(Modifier.fillMaxWidth().height(80.dp).testTag(if (active) "gap" else "preview-gap-$index"))
                        Box(Modifier.fillMaxWidth().height(200.dp).screenSwipeExcluded().testTag(if (active) "control" else "preview-control-$index").pointerInput(Unit) {
                            detectHorizontalDragGestures { _, _ -> controlDrags++ }
                        })
                        Button(onClick = { buttonTaps++ }, modifier = Modifier.fillMaxWidth().height(64.dp)
                            .screenSwipeExcluded(cancelClickOnHorizontalDrag = true).testTag(if (active) "button" else "preview-button-$index")) { Text("Local action") }
                        Spacer(Modifier.height(1200.dp))
                    }
                  }
                }
            }
        }
    }

    @Test fun contentFollowsFingerBeforeReleaseAndCancelledDragSettlesBack() {
        content()
        compose.onNodeWithTag("passive").performTouchInput {
            down(center)
            moveBy(Offset(-width * .10f, 0f), 150)
        }
        compose.runOnIdle {
            assertTrue("The content must move while the finger is still down", motion.offset < -1f)
            assertEquals(0, page.intValue)
        }
        compose.onNodeWithTag("passive").performTouchInput { cancel() }
        compose.runOnIdle {
            assertEquals(0f, motion.offset, 0.5f)
            assertEquals(0, page.intValue)
        }
    }

    @Test fun newDragInterruptsSettlingAndDoesNotWaitForAnimation() {
        content()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("passive").performTouchInput {
            swipe(center, center + Offset(-width * .20f, 0f), 120)
        }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(180)
        compose.runOnUiThread { assertEquals(1, page.intValue) }
        compose.onNodeWithTag("passive").performTouchInput {
            down(center)
            moveBy(Offset(-width * .10f, 0f), 16)
        }
        var heldOffset = 0f
        compose.runOnUiThread { heldOffset = motion.offset }
        compose.mainClock.advanceTimeBy(160)
        compose.runOnUiThread { assertEquals(heldOffset, motion.offset, 0.5f) }
        compose.onNodeWithTag("passive").performTouchInput { cancel() }
        compose.mainClock.autoAdvance = true
        compose.runOnIdle { assertEquals(0f, motion.offset, 0.5f) }
    }

    @Test fun dragTracksPixelsOneToOneAndCommitDoesNotTeleport() {
        content()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("passive").performTouchInput {
            down(center)
            moveBy(Offset(-width * .30f, 0f), 160)
        }
        compose.runOnUiThread {
            val width = motion.pager.layoutInfo.pageSize.toFloat()
            assertEquals(width * .30f, motion.position, 1f)
            val before = motion.position
            motion.finish(true, -600f)
            motion.enter(1)
            page.intValue = 1
            assertEquals("Selecting the next page must not reset the visual position", before, motion.position, .5f)
        }
        compose.mainClock.autoAdvance = true
        compose.runOnIdle { assertEquals(0f, motion.offset, 1f) }
    }

    @Test fun visiblePreviewDoesNotAcquireActiveLifecycleBeforeRelease() {
        content()
        compose.onNodeWithTag("passive").performTouchInput {
            down(center)
            moveBy(Offset(-width * .40f, 0f), 150)
        }
        compose.runOnIdle {
            assertTrue(pageLifecycles.getValue(0).currentState.isAtLeast(Lifecycle.State.STARTED))
            assertEquals(Lifecycle.State.CREATED, pageLifecycles.getValue(1).currentState)
        }
        compose.onNodeWithTag("passive").performTouchInput { up() }
        compose.runOnIdle {
            assertEquals(1, page.intValue)
            assertTrue(pageLifecycles.getValue(1).currentState.isAtLeast(Lifecycle.State.STARTED))
            assertTrue(!pageLifecycles.getValue(0).currentState.isAtLeast(Lifecycle.State.STARTED))
        }
    }

    @Test fun firstEndpointNeverExposesBlankSpace() {
        content()
        compose.onNodeWithTag("passive").performTouchInput {
            down(center)
            moveBy(Offset(width * .35f, 0f), 150)
        }
        compose.runOnIdle { assertEquals(0f, motion.position, .5f) }
        compose.onNodeWithTag("passive").performTouchInput { up() }
        compose.runOnIdle { assertEquals(0, page.intValue) }
    }

    @Test fun slowShortSwipeMovesOnceThroughScrollableParent() {
        content()
        compose.onNodeWithTag("passive").performTouchInput {
            swipe(center, center + Offset(-width * .17f, height * .08f), 500)
        }
        compose.runOnIdle { assertEquals(1, page.intValue); assertEquals(0, scrollOffset()) }
    }

    @Test fun fastShortFlickMovesOnce() {
        content()
        compose.onNodeWithTag("passive").performTouchInput {
            swipe(center, center + Offset(-width * .11f, 0f), 70)
        }
        compose.runOnIdle { assertEquals(1, page.intValue) }
    }

    @Test fun initialJitterAndDiagonalDriftDoNotLoseHorizontalSwipeToScrolling() {
        content()
        compose.onNodeWithTag("passive").performTouchInput {
            down(center)
            moveBy(Offset(-width * .02f, height * .03f), 16)
            moveBy(Offset(-width * .10f, height * .07f), 16)
            moveBy(Offset(-width * .10f, height * .07f), 16)
            up()
        }
        compose.runOnIdle { assertEquals(1, page.intValue); assertEquals(0, scrollOffset()) }
    }

    @Test fun verticalScrollRemainsVerticalScroll() {
        content()
        compose.onNodeWithTag("passive").performTouchInput {
            swipe(center, center + Offset(-width * .02f, -height * .40f), 300)
        }
        compose.runOnIdle { assertEquals(0, page.intValue); assertTrue(scrollOffset() > 0) }
    }

    @Test fun localInteractiveDragNeverNavigates() {
        content()
        compose.onNodeWithTag("control").performTouchInput {
            swipe(center, center + Offset(-width * .30f, 0f), 150)
        }
        compose.runOnIdle { assertEquals(0, page.intValue); assertTrue(controlDrags > 0) }
    }

    @Test fun middleGapIsSwipeableThroughTheVerticalScrollParent() {
        content()
        compose.onNodeWithTag("gap").performTouchInput {
            swipe(center, center + Offset(-width * .25f, height * .15f), 180)
        }
        compose.runOnIdle { assertEquals(1, page.intValue); assertEquals(0, scrollOffset()) }
    }

    @Test fun excludedButtonDragDoesNotNavigateOrTriggerItsAction() {
        content()
        compose.onNodeWithTag("button").performTouchInput {
            swipe(center, center + Offset(-width * .25f, 0f), 180)
        }
        compose.runOnIdle { assertEquals("Button drag must not navigate", 0, page.intValue); assertEquals("Button drag must cancel its tap", 0, buttonTaps) }
        compose.onNodeWithTag("button").performTouchInput { down(center); up() }
        compose.runOnIdle { assertEquals(1, buttonTaps) }
    }

    @Test fun consecutiveSwipesUseLatestDestinationWithoutCooldown() {
        content()
        repeat(3) {
            compose.onNodeWithTag("passive").performTouchInput {
                swipe(center, center + Offset(-width * .20f, 0f), 120)
            }
        }
        compose.runOnIdle { assertEquals(3, page.intValue) }
        compose.onNodeWithTag("passive").performTouchInput {
            swipe(center, center + Offset(width * .20f, 0f), 120)
        }
        compose.runOnIdle { assertEquals(2, page.intValue) }
    }

    @Test fun disabledNavigationLeavesPassiveDragLocal() {
        enabled.value = false
        content()
        compose.onNodeWithTag("passive").performTouchInput {
            swipe(center, center + Offset(-width * .30f, 0f), 150)
        }
        compose.runOnIdle { assertEquals(0, page.intValue) }
    }

    @Test fun secondPointerCancelsAnOtherwiseValidSwipe() {
        content()
        compose.onNodeWithTag("passive").performTouchInput {
            down(0, center)
            moveBy(0, Offset(-width * .20f, 0f), 16)
            down(1, center + Offset(0f, 40f))
            moveBy(0, Offset(-width * .10f, 0f), 16)
            up(1)
            up(0)
        }
        compose.runOnIdle { assertEquals(0, page.intValue) }
    }
}
