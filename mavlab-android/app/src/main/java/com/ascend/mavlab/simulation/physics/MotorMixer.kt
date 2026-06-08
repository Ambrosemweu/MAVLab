package com.ascend.mavlab.simulation.physics

import kotlin.math.sqrt

class MotorMixer(private val params: QuadcopterParams = QuadcopterParams()) {
    fun mix(
        throttle: Float,
        roll: Float,
        pitch: Float,
        yaw: Float,
    ): MotorOutput {
        val base = throttle.coerceIn(0f, 1f)
        val mixScale = 0.16f
        val normalized = floatArrayOf(
            base - roll * mixScale + pitch * mixScale + yaw * mixScale,
            base + roll * mixScale + pitch * mixScale - yaw * mixScale,
            base + roll * mixScale - pitch * mixScale + yaw * mixScale,
            base - roll * mixScale - pitch * mixScale - yaw * mixScale,
        )
        val speeds = FloatArray(4) { index ->
            (normalized[index].coerceIn(0f, 1f) * params.motorMaxSpeedRadS)
                .coerceIn(params.motorMinSpeedRadS, params.motorMaxSpeedRadS)
        }
        return MotorOutput(speeds = speeds, throttle = base)
    }

    fun hoverSpeeds(): FloatArray = FloatArray(4) { params.hoverMotorSpeedRadS }

    fun speedsForTotalThrust(thrustNewtons: Float): FloatArray {
        val perMotor = (thrustNewtons.coerceAtLeast(0f) / 4f)
        val speed = sqrt(perMotor / params.thrustCoefficient)
            .coerceIn(params.motorMinSpeedRadS, params.motorMaxSpeedRadS)
        return FloatArray(4) { speed }
    }
}

data class MotorOutput(
    val speeds: FloatArray,
    val throttle: Float,
)
