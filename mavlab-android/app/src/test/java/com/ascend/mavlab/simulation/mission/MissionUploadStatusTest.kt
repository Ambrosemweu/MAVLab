package com.ascend.mavlab.simulation.mission

import kotlin.test.Test
import kotlin.test.assertEquals

class MissionUploadStatusTest {
    @Test
    fun uploadingStatusDisplaysProgressAndRequest() {
        val status = MissionUploadStatus(
            phase = MissionUploadPhase.UPLOADING,
            expectedCount = 4,
            receivedCount = 3,
            lastRequestedSequence = 3,
            lastReceivedSequence = 2,
        )

        assertEquals("Upload: Receiving 3/4 from QGC - requested 4", status.displayText)
    }

    @Test
    fun acceptedStatusDisplaysWaypointCount() {
        val status = MissionUploadStatus(
            phase = MissionUploadPhase.ACCEPTED,
            expectedCount = 4,
            receivedCount = 4,
        )

        assertEquals("Upload: Accepted 4 waypoints", status.displayText)
    }

    @Test
    fun rejectedStatusDisplaysReason() {
        val status = MissionUploadStatus(
            phase = MissionUploadPhase.REJECTED,
            lastError = "invalid seq 2",
        )

        assertEquals("Upload: Rejected invalid seq 2", status.displayText)
    }
}
