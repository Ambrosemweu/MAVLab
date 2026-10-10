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
    val localNorthMeters: Float? = null,
    val localEastMeters: Float? = null,
    val speedMetersPerSecond: Float? = null,
    val altitudeIsMsl: Boolean = false,
)

enum class MissionCommand(val mavCmdId: Int) {
    WAYPOINT(16),
    TAKEOFF(22),
    LAND(21),
    RTL(20),
    LOITER_TIME(19),
    CHANGE_SPEED(178);

    val isNav: Boolean
        get() = this == WAYPOINT ||
            this == TAKEOFF ||
            this == LAND ||
            this == RTL ||
            this == LOITER_TIME

    val isDo: Boolean
        get() = this == CHANGE_SPEED

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
    val origin: MissionOrigin = MissionOrigin.UNVERIFIED,
    val executionStarted: Boolean = false,
) {
    val loaded: Boolean get() = items.isNotEmpty()
    val received: Boolean get() = loaded && origin == MissionOrigin.GCS_UPLOAD
    val completedCount: Int get() = if (complete) items.size else currentIndex.coerceAtMost(items.size)
}

/** Provenance belongs to the authoritative mission, including after process restoration. */
enum class MissionOrigin { GCS_UPLOAD, UNVERIFIED, INTERNAL_FIXTURE }
