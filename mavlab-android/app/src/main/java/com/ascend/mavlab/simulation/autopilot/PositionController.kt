package com.ascend.mavlab.simulation.autopilot

import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.physics.QuadcopterParams
import kotlin.math.cos
import kotlin.math.sin

class PositionController(
    private val params: QuadcopterParams = QuadcopterParams(),
) {
    private val north = PIDController(kP = 0.22f, kI = 0.015f, kD = 0.08f, iMax = 3f, outputMin = -1f, outputMax = 1f)
    private val east = PIDController(kP = 0.22f, kI = 0.015f, kD = 0.08f, iMax = 3f, outputMin = -1f, outputMax = 1f)

    fun computePilotInput(
        state: DroneState,
        targetNorthMeters: Float,
        targetEastMeters: Float,
        targetAltitudeMeters: Float,
        dt: Float,
    ): PilotInput {
        val northCommand = north.update(targetNorthMeters - state.northMeters, dt)
        val eastCommand = east.update(targetEastMeters - state.eastMeters, dt)

        val yaw = state.yawRadians
        val cosYaw = cos(yaw)
        val sinYaw = sin(yaw)
        val bodyPitch = (northCommand * cosYaw + eastCommand * sinYaw).coerceIn(-1f, 1f)
        val bodyRoll = (-northCommand * sinYaw + eastCommand * cosYaw).coerceIn(-1f, 1f)

        val altitudeError = targetAltitudeMeters - state.altitudeAglMeters
        val throttle = (0.5f + altitudeError * 0.08f - state.verticalSpeedMS * 0.04f)
            .coerceIn(0.35f, 0.65f)

        return PilotInput(
            roll = bodyRoll,
            pitch = bodyPitch,
            throttle = throttle,
            yaw = 0f,
        )
    }

    fun reset() {
        north.reset()
        east.reset()
    }
}
