package com.ascend.mavlab.simulation.autopilot

import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.mission.MissionItem
import com.ascend.mavlab.simulation.mission.MissionProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt

class MissionEngine {
    private val mutableProgress = MutableStateFlow(MissionProgress())
    val progress: StateFlow<MissionProgress> = mutableProgress.asStateFlow()

    fun load(items: List<MissionItem>) {
        val sorted = items.sortedBy { it.sequence }
        mutableProgress.value = MissionProgress(
            items = sorted,
            currentIndex = 0,
            complete = sorted.isEmpty(),
            activeTarget = sorted.firstOrNull(),
        )
    }

    fun clear() {
        mutableProgress.value = MissionProgress()
    }

    fun reset() {
        val items = mutableProgress.value.items
        load(items)
    }

    fun update(state: DroneState): MissionProgress {
        val progress = mutableProgress.value
        val current = progress.activeTarget ?: return progress
        val distance = distanceMeters(state, current)
        val altitudeError = abs(state.altitudeAglMeters - current.altitudeAglMeters)
        if (distance <= current.acceptanceRadiusMeters && altitudeError <= 1.25f) {
            val nextIndex = progress.currentIndex + 1
            val next = progress.items.getOrNull(nextIndex)
            val updated = progress.copy(
                currentIndex = nextIndex,
                complete = next == null,
                lastReachedSequence = current.sequence,
                activeTarget = next,
            )
            mutableProgress.value = updated
            return updated
        }
        return progress
    }

    private fun distanceMeters(state: DroneState, item: MissionItem): Float {
        val north = ((item.latitudeDeg - state.latitudeDeg) * MetersPerLatDeg).toFloat()
        val lonScale = MetersPerLatDeg * max(0.2, cos(Math.toRadians(state.latitudeDeg)))
        val east = ((item.longitudeDeg - state.longitudeDeg) * lonScale).toFloat()
        return sqrt(north * north + east * east)
    }

    private companion object {
        const val MetersPerLatDeg = 111_320.0
    }
}
