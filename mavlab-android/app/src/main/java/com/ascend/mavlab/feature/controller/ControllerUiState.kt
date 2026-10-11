package com.ascend.mavlab.feature.controller

import com.ascend.mavlab.core.common.ControllerInputMode
import com.ascend.mavlab.core.common.RuntimeLifecycleState
import com.ascend.mavlab.core.mavlink.MavlinkIdentityStatus
import com.ascend.mavlab.core.sensors.OrientationSource
import com.ascend.mavlab.feature.dashboard.CockpitDataFreshness
import com.ascend.mavlab.feature.dashboard.resolveCockpitFreshness
import com.ascend.mavlab.simulation.engine.ControlAuthority
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.FlightMode
import com.ascend.mavlab.simulation.failures.FailureState
import kotlin.math.abs

internal enum class ControllerAction {
    ARM, TAKEOFF, LAND, DISARM, CALIBRATE, TAKE_CONTROL, TAKE_CONTROL_AND_LAND, OPEN_MISSION, OPEN_OPS,
}

internal data class ControllerActionSpec(
    val action: ControllerAction,
    val label: String,
    val enabled: Boolean = true,
)

internal data class ControllerUiState(
    val freshness: CockpitDataFreshness,
    val inputStatus: String,
    val blockedReason: String?,
    val warning: String?,
    val criticalWarning: Boolean,
    val controlsEnabled: Boolean,
    val inputSelectionEnabled: Boolean,
    val calibrationEnabled: Boolean,
    val flightModesEnabled: Boolean,
    val primaryAction: ControllerActionSpec,
    val secondaryAction: ControllerActionSpec,
    val actionHint: String,
    val onGround: Boolean,
)

