package com.ascend.mavlab.simulation.engine

import com.ascend.mavlab.simulation.autopilot.PilotInput
import kotlin.test.Test
import kotlin.test.assertEquals

class ControlAuthorityTest {
    @Test
    fun defaultDroneStateAuthorityIsIdle() {
        assertEquals(ControlAuthority.IDLE, DroneState().controlAuthority)
    }

    @Test
    fun controllerTakeoffMarksControllerAuthority() {
        val engine = PhysicsSimulationEngine()

        engine.takeoff(12f)

        assertEquals(ControlAuthority.CONTROLLER, engine.state.value.controlAuthority)
    }

    @Test
    fun armedControllerInputMarksControllerAuthority() {
        val engine = PhysicsSimulationEngine()
        engine.setArmed(true, ControlAuthority.GCS_DIRECT)

        engine.setPilotInput(PilotInput(roll = 0.4f, throttle = 0.6f))

        assertEquals(ControlAuthority.CONTROLLER, engine.state.value.controlAuthority)
    }

    @Test
    fun controllerInputDoesNotStealGcsMissionAuthority() {
        val engine = PhysicsSimulationEngine()
        engine.loadDemoMission()
        engine.setArmed(true, ControlAuthority.GCS_MISSION)
        engine.setMode(FlightMode.AUTO, ControlAuthority.GCS_MISSION)

        engine.setPilotInput(PilotInput(roll = 0.4f, throttle = 0.6f))

        assertEquals(ControlAuthority.GCS_MISSION, engine.state.value.controlAuthority)
    }
}
