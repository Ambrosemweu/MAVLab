package com.ascend.mavlab.simulation.mission

object MissionSnapshotCodec {
    fun encode(items: List<MissionItem>): String {
        if (items.isEmpty()) return ""
        return buildString {
            appendLine(Version)
            items.sortedBy { it.sequence }.forEach { item ->
                appendLine(
                    listOf(
                        item.sequence.toString(),
                        item.command.name,
                        item.latitudeDeg.toString(),
                        item.longitudeDeg.toString(),
                        item.altitudeAglMeters.toString(),
                        item.acceptanceRadiusMeters.toString(),
                        item.autocontinue.toString(),
                        item.localNorthMeters?.toString().orEmpty(),
                        item.localEastMeters?.toString().orEmpty(),
                        item.speedMetersPerSecond?.toString().orEmpty(),
                    ).joinToString(FieldSeparator),
                )
            }
        }
    }

    fun decode(snapshot: String?): List<MissionItem> {
        val lines = snapshot
            ?.lineSequence()
            ?.filter { it.isNotBlank() }
            ?.toList()
            ?: return emptyList()
        if (lines.firstOrNull() != Version) return emptyList()

        return runCatching {
            lines.drop(1).map { line ->
                val fields = line.split(FieldSeparator, limit = FieldCount)
                if (fields.size !in LegacyFieldCount..FieldCount) error("Invalid mission snapshot line")
                MissionItem(
                    sequence = fields[0].toInt(),
                    command = MissionCommand.valueOf(fields[1]),
                    latitudeDeg = fields[2].toDouble(),
                    longitudeDeg = fields[3].toDouble(),
                    altitudeAglMeters = fields[4].toFloat(),
                    acceptanceRadiusMeters = fields[5].toFloat(),
                    autocontinue = fields[6].toBooleanStrict(),
                    localNorthMeters = fields[7].toFloatOrNull(),
                    localEastMeters = fields[8].toFloatOrNull(),
                    speedMetersPerSecond = fields.getOrNull(9)?.toFloatOrNull(),
                )
            }
        }.getOrElse { emptyList() }
    }

    private const val Version = "MAVLAB_MISSION_V1"
    private const val FieldSeparator = "\t"
    private const val LegacyFieldCount = 9
    private const val FieldCount = 10
}
