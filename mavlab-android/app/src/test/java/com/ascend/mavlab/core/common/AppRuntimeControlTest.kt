package com.ascend.mavlab.core.common

import com.ascend.mavlab.simulation.engine.ControlAuthority
import com.ascend.mavlab.simulation.engine.FlightMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppRuntimeControlTest {
    @Test
    fun phoneSensorArmThroughRuntimeClearsStaleNavigationAndMotorOverride() {
        try {
            AppRuntime.setArmed(false)
            AppRuntime.setControllerInputMode(ControllerInputMode.PHONE_SENSORS)
            AppRuntime.setMode(FlightMode.GUIDED)
            AppRuntime.setMotorSpeedOverrideRpm(4_000f)

            AppRuntime.setArmed(true)

            assertTrue(AppRuntime.state.value.armed)
            assertEquals(FlightMode.STABILIZE, AppRuntime.state.value.mode)
            assertEquals(ControlAuthority.CONTROLLER, AppRuntime.state.value.controlAuthority)
            assertEquals(ControllerInputMode.PHONE_SENSORS, AppRuntime.controllerInputState.value.inputMode)
            assertNull(AppRuntime.motorSpeedOverrideRpm.value)
        } finally {
            AppRuntime.setArmed(false)
            AppRuntime.setMode(FlightMode.STABILIZE)
        }
    }

    @Test
    fun runtimePhoneTakeoffKeepsManualAltitudeHoldAndSensorSelection() {
        try {
            AppRuntime.setArmed(false)
            AppRuntime.setControllerInputMode(ControllerInputMode.PHONE_SENSORS)
            AppRuntime.setArmed(true)

            AppRuntime.takeoff(5f)

            assertTrue(AppRuntime.state.value.armed)
            assertEquals(FlightMode.ALT_HOLD, AppRuntime.state.value.mode)
            assertEquals(ControlAuthority.CONTROLLER, AppRuntime.state.value.controlAuthority)
            assertEquals(ControllerInputMode.PHONE_SENSORS, AppRuntime.controllerInputState.value.inputMode)
        } finally {
            AppRuntime.setArmed(false)
            AppRuntime.setMode(FlightMode.STABILIZE)
        }
    }
}
