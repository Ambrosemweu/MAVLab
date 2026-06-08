package com.ascend.mavlab.feature.controller

import com.ascend.mavlab.core.sensors.OrientationData
import com.ascend.mavlab.simulation.autopilot.PilotInput
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

class ControlMapper(
    private val config: ControlConfig = ControlConfig(),
) {
    fun map(
        orientation: OrientationData,
        throttle: Float,
        manualYaw: Float = 0f,
    ): PilotInput {
        val mappedYaw = mapAxis(orientation.yaw, config.maxYawAngleRad)
        return PilotInput(
            roll = mapAxis(orientation.roll, config.maxRollAngleRad),
            pitch = mapAxis(-orientation.pitch, config.maxPitchAngleRad),
            throttle = throttle.coerceIn(0f, 1f),
            yaw = (mappedYaw + manualYaw).coerceIn(-1f, 1f),
        )
    }

    fun mapAxis(value: Float, maxAngle: Float): Float {
        val absolute = abs(value)
        if (absolute < config.deadzoneRad) return 0f
        val normalized = ((absolute - config.deadzoneRad) / (maxAngle - config.deadzoneRad))
            .coerceIn(0f, 1f)
        return normalized.pow(config.expo) * sign(value)
    }
}
