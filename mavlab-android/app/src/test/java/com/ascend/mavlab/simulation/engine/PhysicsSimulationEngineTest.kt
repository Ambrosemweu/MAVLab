package com.ascend.mavlab.simulation.engine

import com.ascend.mavlab.simulation.autopilot.PilotInput
import com.ascend.mavlab.simulation.mission.MissionCommand
import com.ascend.mavlab.simulation.mission.MissionItem
import com.ascend.mavlab.simulation.mission.MissionProgress
import com.ascend.mavlab.simulation.physics.QuadcopterParams
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PhysicsSimulationEngineTest {
    @Test
    fun finalUploadDiagnosticsRetainTheLastRequestedAndValidatedSequence() {
        val engine = PhysicsSimulationEngine()
        engine.beginMissionUpload(expectedCount = 5, requestedSequence = 0)
        engine.recordMissionUploadProgress(5, 2, lastReceivedSequence = 1, nextRequestedSequence = 2)
        engine.rejectMissionUpload("Unsupported command 999 at sequence 2", expectedCount = 5, receivedCount = 2)
        assertEquals(2, engine.missionUploadStatus.value.lastRequestedSequence)
        assertEquals(1, engine.missionUploadStatus.value.lastReceivedSequence)
        engine.beginMissionUpload(expectedCount = 1, requestedSequence = 0)
        engine.acceptMissionUpload(1, lastReceivedSequence = 0)
        assertEquals(0, engine.missionUploadStatus.value.lastRequestedSequence)
        assertEquals(0, engine.missionUploadStatus.value.lastReceivedSequence)
        engine.acceptMissionUpload(0)
        assertEquals(null, engine.missionUploadStatus.value.lastRequestedSequence)
    }

    @Test
    fun receivedSourceAndMslAltitudeNormalizeBeforeSpatialPublication() {
        val engine = PhysicsSimulationEngine()
        val initial = engine.state.value
        engine.loadMission(listOf(MissionItem(0, MissionCommand.WAYPOINT, initial.latitudeDeg, initial.longitudeDeg, initial.altitudeMslMeters + 20f, altitudeIsMsl = true)), com.ascend.mavlab.simulation.mission.MissionOrigin.GCS_UPLOAD)
        val mission = engine.missionProgress.value
        assertTrue(mission.received)
        assertFalse(mission.executionStarted)
        assertEquals(20f, mission.items.single().altitudeAglMeters)
        assertFalse(mission.items.single().altitudeIsMsl)
        assertEquals(0f, mission.items.single().localNorthMeters)
        engine.clearMission()
        assertFalse(engine.missionProgress.value.received)
        engine.loadDemoMission()
        assertFalse(engine.missionProgress.value.received)
    }

    @Test
    fun autoModeAckWhileDisarmedDoesNotAdvanceMissionOrClaimExecution() {
        val engine = PhysicsSimulationEngine()
        val initial = engine.state.value
        engine.loadMission(listOf(MissionItem(0, MissionCommand.WAYPOINT, initial.latitudeDeg, initial.longitudeDeg, 0f)), com.ascend.mavlab.simulation.mission.MissionOrigin.GCS_UPLOAD)
        engine.setMode(FlightMode.AUTO)
        repeat(5) { engine.tickForTest(0.02f) }
        assertFalse(engine.missionProgress.value.executionStarted)
        assertFalse(engine.missionProgress.value.complete)
        engine.setArmed(true, ControlAuthority.GCS_MISSION)
        engine.tickForTest(0.02f)
        assertTrue(engine.missionProgress.value.executionStarted)
        assertTrue(engine.missionProgress.value.complete)
    }

    @Test
    fun localTakeoffKeepsPhonePilotInputEffectiveInAuthoritativeState() {
        val engine = PhysicsSimulationEngine()
        engine.takeoff(5f, ControlAuthority.CONTROLLER)
        engine.setPilotInput(PilotInput(roll = 0.4f, pitch = -0.3f, yaw = 0.2f, throttle = 0.5f))

        repeat(150) { engine.tickForTest(0.02f) }

        val state = engine.state.value
        assertTrue(state.armed)
        assertTrue(state.altitudeAglMeters > 0.5f)
        assertTrue(state.rollRadians > 0.05f, "Local takeoff must retain roll input: ${state.rollRadians}")
        assertTrue(state.pitchRadians < -0.05f, "Local takeoff must retain pitch input: ${state.pitchRadians}")
        assertTrue(state.yawRadians > 0.05f, "Local takeoff must retain yaw input: ${state.yawRadians}")
        assertEquals(ControlAuthority.CONTROLLER, state.controlAuthority)
        assertEquals(FlightMode.ALT_HOLD, state.mode)
    }

    @Test
    fun newPhoneFlightClearsRetainedNavigationModeAndRpmOverride() {
        val engine = PhysicsSimulationEngine()
        engine.setMode(FlightMode.GUIDED, ControlAuthority.GCS_DIRECT)
        engine.setMotorSpeedOverrideRpm(4_000f)
        engine.armForLocalInput(PilotInput(roll = 0.4f, pitch = -0.3f, yaw = 0.2f, throttle = 0.65f))

        repeat(100) { engine.tickForTest(0.02f) }

        val state = engine.state.value
        assertTrue(state.armed)
        assertEquals(FlightMode.STABILIZE, state.mode)
        assertEquals(ControlAuthority.CONTROLLER, state.controlAuthority)
        assertTrue(state.rollRadians > 0.05f)
        assertTrue(state.pitchRadians < -0.05f)
        assertTrue(state.yawRadians > 0.05f)
        assertTrue(state.motors.map { it.rpm }.distinct().size > 1, "Stale RPM override must not mask sensor input")
    }

    @Test
    fun localRearmPreservesAnExplicitManualAltitudeHoldMode() {
        val engine = PhysicsSimulationEngine()
        engine.setMode(FlightMode.ALT_HOLD)
        engine.armForLocalInput(PilotInput(throttle = 0.5f))

        assertTrue(engine.state.value.armed)
        assertEquals(FlightMode.ALT_HOLD, engine.state.value.mode)
    }

    @Test
    fun defaultThrottlePhoneInputChangesMotorCommandsImmediatelyAfterArming() {
        val engine = PhysicsSimulationEngine()
        engine.armForLocalInput(PilotInput(roll = 0.4f, pitch = -0.3f, throttle = 0.5f))
        engine.tickForTest(0.02f)

        val armed = engine.state.value
        assertTrue(armed.armed)
        assertTrue(armed.motors.map { it.command }.distinct().size > 1)
        assertTrue(armed.motors.map { it.rpm }.distinct().size > 1)

        engine.setArmed(false)
        engine.tickForTest(0.02f)
        assertTrue(engine.state.value.motors.all { it.rpm == 0f })
    }

    @Test
    fun gcsTakeoffRetainsGuidedNavigationInsteadOfLocalGyroSteering() {
        val engine = PhysicsSimulationEngine()
        engine.setPilotInput(PilotInput(roll = 0.4f, pitch = -0.3f, yaw = 0.2f))
        engine.takeoff(5f, ControlAuthority.GCS_DIRECT)

        repeat(150) { engine.tickForTest(0.02f) }

        val state = engine.state.value
        assertTrue(state.altitudeAglMeters > 0.5f)
        assertEquals(FlightMode.GUIDED, state.mode)
        assertEquals(ControlAuthority.GCS_DIRECT, state.controlAuthority)
        assertEquals(0f, state.rollRadians, absoluteTolerance = 0.001f)
        assertEquals(0f, state.pitchRadians, absoluteTolerance = 0.001f)
    }

    @Test
    fun initialStateStartsOnGroundForQgcTakeoff() {
        val engine = PhysicsSimulationEngine()

        assertEquals(0f, engine.state.value.altitudeAglMeters)
        assertEquals(1805f, engine.state.value.altitudeMslMeters)
    }

    @Test
    fun setHomeLocationWhileDisarmedUpdatesGcsVisiblePosition() {
        val engine = PhysicsSimulationEngine()

        val changed = engine.setHomeLocation(SimLocation.London)
        engine.tickForTest()

        val state = engine.state.value
        assertTrue(changed)
        assertEquals(SimLocation.London.latitudeDeg, state.latitudeDeg, absoluteTolerance = 0.000001)
        assertEquals(SimLocation.London.longitudeDeg, state.longitudeDeg, absoluteTolerance = 0.000001)
        assertEquals(SimLocation.London.altitudeMslMeters, state.altitudeMslMeters, absoluteTolerance = 0.01f)
        assertEquals(0f, state.altitudeAglMeters)
        assertEquals(0f, state.northMeters)
        assertEquals(0f, state.eastMeters)
    }

    @Test
    fun setHomeLocationIsRejectedWhileArmed() {
        val engine = PhysicsSimulationEngine()
        engine.setArmed(true)
        val before = engine.state.value

        val changed = engine.setHomeLocation(SimLocation.Dubai)

        val after = engine.state.value
        assertFalse(changed)
        assertEquals(before.latitudeDeg, after.latitudeDeg)
        assertEquals(before.longitudeDeg, after.longitudeDeg)
        assertEquals(before.altitudeMslMeters, after.altitudeMslMeters)
    }

    @Test
    fun homePositionDefaultsToInitialLocation() {
        val engine = PhysicsSimulationEngine()

        val home = engine.homePosition()

        assertEquals(SimLocation.Nairobi.latitudeDeg, home.latitudeDeg, absoluteTolerance = 0.000001)
        assertEquals(SimLocation.Nairobi.longitudeDeg, home.longitudeDeg, absoluteTolerance = 0.000001)
        assertEquals(SimLocation.Nairobi.altitudeMslMeters, home.altitudeMslMeters, absoluteTolerance = 0.01f)
    }

    @Test
    fun setHomeToCoordinatesIsReflectedInHomePosition() {
        val engine = PhysicsSimulationEngine()
        val targetLat = SimLocation.Nairobi.latitudeDeg + 0.001
        val targetLon = SimLocation.Nairobi.longitudeDeg - 0.001

        val changed = engine.setHomeToCoordinates(targetLat, targetLon)

        val home = engine.homePosition()
        assertTrue(changed)
        assertEquals(targetLat, home.latitudeDeg, absoluteTolerance = 0.000001)
        assertEquals(targetLon, home.longitudeDeg, absoluteTolerance = 0.000001)
    }

    @Test
    fun setHomeToCoordinatesRejectsInvalidCoordinates() {
        val engine = PhysicsSimulationEngine()

        assertFalse(engine.setHomeToCoordinates(91.0, 0.0))
        assertFalse(engine.setHomeToCoordinates(0.0, 181.0))
        assertFalse(engine.setHomeToCoordinates(Double.NaN, 0.0))
    }

    @Test
    fun setHomeToCurrentPositionCapturesCurrentLocation() {
        val engine = PhysicsSimulationEngine()

        val changed = engine.setHomeToCurrentPosition()

        val home = engine.homePosition()
        val state = engine.state.value
        assertTrue(changed)
        assertEquals(state.latitudeDeg, home.latitudeDeg, absoluteTolerance = 0.000001)
        assertEquals(state.longitudeDeg, home.longitudeDeg, absoluteTolerance = 0.000001)
    }

    @Test
    fun armingAfterLandingStartsNewFlightInsteadOfInstantlyDisarming() {
        val engine = PhysicsSimulationEngine()
        engine.land()
        engine.tickForTest()
        assertEquals(FlightMode.LAND, engine.state.value.mode)

        engine.setArmed(true)
        engine.tickForTest()

        assertTrue(engine.state.value.armed)
        assertEquals(FlightMode.STABILIZE, engine.state.value.mode)
    }

    @Test
    fun disarmedMotorsReportZeroRpm() {
        val engine = PhysicsSimulationEngine()

        engine.tickForTest()

        assertTrue(engine.state.value.motors.all { it.rpm == 0f })
        assertTrue(engine.state.value.motors.all { it.command == 0f })
    }

    @Test
    fun directRpmOverrideDoesNotSpinMotorsWhileDisarmed() {
        val engine = PhysicsSimulationEngine()

        engine.setMotorSpeedOverrideRpm(6000f)
        engine.tickForTest()

        assertTrue(engine.state.value.motors.all { it.rpm == 0f })
        assertTrue(engine.state.value.motors.all { it.command == 0f })

        engine.setArmed(true)
        engine.tickForTest()

        assertTrue(engine.state.value.motors.all { it.rpm > 5900f })
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
    fun landModeHoldsPositionAndBrakesHorizontalVelocity() {
        val engine = PhysicsSimulationEngine()

        engine.takeoff(8f, ControlAuthority.GCS_DIRECT)
        engine.setPilotInput(PilotInput(roll = 1f, pitch = 1f, throttle = 0.6f))
        engine.land(ControlAuthority.GCS_DIRECT)

        val input = engine.previewLandingPilotInput(
            state = DroneState(
                armed = true,
                mode = FlightMode.LAND,
                controlAuthority = ControlAuthority.GCS_DIRECT,
                altitudeAglMeters = 5f,
                northVelocityMS = 3f,
                eastVelocityMS = -2f,
            ),
            dt = 0.1f,
        )

        assertTrue(input.pitch < -0.01f)
        assertTrue(input.roll < -0.01f)
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
    fun autoTakeoffClimbsBeforeFlyingToTakeoffPosition() {
        val engine = PhysicsSimulationEngine()
        val takeoff = MissionItem(
            sequence = 1,
            command = MissionCommand.TAKEOFF,
            latitudeDeg = 0.0,
            longitudeDeg = 0.0,
            altitudeAglMeters = 50f,
            localNorthMeters = 12f,
            localEastMeters = -9f,
        )
        val progress = MissionProgress(
            items = listOf(
                takeoff.copy(sequence = 0, command = MissionCommand.WAYPOINT, altitudeAglMeters = 0f),
                takeoff,
            ),
            currentIndex = 1,
            complete = false,
            activeTarget = takeoff,
        )

        val climbTarget = engine.previewAutoPathTarget(
            state = DroneState(northMeters = 2f, eastMeters = 3f, altitudeAglMeters = 20f),
            progress = progress,
            target = takeoff,
        )
        assertEquals(2f, climbTarget.x, absoluteTolerance = 0.001f)
        assertEquals(3f, climbTarget.y, absoluteTolerance = 0.001f)
        assertEquals(50f, climbTarget.z, absoluteTolerance = 0.001f)

        val horizontalTarget = engine.previewAutoPathTarget(
            state = DroneState(northMeters = 2f, eastMeters = 3f, altitudeAglMeters = 49f),
            progress = progress,
            target = takeoff,
        )
        assertEquals(12f, horizontalTarget.x, absoluteTolerance = 0.001f)
        assertEquals(-9f, horizontalTarget.y, absoluteTolerance = 0.001f)
        assertEquals(50f, horizontalTarget.z, absoluteTolerance = 0.001f)
    }

    @Test
    fun autoMissionUsesLatestUploadedSpeedCommand() {
        val engine = PhysicsSimulationEngine()
        val speed = MissionItem(
            sequence = 1,
            command = MissionCommand.CHANGE_SPEED,
            latitudeDeg = 0.0,
            longitudeDeg = 0.0,
            altitudeAglMeters = 0f,
            speedMetersPerSecond = 7.5f,
        )
        val waypoint = MissionItem(
            sequence = 2,
            command = MissionCommand.WAYPOINT,
            latitudeDeg = 0.0,
            longitudeDeg = 0.0,
            altitudeAglMeters = 20f,
            localNorthMeters = 100f,
            localEastMeters = 0f,
        )

        val speedBeforeCommand = engine.previewAutoMissionSpeed(
            MissionProgress(
                items = listOf(speed, waypoint),
                currentIndex = 0,
                complete = false,
                activeTarget = speed,
            ),
        )
        val speedAfterCommand = engine.previewAutoMissionSpeed(
            MissionProgress(
                items = listOf(speed, waypoint),
                currentIndex = 1,
                complete = false,
                activeTarget = waypoint,
            ),
        )

        assertEquals(7.5f, speedBeforeCommand, absoluteTolerance = 0.001f)
        assertEquals(7.5f, speedAfterCommand, absoluteTolerance = 0.001f)
    }

    @Test
    fun autoPathLookaheadScalesWithUploadedMissionSpeed() {
        val engine = PhysicsSimulationEngine()
        val speed = MissionItem(
            sequence = 0,
            command = MissionCommand.CHANGE_SPEED,
            latitudeDeg = 0.0,
            longitudeDeg = 0.0,
            altitudeAglMeters = 0f,
            speedMetersPerSecond = 20f,
        )
        val start = MissionItem(
            sequence = 1,
            command = MissionCommand.WAYPOINT,
            latitudeDeg = 0.0,
            longitudeDeg = 0.0,
            altitudeAglMeters = 20f,
            localNorthMeters = 0f,
            localEastMeters = 0f,
        )
        val end = start.copy(
            sequence = 2,
            localNorthMeters = 100f,
            localEastMeters = 0f,
        )

        val target = engine.previewAutoPathTarget(
            state = DroneState(northMeters = 0f, eastMeters = 0f, altitudeAglMeters = 20f),
            progress = MissionProgress(
                items = listOf(speed, start, end),
                currentIndex = 2,
                complete = false,
                activeTarget = end,
            ),
            target = end,
        )

        assertEquals(30f, target.x, absoluteTolerance = 0.05f)
        assertEquals(0f, target.y, absoluteTolerance = 0.05f)
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

    @Test
    fun testGcsSpeedOverrideAndParameter() {
        val engine = PhysicsSimulationEngine()
        
        // Default parameter speed limit
        assertEquals(3.0f, engine.wpNavSpeedLimitMps)
        assertEquals(null, engine.gcsSpeedOverride)
        
        // Parameter update
        engine.setWpNavSpeedParameter(5.5f)
        assertEquals(5.5f, engine.wpNavSpeedLimitMps)
        
        // GCS speed override
        engine.setWpNavSpeed(8.0f)
        assertEquals(8.0f, engine.gcsSpeedOverride)
        
        // GCS speed ignore (-1)
        engine.setWpNavSpeed(-1f)
        assertEquals(8.0f, engine.gcsSpeedOverride)
        
        // GCS speed clear (-2)
        engine.setWpNavSpeed(-2f)
        assertEquals(null, engine.gcsSpeedOverride)
    }

    @Test
    fun testMissionSpeedWithAssociatedChangeSpeed() {
        val engine = PhysicsSimulationEngine()
        
        val wp1 = MissionItem(
            sequence = 1,
            command = MissionCommand.WAYPOINT,
            latitudeDeg = 0.0,
            longitudeDeg = 0.0,
            altitudeAglMeters = 20f,
            localNorthMeters = 50f,
            localEastMeters = 0f,
        )
        val speed1 = MissionItem(
            sequence = 2,
            command = MissionCommand.CHANGE_SPEED,
            latitudeDeg = 0.0,
            longitudeDeg = 0.0,
            altitudeAglMeters = 0f,
            speedMetersPerSecond = 12.0f,
        )
        val wp2 = MissionItem(
            sequence = 3,
            command = MissionCommand.WAYPOINT,
            latitudeDeg = 0.0,
            longitudeDeg = 0.0,
            altitudeAglMeters = 20f,
            localNorthMeters = 100f,
            localEastMeters = 0f,
        )
        val progress = MissionProgress(
            items = listOf(wp1, speed1, wp2),
            currentIndex = 0, // flying to wp1 (sequence 1)
            complete = false,
            activeTarget = wp1,
        )
        
        // Flying towards wp1 (sequence 1), the CHANGE_SPEED is at sequence 2.
        // It is associated with the preceding NAV command (wp1), so it should apply while flying to wp1.
        val speed = engine.previewAutoMissionSpeed(progress)
        assertEquals(12.0f, speed)
    }

    @Test
    fun depletedBatteryDisarmsDroneAndCutsMotors() {
        // Create an engine with a tiny battery capacity so it drains instantly
        val params = QuadcopterParams(batteryCapacityWh = 0.0001f)
        val engine = PhysicsSimulationEngine(params = params)

        // It should start armed and motors spinning
        engine.setArmed(true)
        engine.tickForTest()
        assertTrue(engine.state.value.armed)
        assertTrue(engine.state.value.motors.all { it.rpm > 0f })

        // The second tick should detect the depleted battery (0%) and disarm + stop motors
        engine.tickForTest()
        assertFalse(engine.state.value.armed)
        assertTrue(engine.state.value.motors.all { it.rpm == 0f })

        // Attempting to re-arm with a depleted battery should be blocked
        engine.setArmed(true)
        assertFalse(engine.state.value.armed)
    }


    private companion object {
        const val MetersPerLatDeg = 111_320.0

        fun lonMetersPerDeg(latitudeDeg: Double): Double {
            return MetersPerLatDeg * kotlin.math.max(0.2, kotlin.math.cos(Math.toRadians(latitudeDeg)))
        }
    }
}
