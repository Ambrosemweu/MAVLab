package com.ascend.mavlab.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

class RuntimeComponentPolicyTest {
    @Test
    fun visibleRuntimeEnablesSensorsAndMotorAudio() {
        assertEquals(
            RuntimeComponentState(
                phoneSensorInputEnabled = true,
                droneAudioEnabled = true,
            ),
            RuntimeComponentPolicy.decide(runtimeActive = true, appVisible = true),
        )
    }

    @Test
    fun backgroundRuntimeDisablesSensorsButKeepsMotorAudio() {
        assertEquals(
            RuntimeComponentState(
                phoneSensorInputEnabled = false,
                droneAudioEnabled = true,
            ),
            RuntimeComponentPolicy.decide(runtimeActive = true, appVisible = false),
        )
    }

    @Test
    fun stoppedRuntimeDisablesSensorsAndMotorAudio() {
        assertEquals(
            RuntimeComponentState(
                phoneSensorInputEnabled = false,
                droneAudioEnabled = false,
            ),
            RuntimeComponentPolicy.decide(runtimeActive = false, appVisible = true),
        )
    }
}
