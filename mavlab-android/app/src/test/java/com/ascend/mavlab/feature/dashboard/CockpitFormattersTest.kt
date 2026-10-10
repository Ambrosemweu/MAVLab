package com.ascend.mavlab.feature.dashboard

import com.ascend.mavlab.core.mavlink.MavlinkIdentityStatus
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.mission.MissionCommand
import com.ascend.mavlab.simulation.mission.MissionItem
import com.ascend.mavlab.simulation.mission.MissionProgress
import kotlin.test.Test
import kotlin.test.assertEquals

class CockpitFormattersTest {
    @Test
    fun batteryLabelIncludesSeverity() {
        assertEquals("85% Normal", batteryLabel(DroneState(batteryRemainingPercent = 85)))
        assertEquals("30% Low", batteryLabel(DroneState(batteryRemainingPercent = 30)))
        assertEquals("15% Critical", batteryLabel(DroneState(batteryRemainingPercent = 15)))
    }

    @Test
    fun gpsStatusShowsLockAndSatelliteCount() {
        assertEquals(
            "Locked (12 sats)",
            gpsStatusLabel(DroneState(gpsFixType = 3u, gpsSatellites = 12u)),
        )
        assertEquals(
            "No lock (4 sats)",
            gpsStatusLabel(DroneState(gpsFixType = 1u, gpsSatellites = 4u)),
        )
    }

    @Test
    fun mavlinkStatusPrefersConflictAndConnectedStates() {
        assertEquals(
            "SYSID conflict",
            mavlinkStatusLabel("Running", MavlinkIdentityStatus(identityConflict = true)),
        )
        assertEquals(
            "QGC connected",
            mavlinkStatusLabel("Running", MavlinkIdentityStatus(gcsConnected = true)),
        )
        assertEquals("Running", mavlinkStatusLabel("Running", MavlinkIdentityStatus()))
    }

    @Test
    fun missionFocusSummarizesActiveObjective() {
        val mission = MissionProgress(
            items = listOf(
                missionItem(0, MissionCommand.TAKEOFF),
                missionItem(1, MissionCommand.WAYPOINT),
            ),
            currentIndex = 1,
            complete = false,
            activeTarget = missionItem(1, MissionCommand.WAYPOINT),
        )

        assertEquals("Fly to WP 2", missionFocusLabel(mission))
        assertEquals("1/2 reached", missionProgressLabel(mission))
        assertEquals("No active mission", missionFocusLabel(MissionProgress()))
    }

    @Test
    fun distanceFromHomeUsesLocalNorthEastPosition() {
        val state = DroneState(northMeters = 3f, eastMeters = 4f)

        assertEquals(5f, distanceFromHomeMeters(state))
        assertEquals("5.0 m", distanceFromHomeLabel(state))
    }

    private fun missionItem(sequence: Int, command: MissionCommand): MissionItem {
        return MissionItem(
            sequence = sequence,
            command = command,
            latitudeDeg = -1.2921,
            longitudeDeg = 36.8219,
            altitudeAglMeters = 10f,
        )
    }
}
