package com.ascend.mavlab.simulation.engine

import com.ascend.mavlab.simulation.autopilot.PilotInput
import com.ascend.mavlab.simulation.mission.MissionCommand
import com.ascend.mavlab.simulation.mission.MissionItem
import com.ascend.mavlab.simulation.mission.MissionProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PhysicsSimulationEngineTest {
    @Test
    fun initialStateStartsOnGroundForQgcTakeoff() {
        val engine = PhysicsSimulationEngine()

        assertEquals(0f, engine.state.value.altitudeAglMeters)
        assertEquals(1805f, engine.state.value.altitudeMslMeters)
    }

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

    @Test
    fun uploadedMissionPinsGlobalWaypointToLocalTarget() {
        val engine = PhysicsSimulationEngine()
        val initial = engine.state.value

        engine.loadMission(
            listOf(
                MissionItem(
                    sequence = 0,
                    command = MissionCommand.WAYPOINT,
                    latitudeDeg = initial.latitudeDeg + 14f / MetersPerLatDeg,
                    longitudeDeg = initial.longitudeDeg,
                    altitudeAglMeters = initial.altitudeAglMeters,
                ),
            ),
        )

        val target = engine.missionProgress.value.activeTarget

        assertEquals(14f, target?.localNorthMeters ?: -1f, absoluteTolerance = 0.05f)
        assertEquals(0f, target?.localEastMeters ?: Float.NaN, absoluteTolerance = 0.05f)
    }

    @Test
    fun qgcRtlItemReturnsToUploadedHomeAtMissionAltitude() {
        val engine = PhysicsSimulationEngine()
        val initial = engine.state.value
        val homeLat = initial.latitudeDeg + 12f / MetersPerLatDeg
        val homeLon = initial.longitudeDeg - 9f / lonMetersPerDeg(initial.latitudeDeg)

        engine.loadMission(
            listOf(
                MissionItem(
                    sequence = 0,
                    command = MissionCommand.WAYPOINT,
                    latitudeDeg = homeLat,
                    longitudeDeg = homeLon,
                    altitudeAglMeters = 1667f,
                ),
                MissionItem(
                    sequence = 1,
                    command = MissionCommand.TAKEOFF,
                    latitudeDeg = 0.0,
                    longitudeDeg = 0.0,
                    altitudeAglMeters = 50f,
                ),
                MissionItem(
                    sequence = 2,
                    command = MissionCommand.WAYPOINT,
                    latitudeDeg = initial.latitudeDeg + 60f / MetersPerLatDeg,
                    longitudeDeg = initial.longitudeDeg,
                    altitudeAglMeters = 50f,
                ),
                MissionItem(
                    sequence = 3,
                    command = MissionCommand.RTL,
                    latitudeDeg = 0.0,
                    longitudeDeg = 0.0,
                    altitudeAglMeters = 0f,
                ),
            ),
        )

        val takeoff = engine.missionProgress.value.items.first { it.command == MissionCommand.TAKEOFF }
        val rtl = engine.missionProgress.value.items.first { it.command == MissionCommand.RTL }

        assertEquals(12f, takeoff.localNorthMeters ?: Float.NaN, absoluteTolerance = 0.05f)
        assertEquals(-9f, takeoff.localEastMeters ?: Float.NaN, absoluteTolerance = 0.05f)
        assertEquals(12f, rtl.localNorthMeters ?: Float.NaN, absoluteTolerance = 0.05f)
        assertEquals(-9f, rtl.localEastMeters ?: Float.NaN, absoluteTolerance = 0.05f)
        assertEquals(50f, rtl.altitudeAglMeters)
    }

    @Test
    fun autoPathTargetTracksLineSegmentWithLookahead() {
        val engine = PhysicsSimulationEngine()
        val start = MissionItem(
            sequence = 0,
            command = MissionCommand.WAYPOINT,
            latitudeDeg = 0.0,
            longitudeDeg = 0.0,
            altitudeAglMeters = 20f,
            localNorthMeters = 0f,
            localEastMeters = 0f,
        )
        val end = start.copy(
            sequence = 1,
            localNorthMeters = 100f,
            localEastMeters = 0f,
        )
        val target = engine.previewAutoPathTarget(
            state = DroneState(northMeters = 20f, eastMeters = 20f, altitudeAglMeters = 20f),
            progress = MissionProgress(
                items = listOf(start, end),
                currentIndex = 1,
                complete = false,
                activeTarget = end,
            ),
            target = end,
        )

        assertEquals(28f, target.x, absoluteTolerance = 0.05f)
        assertEquals(0f, target.y, absoluteTolerance = 0.05f)
    }

    private companion object {
        const val MetersPerLatDeg = 111_320.0

        fun lonMetersPerDeg(latitudeDeg: Double): Double {
            return MetersPerLatDeg * kotlin.math.max(0.2, kotlin.math.cos(Math.toRadians(latitudeDeg)))
        }
    }
}
