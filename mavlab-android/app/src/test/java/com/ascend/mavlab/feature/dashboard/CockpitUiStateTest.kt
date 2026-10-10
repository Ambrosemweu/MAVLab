package com.ascend.mavlab.feature.dashboard

import com.ascend.mavlab.core.common.ControllerInputMode
import com.ascend.mavlab.core.common.RuntimeLifecycleState
import com.ascend.mavlab.core.mavlink.MavlinkIdentityStatus
import com.ascend.mavlab.simulation.engine.ControlAuthority
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.FlightMode
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.mission.MissionCommand
import com.ascend.mavlab.simulation.mission.MissionItem
import com.ascend.mavlab.simulation.mission.MissionOrigin
import com.ascend.mavlab.simulation.mission.MissionProgress
import com.ascend.mavlab.simulation.mission.MissionUploadStatus
import com.ascend.mavlab.simulation.recording.FlightRecordingStatus
import com.ascend.mavlab.simulation.recording.FlightSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CockpitUiStateTest {
    @Test
    fun readyPresentationShowsArmAsThePrimaryAction() {
        val ui = derive()

        assertEquals(CockpitFlightPhase.DISARMED_GROUND, ui.flightPhase)
        assertEquals("READY · DISARMED", ui.eyebrow)
        assertEquals(CockpitAction.ARM, ui.primaryAction?.action)
        assertTrue(assertNotNull(ui.primaryAction).enabled)
        assertEquals(CockpitAction.OPEN_CONTROLLER, ui.taskActions.getOrNull(0)?.action)
    }

    @Test
    fun armedGroundPresentationProvidesTakeoffAndDisarm() {
        val ui = derive(state = DroneState(armed = true, controlAuthority = ControlAuthority.CONTROLLER))

        assertEquals(CockpitFlightPhase.ARMED_GROUND, ui.flightPhase)
        assertTrue(assertNotNull(ui.primaryAction).enabled)
        assertEquals(CockpitAction.DISARM, ui.primaryAction.action)
        assertEquals(CockpitAction.TAKEOFF, ui.taskActions.getOrNull(0)?.action)
        assertEquals(CockpitAction.DISARM, ui.taskActions.getOrNull(1)?.action)
    }

    @Test
    fun externalAuthorityDoesNotExposeAnImplicitLocalTakeoff() {
        val ui = derive(
            state = DroneState(
                armed = true,
                controlAuthority = ControlAuthority.GCS_DIRECT,
            ),
        )

        assertEquals(CockpitAction.OPEN_OPS, ui.taskActions.getOrNull(0)?.action)
        assertEquals(CockpitAction.TAKE_CONTROL_AND_LAND, ui.taskActions.getOrNull(1)?.action)
    }

    @Test
    fun manualFlightRoutesBackToControllerAndKeepsLandReachable() {
        val ui = derive(
            state = DroneState(
                armed = true,
                altitudeAglMeters = 12.4f,
                controlAuthority = ControlAuthority.CONTROLLER,
            ),
        )

        assertEquals(CockpitFlightPhase.AIRBORNE, ui.flightPhase)
        assertEquals("IN FLIGHT · MANUAL", ui.eyebrow)
        assertEquals(CockpitAction.OPEN_CONTROLLER, ui.taskActions.getOrNull(0)?.action)
        assertEquals(CockpitAction.LAND, ui.taskActions.getOrNull(1)?.action)
    }

    @Test
    fun activeAutoMissionKeepsAuthorityAndTakeoverSemanticsExplicit() {
        val mission = activeMission()
        val ui = derive(
            state = DroneState(
                armed = true,
                altitudeAglMeters = 22.4f,
                mode = FlightMode.AUTO,
                controlAuthority = ControlAuthority.GCS_MISSION,
            ),
            mission = mission,
        )

        assertEquals("AUTO MISSION · LIVE", ui.eyebrow)
        assertEquals("GCS Mission", ui.task.title)
        assertEquals("WP 2/3 · 33%", ui.task.detail)
        assertEquals(CockpitAction.TAKE_CONTROL_AND_LAND, ui.taskActions.single().action)
    }

    @Test
    fun rtlAndLandingHaveDistinctPresentations() {
        val rtl = derive(
            state = DroneState(
                armed = true,
                altitudeAglMeters = 18f,
                mode = FlightMode.RTL,
                controlAuthority = ControlAuthority.CONTROLLER,
            ),
        )
        val landing = derive(
            state = DroneState(
                armed = true,
                altitudeAglMeters = 6f,
                verticalSpeedMS = -0.8f,
                mode = FlightMode.LAND,
                controlAuthority = ControlAuthority.CONTROLLER,
            ),
        )

        assertEquals(CockpitFlightPhase.RETURNING, rtl.flightPhase)
        assertEquals("RETURNING HOME", rtl.eyebrow)
        assertEquals(CockpitAction.LAND, rtl.taskActions.getOrNull(0)?.action)
        assertEquals(CockpitFlightPhase.LANDING, landing.flightPhase)
        assertEquals("LANDING · ACTIVE", landing.eyebrow)
        assertTrue(landing.taskActions.isEmpty())
    }

    @Test
    fun criticalBatterySupersedesNormalFlightAction() {
        val ui = derive(
            state = DroneState(
                armed = true,
                altitudeAglMeters = 14f,
                batteryRemainingPercent = 12,
                controlAuthority = ControlAuthority.CONTROLLER,
            ),
        )

        assertEquals(CockpitAlertSeverity.CRITICAL, assertNotNull(ui.alert).severity)
        assertEquals(CockpitAction.LAND, ui.taskActions.getOrNull(0)?.action)
        assertEquals(CockpitAction.OPEN_OPS, ui.taskActions.getOrNull(1)?.action)
    }

    @Test
    fun staleTelemetryLocksFlightActionsAndRoutesToOps() {
        val ui = derive(
            state = DroneState(armed = true, altitudeAglMeters = 10f),
            updatedAtElapsedMs = 1_000L,
            nowElapsedMs = 8_000L,
        )

        assertEquals(CockpitDataFreshness.STALE, ui.freshness)
        assertFalse(assertNotNull(ui.primaryAction).enabled)
        assertEquals(CockpitAction.OPEN_OPS, ui.taskActions.getOrNull(0)?.action)
        assertEquals(null, ui.taskActions.getOrNull(1))
    }

    @Test
    fun recentlyClosedRecordingProducesFlightCompletePresentation() {
        val session = FlightSession(
            id = "SIM-TEST",
            startedAtMs = 10_000L,
            endedAtMs = 95_000L,
            directoryPath = "/tmp/mavlab-test",
        )
        val ui = derive(
            recording = FlightRecordingStatus(lastSession = session),
            nowEpochMs = 100_000L,
        )

        assertEquals(CockpitFlightPhase.FLIGHT_COMPLETE, ui.flightPhase)
        assertEquals(CockpitAction.REVIEW_FLIGHT, ui.taskActions.getOrNull(0)?.action)
        assertEquals(CockpitAction.NEW_FLIGHT, ui.taskActions.getOrNull(1)?.action)
    }

    @Test
    fun externalControlDisablesArmingWhileDisarmed() {
        val ui = derive(state = DroneState(controlAuthority = ControlAuthority.GCS_DIRECT))
        assertFalse(assertNotNull(ui.primaryAction).enabled)
    }

    private fun derive(
        state: DroneState = DroneState(),
        mission: MissionProgress = MissionProgress(),
        recording: FlightRecordingStatus = FlightRecordingStatus(),
        updatedAtElapsedMs: Long? = 900L,
        nowElapsedMs: Long = 1_000L,
        nowEpochMs: Long = 100_000L,
    ): CockpitUiState {
        val ui = deriveCockpitUiState(
            state = state,
            mission = mission,
            uploadStatus = MissionUploadStatus(),
            recording = recording,
            failures = FailureState(),
            identity = MavlinkIdentityStatus(),
            runtimeLifecycle = RuntimeLifecycleState.RUNNING,
            stateUpdatedAtElapsedRealtimeMs = updatedAtElapsedMs,
            nowElapsedRealtimeMs = nowElapsedMs,
            nowEpochMs = nowEpochMs,
            controllerInputMode = ControllerInputMode.PHONE_SENSORS,
        )
        // The toggle must describe the next action in every phase and recovery state.
        assertEquals(if (state.armed) CockpitAction.DISARM else CockpitAction.ARM, ui.primaryAction?.action)
        assertEquals(if (state.armed) "Disarm" else "Arm", ui.primaryAction?.label)
        assertEquals(CockpitAction.OPEN_SIM, ui.secondaryAction?.action)
        assertEquals("SIM", ui.secondaryAction?.label)
        assertTrue(assertNotNull(ui.secondaryAction).enabled)
        return ui
    }

    private fun activeMission(): MissionProgress {
        val items = listOf(
            missionItem(0),
            missionItem(1),
            missionItem(2),
        )
        return MissionProgress(
            items = items,
            currentIndex = 1,
            complete = false,
            lastReachedSequence = 0,
            origin = MissionOrigin.GCS_UPLOAD,
            activeTarget = items[1],
            executionStarted = true,
        )
    }

    private fun missionItem(sequence: Int): MissionItem {
        return MissionItem(
            sequence = sequence,
            command = MissionCommand.WAYPOINT,
            latitudeDeg = -1.2921,
            longitudeDeg = 36.8219,
            altitudeAglMeters = 10f,
            localNorthMeters = sequence * 10f,
            localEastMeters = 0f,
        )
    }
}