internal fun deriveControllerUiState(
    state: DroneState,
    inputMode: ControllerInputMode,
    sensorSource: OrientationSource,
    failures: FailureState,
    identity: MavlinkIdentityStatus,
    runtimeLifecycle: RuntimeLifecycleState,
    updatedAtElapsedMs: Long?,
    nowElapsedMs: Long,
): ControllerUiState {
    val freshness = resolveCockpitFreshness(runtimeLifecycle, updatedAtElapsedMs, nowElapsedMs)
    val ready = freshness == CockpitDataFreshness.LIVE || freshness == CockpitDataFreshness.DELAYED
    val external = state.controlAuthority == ControlAuthority.GCS_MISSION ||
        state.controlAuthority == ControlAuthority.GCS_DIRECT
    val sensorMissing = inputMode == ControllerInputMode.PHONE_SENSORS && sensorSource == OrientationSource.Unavailable
    val onGround = state.altitudeAglMeters <= 0.25f && abs(state.verticalSpeedMS) <= 0.2f
    val manualMode = state.mode == FlightMode.STABILIZE || state.mode == FlightMode.ALT_HOLD
    val critical = failures.hasMotorFailure || state.batteryRemainingPercent.toInt() in 0..15
    val blockedReason = when {
        !ready -> "Controls unavailable: simulation state is not live. Check Ops."
        state.controlAuthority == ControlAuthority.SAFETY_RECOVERY -> "Fence recovery controls flight. Land or disarm; disable the fence in Ops before resuming manual input."
        state.controlAuthority == ControlAuthority.GCS_MISSION -> "GCS mission has control. Local inputs are paused."
        state.controlAuthority == ControlAuthority.GCS_DIRECT -> if (inputMode == ControllerInputMode.PHONE_SENSORS) {
            "GCS direct command is active. Phone input can reclaim authority; use a deliberate flight action."
        } else "GCS direct has control. Local manipulation is paused; take control deliberately."
        sensorMissing -> "Phone sensors unavailable. Select Custom input while disarmed."
        state.armed && !manualMode && inputMode != ControllerInputMode.DIRECT_RPM ->
            "${state.mode.displayName} controls flight. Local attitude input is limited; choose a manual mode in setup to resume."
        inputMode == ControllerInputMode.DIRECT_RPM && !state.armed -> "Arm to enable motor commands. Target starts at 0 RPM."
        else -> null
    }
    val warning = when {
        !ready -> if (runtimeLifecycle == RuntimeLifecycleState.RUNNING) "State unavailable or stale" else "Runtime ${runtimeLifecycle.name.lowercase()}"
        failures.hasMotorFailure -> "Motor failure · reduced control authority"
        state.controlAuthority == ControlAuthority.SAFETY_RECOVERY -> "Fence breach · ${state.safety.recovery} · ${state.safety.returnTarget}"
        state.batteryRemainingPercent.toInt() in 0..15 -> "Critical battery · land and disarm"
        identity.identityConflict -> "MAVLink identity conflict · inspect Ops"
        sensorMissing -> "No phone orientation source"
        state.batteryRemainingPercent.toInt() in 16..30 -> "Low battery · plan to land"
        !failures.gpsEnabled || state.gpsFixType.toInt() < 3 -> "GPS unavailable · position modes need a fix"
        failures.lostLinkActive -> "Lost-link failure injected · inspect Ops"
        external && !identity.gcsConnected -> "GCS peer absent · authority has not automatically changed"
        inputMode == ControllerInputMode.DIRECT_RPM -> "Direct RPM · autopilot protection is bypassed"
        freshness == CockpitDataFreshness.DELAYED -> "Simulation updates delayed"
        else -> null
    }
    val armReady = ready && !external && state.controlAuthority != ControlAuthority.SAFETY_RECOVERY && !sensorMissing && !critical && !identity.identityConflict && onGround
    val primary = when {
        !ready -> ControllerActionSpec(ControllerAction.OPEN_OPS, "Open Ops")
        state.controlAuthority == ControlAuthority.SAFETY_RECOVERY -> if (state.mode != FlightMode.LAND) ControllerActionSpec(ControllerAction.LAND, "Land now") else ControllerActionSpec(ControllerAction.OPEN_OPS, "Fence settings")
        external && state.armed -> ControllerActionSpec(ControllerAction.TAKE_CONTROL_AND_LAND, "Take control & land")
        external -> ControllerActionSpec(ControllerAction.TAKE_CONTROL, "Take local control")
        !state.armed -> ControllerActionSpec(ControllerAction.ARM, "Arm", armReady)
        inputMode == ControllerInputMode.DIRECT_RPM -> ControllerActionSpec(ControllerAction.DISARM, "Stop motors")
        state.mode == FlightMode.LAND && onGround -> ControllerActionSpec(ControllerAction.DISARM, "Disarm")
        !onGround -> ControllerActionSpec(ControllerAction.LAND, if (critical) "Land now" else "Land", state.mode != FlightMode.LAND)
        else -> ControllerActionSpec(ControllerAction.TAKEOFF, "Takeoff", !sensorMissing && !critical)
    }
    val secondary = when {
        !ready -> ControllerActionSpec(ControllerAction.OPEN_MISSION, "Mission details")
        state.armed && primary.action != ControllerAction.DISARM -> ControllerActionSpec(ControllerAction.DISARM, "Disarm")
        external -> ControllerActionSpec(ControllerAction.OPEN_OPS, "Open Ops")
        !state.armed && inputMode == ControllerInputMode.PHONE_SENSORS && !sensorMissing ->
            ControllerActionSpec(ControllerAction.CALIBRATE, "Calibrate")
        else -> ControllerActionSpec(ControllerAction.OPEN_OPS, "Open Ops")
    }
    return ControllerUiState(
        freshness = freshness,
        inputStatus = when {
            !ready -> "INPUT UNAVAILABLE"
            blockedReason != null -> "INPUT PAUSED"
            !state.armed -> "PREVIEW · DISARMED"
            inputMode == ControllerInputMode.DIRECT_RPM -> "DIRECT MOTOR COMMAND"
            else -> "INPUT ACCEPTED"
        },
        blockedReason = blockedReason,
        warning = warning,
        criticalWarning = critical || !ready,
        controlsEnabled = ready && !external && state.controlAuthority != ControlAuthority.SAFETY_RECOVERY && !sensorMissing &&
            (manualMode || !state.armed || inputMode == ControllerInputMode.DIRECT_RPM) &&
            (inputMode != ControllerInputMode.DIRECT_RPM || state.armed),
        inputSelectionEnabled = ready && !state.armed && !external,
        calibrationEnabled = ready && !state.armed && !external && sensorSource != OrientationSource.Unavailable,
        flightModesEnabled = ready && !external && state.controlAuthority != ControlAuthority.SAFETY_RECOVERY && inputMode != ControllerInputMode.DIRECT_RPM,
        primaryAction = primary,
        secondaryAction = secondary,
        actionHint = when {
            !ready -> "No flight commands are sent from stale or unavailable state."
            state.controlAuthority == ControlAuthority.SAFETY_RECOVERY -> "Fence recovery is active. Land/disarm remain available; use Ops to disable enforcement deliberately."
            external -> "Takeover replaces GCS intent. Disarm stops motors immediately."
            state.mode == FlightMode.LAND && state.armed && !onGround -> "Landing active · wait for touchdown, then disarm."
            inputMode == ControllerInputMode.DIRECT_RPM && state.armed -> "All four motors follow one RPM target. Stop motors exits this mode."
            !state.armed && !armReady -> "Arming blocked · resolve the input, battery or system warning."
            !state.armed && inputMode == ControllerInputMode.PHONE_SENSORS -> "Calibrate neutral before arming. Arming and takeoff are separate."
            !state.armed && inputMode == ControllerInputMode.DIRECT_RPM -> "Arm starts at 0 RPM. This motor test bypasses the autopilot."
            !state.armed -> "Set neutral inputs before arming. Arming and takeoff are separate."
            onGround -> "${if (primary.action == ControllerAction.DISARM) "Touchdown confirmed" else "Takeoff uses altitude hold"} · Disarm stops the motors."
            else -> "Land returns to the ground. In-flight disarm requires confirmation."
        },
        onGround = onGround,
    )
}
