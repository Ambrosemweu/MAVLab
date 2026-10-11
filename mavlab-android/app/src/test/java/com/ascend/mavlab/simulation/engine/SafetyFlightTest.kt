package com.ascend.mavlab.simulation.engine

import com.ascend.mavlab.simulation.autopilot.PilotInput
import com.ascend.mavlab.simulation.mission.*
import com.ascend.mavlab.simulation.safety.*
import kotlin.test.*

class SafetyFlightTest {
    @Test fun originChangeAndAllStoreClearAreAtomicDisarmedOperations() {
        val engine = PhysicsSimulationEngine()
        val origin = engine.state.value
        engine.replaceSafetyPlan(1, listOf(SafetyItem(0, 5003, 0, origin.latitudeDeg, origin.longitudeDeg, param1 = 100f)))
        engine.replaceSafetyPlan(2, listOf(rally(engine)))
        engine.loadDemoMission()
        engine.setSafetyParameter("FENCE_ENABLE", 1f)
        engine.setArmed(true)
        assertFalse(engine.clearSafetyPlans(clearNavigation = true, disableFence = true))
        assertFalse(engine.setHomeLocation(SimLocation.Nairobi))
        assertEquals(1, engine.safetyPlans.value.fence.size)
        assertEquals(1, engine.safetyPlans.value.rally.size)
        assertTrue(engine.missionProgress.value.loaded)
        engine.setArmed(false)
        assertTrue(engine.setHomeLocation(SimLocation.Nairobi))
        assertTrue(engine.safetyPlans.value.fence.isEmpty())
        assertTrue(engine.safetyPlans.value.rally.isEmpty())
        assertFalse(engine.safetyPlans.value.config.enabled)
        assertFalse(engine.missionProgress.value.loaded)
    }
    @Test fun rtlOnGroundNeverLaunchesTheVehicle() {
        val engine = PhysicsSimulationEngine()
        engine.setArmed(true)
        engine.setMode(FlightMode.RTL)
        repeat(20) { engine.tickForTest(.02f) }
        assertFalse(engine.state.value.armed)
        assertEquals(0f, engine.state.value.altitudeAglMeters)
    }
    private fun fly(engine: PhysicsSimulationEngine, seconds: Int) = repeat(seconds * 50) { engine.tickForTest(0.02f) }
    private fun airborne(engine: PhysicsSimulationEngine, altitude: Float = 5f) {
        assertTrue(engine.takeoff(altitude))
        engine.setPilotInput(PilotInput(throttle = 0.5f))
        fly(engine, 15)
        assertTrue(engine.state.value.altitudeAglMeters > 2f)
    }
    private fun rally(engine: PhysicsSimulationEngine, seq: Int = 0, north: Float = 10f, alt: Float = 5f, frame: Int = 3) =
        SafetyItem(seq, 5100, frame, engine.state.value.latitudeDeg + north / 111_320.0, engine.state.value.longitudeDeg, alt)

    @Test fun disabledFencePreservesCanonicalManualPhoneInput() {
        val engine = PhysicsSimulationEngine()
        assertFalse(engine.safetyPlans.value.config.enabled)
        engine.armForLocalInput(PilotInput(roll = .4f, pitch = -.3f, throttle = .65f))
        fly(engine, 2)
        assertTrue(engine.state.value.rollRadians > .05f)
        assertTrue(engine.state.value.pitchRadians < -.05f)
        assertEquals(ControlAuthority.CONTROLLER, engine.state.value.controlAuthority)
        assertFalse(engine.state.value.safety.breached)
    }

    @Test fun reportOnlyBreachDoesNotStealGuidedControlOrCountEveryTick() {
        val engine = PhysicsSimulationEngine()
        assertTrue(engine.setSafetyParameter("FENCE_TYPE", 2f))
        assertTrue(engine.setSafetyParameter("FENCE_RADIUS", 10f))
        assertTrue(engine.setSafetyParameter("FENCE_ACTION", 0f))
        assertTrue(engine.setSafetyParameter("FENCE_ENABLE", 1f))
        airborne(engine)
        engine.setGuidedOffset(30f, 0f, 5f)
        fly(engine, 30)
        val s = engine.state.value
        assertTrue(s.northMeters > 20f)
        assertTrue(s.safety.breached); assertEquals(3, s.safety.breachType)
        assertEquals(1, s.safety.breachCount)
        assertEquals(FlightMode.GUIDED, s.mode)
        assertEquals("None", s.safety.recovery)
    }

    @Test fun fenceLandClearsMotorOverrideAndFinishesDisarmed() {
        val engine = PhysicsSimulationEngine()
        engine.setSafetyParameter("FENCE_TYPE", 1f)
        engine.setSafetyParameter("FENCE_ALT_MAX", 3f)
        engine.setSafetyParameter("FENCE_ACTION", 2f)
        engine.setSafetyParameter("FENCE_ENABLE", 1f)
        engine.takeoff(6f)
        engine.setPilotInput(PilotInput(throttle = .5f))
        fly(engine, 45)
        assertFalse(engine.state.value.armed)
        assertEquals(FlightMode.LAND, engine.state.value.mode)
        assertTrue(engine.state.value.altitudeAglMeters < .1f)
        assertEquals(1, engine.state.value.safety.breachCount)
    }

