package com.ascend.mavlab.simulation.engine

import kotlinx.coroutines.flow.StateFlow

interface SimulationEngine {
    val state: StateFlow<DroneState>
    fun start()
    fun stop()
}
