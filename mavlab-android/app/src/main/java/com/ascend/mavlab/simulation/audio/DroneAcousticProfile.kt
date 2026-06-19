package com.ascend.mavlab.simulation.audio

data class DroneAcousticProfile(
    val id: String,
    val label: String,
    val bladeCount: Int,
    val maxReferenceRpm: Float,
    val harmonicBrightness: Float,
    val propWashBrightness: Float,
    val whineBrightness: Float,
    val sampleBedDefault: Float,
) {
    companion object {
        const val DefaultId = "mavlab_trainer_quad"

        val MavLabTrainerQuad = DroneAcousticProfile(
            id = DefaultId,
            label = "MAVLab Trainer Quad",
            bladeCount = 2,
            maxReferenceRpm = 9500f,
            harmonicBrightness = 0.65f,
            propWashBrightness = 0.55f,
            whineBrightness = 0.35f,
            sampleBedDefault = 0.55f,
        )

        val SmallRacingQuad = DroneAcousticProfile(
            id = "small_racing_quad",
            label = "Small Racing Quad",
            bladeCount = 3,
            maxReferenceRpm = 14500f,
            harmonicBrightness = 0.82f,
            propWashBrightness = 0.48f,
            whineBrightness = 0.62f,
            sampleBedDefault = 0.42f,
        )

        val CargoQuad = DroneAcousticProfile(
            id = "cargo_quad",
            label = "Cargo Quad",
            bladeCount = 2,
            maxReferenceRpm = 7200f,
            harmonicBrightness = 0.48f,
            propWashBrightness = 0.76f,
            whineBrightness = 0.22f,
            sampleBedDefault = 0.62f,
        )

        val all = listOf(MavLabTrainerQuad, SmallRacingQuad, CargoQuad)

        fun byId(id: String): DroneAcousticProfile {
            return all.firstOrNull { it.id == id } ?: MavLabTrainerQuad
        }
    }
}
