package com.ascend.mavlab.simulation.recording

/** Automatic capture requires Ops opt-in. A deliberate stop lasts for the current armed flight. */
class RecordingPolicy(automaticEnabled: Boolean = false) {
    @Volatile
    var automaticEnabled: Boolean = automaticEnabled
        private set
    @Volatile
    var automaticPaused: Boolean = false
        private set

    fun setAutomaticEnabled(enabled: Boolean) { automaticEnabled = enabled }
    fun shouldStartAutomatically(armed: Boolean, active: Boolean): Boolean = automaticEnabled && armed && !active && !automaticPaused
    fun stoppedByUser(armed: Boolean) { automaticPaused = armed }
    fun startedByUser() { automaticPaused = false }
    fun disarmed() { automaticPaused = false }
    fun runtimeStopped() { automaticPaused = false }
}
