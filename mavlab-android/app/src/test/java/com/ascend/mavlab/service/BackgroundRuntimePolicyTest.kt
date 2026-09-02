package com.ascend.mavlab.service

import kotlin.test.Test
import kotlin.test.assertEquals

class BackgroundRuntimePolicyTest {
    private val policy = BackgroundRuntimePolicy()

    @Test
    fun visibleAppAlwaysKeepsRuntime() {
        val decision = policy.decide(snapshot(appVisible = true), nowMs = 100_000L)

        assertEquals(BackgroundRuntimeDecision.KEEP_APP_VISIBLE, decision)
    }

    @Test
    fun backgroundHandoffKeepsDiscoveryAliveOnlyForGracePeriod() {
        val backgrounded = snapshot(backgroundedAtMs = 10_000L)

        assertEquals(
            BackgroundRuntimeDecision.KEEP_GCS_HANDOFF,
            policy.decide(backgrounded, nowMs = 70_000L),
        )
        assertEquals(
            BackgroundRuntimeDecision.STOP_IDLE,
            policy.decide(backgrounded, nowMs = 70_001L),
        )
    }

    @Test
    fun connectedGcsKeepsRuntimeAfterHandoffExpires() {
        val decision = policy.decide(
            snapshot(backgroundedAtMs = 0L, gcsConnected = true),
            nowMs = 500_000L,
        )

        assertEquals(BackgroundRuntimeDecision.KEEP_GCS_CONNECTED, decision)
    }

    @Test
    fun armedVehicleOrActiveMissionKeepsRuntimeWithoutGcs() {
        assertEquals(
            BackgroundRuntimeDecision.KEEP_ACTIVE_FLIGHT,
            policy.decide(snapshot(backgroundedAtMs = 0L, armed = true), nowMs = 500_000L),
        )
        assertEquals(
            BackgroundRuntimeDecision.KEEP_ACTIVE_FLIGHT,
            policy.decide(snapshot(backgroundedAtMs = 0L, missionActive = true), nowMs = 500_000L),
        )
    }

    @Test
    fun idleDisconnectedRuntimeStopsWhenThereWasNoHandoff() {
        val decision = policy.decide(snapshot(), nowMs = 100_000L)

        assertEquals(BackgroundRuntimeDecision.STOP_IDLE, decision)
    }

    private fun snapshot(
        appVisible: Boolean = false,
        backgroundedAtMs: Long? = null,
        gcsConnected: Boolean = false,
        armed: Boolean = false,
        missionActive: Boolean = false,
    ) = BackgroundRuntimeSnapshot(
        appVisible = appVisible,
        backgroundedAtMs = backgroundedAtMs,
        gcsConnected = gcsConnected,
        armed = armed,
        missionActive = missionActive,
    )
}
