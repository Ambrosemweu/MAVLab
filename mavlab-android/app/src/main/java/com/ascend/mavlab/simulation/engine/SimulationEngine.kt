package com.ascend.mavlab.simulation.engine

import kotlinx.coroutines.flow.StateFlow

/**
 * Phase 0 seam only.
 *
 * Phase 2 will implement the fixed-rate physics loop behind this interface.
 */
interface SimulationEngine {
    val state: StateFlow<DroneState>
    fun start()
    fun stop()
}
