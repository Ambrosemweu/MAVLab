package com.ascend.mavlab.feature.dashboard

import com.ascend.mavlab.core.common.ControllerInputMode
import com.ascend.mavlab.core.common.RuntimeLifecycleState
import com.ascend.mavlab.core.mavlink.MavlinkIdentityStatus
import com.ascend.mavlab.simulation.engine.ControlAuthority
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.FlightMode
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.mission.MissionProgress
import com.ascend.mavlab.simulation.mission.MissionUploadPhase
import com.ascend.mavlab.simulation.mission.MissionUploadStatus
import com.ascend.mavlab.simulation.recording.FlightRecordingStatus
import kotlin.math.abs

internal enum class CockpitDataFreshness {
    LIVE,
    DELAYED,
    STALE,
    UNAVAILABLE,
}

internal enum class CockpitFlightPhase {
    DISARMED_GROUND,
    ARMED_GROUND,
    TAKING_OFF,
    AIRBORNE,
    HOLDING,
    RETURNING,
    LANDING,
    LANDED_ARMED,
    FLIGHT_COMPLETE,
}

internal enum class CockpitAlertSeverity {
    ADVISORY,
    CAUTION,
    CRITICAL,
}

internal enum class CockpitAction {
    ARM,
    DISARM,
    TAKEOFF,
    LAND,
    TAKE_CONTROL_AND_LAND,
    OPEN_CONTROLLER,
    OPEN_MISSION,
    OPEN_SIM,
    OPEN_OPS,
    REVIEW_FLIGHT,
    NEW_FLIGHT,
}

internal data class CockpitActionSpec(
    val action: CockpitAction,
    val label: String,
    val enabled: Boolean = true,
)

internal data class CockpitAlert(
    val title: String,
    val detail: String,
    val severity: CockpitAlertSeverity,
    val recoveryAction: CockpitAction? = null,
)

internal data class CockpitTaskPresentation(
    val title: String,
    val detail: String,
    val trailing: String,
    val progress: Float? = null,
)

internal data class CockpitUiState(
    val eyebrow: String,
    val runtimeLifecycle: RuntimeLifecycleState,
    val freshness: CockpitDataFreshness,
    val freshnessAgeMs: Long?,
    val flightPhase: CockpitFlightPhase,
    val authorityLabel: String,
    val modeLabel: String,
    val vehicleLabel: String,
    val alert: CockpitAlert?,
    val task: CockpitTaskPresentation,
    val primaryAction: CockpitActionSpec?,
    val secondaryAction: CockpitActionSpec?,
    val taskActions: List<CockpitActionSpec>,
    val systemStatus: String,
    val linkStatus: String,
)

