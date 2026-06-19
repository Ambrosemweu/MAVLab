package com.ascend.mavlab.simulation.audio

data class DroneSoundFrame(
    val enabled: Boolean,
    val motors: List<MotorSoundFrame>,
    val averageRpm: Float,
    val rpmSpreadPercent: Float,
    val activeMotorCount: Int,
    val averagePlaybackRate: Float,
    val roughness: Float,
    val alert: DroneSoundAlert,
    val procedural: ProceduralSoundFrame = ProceduralSoundFrame.Disabled,
)

data class MotorSoundFrame(
    val index: Int,
    val rpm: Float,
    val volume: Float,
    val playbackRate: Float,
    val failed: Boolean,
)

enum class DroneSoundAlert(val label: String) {
    NONE("None"),
    LOW_BATTERY("Low battery"),
    CRITICAL_BATTERY("Critical battery"),
    LINK_LOST("Link lost"),
    UNSAFE_RESERVE("Unsafe reserve"),
}

data class ProceduralSoundFrame(
    val enabled: Boolean,
    val bladeCount: Int,
    val sampleBedAmount: Float,
    val bladeHarmonicsAmount: Float,
    val propWashAmount: Float,
    val motorWhineAmount: Float,
    val averageBladePassHz: Float,
    val motorBladePassHz: List<Float>,
    val harmonicCount: Int,
    val maxHarmonicFrequencyHz: Float,
    val propWashGain: Float,
    val motorWhineGain: Float,
    val loadStrain: Float,
    val roughness: Float,
) {
    companion object {
        val Disabled = ProceduralSoundFrame(
            enabled = false,
            bladeCount = 2,
            sampleBedAmount = 1f,
            bladeHarmonicsAmount = 0f,
            propWashAmount = 0f,
            motorWhineAmount = 0f,
            averageBladePassHz = 0f,
            motorBladePassHz = emptyList(),
            harmonicCount = 0,
            maxHarmonicFrequencyHz = 0f,
            propWashGain = 0f,
            motorWhineGain = 0f,
            loadStrain = 0f,
            roughness = 0f,
        )
    }
}
