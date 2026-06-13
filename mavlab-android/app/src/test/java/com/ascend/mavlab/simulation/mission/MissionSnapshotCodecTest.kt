package com.ascend.mavlab.simulation.mission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MissionSnapshotCodecTest {
    @Test
    fun roundTripsUploadedMissionItems() {
        val items = listOf(
            MissionItem(
                sequence = 1,
                command = MissionCommand.TAKEOFF,
                latitudeDeg = -1.2345678,
                longitudeDeg = 36.8765432,
                altitudeAglMeters = 8f,
                acceptanceRadiusMeters = 4f,
                autocontinue = true,
                localNorthMeters = 12.5f,
                localEastMeters = -3.25f,
            ),
            MissionItem(
                sequence = 2,
                command = MissionCommand.WAYPOINT,
                latitudeDeg = -1.2344678,
                longitudeDeg = 36.8766432,
                altitudeAglMeters = 12f,
                acceptanceRadiusMeters = 6f,
                autocontinue = false,
            ),
        )

        val restored = MissionSnapshotCodec.decode(MissionSnapshotCodec.encode(items))

        assertEquals(items, restored)
    }

    @Test
    fun decodeReturnsEmptyListForInvalidSnapshots() {
        assertTrue(MissionSnapshotCodec.decode("not a mission").isEmpty())
        assertTrue(MissionSnapshotCodec.decode("MAVLAB_MISSION_V1\n1\tWAYPOINT").isEmpty())
    }
}
