package com.ascend.mavlab.core.common

import com.ascend.mavlab.simulation.engine.ControlAuthority
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun phoneSensorInputDoesNotDependOnControllerScreenLifecycle() {
        val state = ControllerInputState(inputMode = ControllerInputMode.PHONE_SENSORS)

        assertTrue(
            state.acceptsPhoneSensorInput(
                sensorAvailable = true,
                controlAuthority = ControlAuthority.IDLE,
            ),
        )
        assertTrue(
            state.acceptsPhoneSensorInput(
                sensorAvailable = true,
                controlAuthority = ControlAuthority.CONTROLLER,
            ),
        )
    }

    @Test
    fun phoneSensorInputRespectsModeAvailabilityAndMissionAuthority() {
        val phoneSensors = ControllerInputState(inputMode = ControllerInputMode.PHONE_SENSORS)

        assertFalse(
            phoneSensors.acceptsPhoneSensorInput(
                sensorAvailable = false,
                controlAuthority = ControlAuthority.CONTROLLER,
            ),
        )
        assertFalse(
            phoneSensors.acceptsPhoneSensorInput(
                sensorAvailable = true,
                controlAuthority = ControlAuthority.GCS_MISSION,
            ),
        )
        assertFalse(
            ControllerInputState(inputMode = ControllerInputMode.CUSTOM_INPUT).acceptsPhoneSensorInput(
                sensorAvailable = true,
                controlAuthority = ControlAuthority.CONTROLLER,
            ),
        )
        assertFalse(
            ControllerInputState(inputMode = ControllerInputMode.DIRECT_RPM).acceptsPhoneSensorInput(
                sensorAvailable = true,
                controlAuthority = ControlAuthority.CONTROLLER,
            ),
        )
    }
}