    @Test fun rtlUsesNearestRallyThenActuallyLandsAndDisarms() {
        val engine = PhysicsSimulationEngine()
        assertTrue(engine.replaceSafetyPlan(2, listOf(rally(engine, north = 8f), rally(engine, 1, 25f))))
        engine.setSafetyParameter("RALLY_INCL_HOME", 0f)
        airborne(engine)
        engine.setMode(FlightMode.RTL, ControlAuthority.GCS_DIRECT)
        assertEquals("Rally 1", engine.state.value.safety.returnTarget)
        fly(engine, 65)
        assertFalse(engine.state.value.armed)
        assertTrue(engine.state.value.altitudeAglMeters < .1f)
        assertTrue(kotlin.math.abs(engine.state.value.northMeters - 8f) < 3f, engine.state.value.toString())
    }

    @Test fun rallyDistanceAndUnsafeEndpointFallBackToHome() {
        val engine = PhysicsSimulationEngine()
        engine.replaceSafetyPlan(2, listOf(rally(engine, north = 100f)))
        engine.setSafetyParameter("RALLY_LIMIT_KM", .05f)
        engine.setSafetyParameter("RALLY_INCL_HOME", 0f)
        engine.setSafetyParameter("RTL_ALT", 500f)
        airborne(engine)
        engine.setMode(FlightMode.RTL)
        assertEquals("Home", engine.state.value.safety.returnTarget)
        engine.setArmed(false)
        engine.setSafetyParameter("RALLY_LIMIT_KM", 0f)
        engine.setSafetyParameter("FENCE_TYPE", 2f)
        engine.setSafetyParameter("FENCE_RADIUS", 30f)
        engine.setSafetyParameter("FENCE_ENABLE", 1f)
        engine.setArmed(true)
        engine.setMode(FlightMode.RTL)
        assertEquals("Home", engine.state.value.safety.returnTarget)
    }

    @Test fun gpsLossDuringReturnLandsRatherThanUsingUnavailableNavigation() {
        val engine = PhysicsSimulationEngine()
        airborne(engine)
        engine.setMode(FlightMode.RTL)
        engine.failureInjector.setGpsEnabled(false)
        engine.tickForTest(.02f)
        assertEquals(FlightMode.LAND, engine.state.value.mode)
        fly(engine, 40)
        assertFalse(engine.state.value.armed)
    }

    @Test fun missionRtlIsTerminalAndCompletesOnlyAfterLanding() {
        val engine = PhysicsSimulationEngine()
        engine.replaceSafetyPlan(2, listOf(rally(engine, north = 8f)))
        engine.setSafetyParameter("RALLY_INCL_HOME", 0f)
        airborne(engine)
        engine.loadMission(listOf(MissionItem(0, MissionCommand.RTL, engine.state.value.latitudeDeg, engine.state.value.longitudeDeg, 8f)))
        engine.setMode(FlightMode.AUTO)
        engine.tickForTest(.02f)
        assertFalse(engine.missionProgress.value.complete)
        assertEquals(FlightMode.RTL, engine.state.value.mode)
        fly(engine, 65)
        assertTrue(engine.missionProgress.value.complete)
        assertEquals(0, engine.missionProgress.value.lastReachedSequence)
        assertFalse(engine.state.value.armed)
    }

    @Test fun safetyPlansAreImmutableWhileArmedAndInvalidReplacementRetainsPrevious() {
        val engine = PhysicsSimulationEngine()
        val original = listOf(rally(engine))
        engine.replaceSafetyPlan(2, original)
        assertFalse(engine.replaceSafetyPlan(2, listOf(original.single().copy(command = 16))))
        assertEquals(original, engine.safetyPlans.value.rally)
        engine.setArmed(true)
        assertFalse(engine.replaceSafetyPlan(2, emptyList()))
        assertFalse(engine.setSafetyParameter("FENCE_ENABLE", 1f))
        assertTrue(engine.setSafetyParameter("FENCE_ENABLE", 0f))
        assertEquals(original, engine.safetyPlans.value.rally)
    }

    @Test fun prearmFenceChecksDenyOutOfBoundsLaunch() {
        val engine = PhysicsSimulationEngine()
        val s = engine.state.value
        engine.replaceSafetyPlan(1, listOf(SafetyItem(0, 5003, 0, s.latitudeDeg + .01, s.longitudeDeg, param1 = 10f)))
        engine.setSafetyParameter("FENCE_TYPE", 4f)
        engine.setSafetyParameter("FENCE_ENABLE", 1f)
        engine.setArmed(true)
        assertFalse(engine.state.value.armed)
        assertFalse(engine.takeoff(5f))
    }

