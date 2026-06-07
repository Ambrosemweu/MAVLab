package com.ascend.mavlab.simulation.engine

/**
 * Phase 1 protocol-demo state.
 * Phase 2 replaces the generated telemetry with values from the real physics engine.
 */
data class DroneState(
    val armed: Boolean = false,
    val mode: FlightMode = FlightMode.STABILIZE,
    val uptimeMs: UInt = 0u,
    val latitudeDeg: Double = -1.2921,
    val longitudeDeg: Double = 36.8219,
    val altitudeMslMeters: Float = 1805f,
    val altitudeAglMeters: Float = 10f,
    val rollRadians: Float = 0f,
    val pitchRadians: Float = 0f,
    val yawRadians: Float = 0f,
    val rollSpeedRadS: Float = 0f,
    val pitchSpeedRadS: Float = 0f,
    val yawSpeedRadS: Float = 0f,
    val groundSpeedMS: Float = 0f,
    val verticalSpeedMS: Float = 0f,
    val headingDegrees: Short = 90,
    val batteryVoltageMv: UShort = 12000u,
    val batteryCurrentCa: Short = 120,
    val batteryRemainingPercent: Byte = 85,
    val gpsSatellites: UByte = 12u,
    val gpsFixType: UByte = 3u,
    val lastInboundMessage: String = "None",
    val lastAck: String = "None",
)

enum class FlightMode(val customMode: UInt, val displayName: String) {
    STABILIZE(0u, "Stabilize"),
    ALT_HOLD(2u, "Alt Hold"),
    AUTO(3u, "Auto"),
    GUIDED(4u, "Guided"),
    LOITER(5u, "Loiter"),
    RTL(6u, "RTL"),
    LAND(9u, "Land");

    companion object {
        fun fromCustomMode(customMode: UInt): FlightMode {
            return entries.firstOrNull { it.customMode == customMode } ?: STABILIZE
        }
    }
}
