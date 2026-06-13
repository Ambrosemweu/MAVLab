package com.ascend.mavlab.feature.drone3d

import com.ascend.mavlab.simulation.engine.DroneState
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import kotlin.math.PI
import kotlin.math.tanh

class DroneModelController {
    private var propellerAnimationPhaseSeconds = 0f
    private var lastPropellerFrameNanos: Long? = null

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

    fun propAnimationSpeed(state: DroneState): Float {
        if (!state.armed) return 0f
        val activeRpm = state.motors
            .filter { !it.failed && it.rpm > MinimumAnimatedRpm }
            .map { it.rpm.coerceAtLeast(0f) }
        if (activeRpm.isEmpty()) return 0f
        return activeRpm.average().toFloat() / AnimationClipRevolutionsPerMinute
    }

    fun applyPropellerAnimation(modelNode: ModelNode, state: DroneState, animationName: String, frameTimeNanos: Long) {
        val animator = modelNode.animator
        val animationIndex = (0 until animator.animationCount).firstOrNull { index ->
            animator.getAnimationName(index) == animationName
        } ?: return
        val durationSeconds = animator.getAnimationDuration(animationIndex).coerceAtLeast(MinimumAnimationDurationSeconds)
        val previousFrameNanos = lastPropellerFrameNanos
        lastPropellerFrameNanos = frameTimeNanos
        if (previousFrameNanos != null) {
            propellerAnimationPhaseSeconds = propellerAnimationPhaseSeconds(
                currentPhaseSeconds = propellerAnimationPhaseSeconds,
                animationDurationSeconds = durationSeconds,
                rpmScale = propAnimationSpeed(state),
                deltaSeconds = ((frameTimeNanos - previousFrameNanos) / NanosPerSecond).coerceIn(0f, MaxFrameDeltaSeconds),
            )
        }
        animator.applyAnimation(animationIndex, propellerAnimationPhaseSeconds)
    }

    internal fun propellerAnimationPhaseSeconds(
        currentPhaseSeconds: Float,
        animationDurationSeconds: Float,
        rpmScale: Float,
        deltaSeconds: Float,
    ): Float {
        if (rpmScale <= 0f) return currentPhaseSeconds
        val duration = animationDurationSeconds.coerceAtLeast(MinimumAnimationDurationSeconds)
        val next = currentPhaseSeconds + rpmScale * deltaSeconds
        return next % duration
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
        const val AnimationClipRevolutionsPerMinute = 60f
        const val NanosPerSecond = 1_000_000_000f
        const val MaxFrameDeltaSeconds = 0.1f
        const val MinimumAnimationDurationSeconds = 0.001f
    }
}
