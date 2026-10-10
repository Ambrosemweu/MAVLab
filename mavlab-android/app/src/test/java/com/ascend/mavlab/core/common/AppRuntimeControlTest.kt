package com.ascend.mavlab.core.common

import com.ascend.mavlab.simulation.engine.ControlAuthority
import com.ascend.mavlab.simulation.engine.FlightMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppRuntimeControlTest {
    @Test
    fun customInputIsAppliedWithoutComposingControllerAndFreshArmRestoresManualMode() {
        try {
            AppRuntime.setArmed(false)
            AppRuntime.setMode(FlightMode.GUIDED)
            AppRuntime.setControllerInputMode(ControllerInputMode.CUSTOM_INPUT)
            AppRuntime.setControllerManualRoll(0.4f)
            AppRuntime.setControllerManualPitch(-0.3f)
            AppRuntime.setControllerThrottle(0.65f)
            AppRuntime.setArmed(true)
            assertEquals(FlightMode.STABILIZE, AppRuntime.state.value.mode)
            assertEquals(ControlAuthority.CONTROLLER, AppRuntime.state.value.controlAuthority)
            AppRuntime.takeoff(5f)
            AppRuntime.setControllerThrottle(0.5f)
            repeat(150) { AppRuntime.tickSimulationForTest(0.02f) }
            val flight = AppRuntime.state.value
            assertTrue(flight.altitudeAglMeters > 0.5f)
            assertTrue(flight.rollRadians > 0.05f, "Custom roll must reach engine physics without a screen")
            assertTrue(flight.pitchRadians < -0.05f, "Custom pitch must reach engine physics without a screen")
            AppRuntime.setMode(FlightMode.AUTO)
            AppRuntime.setControllerManualYaw(0.2f)
            assertEquals(ControlAuthority.GCS_MISSION, AppRuntime.state.value.controlAuthority)
        } finally {
            AppRuntime.setArmed(false)
            repeat(300) { AppRuntime.tickSimulationForTest(0.02f) }
            AppRuntime.setMode(FlightMode.STABILIZE)
        }
    }

    @Test
    fun directRpmAppliesThroughRuntimeAndMissionSuspendsOverride() {
        try {
            AppRuntime.setArmed(false)
            AppRuntime.setControllerInputMode(ControllerInputMode.DIRECT_RPM)
            assertEquals(0f, AppRuntime.controllerInputState.value.directRpm)
            assertNull(AppRuntime.motorSpeedOverrideRpm.value)
            AppRuntime.setArmed(true)
            AppRuntime.setControllerDirectRpm(3_500f)
            assertEquals(3_500f, AppRuntime.motorSpeedOverrideRpm.value)
            repeat(50) { AppRuntime.tickSimulationForTest(0.02f) }
            assertTrue(AppRuntime.state.value.motors.all { it.rpm > 3_400f && it.rpm < 3_600f })
            AppRuntime.setMode(FlightMode.AUTO)
            assertNull(AppRuntime.motorSpeedOverrideRpm.value)
            AppRuntime.setControllerDirectRpm(4_000f)
            assertNull(AppRuntime.motorSpeedOverrideRpm.value)
        } finally {
            AppRuntime.setArmed(false)
            repeat(300) { AppRuntime.tickSimulationForTest(0.02f) }
            AppRuntime.setMode(FlightMode.STABILIZE)
        }
    }

    @Test
    fun leavingDirectRpmAndLandingClearTheGlobalMotorOverride() {
        try {
            AppRuntime.setArmed(false)
            AppRuntime.setControllerInputMode(ControllerInputMode.DIRECT_RPM)
            AppRuntime.setArmed(true)
            AppRuntime.setControllerDirectRpm(3_500f)
            AppRuntime.setControllerInputMode(ControllerInputMode.PHONE_SENSORS)
            assertNull(AppRuntime.motorSpeedOverrideRpm.value)
            AppRuntime.setControllerInputMode(ControllerInputMode.DIRECT_RPM)
            AppRuntime.setControllerDirectRpm(3_500f)
            AppRuntime.land()
            assertNull(AppRuntime.motorSpeedOverrideRpm.value)
            assertEquals(FlightMode.LAND, AppRuntime.state.value.mode)
            assertEquals(ControllerInputMode.CUSTOM_INPUT, AppRuntime.controllerInputState.value.inputMode)
        } finally {
            AppRuntime.setArmed(false)
            AppRuntime.setMode(FlightMode.STABILIZE)
        }
    }

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
