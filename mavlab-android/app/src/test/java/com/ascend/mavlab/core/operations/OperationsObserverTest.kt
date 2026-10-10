package com.ascend.mavlab.core.operations

import com.ascend.mavlab.core.common.RuntimeLifecycleState
import com.ascend.mavlab.core.mavlink.MavlinkIdentityStatus
import com.ascend.mavlab.simulation.engine.ControlAuthority
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.mission.MissionProgress
import com.ascend.mavlab.simulation.mission.MissionUploadPhase
import com.ascend.mavlab.simulation.mission.MissionUploadStatus
import kotlin.test.*

class OperationsObserverTest {
    private fun healthy() = OpsObservation(DroneState(batteryRemainingPercent = 100), FailureState(),
        MavlinkIdentityStatus(), MissionProgress(), MissionUploadStatus(), RuntimeLifecycleState.RUNNING,
        appVisible = true, stateFresh = true, recording = false)

    @Test fun noPeerIsNeutralAndPacketsDoNotGrantAuthority() {
        val observer = OperationsObserver()
        val state = observer.update(healthy().copy(identity = MavlinkIdentityStatus(
            lastGcsSystemId = 255, lastGcsMessageAtMs = 1000, gcsConnected = false)), 1000)
        assertTrue(state.incidents.isEmpty())
        assertTrue(state.events.isEmpty())
    }

    @Test fun observedIncidentTimeSurvivesSamplesAndClearsOnlyWithState() {
        val observer = OperationsObserver()
        val initial = healthy()
        observer.update(initial, 1000)
        val failed = initial.copy(failures = FailureState(gpsEnabled = false), vehicle = initial.vehicle.copy(gpsFixType = 0u))
        val issue = observer.update(failed, 2000)
        assertEquals(2000, issue.incidents.single().observedAtMs)
        assertEquals(2000, observer.update(failed, 9000).incidents.single().observedAtMs)
        val resetButNoFix = observer.update(initial.copy(vehicle = initial.vehicle.copy(gpsFixType = 0u)), 10000)
        assertEquals("gps", resetButNoFix.incidents.single().id)
        val recovered = observer.update(initial, 11000)
        assertTrue(recovered.incidents.isEmpty())
        assertEquals(1, recovered.events.count { it.state == OpsEventState.RECOVERED })
        assertTrue(recovered.events.any { it.message.contains("No 3D fix → 3D fix") })
    }

    @Test fun rawTelemetryDoesNotFloodHistory() {
        val observer = OperationsObserver()
        val initial = healthy()
        observer.update(initial, 1000)
        repeat(1000) { index ->
            observer.update(initial.copy(vehicle = initial.vehicle.copy(northMeters = index.toFloat(),
                batteryRemainingPercent = (99 - index % 20).toByte())), 1001L + index)
        }
        assertTrue(observer.update(initial, 3000).events.isEmpty())
    }

    @Test fun transitionsAreBoundedAndKeepNewestOrderForFilters() {
        val observer = OperationsObserver(4)
        val initial = healthy()
        observer.update(initial, 0)
        repeat(10) { i -> observer.update(initial.copy(vehicle = initial.vehicle.copy(armed = i % 2 == 0)), i + 1L) }
        val result = observer.update(initial, 20)
        assertEquals(4, result.events.size)
        val filtered = filterOpsEvents(result.events, component = OpsComponent.CONTROL, state = OpsEventState.CHANGED, sinceMs = 9)
        assertEquals(listOf(10L, 9L), filtered.map { it.timestampMs })
        assertTrue(filterOpsEvents(result.events, severity = OpsSeverity.CRITICAL).isEmpty())
    }

    @Test fun resetDrainDoesNotRecoverActualLowBattery() {
        val observer = OperationsObserver()
        val low = healthy().copy(vehicle = healthy().vehicle.copy(batteryRemainingPercent = 10), failures = FailureState(batteryDrainMultiplier = 15f))
        assertEquals(setOf("battery", "drain"), observer.update(low, 0).incidents.map { it.id }.toSet())
        val reset = observer.update(low.copy(failures = FailureState()), 1000)
        assertEquals("battery", reset.incidents.single().id)
        assertEquals(OpsRecovery.RESET_BATTERY, reset.incidents.single().recovery)
        assertEquals("battery", observer.update(low.copy(failures = FailureState(), stateFresh = false,
            runtime = RuntimeLifecycleState.STOPPED), 2000).incidents.single().id)
    }

    @Test fun actualPeerExpiryIsSeparateFromInjectedLostLink() {
        val initial = healthy().copy(vehicle = healthy().vehicle.copy(armed = true, controlAuthority = ControlAuthority.GCS_MISSION),
            identity = MavlinkIdentityStatus(lastGcsHeartbeatAtMs = 1000, gcsConnected = false), failures = FailureState(lostLinkActive = true))
        val issues = OperationsObserver().update(initial, 20000).incidents
        assertEquals(setOf("lost_link", "gcs_expired"), issues.map { it.id }.toSet())
        assertEquals(OpsRecovery.CONTROL, issues.single { it.id == "gcs_expired" }.recovery)
    }

    @Test fun rejectionHasExactReasonAndDoesNotClaimFlightStopped() {
        val reason = "Unsupported command 999 at sequence 2"
        val result = OperationsObserver().update(healthy().copy(upload = MissionUploadStatus(
            phase = MissionUploadPhase.REJECTED, lastError = reason)), 1000)
        assertEquals(reason, result.incidents.single().consequence)
        assertEquals(OpsRecovery.REUPLOAD, result.incidents.single().recovery)
    }

    @Test fun everyFailureFieldHasAnIncidentAndSeverityIsOrdered() {
        val failures = FailureState(gpsEnabled = false, gpsNoiseMultiplier = 5f, compassEnabled = false,
            compassOffsetDeg = 20f, windSpeedMs = 8f, windGustsMs = 2f, motorFailureMask = 4,
            batteryDrainMultiplier = 8f, payloadMassKg = 1f, lostLinkActive = true, barometerOffsetMeters = 3f,
            unsafeMissionReserveActive = true)
        val result = OperationsObserver().update(healthy().copy(failures = failures), 0)
        assertEquals(setOf("gps", "gps_noise", "compass", "wind", "motor", "drain", "payload", "lost_link", "barometer", "reserve"), result.incidents.map { it.id }.toSet())
        assertEquals(OpsSeverity.CRITICAL, result.incidents.first().severity)
    }

    @Test fun foregroundBackgroundAndRecordingTransitionsRemainInProcessHistory() {
        val observer = OperationsObserver()
        observer.update(healthy(), 0)
        observer.update(healthy().copy(appVisible = false, recording = true), 1000)
        val result = observer.update(healthy().copy(recording = true), 2000)
        assertTrue(result.events.any { it.message == "App visibility: Foreground → Background" })
        assertTrue(result.events.any { it.message == "Recording: Stopped → Active" })
        assertTrue(OperationsObserver().update(healthy(), 3000).events.isEmpty())
    }
}
