package com.ascend.mavlab.feature.controller

import com.ascend.mavlab.core.common.ControllerInputMode
import com.ascend.mavlab.core.common.RuntimeLifecycleState
import com.ascend.mavlab.core.mavlink.MavlinkIdentityStatus
import com.ascend.mavlab.core.sensors.OrientationSource
import com.ascend.mavlab.simulation.engine.ControlAuthority
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.FlightMode
import com.ascend.mavlab.simulation.failures.FailureState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ControllerUiStateTest {
    private fun derive(
        state: DroneState = DroneState(),
        mode: ControllerInputMode = ControllerInputMode.PHONE_SENSORS,
        source: OrientationSource = OrientationSource.GameRotationVector,
        failures: FailureState = FailureState(),
        identity: MavlinkIdentityStatus = MavlinkIdentityStatus(),
        lifecycle: RuntimeLifecycleState = RuntimeLifecycleState.RUNNING,
        updatedAt: Long? = 9_500L,
    ) = deriveControllerUiState(state, mode, source, failures, identity, lifecycle, updatedAt, 10_000L)

    @Test
    fun groundedPreviewCanCalibrateAndArmButDoesNotImplyAcceptedFlightInput() {
        val ui = derive()
        assertEquals("PREVIEW · DISARMED", ui.inputStatus)
        assertTrue(ui.calibrationEnabled)
        assertTrue(ui.inputSelectionEnabled)
        assertEquals(ControllerAction.ARM, ui.primaryAction.action)
        assertTrue(ui.primaryAction.enabled)
    }

    @Test
    fun armedLocalPhoneInputStaysEnabledAndSetupLocks() {
        val ui = derive(DroneState(armed = true, mode = FlightMode.ALT_HOLD, controlAuthority = ControlAuthority.CONTROLLER))
        assertTrue(ui.controlsEnabled)
        assertEquals("INPUT ACCEPTED", ui.inputStatus)
        assertFalse(ui.calibrationEnabled)
        assertFalse(ui.inputSelectionEnabled)
        assertEquals(ControllerAction.TAKEOFF, ui.primaryAction.action)
        assertEquals(ControllerAction.DISARM, ui.secondaryAction.action)
    }

    @Test
    fun flightOffersLandAndLandingRetainsAReachableDisarm() {
        val state = DroneState(armed = true, controlAuthority = ControlAuthority.CONTROLLER, altitudeAglMeters = 8f)
        assertEquals(ControllerAction.LAND, derive(state).primaryAction.action)
        val landing = derive(state.copy(mode = FlightMode.LAND))
        assertFalse(landing.controlsEnabled)
        assertFalse(landing.primaryAction.enabled)
        assertEquals(ControllerAction.DISARM, landing.secondaryAction.action)
        assertEquals(ControllerAction.DISARM, derive(state.copy(mode = FlightMode.LAND, altitudeAglMeters = 0f)).primaryAction.action)
    }

    @Test
    fun missionAuthorityBlocksLocalInputAndProvidesDeliberateRecovery() {
        val ui = derive(DroneState(armed = true, mode = FlightMode.AUTO, controlAuthority = ControlAuthority.GCS_MISSION, altitudeAglMeters = 8f))
        assertFalse(ui.controlsEnabled)
        assertFalse(ui.inputSelectionEnabled)
        assertFalse(ui.flightModesEnabled)
        assertTrue(ui.blockedReason!!.contains("GCS mission"))
        assertEquals(ControllerAction.TAKE_CONTROL_AND_LAND, ui.primaryAction.action)
        val disarmed = derive(DroneState(mode = FlightMode.AUTO, controlAuthority = ControlAuthority.GCS_MISSION))
        assertFalse(disarmed.controlsEnabled)
        assertEquals(ControllerAction.TAKE_CONTROL, disarmed.primaryAction.action)
    }

    @Test
    fun connectedPeerDoesNotBlockLocalControllerOrEstablishGcsAuthority() {
        val ui = derive(identity = MavlinkIdentityStatus(gcsConnected = true))
        assertTrue(ui.controlsEnabled)
        assertTrue(ui.primaryAction.enabled)
        val direct = derive(DroneState(controlAuthority = ControlAuthority.GCS_DIRECT))
        assertFalse(direct.controlsEnabled)
        assertEquals(ControllerAction.TAKE_CONTROL, direct.primaryAction.action)
    }

    @Test
    fun unavailableSensorBlocksPhoneArmingButAllowsExplicitCustomFallback() {
        val phone = derive(source = OrientationSource.Unavailable)
        assertFalse(phone.primaryAction.enabled)
        assertFalse(phone.controlsEnabled)
        assertFalse(phone.calibrationEnabled)
        assertTrue(phone.inputSelectionEnabled)
        val custom = derive(mode = ControllerInputMode.CUSTOM_INPUT, source = OrientationSource.Unavailable)
        assertTrue(custom.controlsEnabled)
        assertTrue(custom.primaryAction.enabled)
    }

    @Test
    fun staleAndStoppedRuntimeNeverOfferFlightCommands() {
        listOf(derive(updatedAt = 1_000L), derive(lifecycle = RuntimeLifecycleState.STOPPED), derive(updatedAt = null)).forEach { ui ->
            assertFalse(ui.controlsEnabled)
            assertFalse(ui.calibrationEnabled)
            assertFalse(ui.inputSelectionEnabled)
            assertFalse(ui.flightModesEnabled)
            assertEquals(ControllerAction.OPEN_OPS, ui.primaryAction.action)
        }
    }

    @Test
    fun criticalFailureBlocksArmingAndOffersLandWithoutSilencingArmedPhoneControl() {
        val failure = FailureState(motorFailureMask = 4)
        assertFalse(derive(failures = failure).primaryAction.enabled)
        val flight = derive(DroneState(armed = true, controlAuthority = ControlAuthority.CONTROLLER, altitudeAglMeters = 8f), failures = failure)
        assertTrue(flight.controlsEnabled)
        assertEquals("Land now", flight.primaryAction.label)
        assertTrue(flight.criticalWarning)
    }

    @Test
    fun directRpmStartsBlockedUntilArmedAndExposesMotorStop() {
        assertFalse(derive(mode = ControllerInputMode.DIRECT_RPM).controlsEnabled)
        val armed = derive(DroneState(armed = true, controlAuthority = ControlAuthority.CONTROLLER), ControllerInputMode.DIRECT_RPM)
        assertTrue(armed.controlsEnabled)
        assertFalse(armed.flightModesEnabled)
        assertEquals(ControllerAction.DISARM, armed.primaryAction.action)
        assertEquals("Stop motors", armed.primaryAction.label)
    }
}
