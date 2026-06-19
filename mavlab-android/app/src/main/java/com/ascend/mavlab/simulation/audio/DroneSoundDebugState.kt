package com.ascend.mavlab.simulation.audio

data class DroneSoundDebugState(
    val averageRpm: Float = 0f,
    val rpmSpreadPercent: Float = 0f,
    val activeMotorCount: Int = 0,
    val averagePlaybackRate: Float = 0f,
    val roughness: Float = 0f,
    val alertLabel: String = DroneSoundAlert.NONE.label,
    val proceduralEnabled: Boolean = false,
    val averageBladePassHz: Float = 0f,
    val motorBladePassLabel: String = "0 / 0 / 0 / 0 Hz",
    val harmonicCount: Int = 0,
    val propWashPercent: Float = 0f,
    val motorWhinePercent: Float = 0f,
    val synthStatus: String = "Disabled",
    val sampleRateHz: Int = 0,
    val bufferFrames: Int = 0,
    val underrunCount: Int = 0,
) {
    companion object {
        fun fromFrame(
            frame: DroneSoundFrame,
            synthStatus: ProceduralSynthStatus = ProceduralSynthStatus(),
        ): DroneSoundDebugState {
            val procedural = frame.procedural
            return DroneSoundDebugState(
                averageRpm = frame.averageRpm,
                rpmSpreadPercent = frame.rpmSpreadPercent,
                activeMotorCount = frame.activeMotorCount,
                averagePlaybackRate = frame.averagePlaybackRate,
                roughness = frame.roughness,
                alertLabel = frame.alert.label,
                proceduralEnabled = procedural.enabled,
                averageBladePassHz = procedural.averageBladePassHz,
                motorBladePassLabel = procedural.motorBladePassHz
                    .take(4)
                    .joinToString(" / ") { "%.0f".format(it) } + " Hz",
                harmonicCount = procedural.harmonicCount,
                propWashPercent = procedural.propWashGain * 100f,
                motorWhinePercent = procedural.motorWhineGain * 100f,
                synthStatus = synthStatus.displayLabel(procedural.enabled),
                sampleRateHz = synthStatus.sampleRateHz,
                bufferFrames = synthStatus.bufferFrames,
                underrunCount = synthStatus.underrunCount,
            )
        }
    }
}

data class ProceduralSynthStatus(
    val running: Boolean = false,
    val sampleRateHz: Int = 0,
    val bufferFrames: Int = 0,
    val underrunCount: Int = 0,
    val fallbackReason: String? = null,
) {
    fun displayLabel(proceduralEnabled: Boolean): String {
        return when {
            fallbackReason != null -> "Fallback"
            !proceduralEnabled -> "Disabled"
            running && underrunCount > 8 -> "Underrun risk"
            running -> "Running"
            else -> "Disabled"
        }
    }
}
