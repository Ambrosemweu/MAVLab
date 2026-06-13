package com.ascend.mavlab.feature.drone3d

import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.MotorTelemetry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DroneModelControllerTest {
    private val controller = DroneModelController()

    @Test
    fun propellerAnimationSpeedUsesAverageRecordedMotorRpm() {
        val state = DroneState(
            armed = true,
            motors = listOf(
                MotorTelemetry(rpm = 120f),
                MotorTelemetry(rpm = 180f),
                MotorTelemetry(rpm = 60f),
                MotorTelemetry(rpm = 120f),
            ),
        )

        assertEquals(2f, controller.propAnimationSpeed(state))
    }

    @Test
    fun propellerAnimationSpeedPreservesHighRecordedRpmScale() {
        val state = DroneState(
            armed = true,
            motors = List(4) { MotorTelemetry(rpm = 6_000f) },
        )

        assertEquals(100f, controller.propAnimationSpeed(state))
    }

    @Test
    fun propellerAnimationPhaseAdvancesByRecordedRpmScale() {
        val nextPhase = controller.propellerAnimationPhaseSeconds(
            currentPhaseSeconds = 0.1f,
            animationDurationSeconds = 1f,
            rpmScale = 100f,
            deltaSeconds = 0.016f,
        )

        assertEquals(0.7f, nextPhase, absoluteTolerance = 0.001f)
    }

    @Test
    fun propellerAnimationPhaseFreezesWhenRpmStops() {
        val nextPhase = controller.propellerAnimationPhaseSeconds(
            currentPhaseSeconds = 0.4f,
            animationDurationSeconds = 1f,
            rpmScale = 0f,
            deltaSeconds = 0.016f,
        )

        assertEquals(0.4f, nextPhase)
    }

    @Test
    fun propellerAnimationSpeedIgnoresDisarmedFailedAndStoppedMotors() {
        val disarmed = DroneState(
            armed = false,
            motors = listOf(MotorTelemetry(rpm = 120f)),
        )
        val mixed = DroneState(
            armed = true,
            motors = listOf(
                MotorTelemetry(rpm = 500f, failed = true),
                MotorTelemetry(rpm = 50f),
                MotorTelemetry(rpm = 120f),
            ),
        )

        assertEquals(0f, controller.propAnimationSpeed(disarmed))
        assertEquals(2f, controller.propAnimationSpeed(mixed))
    }

    @Test
    fun propellerAnimationEnabledFollowsActiveRecordedRpm() {
        assertFalse(
            controller.propAnimationEnabled(
                DroneState(
                    armed = true,
                    motors = listOf(MotorTelemetry(rpm = 50f)),
                ),
            ),
        )
        assertTrue(
            controller.propAnimationEnabled(
                DroneState(
                    armed = true,
                    motors = listOf(MotorTelemetry(rpm = 51f)),
                ),
            ),
        )
        assertFalse(
            controller.propAnimationEnabled(
                DroneState(
                    armed = true,
                    motors = listOf(MotorTelemetry(rpm = 500f, failed = true)),
                ),
            ),
        )
    }
}
