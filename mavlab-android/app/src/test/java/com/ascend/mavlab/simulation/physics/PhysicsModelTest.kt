package com.ascend.mavlab.simulation.physics

import com.ascend.mavlab.simulation.engine.DroneState
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class PhysicsModelTest {
    private val params = QuadcopterParams()
    private val physics = PhysicsModel(params)
    private val mixer = MotorMixer(params)

    @Test
    fun disarmedFreeFallAcceleratesDownwardAtGravity() {
        val initial = DroneState(altitudeAglMeters = 20f, altitudeMslMeters = 1825f)

        val afterOneSecond = simulate(initial, FloatArray(4), seconds = 1f)

        assertTrue(
            afterOneSecond.verticalSpeedMS < -9.0f && afterOneSecond.verticalSpeedMS > -10.2f,
            "Expected near gravity free fall, got ${afterOneSecond.verticalSpeedMS} m/s",
        )
    }

    @Test
    fun hoverMotorSpeedsProduceNearZeroVerticalAcceleration() {
        val initial = DroneState(altitudeAglMeters = 10f, altitudeMslMeters = 1815f)

        val afterOneSecond = simulate(initial, mixer.hoverSpeeds(), seconds = 1f)

        assertTrue(
            abs(afterOneSecond.verticalSpeedMS) < 0.25f,
            "Expected hover vertical speed near zero, got ${afterOneSecond.verticalSpeedMS} m/s",
        )
    }

    @Test
    fun stateRemainsFiniteUnderMaxInputsForTenSimulatedMinutes() {
        var state = DroneState(
            altitudeAglMeters = 10f,
            altitudeMslMeters = 1815f,
            rollRadians = 0.1f,
            pitchRadians = -0.1f,
        )
        val speeds = floatArrayOf(
            params.motorMaxSpeedRadS,
            params.motorMinSpeedRadS,
            params.motorMaxSpeedRadS,
            params.motorMinSpeedRadS,
        )

        repeat(60_000) {
            state = physics.step(state, speeds, 0.01f)
            assertTrue(state.isFinite(), "State diverged at step $it: $state")
        }
    }

    @Test
    fun rollPitchYawTorqueSignsAreObservable() {
        val initial = DroneState(altitudeAglMeters = 10f, altitudeMslMeters = 1815f)

        val rollPositive = physics.step(initial, mixer.mix(0.5f, roll = 1f, pitch = 0f, yaw = 0f).speeds, 0.02f)
        val pitchPositive = physics.step(initial, mixer.mix(0.5f, roll = 0f, pitch = 1f, yaw = 0f).speeds, 0.02f)
        val yawPositive = physics.step(initial, mixer.mix(0.5f, roll = 0f, pitch = 0f, yaw = 1f).speeds, 0.02f)

        assertTrue(rollPositive.rollSpeedRadS > 0f, "Positive roll mix should create positive roll rate")
        assertTrue(pitchPositive.pitchSpeedRadS > 0f, "Positive pitch mix should create positive pitch rate")
        assertTrue(yawPositive.yawSpeedRadS > 0f, "Positive yaw mix should create positive yaw rate")
    }

    private fun simulate(initial: DroneState, motorSpeeds: FloatArray, seconds: Float): DroneState {
        var state = initial
        repeat((seconds / 0.01f).toInt()) {
            state = physics.step(state, motorSpeeds, 0.01f)
        }
        return state
    }

    private fun DroneState.isFinite(): Boolean = listOf(
        latitudeDeg.toFloat(),
        longitudeDeg.toFloat(),
        altitudeMslMeters,
        altitudeAglMeters,
        rollRadians,
        pitchRadians,
        yawRadians,
        rollSpeedRadS,
        pitchSpeedRadS,
        yawSpeedRadS,
        groundSpeedMS,
        verticalSpeedMS,
        northMeters,
        eastMeters,
        northVelocityMS,
        eastVelocityMS,
    ).all { it.isFinite() }
}
