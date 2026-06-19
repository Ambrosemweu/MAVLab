package com.ascend.mavlab.simulation.audio

object BladePassModel {
    fun bladePassHz(rpm: Float, bladeCount: Int): Float {
        return bladeCount.coerceAtLeast(1) * rpm.coerceAtLeast(0f) / 60f
    }

    fun harmonics(
        fundamentalHz: Float,
        maxCount: Int,
        maxFrequencyHz: Float,
    ): List<Float> {
        if (fundamentalHz <= 0f || maxCount <= 0 || maxFrequencyHz <= 0f) return emptyList()
        return buildList {
            for (index in 1..maxCount) {
                val harmonic = fundamentalHz * index
                if (harmonic > maxFrequencyHz) break
                add(harmonic)
            }
        }
    }
}
