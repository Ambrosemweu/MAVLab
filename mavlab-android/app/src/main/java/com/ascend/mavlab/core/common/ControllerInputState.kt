package com.ascend.mavlab.core.common

data class ControllerInputState(
    val inputMode: ControllerInputMode = ControllerInputMode.PHONE_SENSORS,
    val throttle: Float = DefaultThrottle,
    val manualRoll: Float = 0f,
    val manualPitch: Float = 0f,
    val manualYaw: Float = 0f,
    val directRpm: Float = 0f,
) {
    fun sanitized(): ControllerInputState {
        return copy(
            throttle = throttle.coerceIn(0f, 1f),
            manualRoll = manualRoll.coerceIn(-1f, 1f),
            manualPitch = manualPitch.coerceIn(-1f, 1f),
            manualYaw = manualYaw.coerceIn(-1f, 1f),
            directRpm = directRpm.coerceIn(0f, MaxDirectRpm),
        )
    }

    companion object {
        const val DefaultThrottle = 0.5f
        const val MaxDirectRpm = 10000f
    }
}