internal fun deriveCockpitUiState(
    state: DroneState,
    mission: MissionProgress,
    uploadStatus: MissionUploadStatus,
    recording: FlightRecordingStatus,
    failures: FailureState,
    identity: MavlinkIdentityStatus,
    runtimeLifecycle: RuntimeLifecycleState,
    stateUpdatedAtElapsedRealtimeMs: Long?,
    nowElapsedRealtimeMs: Long,
    nowEpochMs: Long,
    controllerInputMode: ControllerInputMode,
): CockpitUiState {
    val freshness = resolveCockpitFreshness(
        runtimeLifecycle = runtimeLifecycle,
        stateUpdatedAtElapsedRealtimeMs = stateUpdatedAtElapsedRealtimeMs,
        nowElapsedRealtimeMs = nowElapsedRealtimeMs,
    )
    val freshnessAgeMs = stateUpdatedAtElapsedRealtimeMs?.let {
        (nowElapsedRealtimeMs - it).coerceAtLeast(0L)
    }
    val flightPhase = resolveCockpitFlightPhase(state, recording, nowEpochMs)
    val alert = resolveCockpitAlert(
        state = state,
        failures = failures,
        identity = identity,
        runtimeLifecycle = runtimeLifecycle,
        freshness = freshness,
        freshnessAgeMs = freshnessAgeMs,
    )
    val task = resolveCockpitTask(
        state = state,
        mission = mission,
        uploadStatus = uploadStatus,
        recording = recording,
        identity = identity,
        flightPhase = flightPhase,
        controllerInputMode = controllerInputMode,
    )
    val actions = resolveCockpitActions(
        state = state,
        mission = mission,
        flightPhase = flightPhase,
        runtimeLifecycle = runtimeLifecycle,
        freshness = freshness,
        alert = alert,
    )

    return CockpitUiState(
        eyebrow = cockpitEyebrow(state, flightPhase, freshness, freshnessAgeMs),
        runtimeLifecycle = runtimeLifecycle,
        freshness = freshness,
        freshnessAgeMs = freshnessAgeMs,
        flightPhase = flightPhase,
        authorityLabel = state.controlAuthority.displayName.uppercase(),
        modeLabel = state.mode.displayName.uppercase(),
        vehicleLabel = vehicleLabel(flightPhase),
        alert = alert,
        task = task,
        primaryAction = CockpitActionSpec(
            action = if (state.armed) CockpitAction.DISARM else CockpitAction.ARM,
            label = if (state.armed) "Disarm" else "Arm",
            enabled = runtimeLifecycle == RuntimeLifecycleState.RUNNING &&
                freshness in setOf(CockpitDataFreshness.LIVE, CockpitDataFreshness.DELAYED) &&
                (state.armed || (
                    state.controlAuthority !in ExternalAuthorities &&
                        alert?.severity != CockpitAlertSeverity.CRITICAL &&
                        state.altitudeAglMeters <= GroundAltitudeThresholdMeters &&
                        abs(state.verticalSpeedMS) <= GroundVerticalSpeedThresholdMs
                    )),
        ),
        secondaryAction = CockpitActionSpec(CockpitAction.OPEN_SIM, "SIM"),
        taskActions = listOfNotNull(actions.first, actions.second).filter {
            it.action != CockpitAction.ARM && it.action != CockpitAction.OPEN_SIM
        },
        systemStatus = when {
            failures.activeCount > 0 -> "${failures.activeCount} ACTIVE ISSUE${if (failures.activeCount == 1) "" else "S"}"
            else -> "SYSTEMS NOMINAL"
        },
        linkStatus = when {
            identity.identityConflict -> "IDENTITY CONFLICT"
            identity.gcsConnected -> "QGC CONNECTED"
            else -> "QGC WAITING"
        },
    )
}

internal fun resolveCockpitFreshness(
    runtimeLifecycle: RuntimeLifecycleState,
    stateUpdatedAtElapsedRealtimeMs: Long?,
    nowElapsedRealtimeMs: Long,
): CockpitDataFreshness {
    if (runtimeLifecycle != RuntimeLifecycleState.RUNNING) return CockpitDataFreshness.UNAVAILABLE
    val updatedAt = stateUpdatedAtElapsedRealtimeMs ?: return CockpitDataFreshness.UNAVAILABLE
    return when (nowElapsedRealtimeMs - updatedAt) {
        in Long.MIN_VALUE..LiveTelemetryMaxAgeMs -> CockpitDataFreshness.LIVE
        in (LiveTelemetryMaxAgeMs + 1)..DelayedTelemetryMaxAgeMs -> CockpitDataFreshness.DELAYED
        else -> CockpitDataFreshness.STALE
    }
}

internal fun resolveCockpitFlightPhase(
    state: DroneState,
    recording: FlightRecordingStatus,
    nowEpochMs: Long,
): CockpitFlightPhase {
    if (!state.armed) {
        val endedAtMs = recording.lastSession?.endedAtMs
        val recentlyCompleted = endedAtMs != null && nowEpochMs - endedAtMs in 0..FlightCompleteWindowMs
        return if (recentlyCompleted) {
            CockpitFlightPhase.FLIGHT_COMPLETE
        } else {
            CockpitFlightPhase.DISARMED_GROUND
        }
    }
    if (state.mode == FlightMode.LAND) {
        return if (state.altitudeAglMeters <= GroundAltitudeThresholdMeters &&
            abs(state.verticalSpeedMS) <= GroundVerticalSpeedThresholdMs
        ) {
            CockpitFlightPhase.LANDED_ARMED
        } else {
            CockpitFlightPhase.LANDING
        }
    }
    if (state.mode == FlightMode.RTL) return CockpitFlightPhase.RETURNING
    if (state.mode == FlightMode.LOITER) return CockpitFlightPhase.HOLDING
    if (state.altitudeAglMeters <= GroundAltitudeThresholdMeters) return CockpitFlightPhase.ARMED_GROUND
    if (state.altitudeAglMeters < TakeoffPhaseCeilingMeters &&
        state.verticalSpeedMS >= TakeoffVerticalSpeedThresholdMs
    ) {
        return CockpitFlightPhase.TAKING_OFF
    }
    return CockpitFlightPhase.AIRBORNE
}

