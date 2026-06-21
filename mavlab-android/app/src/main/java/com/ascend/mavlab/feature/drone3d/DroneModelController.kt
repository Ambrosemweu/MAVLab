package com.ascend.mavlab.feature.drone3d

import com.ascend.mavlab.simulation.engine.DroneState
import com.google.android.filament.TransformManager
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.tanh

class DroneModelController {
    private var propellerAnimationPhaseSeconds = 0f
    private var lastPropellerFrameNanos: Long? = null
    private var boundModelNode: ModelNode? = null
    private var propellerTransforms: List<PropellerTransformBinding?> = emptyList()

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
        val linearScale = activeRpm.average().toFloat() / AnimationClipRevolutionsPerMinute
        // Cap the visual speed to prevent stroboscopic aliasing (wagon-wheel effect)
        // on typical 60fps displays. Above the threshold, scale logarithmically so
        // higher RPMs still look perceptibly faster without phase-wrapping artifacts.
        return if (linearScale <= MaxLinearAnimationSpeed) {
            linearScale
        } else {
            MaxLinearAnimationSpeed + LogarithmicSpeedGain * ln(linearScale / MaxLinearAnimationSpeed)
        }
    }

    fun applyPropellerAnimation(modelNode: ModelNode, state: DroneState, animationName: String, frameTimeNanos: Long) {
        val animator = modelNode.animator
        val animationIndex = (0 until animator.animationCount).firstOrNull { index ->
            animator.getAnimationName(index) == animationName
        } ?: return
        val durationSeconds = animator.getAnimationDuration(animationIndex).coerceAtLeast(MinimumAnimationDurationSeconds)
        val transforms = bindPropellerTransforms(modelNode)
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
        freezeStoppedPropellerTransforms(transforms, state)
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

    internal fun propellerShouldFreeze(state: DroneState, motorIndex: Int): Boolean {
        val motor = state.motors.getOrNull(motorIndex) ?: return true
        return motor.failed || motor.rpm <= MinimumAnimatedRpm
    }

    private fun bindPropellerTransforms(modelNode: ModelNode): List<PropellerTransformBinding?> {
        if (boundModelNode !== modelNode) {
            boundModelNode = modelNode
            lastPropellerFrameNanos = null
            propellerAnimationPhaseSeconds = 0f
            val transformManager = modelNode.engine.transformManager
            propellerTransforms = PropellerNodeNames.map { name ->
                val entity = modelNode.model.getFirstEntityByName(name)
                if (entity != 0 && transformManager.hasComponent(entity)) {
                    PropellerTransformBinding(
                        transformManager = transformManager,
                        transformInstance = transformManager.getInstance(entity),
                    )
                } else {
                    null
                }
            }
        }
        return propellerTransforms
    }

    private fun freezeStoppedPropellerTransforms(
        transforms: List<PropellerTransformBinding?>,
        state: DroneState,
    ) {
        transforms.forEachIndexed { index, binding ->
            binding ?: return@forEachIndexed
            if (propellerShouldFreeze(state, index)) {
                val frozenTransform = binding.frozenTransform
                    ?: binding.transformManager.getTransform(binding.transformInstance, FloatArray(16)).copyOf()
                        .also { binding.frozenTransform = it }
                binding.transformManager.setTransform(binding.transformInstance, frozenTransform)
            } else {
                binding.frozenTransform = null
            }
        }
    }

    private fun inspectionOffset(value: Float, rangeMeters: Float, maxOffset: Float): Float {
        return (tanh((value / rangeMeters).toDouble()) * maxOffset).toFloat()
    }

    private fun radiansToDegrees(radians: Float): Float = radians * 180f / PI.toFloat()

    private data class PropellerTransformBinding(
        val transformManager: TransformManager,
        val transformInstance: Int,
        var frozenTransform: FloatArray? = null,
    )

    private companion object {
        val PropellerNodeNames = listOf("Prop_FL", "Prop_FR", "Prop_RL", "Prop_RR")
        const val SceneHorizontalRangeMeters = 45f
        const val SceneHorizontalMaxOffset = 0.85f
        const val SceneAltitudeRangeMeters = 28f
        const val SceneAltitudeMaxOffset = 1.2f
        const val MinimumAnimatedRpm = 50f
        const val AnimationClipRevolutionsPerMinute = 60f
        // At 60fps, MaxLinearAnimationSpeed of 18 advances ~0.30 of the animation
        // per frame — fast visible spin without aliasing.
        const val MaxLinearAnimationSpeed = 18f
        const val LogarithmicSpeedGain = 3f
        const val NanosPerSecond = 1_000_000_000f
        const val MaxFrameDeltaSeconds = 0.1f
        const val MinimumAnimationDurationSeconds = 0.001f
    }
}
