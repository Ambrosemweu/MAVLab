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
    fun propellerAnimationSpeedCapsHighRpmToPreventAliasing() {
        val state = DroneState(
            armed = true,
            motors = List(4) { MotorTelemetry(rpm = 6_000f) },
        )
        val speed = controller.propAnimationSpeed(state)
        // At 6000 RPM, linear scale would be 100 (6000/60).
        // With capping: 18 + 3 * ln(100/18) ≈ 23.1
        // Must be well below the uncapped 100 and within the anti-aliased range.
        assertTrue(speed > 18f, "Speed should be above linear cap threshold")
        assertTrue(speed < 30f, "Speed should be logarithmically capped")
    }

    @Test
    fun propellerAnimationSpeedIsLinearBelowCap() {
        val state = DroneState(
            armed = true,
            motors = List(4) { MotorTelemetry(rpm = 600f) },
        )
        // 600 RPM → linearScale = 10, below the 18 cap → stays linear
        assertEquals(10f, controller.propAnimationSpeed(state))
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
    fun stoppedTransformDecisionFollowsIndividualMotorTelemetry() {
        val state = DroneState(
            armed = true,
            motors = listOf(
                MotorTelemetry(rpm = 600f),
                MotorTelemetry(rpm = 600f, failed = true),
                MotorTelemetry(rpm = 900f),
                MotorTelemetry(rpm = 0f),
            ),
        )

        assertFalse(controller.propellerShouldFreeze(state, 0))
        assertTrue(controller.propellerShouldFreeze(state, 1))
        assertFalse(controller.propellerShouldFreeze(state, 2))
        assertTrue(controller.propellerShouldFreeze(state, 3))
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