private fun resolveCockpitAlert(
    state: DroneState,
    failures: FailureState,
    identity: MavlinkIdentityStatus,
    runtimeLifecycle: RuntimeLifecycleState,
    freshness: CockpitDataFreshness,
    freshnessAgeMs: Long?,
): CockpitAlert? {
    if (runtimeLifecycle == RuntimeLifecycleState.ERROR) {
        return CockpitAlert(
            title = "Simulation runtime error",
            detail = "Live simulation state is unavailable. Open Ops before continuing.",
            severity = CockpitAlertSeverity.CRITICAL,
            recoveryAction = CockpitAction.OPEN_OPS,
        )
    }
    if (runtimeLifecycle == RuntimeLifecycleState.STOPPED || runtimeLifecycle == RuntimeLifecycleState.STOPPING) {
        return CockpitAlert(
            title = "Simulation runtime is stopped",
            detail = "Displayed values are not live. Open Ops to inspect runtime state.",
            severity = CockpitAlertSeverity.CRITICAL,
            recoveryAction = CockpitAction.OPEN_OPS,
        )
    }
    if (freshness == CockpitDataFreshness.STALE) {
        return CockpitAlert(
            title = "Live data is stale",
            detail = "Last authoritative update was ${formatAge(freshnessAgeMs)} ago. Actions are locked.",
            severity = CockpitAlertSeverity.CRITICAL,
            recoveryAction = CockpitAction.OPEN_OPS,
        )
    }
    if (freshness == CockpitDataFreshness.UNAVAILABLE) {
        return CockpitAlert(
            title = "Waiting for simulation state",
            detail = "Controls remain locked until authoritative telemetry is available.",
            severity = CockpitAlertSeverity.CAUTION,
            recoveryAction = CockpitAction.OPEN_OPS,
        )
    }
    if (identity.identityConflict) {
        return CockpitAlert(
            title = "MAVLink identity conflict",
            detail = "Vehicle and GCS identity cannot be trusted. Review peer identity in Ops.",
            severity = CockpitAlertSeverity.CRITICAL,
            recoveryAction = CockpitAction.OPEN_OPS,
        )
    }
    if (failures.hasMotorFailure && state.armed) {
        return flightCriticalAlert(
            state = state,
            title = "Motor response degraded",
            detail = "Thrust authority is reduced. Use the available safe landing path.",
        )
    }
    if (state.batteryRemainingPercent.toInt() <= CriticalBatteryPercent && state.armed) {
        return flightCriticalAlert(
            state = state,
            title = "Battery critical · land now",
            detail = "Reserve is at ${state.batteryRemainingPercent.toInt()}%. Continued flight is unsafe.",
        )
    }
    val positionMode = state.mode in setOf(FlightMode.AUTO, FlightMode.GUIDED, FlightMode.LOITER, FlightMode.RTL)
    if ((!failures.gpsEnabled || state.gpsFixType.toInt() < MinimumPositionFixType) && state.armed && positionMode) {
        return flightCriticalAlert(
            state = state,
            title = "GPS lost · position guidance degraded",
            detail = "${state.mode.displayName} cannot rely on a current position fix.",
        )
    }
    if (failures.lostLinkActive && state.controlAuthority in ExternalAuthorities) {
        return CockpitAlert(
            title = "GCS link unavailable",
            detail = "External commands may not arrive. MAVLab does not imply an automatic failsafe response.",
            severity = CockpitAlertSeverity.CRITICAL,
            recoveryAction = CockpitAction.OPEN_OPS,
        )
    }
    if (state.batteryRemainingPercent.toInt() <= LowBatteryPercent) {
        return CockpitAlert(
            title = "Battery reserve low",
            detail = "${state.batteryRemainingPercent.toInt()}% remains. Prepare to return or land.",
            severity = CockpitAlertSeverity.CAUTION,
        )
    }
    if (!failures.gpsEnabled || state.gpsFixType.toInt() < MinimumPositionFixType) {
        return CockpitAlert(
            title = "GPS unavailable",
            detail = "Position guidance is unavailable until a valid fix returns.",
            severity = CockpitAlertSeverity.CAUTION,
        )
    }
    if (freshness == CockpitDataFreshness.DELAYED) {
        return CockpitAlert(
            title = "Live data delayed",
            detail = "Last update was ${formatAge(freshnessAgeMs)} ago.",
            severity = CockpitAlertSeverity.CAUTION,
        )
    }
    if (failures.activeCount > 0) {
        return CockpitAlert(
            title = "Simulation condition active",
            detail = "${failures.activeCount} condition${if (failures.activeCount == 1) "" else "s"} may affect flight behaviour.",
            severity = CockpitAlertSeverity.ADVISORY,
            recoveryAction = CockpitAction.OPEN_OPS,
        )
    }
    return null
}

