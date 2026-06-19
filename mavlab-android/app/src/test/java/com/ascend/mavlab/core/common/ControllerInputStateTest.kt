package com.ascend.mavlab.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

class ControllerInputStateTest {
    @Test
    fun defaultStateMatchesDisarmResetValues() {
        val state = ControllerInputState()

        assertEquals(ControllerInputMode.PHONE_SENSORS, state.inputMode)
        assertEquals(0.5f, state.throttle)
        assertEquals(0f, state.manualRoll)
        assertEquals(0f, state.manualPitch)
        assertEquals(0f, state.manualYaw)
        assertEquals(0f, state.directRpm)
    }

    @Test
    fun sanitizedClampsControllerInputs() {
        val state = ControllerInputState(
            throttle = 2f,
            manualRoll = -2f,
            manualPitch = 2f,
            manualYaw = -2f,
            directRpm = 20000f,
        ).sanitized()

        assertEquals(1f, state.throttle)
        assertEquals(-1f, state.manualRoll)
        assertEquals(1f, state.manualPitch)
        assertEquals(-1f, state.manualYaw)
        assertEquals(ControllerInputState.MaxDirectRpm, state.directRpm)
    }
}
