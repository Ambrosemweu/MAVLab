package com.ascend.mavlab.simulation.mission

data class Mission(
    val items: List<MissionItem> = emptyList(),
    val currentIndex: Int = 0,
    val complete: Boolean = items.isEmpty(),
) {
    val currentItem: MissionItem? get() = items.getOrNull(currentIndex)
}

data class MissionItem(
    val sequence: Int,
    val command: MissionCommand,
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val altitudeAglMeters: Float,
    val acceptanceRadiusMeters: Float = 2f,
    val autocontinue: Boolean = true,
)

enum class MissionCommand(val mavCmdId: Int) {
    WAYPOINT(16),
    TAKEOFF(22),
    LAND(21),
    RTL(20),
    LOITER_TIME(19);

    companion object {
        fun fromMavCmdId(mavCmdId: Int): MissionCommand {
            return entries.firstOrNull { it.mavCmdId == mavCmdId } ?: WAYPOINT
        }
    }
}

data class MissionProgress(
    val items: List<MissionItem> = emptyList(),
    val currentIndex: Int = 0,
    val complete: Boolean = true,
    val lastReachedSequence: Int? = null,
    val activeTarget: MissionItem? = null,
) {
    val loaded: Boolean get() = items.isNotEmpty()
    val completedCount: Int get() = if (complete) items.size else currentIndex.coerceAtMost(items.size)
}
