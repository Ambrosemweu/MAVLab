package com.ascend.mavlab.service

data class BackgroundRuntimeSnapshot(
    val appVisible: Boolean,
    val backgroundedAtMs: Long?,
    val gcsConnected: Boolean,
    val armed: Boolean,
    val missionActive: Boolean,
)

enum class BackgroundRuntimeDecision {
    KEEP_APP_VISIBLE,
    KEEP_GCS_HANDOFF,
    KEEP_GCS_CONNECTED,
    KEEP_ACTIVE_FLIGHT,
    STOP_IDLE,
}

class BackgroundRuntimePolicy(
    private val handoffGraceMs: Long = DefaultGcsHandoffGraceMs,
) {
    init {
        require(handoffGraceMs >= 0L) { "GCS handoff grace must not be negative" }
    }

    fun decide(snapshot: BackgroundRuntimeSnapshot, nowMs: Long): BackgroundRuntimeDecision {
        if (snapshot.appVisible) return BackgroundRuntimeDecision.KEEP_APP_VISIBLE
        if (snapshot.gcsConnected) return BackgroundRuntimeDecision.KEEP_GCS_CONNECTED
        if (snapshot.armed || snapshot.missionActive) return BackgroundRuntimeDecision.KEEP_ACTIVE_FLIGHT

        val backgroundedAtMs = snapshot.backgroundedAtMs
            ?: return BackgroundRuntimeDecision.STOP_IDLE
        return if (nowMs - backgroundedAtMs <= handoffGraceMs) {
            BackgroundRuntimeDecision.KEEP_GCS_HANDOFF
        } else {
            BackgroundRuntimeDecision.STOP_IDLE
        }
    }

    companion object {
        const val DefaultGcsHandoffGraceMs = 60_000L
    }
}
