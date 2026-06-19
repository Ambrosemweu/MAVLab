package com.ascend.mavlab.simulation.audio

data class DroneSoundSettings(
    val enabled: Boolean = true,
    val masterVolume: Float = 0.65f,
    val perMotorMix: Float = 0.75f,
    val roughness: Float = 0.45f,
    val alertsEnabled: Boolean = true,
    val testMode: Boolean = false,
    val testRpm: Float = 3500f,
    val proceduralEnabled: Boolean = true,
    val sampleBedAmount: Float = 0.55f,
    val bladeHarmonicsAmount: Float = 0.60f,
    val propWashAmount: Float = 0.50f,
    val motorWhineAmount: Float = 0.25f,
    val bladeCount: Int = 2,
    val acousticProfileId: String = DroneAcousticProfile.DefaultId,
    val synthQuality: DroneSynthQuality = DroneSynthQuality.BALANCED,
    val showAcousticTelemetry: Boolean = false,
) {
    fun sanitized(): DroneSoundSettings {
        return copy(
            masterVolume = masterVolume.coerceIn(0f, 1f),
            perMotorMix = perMotorMix.coerceIn(0f, 1f),
            roughness = roughness.coerceIn(0f, 1f),
            testRpm = testRpm.coerceIn(0f, MaxReferenceRpm),
            sampleBedAmount = sampleBedAmount.coerceIn(0f, 1f),
            bladeHarmonicsAmount = bladeHarmonicsAmount.coerceIn(0f, 1f),
            propWashAmount = propWashAmount.coerceIn(0f, 1f),
            motorWhineAmount = motorWhineAmount.coerceIn(0f, 1f),
            bladeCount = bladeCount.coerceIn(2, 4),
            acousticProfileId = DroneAcousticProfile.byId(acousticProfileId).id,
        )
    }

    companion object {
        const val MaxReferenceRpm = 9500f
    }
}

enum class DroneSynthQuality(
    val label: String,
    val harmonicCount: Int,
    val maxHarmonicFrequencyHz: Float,
) {
    ECO("Eco", 4, 2500f),
    BALANCED("Balanced", 8, 4500f),
    HIGH("High", 12, 6500f),
}
