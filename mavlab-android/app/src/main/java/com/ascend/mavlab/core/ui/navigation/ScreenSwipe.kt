package com.ascend.mavlab.core.ui.navigation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Opt-in passive areas, rather than a pager that intercepts every flight control. */
class ScreenSwipeAreas {
    private val bounds = mutableMapOf<Any, () -> Rect>()
    private val exclusions = mutableSetOf<Any>()
    internal fun update(key: Any, rect: Rect, excluded: Boolean = false) {
        bounds[key] = { rect }
        if (excluded) exclusions.add(key) else exclusions.remove(key)
    }
    internal fun update(key: Any, coordinates: LayoutCoordinates, excluded: Boolean = false) {
        // Layer translation can change without a new layout callback. Resolve
        // live coordinates at DOWN so a settling screen remains swipeable.
        bounds[key] = { if (coordinates.isAttached) coordinates.boundsInRoot() else Rect.Zero }
        if (excluded) exclusions.add(key) else exclusions.remove(key)
    }
    internal fun remove(key: Any) { bounds.remove(key); exclusions.remove(key) }
    internal fun contains(point: Offset) = bounds.none { (key, rect) -> key in exclusions && rect().contains(point) } &&
        bounds.any { (key, rect) -> key !in exclusions && rect().contains(point) }
}

val LocalScreenSwipeAreas = compositionLocalOf<ScreenSwipeAreas?> { null }

/** Protect local controls when their containing surface accepts navigation swipes. */
fun Modifier.screenSwipeExcluded(cancelClickOnHorizontalDrag: Boolean = false): Modifier = composed {
    val areas = LocalScreenSwipeAreas.current ?: return@composed this
    val key = remember { Any() }
    DisposableEffect(areas, key) { onDispose { areas.remove(key) } }
    val exclusion = onGloballyPositioned { areas.update(key, it, excluded = true) }
    if (!cancelClickOnHorizontalDrag) exclusion else exclusion.pointerInput(Unit) {
        // A clickable can otherwise treat an in-bounds horizontal drag as a tap.
        // Standard horizontal slop arbitration cancels that press; vertical scrolling wins normally.
        detectHorizontalDragGestures { change, _ -> change.consume() }
    }
}

/** Passive surface. Nested controls must use screenSwipeExcluded before expanding a surface. */
fun Modifier.screenSwipeArea(): Modifier = composed {
    val areas = LocalScreenSwipeAreas.current ?: return@composed this
    val key = remember { Any() }
    DisposableEffect(areas, key) { onDispose { areas.remove(key) } }
    onGloballyPositioned { areas.update(key, it) }
        .pointerInput(Unit) {
            // This passive hit surface also prevents an underlying Android 3D view
            // from receiving gestures that began on an overlaid status strip.
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do { val event = awaitPointerEvent() } while (event.changes.any { it.pressed })
            }
        }
}

/** Lock the dominant axis once, allowing initial finger jitter and short horizontal flicks. */
internal class ScreenSwipeGesture(
    private val touchSlop: Float,
    private val commitDistance: Float,
    private val flingDistance: Float,
    private val flingVelocity: Float,
) {
    private var horizontal = false
    private var cancelled = false
    private var dx = 0f
    val isCancelled: Boolean get() = cancelled

    fun move(x: Float, y: Float, pointerCount: Int = 1, consumed: Boolean = false): Boolean {
        if (pointerCount != 1 || consumed) cancelled = true
        if (cancelled) return false
        if (!horizontal) {
            // An ambiguous start waits for a direction; it is not a failed swipe.
            if (abs(x) > touchSlop && abs(x) > abs(y) * 1.2f) horizontal = true
            else if (abs(y) > touchSlop && abs(y) > abs(x) * 1.2f) cancelled = true
        }
        dx = x
        return horizontal && !cancelled
    }

    fun cancel() { cancelled = true }

    fun destination(current: Int, count: Int, velocityX: Float = 0f, velocityY: Float = 0f): Int {
        val flick = abs(dx) >= flingDistance && abs(velocityX) >= flingVelocity &&
            abs(velocityX) > abs(velocityY) && dx * velocityX > 0f
        if (cancelled || !horizontal || (abs(dx) < commitDistance && !flick)) return current
        return (current + if (dx < 0f) 1 else -1).coerceIn(0, count - 1)
    }
}

