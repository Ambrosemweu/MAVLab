package com.ascend.mavlab.simulation.recording

data class FlightEvent(
    val timestampMs: Long,
    val type: String,
    val message: String,
)
