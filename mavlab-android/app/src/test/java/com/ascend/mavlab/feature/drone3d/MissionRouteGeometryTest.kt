package com.ascend.mavlab.feature.drone3d

import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.mission.*
import kotlin.test.*

class MissionRouteGeometryTest {
    private fun item(seq: Int, n: Float, e: Float, command: MissionCommand = MissionCommand.WAYPOINT) = MissionItem(seq, command, -1.0, 36.0, 10f, localNorthMeters = n, localEastMeters = e)
    private fun mission(items: List<MissionItem>, current: Int = 1) = MissionProgress(items, current, false, activeTarget = items[current], origin = MissionOrigin.GCS_UPLOAD)

    @Test fun noReceivedMissionMeansNoRoute() {
        val route = mission(listOf(item(0, 0f, 0f), item(1, 10f, 0f)))
        assertTrue(missionRoutePoints(MissionProgress()).isEmpty())
        assertTrue(missionRoutePoints(route.copy(origin = MissionOrigin.UNVERIFIED)).isEmpty())
        assertTrue(missionRoutePoints(route.copy(origin = MissionOrigin.INTERNAL_FIXTURE)).isEmpty())
    }
    @Test fun allNodesRemainAndNonSpatialItemsDoNotShiftActiveIndex() {
        val items = listOf(item(0, 0f, 0f), item(1, 0f, 0f, MissionCommand.CHANGE_SPEED)) + (2..12).map { item(it, it * 10f, 0f) }
        val points = missionRoutePoints(mission(items, 2))
        assertEquals(12, points.size)
        assertEquals(0, points.first().sequence)
        assertEquals(12, points.last().sequence)
        assertEquals(2, points.single { it.stage == RouteStage.ACTIVE }.sequence)
        assertEquals(RouteStage.COMPLETED, points.first().stage)
    }
    @Test fun missingNormalizedCoordinateNeverCreatesDecorativePoint() {
        val items = listOf(item(0, 0f, 0f), item(1, 10f, 0f).copy(localEastMeters = null))
        assertTrue(missionRoutePoints(mission(items)).isEmpty())
    }
    @Test fun fitUsesEqualScaleAndIncludesOffRouteVehicleWithoutSnapping() {
        val points = missionRoutePoints(mission(listOf(item(0, 0f, 0f), item(1, 1000f, 500f))))
        val vehicle = DroneState(northMeters = 100f, eastMeters = -100f)
        val transform = fitMissionRoute(points, vehicle, emptyList(), 300f, 500f, 30f)
        assertEquals(2f, (transform.y(0f) - transform.y(1000f)) / (transform.x(500f) - transform.x(0f)), 0.001f)
        assertTrue(transform.x(vehicle.eastMeters) in 30f..270f)
        assertTrue(transform.y(vehicle.northMeters) in 30f..470f)
        assertTrue(transform.x(-100f) != transform.x(0f))
    }
    @Test fun deviationMeasuresFiniteLegAndCompletedRouteHasNoActiveLeg() {
        val route = mission(listOf(item(0, 0f, 0f), item(1, 100f, 0f)))
        assertEquals(20f, activeRouteDeviationMeters(missionRoutePoints(route), DroneState(northMeters = 50f, eastMeters = 20f)))
        assertEquals(10f, activeRouteDeviationMeters(missionRoutePoints(route), DroneState(northMeters = 110f)))
        assertNull(activeRouteDeviationMeters(missionRoutePoints(route.copy(complete = true)), DroneState()))
    }
}