fun Modifier.screenSwipeNavigation(
    areas: ScreenSwipeAreas,
    enabled: Boolean,
    page: Int,
    pageCount: Int,
    edgeGuardPx: Float,
    onDrag: (Float) -> Unit = {},
    onDragEnd: (committed: Boolean, velocity: Float) -> Unit = { _, _ -> },
    onPage: (Int) -> Unit,
): Modifier = composed {
    val rootPosition = remember { arrayOf(Offset.Zero) }
    val currentEnabled = rememberUpdatedState(enabled)
    val currentPage = rememberUpdatedState(page)
    val currentPageCount = rememberUpdatedState(pageCount)
    val currentEdgeGuard = rememberUpdatedState(edgeGuardPx)
    val currentOnPage = rememberUpdatedState(onPage)
    val currentOnDrag = rememberUpdatedState(onDrag)
    val currentOnDragEnd = rememberUpdatedState(onDragEnd)
    val density = LocalDensity.current
    val commitDistance = with(density) { 48.dp.toPx() }
    val flingDistance = with(density) { 24.dp.toPx() }
    val flingVelocity = with(density) { 400.dp.toPx() } // pixels/second; Compose snapping baseline
    onGloballyPositioned { rootPosition[0] = it.positionInRoot() }
        // Page, callback and animation updates must not restart the detector.
        .pointerInput(areas, density) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val startPage = currentPage.value
                if (!currentEnabled.value || down.position.x < currentEdgeGuard.value ||
                    down.position.x > size.width - currentEdgeGuard.value ||
                    !areas.contains(down.position + rootPosition[0])) return@awaitEachGesture
                val tracker = VelocityTracker().also { it.addPointerInputChange(down) }
                // Finish DOWN. A scrollable consuming DOWN is not a drag owner.
                awaitPointerEvent(PointerEventPass.Final)
                val gesture = ScreenSwipeGesture(viewConfiguration.touchSlop, commitDistance, flingDistance, flingVelocity)
                var moved = false
                var finished = false
                try {
                  while (true) {
                    // Resolve direction before a vertical scrollable can consume
                    // a horizontal drag with a small amount of vertical drift.
                    // Only explicitly passive regions reach this arbitration.
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    tracker.addPointerInputChange(change)
                    val delta = change.position - down.position
                    if (!currentEnabled.value || currentPage.value != startPage) gesture.cancel()
                    val owned = gesture.move(delta.x, delta.y, event.changes.count { it.pressed }.coerceAtLeast(1), change.isConsumed)
                    if (gesture.isCancelled) break
                    if (owned) {
                        moved = true
                        currentOnDrag.value(delta.x)
                    }
                    if (!change.pressed) {
                        val velocity = tracker.calculateVelocity()
                        val destination = gesture.destination(startPage, currentPageCount.value, velocity.x, velocity.y)
                        if (owned) change.consume()
                        if (moved) currentOnDragEnd.value(destination != startPage, velocity.x)
                        finished = true
                        if (destination != startPage) currentOnPage.value(destination)
                        break
                    }
                    if (owned) change.consume()
                    // If a local gesture wins while direction is still pending,
                    // never steal it later. Ignore our own horizontal consumption.
                    awaitPointerEvent(PointerEventPass.Final)
                    if (!owned && change.isConsumed) gesture.cancel()
                  }
                } finally {
                    // Android cancellation, a removed surface or a second pointer
                    // must never leave the current screen stranded mid-drag.
                    if (moved && !finished) currentOnDragEnd.value(false, 0f)
                }
            }
        }
}
