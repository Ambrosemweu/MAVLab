package com.ascend.mavlab.feature.mission

import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.FlightMode
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.mission.MissionCommand
import com.ascend.mavlab.simulation.mission.MissionItem
import com.ascend.mavlab.simulation.mission.MissionProgress
import kotlin.test.Test
import kotlin.test.assertEquals

class MissionDisplayTest {
    @Test
    fun missionRunStatusSeparatesReadyRunningAndComplete() {
        val mission = MissionProgress(
            items = listOf(missionItem(0, MissionCommand.WAYPOINT)),
            complete = false,
            activeTarget = missionItem(0, MissionCommand.WAYPOINT),
        )

        assertEquals("No mission loaded", missionRunStatus(DroneState(), MissionProgress()))
        assertEquals("Mission ready", missionRunStatus(DroneState(mode = FlightMode.STABILIZE), mission))
        assertEquals("Mission running", missionRunStatus(DroneState(mode = FlightMode.AUTO), mission))
        assertEquals("Mission complete", missionRunStatus(DroneState(), mission.copy(complete = true)))
    }

    @Test
    fun missionProgressAndObjectiveDescribeActiveWaypoint() {
        val mission = MissionProgress(
            items = listOf(
                missionItem(0, MissionCommand.TAKEOFF),
                missionItem(1, MissionCommand.WAYPOINT),
                missionItem(2, MissionCommand.LAND),
            ),
            currentIndex = 1,
            complete = false,
            activeTarget = missionItem(1, MissionCommand.WAYPOINT),
        )

        assertEquals(33, missionProgressPercent(mission))
        assertEquals("Fly to WP2", missionObjectiveLabel(mission))
        assertEquals("Active", waypointStatusLabel(mission.items[1], mission))
        assertEquals("Queued", waypointStatusLabel(mission.items[2], mission))
    }

    @Test
    fun missionDistanceEtaAndSpeedUseLocalMissionData() {
        val mission = MissionProgress(
            items = listOf(
                missionItem(0, MissionCommand.WAYPOINT, north = 0f, east = 0f),
                missionItem(1, MissionCommand.CHANGE_SPEED, speed = 5f),
                missionItem(2, MissionCommand.WAYPOINT, north = 30f, east = 40f),
            ),
            currentIndex = 2,
            complete = false,
            activeTarget = missionItem(2, MissionCommand.WAYPOINT, north = 30f, east = 40f),
        )
        val state = DroneState(northMeters = 0f, eastMeters = 0f, groundSpeedMS = 2f)
        val distance = missionDistanceToActiveMeters(state, mission)
        val speed = missionTargetSpeedMetersPerSecond(mission)

        assertEquals(50f, distance ?: -1f)
        assertEquals(5f, speed)
        assertEquals("10s", missionEtaLabel(distance, state, speed))
    }

    @Test
    fun waypointLabelsIncludeCoordinatesAltitudeAndSpeed() {
        val item = missionItem(3, MissionCommand.WAYPOINT, north = 12.5f, east = -4f, speed = 6.25f)

        assertEquals("N 12.5 m, E -4.0 m", waypointCoordinateLabel(item))
        assertEquals("N 12.5 m, E -4.0 m | 10.0 m AGL | target 6.3 m/s", waypointDetailLabel(item))
    }

    @Test
    fun missionFailureWarningsOnlyShowMissionRelevantFailures() {
        assertEquals(emptyList(), missionFailureWarnings(FailureState()))

        val warnings = missionFailureWarnings(
            FailureState(
                gpsEnabled = false,
                lostLinkActive = true,
                unsafeMissionReserveActive = true,
                barometerOffsetMeters = 3f,
            ),
        )

        assertEquals(4, warnings.size)
        assertEquals("GPS unavailable: do not start or continue AUTO.", warnings[0])
        assertEquals("Lost link active: verify GCS control before mission changes.", warnings[1])
        assertEquals("Unsafe reserve: shorten the mission or recharge before AUTO.", warnings[2])
        assertEquals("Barometer offset active: avoid low-altitude autonomous segments.", warnings[3])
    }

    private fun missionItem(
        sequence: Int,
        command: MissionCommand,
        north: Float = 0f,
        east: Float = 0f,
        speed: Float? = null,
    ): MissionItem {
        return MissionItem(
            sequence = sequence,
            command = command,
            latitudeDeg = -1.2921,
            longitudeDeg = 36.8219,
            altitudeAglMeters = 10f,
            localNorthMeters = north,
            localEastMeters = east,
            speedMetersPerSecond = speed,
        )
    }
}
