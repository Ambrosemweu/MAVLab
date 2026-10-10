package com.ascend.mavlab.feature.dashboard

import com.ascend.mavlab.core.sensors.OrientationData
import com.ascend.mavlab.core.sensors.OrientationSource
import com.ascend.mavlab.simulation.engine.DroneState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CockpitInstrumentAttitudeTest {
    private val phone = OrientationData(
        roll = 0.4f, pitch = -0.2f, yaw = 0.8f,
        timestampNanos = 10L, source = OrientationSource.GameRotationVector,
    )

    @Test
    fun gyroPreviewWorksBothDisarmedAndArmedOnGround() {
        for (armed in listOf(false, true)) {
            val attitude = resolveCockpitInstrumentAttitude(DroneState(armed = armed), phone, CockpitDataFreshness.LIVE)
            assertTrue(attitude.phonePreview)
            assertEquals(phone.roll, attitude.rollRadians)
            assertEquals(phone.pitch, attitude.pitchRadians)
            assertEquals(phone.yaw, attitude.yawRadians)
        }
    }

    @Test
    fun flightAndTouchdownMotionAlwaysUseSimulationAttitude() {
        for (state in listOf(
            DroneState(armed = true, altitudeAglMeters = 8f, rollRadians = -0.3f),
            DroneState(armed = false, altitudeAglMeters = 8f, rollRadians = -0.3f),
            DroneState(armed = true, altitudeAglMeters = 0.1f, verticalSpeedMS = -0.8f, rollRadians = -0.3f),
        )) {
            val attitude = resolveCockpitInstrumentAttitude(state, phone, CockpitDataFreshness.LIVE)
            assertFalse(attitude.phonePreview)
            assertEquals(state.rollRadians, attitude.rollRadians)
        }
    }

    @Test
    fun unavailableInvalidOrUninitialisedSensorsFallBackToSimulation() {
        for (orientation in listOf(
            phone.copy(source = OrientationSource.Unavailable),
            phone.copy(timestampNanos = 0L),
            phone.copy(roll = Float.NaN),
        )) {
            assertFalse(resolveCockpitInstrumentAttitude(DroneState(), orientation, CockpitDataFreshness.LIVE).phonePreview)
        }
        assertFalse(resolveCockpitInstrumentAttitude(
            DroneState(), phone, CockpitDataFreshness.LIVE, OrientationSource.Unavailable,
        ).phonePreview)
    }

    @Test
    fun staleOrUnavailableVehicleDataCannotBeMaskedByLivePhoneMotion() {
        for (freshness in listOf(CockpitDataFreshness.STALE, CockpitDataFreshness.UNAVAILABLE)) {
            assertFalse(resolveCockpitInstrumentAttitude(DroneState(), phone, freshness).phonePreview)
        }
    }
}