private fun flightCriticalAlert(
    state: DroneState,
    title: String,
    detail: String,
): CockpitAlert {
    val action = if (state.controlAuthority in ExternalAuthorities) {
        CockpitAction.TAKE_CONTROL_AND_LAND
    } else {
        CockpitAction.LAND
    }
    return CockpitAlert(title, detail, CockpitAlertSeverity.CRITICAL, action)
}

private fun resolveCockpitTask(
    state: DroneState,
    mission: MissionProgress,
    uploadStatus: MissionUploadStatus,
    recording: FlightRecordingStatus,
    identity: MavlinkIdentityStatus,
    flightPhase: CockpitFlightPhase,
    controllerInputMode: ControllerInputMode,
): CockpitTaskPresentation {
    if (flightPhase == CockpitFlightPhase.FLIGHT_COMPLETE) {
        val session = recording.lastSession
        return CockpitTaskPresentation(
            title = "Flight complete",
            detail = session?.let { "Session ${it.id} saved locally" } ?: "Session saved locally",
            trailing = "LOG SAVED",
            progress = 1f,
        )
    }
    if (state.mode == FlightMode.LAND || flightPhase == CockpitFlightPhase.LANDING) {
        return CockpitTaskPresentation(
            title = "Landing at home",
            detail = "%.1f m AGL · %+.2f m/s vertical".format(state.altitudeAglMeters, state.verticalSpeedMS),
            trailing = "LANDING",
        )
    }
    if (state.mode == FlightMode.RTL) {
        return CockpitTaskPresentation(
            title = "Return to launch",
            detail = "Home %.1f m · battery %d%%".format(
                distanceFromHomeMeters(state),
                state.batteryRemainingPercent.toInt(),
            ),
            trailing = "HOME LOCKED",
        )
    }
    if (state.mode == FlightMode.LOITER) {
        return CockpitTaskPresentation(
            title = "Holding position",
            detail = "Monitor drift and navigation health",
            trailing = "LOITER",
        )
    }
    val missionActive = mission.loaded && !mission.complete &&
        state.mode == FlightMode.AUTO && state.controlAuthority == ControlAuthority.GCS_MISSION
    if (missionActive) {
        val current = mission.activeTarget?.sequence?.plus(1) ?: (mission.currentIndex + 1)
        val total = mission.items.size.coerceAtLeast(1)
        return CockpitTaskPresentation(
            title = "WP $current of $total · Active",
            detail = missionFocusLabel(mission),
            trailing = "AUTO · GCS MISSION",
            progress = (mission.completedCount.toFloat() / total).coerceIn(0f, 1f),
        )
    }
    if (state.controlAuthority == ControlAuthority.GCS_DIRECT) {
        return CockpitTaskPresentation(
            title = "GCS direct control",
            detail = identity.lastGcsSystemId?.let { "Commands from GCS SYSID $it" } ?: "External commands are active",
            trailing = "GCS DIRECT",
        )
    }
    if (state.controlAuthority == ControlAuthority.CONTROLLER) {
        return CockpitTaskPresentation(
            title = if (state.armed) "${controllerInputMode.label} active" else "Controller ready",
            detail = if (state.armed) "Local input is accepted by the simulation engine" else "Arm when the control path is ready",
            trailing = "AUTH · CONTROLLER",
        )
    }
    if (mission.loaded && (uploadStatus.phase == MissionUploadPhase.ACCEPTED || !mission.complete)) {
        return CockpitTaskPresentation(
            title = "Mission accepted · not running",
            detail = "${mission.items.size} items loaded into the simulated vehicle",
            trailing = "MISSION READY",
        )
    }
    return CockpitTaskPresentation(
        title = "Ready for a new flight",
        detail = "No active task",
        trailing = if (identity.gcsConnected) "QGC · CONNECTED" else "AUTH · IDLE",
    )
}

