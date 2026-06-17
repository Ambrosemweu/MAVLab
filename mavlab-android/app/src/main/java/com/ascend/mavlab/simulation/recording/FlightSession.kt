package com.ascend.mavlab.simulation.recording

import java.io.File

data class FlightSession(
    val id: String,
    val startedAtMs: Long,
    val endedAtMs: Long? = null,
    val directoryPath: String,
) {
    val hasReport: Boolean
        get() = File(directoryPath, "report.md").isFile

    val reportPath: String
        get() = File(directoryPath, "report.md").absolutePath
}

data class FlightRecordingStatus(
    val active: Boolean = false,
    val currentSession: FlightSession? = null,
    val lastSession: FlightSession? = null,
    val sessionHistory: List<FlightSession> = emptyList(),
) {
    val displayText: String
        get() = when {
            active && currentSession != null -> "Recording: Active"
            lastSession != null -> "Last log: mavlab/flights/${lastSession.id}"
            else -> "Recording: Idle"
        }
}
