package com.ascend.mavlab.feature.mission

import com.ascend.mavlab.simulation.engine.*
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.mission.*
import kotlin.test.*

class MissionUiStateTest {
    private val items = listOf(
        MissionItem(0, MissionCommand.WAYPOINT, -1.0, 36.0, 10f, localNorthMeters = 0f, localEastMeters = 0f),
        MissionItem(1, MissionCommand.WAYPOINT, -1.0, 36.0, 10f, localNorthMeters = 30f, localEastMeters = 40f),
    )
    private val received = MissionProgress(items, 1, false, activeTarget = items[1], origin = MissionOrigin.GCS_UPLOAD)
    private val flight = DroneState(armed = true, mode = FlightMode.AUTO, controlAuthority = ControlAuthority.GCS_MISSION, groundSpeedMS = 2f, altitudeAglMeters = 10f)
    private fun ui(mission: MissionProgress = received, state: DroneState = DroneState(), upload: MissionUploadStatus = MissionUploadStatus(), live: Boolean = true, failures: FailureState = FailureState()) = deriveMissionUiState(state, mission, upload, failures, live)

    @Test fun emptyMissionHasNoRowEvenAfterZeroItemAck() {
        assertFalse(ui(MissionProgress()).visible)
        assertFalse(ui(MissionProgress(), upload = MissionUploadStatus(MissionUploadPhase.ACCEPTED)).visible)
    }
    @Test fun receivingAndRejectedAreVisibleWithoutLoadedItems() {
        assertEquals(MissionPhase.RECEIVING, ui(MissionProgress(), upload = MissionUploadStatus(MissionUploadPhase.UPLOADING, 7, 3)).phase)
        val rejected = ui(MissionProgress(), upload = MissionUploadStatus(MissionUploadPhase.REJECTED, lastError = "unsupported command"))
        assertEquals(MissionPhase.BLOCKED, rejected.phase)
        assertFalse(rejected.policy.canStart)
    }
    @Test fun acceptedAckDoesNotImplyAutoExecution() {
        assertEquals(MissionPhase.ACCEPTED, ui(upload = MissionUploadStatus(MissionUploadPhase.ACCEPTED, 2, 2)).phase)
        assertEquals(MissionPhase.ACTIVE, ui(state = flight).phase)
        assertEquals(MissionPhase.INTERRUPTED, ui(received.copy(executionStarted = true), flight.copy(mode = FlightMode.LAND, controlAuthority = ControlAuthority.CONTROLLER)).phase)
        assertEquals(MissionPhase.COMPLETED, ui(received.copy(complete = true, activeTarget = null)).phase)
    }
    @Test fun unverifiedAndInternalRoutesCannotStart() {
        for (origin in listOf(MissionOrigin.UNVERIFIED, MissionOrigin.INTERNAL_FIXTURE)) {
            val result = ui(received.copy(origin = origin))
            assertEquals(MissionPhase.BLOCKED, result.phase)
            assertFalse(result.policy.canStart)
        }
    }
    @Test fun safetyAndFreshnessGuardStartAndClear() {
        assertTrue(ui().policy.canStart)
        assertTrue(ui().policy.canClear)
        assertFalse(ui(live = false).policy.canStart)
        assertFalse(ui(live = false).policy.canClear)
        assertFalse(ui(state = flight).policy.canClear)
        assertFalse(ui(state = DroneState(altitudeAglMeters = 5f)).policy.canStart)
        assertFalse(ui(failures = FailureState(gpsEnabled = false)).policy.canStart)
        assertFalse(ui(failures = FailureState(motorFailureMask = 1)).policy.canStart)
        assertFalse(ui(failures = FailureState(unsafeMissionReserveActive = true)).policy.canStart)
        assertFalse(ui(state = DroneState(batteryRemainingPercent = 10)).policy.canStart)
        assertFalse(ui(upload = MissionUploadStatus(MissionUploadPhase.UPLOADING)).policy.canClear)
    }
    @Test fun remainingDistanceUsesActualLocalPositionAndEtaRequiresMotion() {
        assertEquals(50f, ui().remainingMeters)
        assertNull(ui().horizontalEtaSeconds)
        assertEquals(25, ui(state = flight).horizontalEtaSeconds)
        assertNull(ui(state = flight.copy(groundSpeedMS = 0f)).horizontalEtaSeconds)
        assertNull(ui(state = flight, live = false).remainingMeters)
    }
    @Test fun replacementUploadFailureDoesNotMisrepresentAnExecutingMission() {
        val rejected = ui(state = flight, upload = MissionUploadStatus(MissionUploadPhase.REJECTED, lastError = "unsupported"))
        assertEquals(MissionPhase.BLOCKED, rejected.phase)
        assertTrue(rejected.summary.contains("prior AUTO"))
        assertTrue(rejected.warning!!.contains("still executing"))
        assertFalse(rejected.policy.canStart)
        val receiving = ui(state = flight, upload = MissionUploadStatus(MissionUploadPhase.UPLOADING, 7, 2))
        assertTrue(receiving.summary.contains("Receiving 2/7"))
        assertTrue(receiving.summary.contains("prior AUTO"))
    }
}
