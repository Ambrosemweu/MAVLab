package com.ascend.mavlab.feature.dashboard

import com.ascend.mavlab.core.sensors.OrientationData
import com.ascend.mavlab.core.sensors.OrientationSource
import com.ascend.mavlab.simulation.engine.DroneState
import kotlin.math.abs

internal data class CockpitInstrumentAttitude(
    val rollRadians: Float,
    val pitchRadians: Float,
    val yawRadians: Float,
    val phonePreview: Boolean,
)

internal fun resolveCockpitInstrumentAttitude(
    state: DroneState,
    phoneOrientation: OrientationData,
    freshness: CockpitDataFreshness,
    sensorSource: OrientationSource = phoneOrientation.source,
): CockpitInstrumentAttitude {
    // Ground preview uses calibrated phone angles in radians. Flight telemetry
    // and the digital twin continue to observe only the simulation engine.
    val phonePreview = freshness in setOf(CockpitDataFreshness.LIVE, CockpitDataFreshness.DELAYED) &&
        state.altitudeAglMeters <= 0.25f && abs(state.verticalSpeedMS) <= 0.2f &&
        sensorSource != OrientationSource.Unavailable &&
        phoneOrientation.source != OrientationSource.Unavailable &&
        phoneOrientation.timestampNanos > 0L &&
        phoneOrientation.roll.isFinite() && phoneOrientation.pitch.isFinite() && phoneOrientation.yaw.isFinite()
    return CockpitInstrumentAttitude(
        rollRadians = if (phonePreview) phoneOrientation.roll else state.rollRadians,
        pitchRadians = if (phonePreview) phoneOrientation.pitch else state.pitchRadians,
        yawRadians = if (phonePreview) phoneOrientation.yaw else state.yawRadians,
        phonePreview = phonePreview,
    )
}
