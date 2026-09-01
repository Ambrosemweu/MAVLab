package com.ascend.mavlab.core.mavlink

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MavlinkIdentityStatusTest {
    @Test
    fun gcsConnectionRequiresContinuousHeartbeatsBeforeLockingConnected() {
        val firstHeartbeatMs = 10_000L

        val first = MavlinkIdentityStatus().withGcsHeartbeat(firstHeartbeatMs)
        assertFalse(first.gcsConnected)

        val second = first.withGcsHeartbeat(firstHeartbeatMs + 1_000L)
        assertFalse(second.gcsConnected)

        val locked = second.withGcsHeartbeat(firstHeartbeatMs + GcsConnectionWarmupMs)
        assertTrue(locked.gcsConnected)
    }

    @Test
    fun gcsConnectionDropsOnlyAfterHeartbeatTimeout() {
        val connected = MavlinkIdentityStatus()
            .withGcsHeartbeat(0L)
            .withGcsHeartbeat(1_000L)
            .withGcsHeartbeat(GcsConnectionWarmupMs)
        assertTrue(connected.gcsConnected)

        assertTrue(connected.refreshGcsConnection(GcsConnectionWarmupMs + GcsConnectionTimeoutMs).gcsConnected)
        assertFalse(connected.refreshGcsConnection(GcsConnectionWarmupMs + GcsConnectionTimeoutMs + 1L).gcsConnected)
    }

    @Test
    fun gcsHeartbeatSeriesResetsAfterLongGapBeforeConnectionLocks() {
        val first = MavlinkIdentityStatus().withGcsHeartbeat(0L)
        val reset = first.withGcsHeartbeat(GcsHeartbeatContinuityGapMs + 1L)

        assertFalse(reset.gcsConnected)
        assertTrue(reset.gcsHeartbeatSeriesStartedAtMs == GcsHeartbeatContinuityGapMs + 1L)
    }

    @Test
    fun onlyGroundControlHeartbeatQualifiesForConnectionRetention() {
        val gcsHeartbeat = MavlinkPacket(
            messageId = 0,
            systemId = 255,
            componentId = 190,
            payload = byteArrayOf(0, 0, 0, 0, 6),
        )
        val vehicleHeartbeat = gcsHeartbeat.copy(
            systemId = 2,
            componentId = 1,
            payload = byteArrayOf(0, 0, 0, 0, 2),
        )

        assertTrue(gcsHeartbeat.isGroundControlHeartbeat())
        assertFalse(vehicleHeartbeat.isGroundControlHeartbeat())
        assertFalse(gcsHeartbeat.copy(messageId = 30).isGroundControlHeartbeat())
    }
}