private fun resolveCockpitActions(
    state: DroneState,
    mission: MissionProgress,
    flightPhase: CockpitFlightPhase,
    runtimeLifecycle: RuntimeLifecycleState,
    freshness: CockpitDataFreshness,
    alert: CockpitAlert?,
): Pair<CockpitActionSpec?, CockpitActionSpec?> {
    if (runtimeLifecycle != RuntimeLifecycleState.RUNNING ||
        freshness == CockpitDataFreshness.STALE ||
        freshness == CockpitDataFreshness.UNAVAILABLE
    ) {
        return actionSpec(CockpitAction.OPEN_OPS) to null
    }
    if (alert?.severity == CockpitAlertSeverity.CRITICAL && alert.recoveryAction != null) {
        val secondary = if (alert.recoveryAction != CockpitAction.OPEN_OPS) {
            actionSpec(CockpitAction.OPEN_OPS)
        } else {
            null
        }
        return actionSpec(alert.recoveryAction) to secondary
    }
    return when (flightPhase) {
        CockpitFlightPhase.DISARMED_GROUND -> if (state.controlAuthority in ExternalAuthorities) {
            actionSpec(
                if (state.controlAuthority == ControlAuthority.GCS_MISSION || mission.loaded) {
                    CockpitAction.OPEN_MISSION
                } else {
                    CockpitAction.OPEN_OPS
                },
            ) to actionSpec(CockpitAction.OPEN_CONTROLLER)
        } else {
            actionSpec(CockpitAction.ARM) to actionSpec(
                if (mission.loaded) CockpitAction.OPEN_MISSION else CockpitAction.OPEN_CONTROLLER,
            )
        }
        CockpitFlightPhase.ARMED_GROUND -> if (state.controlAuthority in ExternalAuthorities) {
            actionSpec(
                if (state.controlAuthority == ControlAuthority.GCS_MISSION) {
                    CockpitAction.OPEN_MISSION
                } else {
                    CockpitAction.OPEN_OPS
                },
            ) to actionSpec(CockpitAction.TAKE_CONTROL_AND_LAND)
        } else {
            actionSpec(CockpitAction.TAKEOFF) to actionSpec(CockpitAction.DISARM)
        }
        CockpitFlightPhase.TAKING_OFF,
        CockpitFlightPhase.AIRBORNE,
        CockpitFlightPhase.HOLDING,
        -> if (state.controlAuthority in ExternalAuthorities) {
            actionSpec(
                if (state.controlAuthority == ControlAuthority.GCS_MISSION) {
                    CockpitAction.OPEN_MISSION
                } else {
                    CockpitAction.OPEN_OPS
                },
            ) to actionSpec(CockpitAction.TAKE_CONTROL_AND_LAND)
        } else {
            actionSpec(CockpitAction.OPEN_CONTROLLER) to actionSpec(CockpitAction.LAND)
        }
        CockpitFlightPhase.RETURNING ->
            actionSpec(CockpitAction.OPEN_SIM) to actionSpec(
                if (state.controlAuthority in ExternalAuthorities) {
                    CockpitAction.TAKE_CONTROL_AND_LAND
                } else {
                    CockpitAction.LAND
                },
            )
        CockpitFlightPhase.LANDING -> actionSpec(CockpitAction.OPEN_SIM) to null
        CockpitFlightPhase.LANDED_ARMED -> actionSpec(CockpitAction.DISARM) to null
        CockpitFlightPhase.FLIGHT_COMPLETE ->
            actionSpec(CockpitAction.REVIEW_FLIGHT) to actionSpec(CockpitAction.NEW_FLIGHT)
    }
}

