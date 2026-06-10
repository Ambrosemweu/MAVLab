package com.ascend.mavlab.simulation.recording

data class FlightSession(
    val id: String,
    val startedAtMs: Long,
    val endedAtMs: Long? = null,
    val directoryPath: String,
)

data class FlightRecordingStatus(
    val active: Boolean = false,
    val currentSession: FlightSession? = null,
    val lastSession: FlightSession? = null,
) {
    val displayText: String
        get() = when {
            active && currentSession != null -> "Recording: Active"
            lastSession != null -> "Last log: mavlab/flights/${lastSession.id}"
            else -> "Recording: Idle"
        }
}
