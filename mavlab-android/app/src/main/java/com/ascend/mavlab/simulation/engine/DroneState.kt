package com.ascend.mavlab.simulation.engine

/**
 * Minimal state shell for Phase 0.
 *
 * Phase 1 fills this with hardcoded protocol-demo telemetry. Phase 2 replaces
 * that with values from the real physics engine.
 */
data class DroneState(
    val armed: Boolean = false,
    val mode: String = "Skeleton",
)
