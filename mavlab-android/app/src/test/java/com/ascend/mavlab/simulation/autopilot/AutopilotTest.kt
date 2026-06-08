package com.ascend.mavlab.simulation.autopilot

import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.FlightMode
import com.ascend.mavlab.simulation.engine.PhysicsSimulationEngine
import com.ascend.mavlab.simulation.physics.QuadcopterParams
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutopilotTest {
    @Test
    fun disarmedAutopilotOutputsZeroMotors() {
        val autopilot = Autopilot()

        val output = autopilot.computeMotorOutput(DroneState(), PilotInput(), dt = 0.01f)

        assertTrue(output.speeds.all { it == 0f })
        assertEquals(0f, output.throttle)
    }

    @Test
    fun armedStabilizeUsesPilotThrottle() {
        val params = QuadcopterParams()
        val autopilot = Autopilot(params)
        val state = DroneState(altitudeAglMeters = 10f)

        autopilot.setArmed(true, state)
        val output = autopilot.computeMotorOutput(state, PilotInput(throttle = params.hoverThrottle), dt = 0.01f)

        assertTrue(output.speeds.all { it > 0f })
        assertTrue(output.throttle > 0.45f)
    }

    @Test
    fun takeoffSwitchesToGuidedAndArms() {
        val engine = PhysicsSimulationEngine()

        engine.takeoff(12f)
        val state = engine.state.value

        assertTrue(state.armed)
        assertEquals(FlightMode.GUIDED, state.mode)
    }

    @Test
    fun altitudeHoldThrottleAboveCenterIncreasesTargetAltitude() {
        val autopilot = Autopilot()
        val state = DroneState(altitudeAglMeters = 5f)

        autopilot.setArmed(true, state)
        autopilot.setMode(FlightMode.ALT_HOLD, state)
        val before = autopilot.targetAltitudeM

        autopilot.computeMotorOutput(state, PilotInput(throttle = 1f), dt = 1f)

        assertTrue(autopilot.targetAltitudeM > before)
    }
}
