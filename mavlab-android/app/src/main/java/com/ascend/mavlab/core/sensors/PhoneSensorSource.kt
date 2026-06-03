package com.ascend.mavlab.core.sensors

import kotlinx.coroutines.flow.Flow

/**
 * Phase 0 seam only.
 *
 * Phase 3 will implement Android sensor reading and fallback handling.
 */
interface PhoneSensorSource {
    fun orientation(): Flow<PhoneOrientation>
}

data class PhoneOrientation(
    val rollRadians: Float,
    val pitchRadians: Float,
    val yawRadians: Float,
    val timestampNanos: Long,
)
