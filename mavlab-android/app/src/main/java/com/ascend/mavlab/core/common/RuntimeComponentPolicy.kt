package com.ascend.mavlab.core.common

internal data class RuntimeComponentState(
    val phoneSensorInputEnabled: Boolean,
    val droneAudioEnabled: Boolean,
)

internal object RuntimeComponentPolicy {
    fun decide(runtimeActive: Boolean, appVisible: Boolean): RuntimeComponentState {
        return RuntimeComponentState(
            phoneSensorInputEnabled = runtimeActive && appVisible,
            droneAudioEnabled = runtimeActive,
        )
    }
}
