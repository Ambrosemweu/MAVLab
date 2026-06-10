package com.ascend.mavlab.simulation.mission

data class MissionUploadStatus(
    val phase: MissionUploadPhase = MissionUploadPhase.IDLE,
    val expectedCount: Int = 0,
    val receivedCount: Int = 0,
    val lastRequestedSequence: Int? = null,
    val lastReceivedSequence: Int? = null,
    val lastError: String? = null,
) {
    val displayText: String
        get() = when (phase) {
            MissionUploadPhase.IDLE -> "Upload: Idle"
            MissionUploadPhase.UPLOADING -> "Upload: Receiving $receivedCount/$expectedCount from QGC" +
                (lastRequestedSequence?.let { " - requested ${it + 1}" } ?: "")
            MissionUploadPhase.ACCEPTED -> "Upload: Accepted $receivedCount waypoints"
            MissionUploadPhase.REJECTED -> "Upload: Rejected ${lastError ?: "unknown error"}"
        }
}

enum class MissionUploadPhase {
    IDLE,
    UPLOADING,
    ACCEPTED,
    REJECTED,
}
