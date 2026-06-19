package com.ascend.mavlab.simulation.engine

data class SimLocation(
    val id: String,
    val label: String,
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val altitudeMslMeters: Float,
) {
    fun sanitized(): SimLocation {
        return copy(
            latitudeDeg = latitudeDeg.coerceIn(-90.0, 90.0),
            longitudeDeg = longitudeDeg.coerceIn(-180.0, 180.0),
            altitudeMslMeters = altitudeMslMeters.coerceIn(-500f, 9000f),
        )
    }

    companion object {
        const val CustomId = "custom"

        val Nairobi = SimLocation(
            id = "nairobi",
            label = "Nairobi",
            latitudeDeg = -1.2921,
            longitudeDeg = 36.8219,
            altitudeMslMeters = 1805f,
        )

        val Mombasa = SimLocation(
            id = "mombasa",
            label = "Mombasa",
            latitudeDeg = -4.0435,
            longitudeDeg = 39.6682,
            altitudeMslMeters = 50f,
        )

        val Johannesburg = SimLocation(
            id = "johannesburg",
            label = "Johannesburg",
            latitudeDeg = -26.2041,
            longitudeDeg = 28.0473,
            altitudeMslMeters = 1753f,
        )

        val Dubai = SimLocation(
            id = "dubai",
            label = "Dubai",
            latitudeDeg = 25.2048,
            longitudeDeg = 55.2708,
            altitudeMslMeters = 16f,
        )

        val London = SimLocation(
            id = "london",
            label = "London",
            latitudeDeg = 51.5072,
            longitudeDeg = -0.1276,
            altitudeMslMeters = 35f,
        )

        val SanFrancisco = SimLocation(
            id = "san_francisco",
            label = "San Francisco",
            latitudeDeg = 37.7749,
            longitudeDeg = -122.4194,
            altitudeMslMeters = 16f,
        )

        val presets = listOf(Nairobi, Mombasa, Johannesburg, Dubai, London, SanFrancisco)

        fun custom(
            latitudeDeg: Double,
            longitudeDeg: Double,
            altitudeMslMeters: Float,
        ): SimLocation {
            return SimLocation(
                id = CustomId,
                label = "Custom",
                latitudeDeg = latitudeDeg,
                longitudeDeg = longitudeDeg,
                altitudeMslMeters = altitudeMslMeters,
            ).sanitized()
        }

        fun byId(id: String): SimLocation {
            return presets.firstOrNull { it.id == id } ?: Nairobi
        }
    }
}
