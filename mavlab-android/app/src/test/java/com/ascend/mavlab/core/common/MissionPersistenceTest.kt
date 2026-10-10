package com.ascend.mavlab.core.common

import com.ascend.mavlab.simulation.mission.*
import kotlin.test.*

class MissionPersistenceTest {
    private val items = listOf(MissionItem(0, MissionCommand.WAYPOINT, -1.0, 36.0, 10f, altitudeIsMsl = true))
    @Test fun legacyItemsAreRetainedWithoutInventingSource() {
        val restored = decodeStoredMission(MissionSnapshotCodec.encode(items), null)
        assertEquals(items, restored.items)
        assertEquals(MissionOrigin.UNVERIFIED, restored.origin)
    }
    @Test fun onlyExplicitGcsSourceCanRestoreReceivedStatus() {
        for (source in listOf(null, "", "future", MissionOrigin.INTERNAL_FIXTURE.name)) {
            assertEquals(MissionOrigin.UNVERIFIED, decodeStoredMission(MissionSnapshotCodec.encode(items), source).origin)
        }
        assertEquals(MissionOrigin.GCS_UPLOAD, decodeStoredMission(MissionSnapshotCodec.encode(items), MissionOrigin.GCS_UPLOAD.name).origin)
    }
    @Test fun codecReadsPriorSnapshotFieldsAndPreservesMslOnNewSnapshots() {
        val new = MissionSnapshotCodec.encode(items)
        assertEquals(items, MissionSnapshotCodec.decode(new))
        val old = new.lineSequence().filter { it.isNotBlank() }.joinToString("\n") { if (it.startsWith("MAVLAB")) it else it.substringBeforeLast("\t") }
        assertEquals(items.map { it.copy(altitudeIsMsl = false) }, MissionSnapshotCodec.decode(old.replace("MAVLAB_MISSION_V2", "MAVLAB_MISSION_V1")))
        val nineFields = old.lineSequence().joinToString("\n") { if (it.startsWith("MAVLAB")) "MAVLAB_MISSION_V1" else it.substringBeforeLast("\t") }
        assertEquals(items.map { it.copy(altitudeIsMsl = false) }, MissionSnapshotCodec.decode(nineFields))
    }
}