    @Test fun fenceReturnHasItsOwnAuthorityAndDeliberateReversePath() {
        val engine = PhysicsSimulationEngine()
        engine.setSafetyParameter("FENCE_TYPE", 2f)
        engine.setSafetyParameter("FENCE_RADIUS", 8f)
        engine.setSafetyParameter("RTL_ALT", 500f)
        engine.setSafetyParameter("FENCE_ENABLE", 1f)
        airborne(engine)
        engine.setGuidedOffset(25f, 0f, 5f)
        repeat(1500) {
            if (engine.state.value.controlAuthority != ControlAuthority.SAFETY_RECOVERY) engine.tickForTest(.02f)
        }
        assertEquals(ControlAuthority.SAFETY_RECOVERY, engine.state.value.controlAuthority)
        assertEquals("Home", engine.state.value.safety.returnTarget)
        assertEquals(FlightMode.RTL, engine.state.value.mode)
        engine.setMotorSpeedOverrideRpm(4000f)
        engine.setMode(FlightMode.STABILIZE)
        engine.setPilotInput(PilotInput(roll = .5f, throttle = .8f))
        assertEquals(FlightMode.RTL, engine.state.value.mode)
        assertEquals(ControlAuthority.SAFETY_RECOVERY, engine.state.value.controlAuthority)
        assertTrue(engine.setSafetyParameter("FENCE_ENABLE", 0f))
        assertEquals(FlightMode.RTL, engine.state.value.mode) // disabling does not secretly cancel flight intent
        engine.setMode(FlightMode.ALT_HOLD)
        assertEquals(ControlAuthority.CONTROLLER, engine.state.value.controlAuthority)
        assertEquals(FlightMode.ALT_HOLD, engine.state.value.mode)
        engine.setArmed(false)
        assertEquals("None", engine.state.value.safety.recovery)
    }

    @Test fun enabledFenceRtlReturnsInsideAndLandsWithoutRepeatedBreachActions() {
        val engine = PhysicsSimulationEngine()
        engine.setSafetyParameter("FENCE_TYPE", 2f)
        engine.setSafetyParameter("FENCE_RADIUS", 10f)
        engine.setSafetyParameter("RTL_ALT", 500f)
        engine.setSafetyParameter("FENCE_ENABLE", 1f)
        airborne(engine)
        engine.setGuidedOffset(30f, 0f, 5f)
        fly(engine, 65)
        assertFalse(engine.state.value.armed)
        assertTrue(kotlin.math.abs(engine.state.value.northMeters) < 3f)
        assertFalse(engine.state.value.safety.breached)
        assertEquals(1, engine.state.value.safety.breachCount)
        assertEquals("None", engine.state.value.safety.recovery)
    }

    @Test fun impossibleReturnAltitudeFallsBackToLocalLanding() {
        val engine = PhysicsSimulationEngine()
        engine.setSafetyParameter("FENCE_TYPE", 1f)
        engine.setSafetyParameter("FENCE_ALT_MAX", 3f)
        engine.setSafetyParameter("FENCE_ACTION", 1f)
        engine.setSafetyParameter("FENCE_ENABLE", 1f)
        engine.takeoff(6f)
        engine.setPilotInput(PilotInput(throttle = .5f))
        fly(engine, 45)
        assertFalse(engine.state.value.armed)
        assertEquals(FlightMode.LAND, engine.state.value.mode)
        assertTrue(engine.state.value.safety.returnTarget.startsWith("Land here"))
    }

    @Test fun minimumAltitudeAllowsTakeoffAndLandingButDetectsDescentBreach() {
        val engine = PhysicsSimulationEngine()
        engine.setSafetyParameter("FENCE_TYPE", 8f)
        engine.setSafetyParameter("FENCE_ALT_MIN", 2f)
        engine.setSafetyParameter("FENCE_ACTION", 0f)
        engine.setSafetyParameter("FENCE_ENABLE", 1f)
        airborne(engine)
        assertEquals(0, engine.state.value.safety.breachCount)
        engine.setGuidedOffset(0f, 0f, 1f)
        fly(engine, 15)
        assertTrue(engine.state.value.safety.breached)
        assertEquals(1, engine.state.value.safety.breachType)
        engine.land()
        fly(engine, 25)
        assertFalse(engine.state.value.safety.breached)
        assertFalse(engine.state.value.armed)
    }

    @Test fun mslRallyAltitudeIsConvertedOnceAndHomeCanBeNearest() {
        val engine = PhysicsSimulationEngine()
        engine.replaceSafetyPlan(2, listOf(rally(engine, north = 8f, alt = engine.state.value.altitudeMslMeters + 5f, frame = 0)))
        engine.setSafetyParameter("RALLY_INCL_HOME", 0f)
        airborne(engine)
        engine.setMode(FlightMode.RTL)
        fly(engine, 65)
        assertFalse(engine.state.value.armed)
        assertTrue(kotlin.math.abs(engine.state.value.northMeters - 8f) < 3f)
    }
}
