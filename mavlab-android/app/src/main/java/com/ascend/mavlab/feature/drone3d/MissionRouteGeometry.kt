package com.ascend.mavlab.feature.drone3d

import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.mission.MissionCommand
import com.ascend.mavlab.simulation.mission.MissionProgress
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

internal enum class RouteStage(val label: String) {
    COMPLETED("Earlier item"), ACTIVE("Current target"), REMAINING("Remaining item"),
}
internal data class RoutePoint(val itemIndex: Int, val sequence: Int, val north: Float, val east: Float, val stage: RouteStage, val command: MissionCommand, val partial: Boolean)

/** Uses only engine-normalized local N/E metres; no screen-specific geographic conversion. */
internal fun missionRoutePoints(mission: MissionProgress): List<RoutePoint> {
    if (!mission.received) return emptyList()
    return mission.items.mapIndexedNotNull { index, item ->
        if (!item.command.isNav) return@mapIndexedNotNull null
        val north = item.localNorthMeters?.takeIf { it.isFinite() } ?: return emptyList()
        val east = item.localEastMeters?.takeIf { it.isFinite() } ?: return emptyList()
        RoutePoint(index, item.sequence, north, east, when {
            mission.complete || index < mission.currentIndex -> RouteStage.COMPLETED
            index == mission.currentIndex -> RouteStage.ACTIVE
            else -> RouteStage.REMAINING
        }, item.command, item.command == MissionCommand.LOITER_TIME || !item.autocontinue)
    }
}

internal data class RouteTransform(val centerNorth: Float, val centerEast: Float, val pixelsPerMeter: Float, val width: Float, val height: Float) {
    fun x(east: Float): Float = width / 2f + (east - centerEast) * pixelsPerMeter
    fun y(north: Float): Float = height / 2f - (north - centerNorth) * pixelsPerMeter
}

internal fun fitMissionRoute(points: List<RoutePoint>, vehicle: DroneState?, trail: List<SimPathPoint>, width: Float, height: Float, padding: Float): RouteTransform {
    val north = points.map { it.north } + listOfNotNull(vehicle?.northMeters) + trail.map { it.northMeters }
    val east = points.map { it.east } + listOfNotNull(vehicle?.eastMeters) + trail.map { it.eastMeters }
    val minNorth = north.minOrNull() ?: 0f
    val maxNorth = north.maxOrNull() ?: 0f
    val minEast = east.minOrNull() ?: 0f
    val maxEast = east.maxOrNull() ?: 0f
    val scale = min(max(1f, width - 2 * padding) / max(10f, maxEast - minEast), max(1f, height - 2 * padding) / max(10f, maxNorth - minNorth))
    return RouteTransform((minNorth + maxNorth) / 2, (minEast + maxEast) / 2, scale, width, height)
}

/** Cross-track distance to the finite active planned leg; null for a first target without a prior route point. */
internal fun activeRouteDeviationMeters(points: List<RoutePoint>, state: DroneState): Float? {
    val activeIndex = points.indexOfFirst { it.stage == RouteStage.ACTIVE }
    if (activeIndex <= 0) return null
    val start = points[activeIndex - 1]
    val end = points[activeIndex]
    val dn = end.north - start.north
    val de = end.east - start.east
    val squared = dn * dn + de * de
    val along = if (squared > 0.0001f) (((state.northMeters - start.north) * dn + (state.eastMeters - start.east) * de) / squared).coerceIn(0f, 1f) else 0f
    return hypot(state.northMeters - (start.north + along * dn), state.eastMeters - (start.east + along * de))
}
