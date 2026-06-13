package com.ascend.mavlab.simulation.autopilot

import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.physics.QuadcopterParams
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class PositionController(
    private val params: QuadcopterParams = QuadcopterParams(),
) {
    private val KpPos = 0.8f
    private val maxSpeed = 3.0f

    // Inner velocity PID loops
    private val velocityNorth = PIDController(kP = 0.18f, kI = 0.04f, kD = 0.01f, iMax = 1.0f, outputMin = -1f, outputMax = 1f)
    private val velocityEast = PIDController(kP = 0.18f, kI = 0.04f, kD = 0.01f, iMax = 1.0f, outputMin = -1f, outputMax = 1f)

    fun computePilotInput(
        state: DroneState,
        targetNorthMeters: Float,
        targetEastMeters: Float,
        targetAltitudeMeters: Float,
        dt: Float,
        maxHorizontalSpeedMS: Float = maxSpeed,
    ): PilotInput {
        // 1. Position loop (Proportional only): Target Position -> Desired Velocity
        val errorNorth = targetNorthMeters - state.northMeters
        val errorEast = targetEastMeters - state.eastMeters
        val horizontalDistance = kotlin.math.sqrt(errorNorth * errorNorth + errorEast * errorEast)
        val yawError = if (horizontalDistance > 0.5f) {
            normalizeRadians(atan2(errorEast, errorNorth) - state.yawRadians)
        } else {
            0f
        }
        val translationScale = translationScaleForYawError(yawError)

        val speedLimit = maxHorizontalSpeedMS
            .takeIf { it.isFinite() && it > 0f }
            ?: maxSpeed
        var desiredVelNorth = errorNorth * KpPos
        var desiredVelEast = errorEast * KpPos
        val desiredSpeed = kotlin.math.sqrt(desiredVelNorth * desiredVelNorth + desiredVelEast * desiredVelEast)
        if (desiredSpeed > speedLimit) {
            val scale = speedLimit / desiredSpeed
            desiredVelNorth *= scale
            desiredVelEast *= scale
        }
        desiredVelNorth *= translationScale
        desiredVelEast *= translationScale

        // 2. Velocity loop (PID): Desired Velocity -> Tilt Commands
        val velErrorNorth = desiredVelNorth - state.northVelocityMS
        val velErrorEast = desiredVelEast - state.eastVelocityMS

        val northCommand = velocityNorth.update(velErrorNorth, dt)
        val eastCommand = velocityEast.update(velErrorEast, dt)

        // 3. Coordinate rotation from local (NED) to body frame
        val yaw = state.yawRadians
        val cosYaw = cos(yaw)
        val sinYaw = sin(yaw)
        val bodyPitch = (northCommand * cosYaw + eastCommand * sinYaw).coerceIn(-1f, 1f)
        val bodyRoll = (northCommand * sinYaw - eastCommand * cosYaw).coerceIn(-1f, 1f)

        val altitudeError = targetAltitudeMeters - state.altitudeAglMeters
        val throttle = (0.5f + altitudeError * 0.08f - state.verticalSpeedMS * 0.04f)
            .coerceIn(0.35f, 0.65f)

        return PilotInput(
            roll = bodyRoll,
            pitch = bodyPitch,
            throttle = throttle,
            yaw = (yawError * YawRateGain).coerceIn(-1f, 1f),
        )
    }

    fun reset() {
        velocityNorth.reset()
        velocityEast.reset()
    }

    private fun translationScaleForYawError(yawError: Float): Float {
        val magnitude = abs(yawError)
        return when {
            magnitude >= TurnInPlaceYawErrorRad -> 0f
            magnitude >= SlowFlightYawErrorRad -> 0.35f
            else -> 1f
        }
    }

    private fun normalizeRadians(value: Float): Float {
        var angle = value
        val twoPi = (2f * PI).toFloat()
        while (angle > PI) angle -= twoPi
        while (angle < -PI) angle += twoPi
        return angle
    }

    private companion object {
        const val YawRateGain = 0.8f
        const val SlowFlightYawErrorRad = 0.45f
        const val TurnInPlaceYawErrorRad = 1.0f
    }
}
