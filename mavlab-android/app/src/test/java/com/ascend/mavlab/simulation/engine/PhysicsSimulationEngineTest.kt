package com.ascend.mavlab.simulation.engine

import com.ascend.mavlab.simulation.autopilot.PilotInput
import com.ascend.mavlab.simulation.mission.MissionCommand
import com.ascend.mavlab.simulation.mission.MissionItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PhysicsSimulationEngineTest {
    @Test
    fun disarmedMotorsReportZeroRpm() {
        val engine = PhysicsSimulationEngine()

        engine.tickForTest()

        assertTrue(engine.state.value.motors.all { it.rpm == 0f })
        assertTrue(engine.state.value.motors.all { it.command == 0f })
    }

    @Test
    fun armedTakeoffReportsNonZeroRpm() {
        val engine = PhysicsSimulationEngine()

        engine.takeoff(12f)
        engine.tickForTest()

        assertTrue(engine.state.value.motors.all { it.rpm > 0f })
    }

    @Test
    fun failedMotorReportsZeroRpm() {
        val engine = PhysicsSimulationEngine()

        engine.setArmed(true)
        engine.setPilotInput(PilotInput(throttle = 0.6f))
        engine.failureInjector.setMotorFailed(index = 2, failed = true)
        engine.tickForTest()

        val motors = engine.state.value.motors
        assertEquals(0f, motors[2].rpm)
        assertTrue(motors[2].failed)
        assertTrue(motors[2].command > 0f)
        assertTrue(motors.filterIndexed { index, _ -> index != 2 }.all { it.rpm > 0f })
    }

    @Test
    fun missionAutoFlightReportsChangingRpm() {
        val engine = PhysicsSimulationEngine()

        engine.loadDemoMission()
        engine.setArmed(true, ControlAuthority.GCS_MISSION)
        engine.setMode(FlightMode.AUTO, ControlAuthority.GCS_MISSION)
        engine.tickForTest()
        val first = engine.state.value.motors.map { it.rpm }

        Thread.sleep(15)
        engine.tickForTest()
        val second = engine.state.value.motors.map { it.rpm }

        assertTrue(first.all { it > 0f })
        assertTrue(second.all { it > 0f })
        assertNotEquals(first, second)
    }

    @Test
    fun autoMissionAdvancesTargetAndMovesPhysicsState() {
        val engine = PhysicsSimulationEngine()
        val initial = engine.state.value
        engine.loadMission(
            listOf(
                MissionItem(
                    sequence = 0,
                    command = MissionCommand.WAYPOINT,
                    latitudeDeg = initial.latitudeDeg,
                    longitudeDeg = initial.longitudeDeg,
                    altitudeAglMeters = initial.altitudeAglMeters,
                    acceptanceRadiusMeters = 3f,
                ),
                MissionItem(
                    sequence = 1,
                    command = MissionCommand.WAYPOINT,
                    latitudeDeg = initial.latitudeDeg + 12f / MetersPerLatDeg,
                    longitudeDeg = initial.longitudeDeg,
                    altitudeAglMeters = initial.altitudeAglMeters + 2f,
                    acceptanceRadiusMeters = 2f,
                ),
            ),
        )
        engine.setArmed(true, ControlAuthority.GCS_MISSION)
        engine.setMode(FlightMode.AUTO, ControlAuthority.GCS_MISSION)

        repeat(6) {
            Thread.sleep(12)
            engine.tickForTest()
        }

        val state = engine.state.value
        assertEquals(FlightMode.AUTO, state.mode)
        assertEquals(ControlAuthority.GCS_MISSION, state.controlAuthority)
        assertEquals(1, engine.missionProgress.value.currentIndex)
        assertTrue(state.motors.any { it.rpm > 0f })
        assertTrue(state.northMeters != initial.northMeters || state.altitudeAglMeters != initial.altitudeAglMeters)
    }

    private companion object {
        const val MetersPerLatDeg = 111_320.0
    }
}
