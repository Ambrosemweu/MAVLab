package com.ascend.mavlab.feature.drone3d

import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.MotorTelemetry
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.mission.MissionCommand
import com.ascend.mavlab.simulation.mission.MissionItem
import com.ascend.mavlab.simulation.mission.MissionProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Drone3DScreenTest {
    @Test
    fun aircraftVisualStatePrioritizesFailuresAndBatteryWarnings() {
        assertEquals(
            SimAircraftVisualState.Normal,
            aircraftVisualState(DroneState(batteryRemainingPercent = 80), FailureState()),
        )
        assertEquals(
            SimAircraftVisualState.Warning,
            aircraftVisualState(DroneState(batteryRemainingPercent = 25), FailureState()),
        )
        assertEquals(
            SimAircraftVisualState.Critical,
            aircraftVisualState(DroneState(batteryRemainingPercent = 12), FailureState()),
        )
        assertEquals(
            SimAircraftVisualState.Failure,
            aircraftVisualState(
                DroneState(motors = listOf(MotorTelemetry(rpm = 0f, failed = true))),
                FailureState(),
            ),
        )
        assertEquals(
            SimAircraftVisualState.Failure,
            aircraftVisualState(DroneState(batteryRemainingPercent = 12), FailureState(gpsEnabled = false)),
        )
    }

    @Test
    fun missionMarkersExposeHomeActiveAndRemainingWaypoints() {
        val mission = MissionProgress(
            items = listOf(
                missionItem(0, MissionCommand.WAYPOINT, north = 0f, east = 0f),
                missionItem(1, MissionCommand.WAYPOINT, north = 20f, east = 0f),
                missionItem(2, MissionCommand.WAYPOINT, north = 20f, east = 10f),
            ),
            currentIndex = 1,
            complete = false,
            activeTarget = missionItem(1, MissionCommand.WAYPOINT, north = 20f, east = 0f),
        )

        val markers = missionMarkers(DroneState(), mission)

        assertTrue(markers.any { it.type == SimMissionMarkerType.Home && it.northMeters == 0f && it.eastMeters == 0f })
        assertTrue(markers.any { it.type == SimMissionMarkerType.Active && it.label == "WP2" })
        assertTrue(markers.any { it.type == SimMissionMarkerType.Remaining && it.label == "WP3" })
    }

    private fun missionItem(
        sequence: Int,
        command: MissionCommand,
        north: Float,
        east: Float,
    ): MissionItem {
        return MissionItem(
            sequence = sequence,
            command = command,
            latitudeDeg = -1.2921,
            longitudeDeg = 36.8219,
            altitudeAglMeters = 10f,
            localNorthMeters = north,
            localEastMeters = east,
        )
    }
}
