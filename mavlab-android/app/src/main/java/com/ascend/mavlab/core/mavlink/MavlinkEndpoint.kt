package com.ascend.mavlab.core.mavlink

/**
 * Phase 0 seam only.
 *
 * Phase 1 will implement MAVLink packet IO behind this interface.
 */
interface MavlinkEndpoint {
    suspend fun start()
    suspend fun stop()
}
