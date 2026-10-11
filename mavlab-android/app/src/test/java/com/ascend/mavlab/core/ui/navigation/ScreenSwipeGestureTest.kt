package com.ascend.mavlab.core.ui.navigation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScreenSwipeGestureTest {
    private fun gesture() = ScreenSwipeGesture(touchSlop = 12f, commitDistance = 48f, flingDistance = 24f, flingVelocity = 400f)

    @Test fun horizontalReleaseMovesOneDestinationWithoutWrapping() {
        val left = gesture()
        assertTrue(left.move(-180f, 4f))
        assertEquals(2, left.destination(1, 4))
        assertEquals(3, left.destination(3, 4))
        val right = gesture()
        right.move(180f, -3f)
        assertEquals(0, right.destination(1, 4))
        assertEquals(0, right.destination(0, 4))
    }

    @Test fun ShortDragOrReturningToOriginDoesNotChangeTheScreen() {
        val short = gesture()
        short.move(-40f, 1f)
        assertEquals(1, short.destination(1, 4))
        short.move(-10f, 0f)
        assertEquals(1, short.destination(1, 4))
    }

    @Test fun VerticalGesturesRemainLocalButAmbiguousStartsCanResolveHorizontally() {
        val vertical = gesture()
        assertFalse(vertical.move(5f, 20f))
        assertFalse(vertical.move(-120f, 25f))
        assertEquals(1, vertical.destination(1, 4))
        val diagonal = gesture()
        assertFalse(diagonal.move(-16f, 16f))
        assertTrue(diagonal.move(-30f, 20f))
        diagonal.move(-60f, 25f)
        assertEquals(2, diagonal.destination(1, 4))
    }

    @Test fun ConsumedChildGesturesAndMultitouchNeverNavigate() {
        val consumed = gesture()
        consumed.move(-100f, 0f, consumed = true)
        consumed.move(-150f, 0f)
        assertEquals(1, consumed.destination(1, 4))
        val multitouch = gesture()
        multitouch.move(-80f, 0f)
        assertFalse(multitouch.move(-90f, 0f, pointerCount = 2))
        multitouch.move(-150f, 0f)
        assertEquals(1, multitouch.destination(1, 4))
    }

    @Test fun HorizontalDirectionStaysLockedDespiteFingerCurvature() {
        val drag = gesture()
        drag.move(-30f, 1f)
        assertTrue(drag.move(-35f, 80f))
        drag.move(-100f, 20f)
        assertEquals(2, drag.destination(1, 4))
    }

    @Test fun ShortFastFlickCommitsButSlowShortMovementDoesNot() {
        val drag = gesture()
        drag.move(-30f, 2f)
        assertEquals(2, drag.destination(1, 4, -800f, 30f))
        assertEquals(2, drag.destination(1, 4, -500f, 30f))
        assertEquals(1, drag.destination(1, 4, -300f, 30f))
        assertEquals(1, drag.destination(1, 4, 800f, 30f))
        assertEquals(1, drag.destination(1, 4, -800f, 900f))
    }

    @Test fun TinyFastTwitchOrFlickReturningToOriginDoesNotNavigate() {
        val drag = gesture()
        drag.move(-18f, 0f)
        assertEquals(1, drag.destination(1, 4, -2000f))
        drag.move(-80f, 0f)
        drag.move(-10f, 0f)
        assertEquals(1, drag.destination(1, 4, -2000f))
    }

    @Test fun OrdinaryShortDragCommitsWithoutVelocityOrScreenWidthDependence() {
        val drag = gesture()
        drag.move(-49f, 15f)
        assertEquals(2, drag.destination(1, 4))
    }

    @Test fun ExplicitCancellationSuppressesAnAlreadyRecognizedSwipe() {
        val drag = gesture()
        drag.move(-80f, 2f)
        drag.cancel()
        assertEquals(1, drag.destination(1, 4, -1000f))
    }

    @Test fun passiveAreaBoundsMoveWithScrollingAndAreRemovedOnDisposal() {
        val areas = ScreenSwipeAreas()
        val header = Any()
        areas.update(header, Rect(20f, 40f, 300f, 100f))
        assertTrue(areas.contains(Offset(150f, 60f)))
        assertFalse(areas.contains(Offset(150f, 200f)))
        areas.update(header, Rect(20f, 140f, 300f, 200f))
        assertFalse(areas.contains(Offset(150f, 60f)))
        assertTrue(areas.contains(Offset(150f, 160f)))
        areas.remove(header)
        assertFalse(areas.contains(Offset(150f, 160f)))
    }

    @Test fun controlExclusionWinsInsideAnOtherwiseSwipeableSurface() {
        val areas = ScreenSwipeAreas()
        val surface = Any()
        val button = Any()
        areas.update(surface, Rect(0f, 0f, 300f, 600f))
        areas.update(button, Rect(100f, 250f, 200f, 300f), excluded = true)
        assertTrue(areas.contains(Offset(150f, 200f)))
        assertFalse(areas.contains(Offset(150f, 275f)))
        areas.update(button, Rect(100f, 350f, 200f, 400f), excluded = true)
        assertTrue(areas.contains(Offset(150f, 275f)))
        assertFalse(areas.contains(Offset(150f, 375f)))
        areas.remove(button)
        assertTrue(areas.contains(Offset(150f, 375f)))
    }
}
