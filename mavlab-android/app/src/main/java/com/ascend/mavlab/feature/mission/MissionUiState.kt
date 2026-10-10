package com.ascend.mavlab.feature.mission

import com.ascend.mavlab.simulation.engine.ControlAuthority
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.FlightMode
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.mission.*
import kotlin.math.hypot

internal enum class MissionPhase(val label: String) {
    NONE("No mission"), RECEIVING("Receiving"), ACCEPTED("Accepted · not running"),
    ACTIVE("AUTO active"), INTERRUPTED("Stopped · not in AUTO"), COMPLETED("Completed"), BLOCKED("Blocked"),
}

internal data class MissionUiState(
    val phase: MissionPhase,
    val summary: String,
    val warning: String?,
    val policy: MissionActionPolicy,
    val remainingMeters: Float?,
    val horizontalEtaSeconds: Int?,
) { val visible: Boolean get() = phase != MissionPhase.NONE }

internal fun deriveMissionUiState(state: DroneState, mission: MissionProgress, upload: MissionUploadStatus, failures: FailureState, live: Boolean): MissionUiState {
    val policy = missionActionPolicy(state, mission, upload, failures, live)
    val active = mission.received && state.armed && state.mode == FlightMode.AUTO && state.controlAuthority == ControlAuthority.GCS_MISSION && !mission.complete
    val phase = when {
        upload.phase == MissionUploadPhase.UPLOADING -> MissionPhase.RECEIVING
        upload.phase == MissionUploadPhase.REJECTED -> MissionPhase.BLOCKED
        !mission.loaded -> MissionPhase.NONE
        !mission.received || !live -> MissionPhase.BLOCKED
        mission.complete -> MissionPhase.COMPLETED
        active -> MissionPhase.ACTIVE
        mission.executionStarted -> MissionPhase.INTERRUPTED
        policy.startBlockedReason != null -> MissionPhase.BLOCKED
        else -> MissionPhase.ACCEPTED
    }
    val warning = when {
        upload.phase == MissionUploadPhase.REJECTED -> if (active) "Replacement upload rejected · previous AUTO mission is still executing. Inspect Ops." else "Upload rejected · inspect Ops. Previous loaded mission is retained."
        upload.phase == MissionUploadPhase.UPLOADING && active -> "Receiving replacement · previous AUTO mission is still executing"
        mission.loaded && !mission.received -> "Saved source unverified · upload again from QGC"
        !live -> "Mission state is not live · inspect Ops"
        active -> missionFailureWarnings(failures).firstOrNull()
        phase == MissionPhase.BLOCKED -> policy.startBlockedReason
        else -> null
    }
    val progress = "WP ${mission.currentIndex + 1}/${mission.items.size} · ${missionProgressPercent(mission)}%"
    val summary = when (phase) {
        MissionPhase.RECEIVING -> "Receiving ${upload.receivedCount}/${upload.expectedCount}"
        MissionPhase.NONE -> "No mission"
        MissionPhase.BLOCKED -> "Mission blocked"
        MissionPhase.ACTIVE -> progress
        MissionPhase.COMPLETED -> "Completed · 100%"
        MissionPhase.INTERRUPTED -> "Stopped · ${missionProgressPercent(mission)}%"
        MissionPhase.ACCEPTED -> "${mission.items.size} items · not running"
    } + if (active && phase != MissionPhase.ACTIVE) " · prior AUTO $progress" else ""
    val remaining = if (live && mission.received && !mission.complete) remainingRouteMeters(state, mission) else null
    val eta = if (active && remaining != null && state.groundSpeedMS.isFinite() && state.groundSpeedMS > 0.3f) (remaining / state.groundSpeedMS).toInt() else null
    return MissionUiState(phase, summary, warning, policy, remaining, eta)
}

/** Horizontal route length in the engine's local north/east metre frame. */
private fun remainingRouteMeters(state: DroneState, mission: MissionProgress): Float? {
    val targets = mission.items.drop(mission.currentIndex).filter { it.command.isNav }
    if (targets.isEmpty()) return null
    var north = state.northMeters
    var east = state.eastMeters
    var distance = 0f
    for (item in targets) {
        val nextNorth = item.localNorthMeters ?: return null
        val nextEast = item.localEastMeters ?: return null
        if (!nextNorth.isFinite() || !nextEast.isFinite()) return null
        distance += hypot(nextNorth - north, nextEast - east)
        north = nextNorth
        east = nextEast
    }
    return distance.takeIf { it.isFinite() }
}

internal fun missionLimitations(mission: MissionProgress): List<String> = buildList {
    mission.items.forEach { item ->
        if (item.command == MissionCommand.LOITER_TIME) add("Item ${item.sequence + 1}: LOITER_TIME uses arrival only; dwell time is not simulated.")
        if (!item.autocontinue) add("Item ${item.sequence + 1}: autocontinue=false is accepted; a waypoint pause is not implemented.")
    }
}
