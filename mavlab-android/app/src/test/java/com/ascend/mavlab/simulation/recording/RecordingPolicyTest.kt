package com.ascend.mavlab.simulation.recording

import com.ascend.mavlab.simulation.engine.DroneState
import java.nio.file.Files
import kotlin.test.*

class RecordingPolicyTest {
    @Test fun recordingIsOffByDefaultForEveryArmedFlightIncludingMissions() {
        val policy = RecordingPolicy()
        assertFalse(policy.automaticEnabled)
        assertFalse(policy.shouldStartAutomatically(armed = true, active = false))
        policy.startedByUser()
        assertFalse(policy.automaticEnabled)
        policy.disarmed()
        policy.runtimeStopped()
        assertFalse(policy.shouldStartAutomatically(true, false))
    }

    @Test fun opsOptInAndOptOutControlAutomaticCapture() {
        val policy = RecordingPolicy()
        policy.setAutomaticEnabled(true)
        assertTrue(policy.shouldStartAutomatically(true, false))
        policy.setAutomaticEnabled(false)
        assertFalse(policy.shouldStartAutomatically(true, false))
        policy.disarmed()
        policy.runtimeStopped()
        assertFalse(policy.automaticEnabled)
    }

    @Test fun deliberateStopDoesNotRestartUntilNextArm() {
        val policy = RecordingPolicy(automaticEnabled = true)
        assertTrue(policy.shouldStartAutomatically(armed = true, active = false))
        policy.stoppedByUser(armed = true)
        repeat(100) { assertFalse(policy.shouldStartAutomatically(armed = true, active = false)) }
        policy.disarmed()
        assertTrue(policy.shouldStartAutomatically(armed = true, active = false))
        assertFalse(policy.shouldStartAutomatically(armed = false, active = false))
    }

    @Test fun explicitResumeAndRuntimeStopRestoreAutomaticCapture() {
        val policy = RecordingPolicy(automaticEnabled = true)
        policy.stoppedByUser(true)
        policy.startedByUser()
        assertTrue(policy.shouldStartAutomatically(true, false))
        assertFalse(policy.shouldStartAutomatically(true, true))
        policy.stoppedByUser(true)
        policy.runtimeStopped()
        assertTrue(policy.shouldStartAutomatically(true, false))
    }

    @Test fun explicitGroundRecordingAndStopSaveWithoutChangingVehicle() {
        val base = Files.createTempDirectory("mavlab-recording-policy").toFile()
        try {
            val vehicle = DroneState(armed = false)
            val recorder = FlightRecorder(base, clockMs = { 1000 })
            val policy = RecordingPolicy()
            policy.startedByUser()
            recorder.startSession("manual ground exercise", "ground")
            recorder.appendTelemetry(vehicle)
            recorder.closeSession("explicit stop", 2000)
            policy.stoppedByUser(vehicle.armed)
            assertFalse(recorder.status.value.active)
            assertTrue(base.resolve("mavlab/flights/ground/report.md").isFile)
            assertFalse(vehicle.armed)
            assertFalse(policy.shouldStartAutomatically(false, false))
        } finally { base.deleteRecursively() }
    }
}
