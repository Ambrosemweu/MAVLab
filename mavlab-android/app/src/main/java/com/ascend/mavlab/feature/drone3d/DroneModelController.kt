package com.ascend.mavlab.feature.drone3d

import com.ascend.mavlab.simulation.engine.DroneState
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import kotlin.math.PI
import kotlin.math.tanh

class DroneModelController {
    fun bodyPosition(state: DroneState): Position {
        return Position(
            x = inspectionOffset(state.eastMeters, SceneHorizontalRangeMeters, SceneHorizontalMaxOffset),
            y = 0.35f + inspectionOffset(state.altitudeAglMeters, SceneAltitudeRangeMeters, SceneAltitudeMaxOffset),
            z = -inspectionOffset(state.northMeters, SceneHorizontalRangeMeters, SceneHorizontalMaxOffset),
        )
    }

    fun bodyRotation(state: DroneState): Rotation {
        return Rotation(
            x = radiansToDegrees(state.pitchRadians),
            y = radiansToDegrees(state.yawRadians),
            z = -radiansToDegrees(state.rollRadians),
        )
    }

    fun rpmSummary(state: DroneState): String {
        return state.motors.joinToString(" / ") { motor ->
            if (motor.failed) "0!" else "%.0f".format(motor.rpm)
        }
    }

    fun propAnimationEnabled(state: DroneState): Boolean {
        return state.armed && state.motors.any { it.rpm > MinimumAnimatedRpm && !it.failed }
    }

    private fun inspectionOffset(value: Float, rangeMeters: Float, maxOffset: Float): Float {
        return (tanh((value / rangeMeters).toDouble()) * maxOffset).toFloat()
    }

    private fun radiansToDegrees(radians: Float): Float = radians * 180f / PI.toFloat()

    private companion object {
        const val SceneHorizontalRangeMeters = 45f
        const val SceneHorizontalMaxOffset = 1.25f
        const val SceneAltitudeRangeMeters = 28f
        const val SceneAltitudeMaxOffset = 1.85f
        const val MinimumAnimatedRpm = 50f
    }
}