private fun actionSpec(action: CockpitAction): CockpitActionSpec {
    val label = when (action) {
        CockpitAction.ARM -> "Arm"
        CockpitAction.DISARM -> "Disarm"
        CockpitAction.TAKEOFF -> "Takeoff"
        CockpitAction.LAND -> "Land"
        CockpitAction.TAKE_CONTROL_AND_LAND -> "Take control & land"
        CockpitAction.OPEN_CONTROLLER -> "Open Controller"
        CockpitAction.OPEN_MISSION -> "View Mission"
        CockpitAction.OPEN_SIM -> "Open SIM"
        CockpitAction.OPEN_OPS -> "Open Ops"
        CockpitAction.REVIEW_FLIGHT -> "Review flight"
        CockpitAction.NEW_FLIGHT -> "New flight"
    }
    return CockpitActionSpec(action, label)
}

private fun cockpitEyebrow(
    state: DroneState,
    flightPhase: CockpitFlightPhase,
    freshness: CockpitDataFreshness,
    freshnessAgeMs: Long?,
): String {
    if (freshness == CockpitDataFreshness.STALE) return "RECONNECTING · ${formatAge(freshnessAgeMs)}"
    if (freshness == CockpitDataFreshness.UNAVAILABLE) return "RUNTIME · RESTORING"
    return when (flightPhase) {
        CockpitFlightPhase.DISARMED_GROUND -> "READY · DISARMED"
        CockpitFlightPhase.ARMED_GROUND -> "ARMED · GROUND"
        CockpitFlightPhase.TAKING_OFF -> "TAKEOFF · ACTIVE"
        CockpitFlightPhase.AIRBORNE -> when (state.controlAuthority) {
            ControlAuthority.GCS_MISSION -> "AUTO MISSION · LIVE"
            ControlAuthority.GCS_DIRECT -> "IN FLIGHT · GCS DIRECT"
            else -> "IN FLIGHT · MANUAL"
        }
        CockpitFlightPhase.HOLDING -> "HOLDING POSITION"
        CockpitFlightPhase.RETURNING -> "RETURNING HOME"
        CockpitFlightPhase.LANDING -> "LANDING · ACTIVE"
        CockpitFlightPhase.LANDED_ARMED -> "TOUCHDOWN · DISARM REQUIRED"
        CockpitFlightPhase.FLIGHT_COMPLETE -> "FLIGHT COMPLETE"
    }
}

private fun vehicleLabel(phase: CockpitFlightPhase): String {
    return when (phase) {
        CockpitFlightPhase.DISARMED_GROUND,
        CockpitFlightPhase.FLIGHT_COMPLETE,
        -> "DISARMED"
        CockpitFlightPhase.LANDED_ARMED -> "LANDED · ARMED"
        CockpitFlightPhase.LANDING -> "LANDING"
        else -> "ARMED"
    }
}

private fun formatAge(ageMs: Long?): String {
    if (ageMs == null) return "unknown"
    return if (ageMs < 1_000L) "<1s" else "${ageMs / 1_000L}s"
}

private val ExternalAuthorities = setOf(ControlAuthority.GCS_DIRECT, ControlAuthority.GCS_MISSION)
private const val LiveTelemetryMaxAgeMs = 1_500L
private const val DelayedTelemetryMaxAgeMs = 5_000L
private const val FlightCompleteWindowMs = 60_000L
private const val GroundAltitudeThresholdMeters = 0.25f
private const val GroundVerticalSpeedThresholdMs = 0.2f
private const val TakeoffPhaseCeilingMeters = 3f
private const val TakeoffVerticalSpeedThresholdMs = 0.25f
private const val CriticalBatteryPercent = 15
private const val LowBatteryPercent = 30
private const val MinimumPositionFixType = 3
