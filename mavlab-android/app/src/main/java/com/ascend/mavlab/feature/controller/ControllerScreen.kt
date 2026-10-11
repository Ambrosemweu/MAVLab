package com.ascend.mavlab.feature.controller

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.mavlab.core.common.AppRuntime
import com.ascend.mavlab.core.ui.navigation.screenSwipeArea
import com.ascend.mavlab.core.common.ControllerInputMode
import com.ascend.mavlab.core.common.ControllerInputState
import com.ascend.mavlab.core.sensors.OrientationData
import com.ascend.mavlab.core.sensors.OrientationSource
import com.ascend.mavlab.feature.mission.MissionControlRow
import com.ascend.mavlab.feature.mission.deriveMissionUiState
import com.ascend.mavlab.simulation.engine.ControlAuthority
import com.ascend.mavlab.feature.dashboard.CockpitDataFreshness
import com.ascend.mavlab.simulation.autopilot.PilotInput
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.FlightMode
import com.ascend.mavlab.simulation.engine.MotorTelemetry
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI

@Composable
fun ControllerScreen(
    modifier: Modifier = Modifier,
    missionInitiallyExpanded: Boolean = false,
    onOpenSim: () -> Unit = {},
    onOpenOps: () -> Unit = {},
    onMissionDetailsShown: () -> Unit = {},
) {
    val state by AppRuntime.state.collectAsStateWithLifecycle()
    val failures by AppRuntime.failures.collectAsStateWithLifecycle()
    val mission by AppRuntime.missionProgress.collectAsStateWithLifecycle()
    val upload by AppRuntime.missionUploadStatus.collectAsStateWithLifecycle()
    var missionExpanded by rememberSaveable { mutableStateOf(missionInitiallyExpanded) }
    val source by AppRuntime.phoneSensorSource.collectAsStateWithLifecycle()
    val orientation by AppRuntime.phoneSensorOrientation.collectAsStateWithLifecycle()
    val pilotInput by AppRuntime.phoneSensorPilotInput.collectAsStateWithLifecycle()
    val inputs by AppRuntime.controllerInputState.collectAsStateWithLifecycle()
    val identity by AppRuntime.mavlinkIdentityStatus.collectAsStateWithLifecycle()
    val lifecycle by AppRuntime.runtimeLifecycle.collectAsStateWithLifecycle()
    val updatedAt by AppRuntime.stateUpdatedAtElapsedRealtimeMs.collectAsStateWithLifecycle()
    val soundSettings by AppRuntime.soundSettings.collectAsStateWithLifecycle()
    val soundDebug by AppRuntime.soundDebugState.collectAsStateWithLifecycle()
    var nowElapsed by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var setupExpanded by rememberSaveable { mutableStateOf(false) }
    var learningExpanded by rememberSaveable { mutableStateOf(false) }
    var testsExpanded by rememberSaveable { mutableStateOf(false) }
    var soundExpanded by rememberSaveable { mutableStateOf(false) }
    var takeoffHeight by rememberSaveable { mutableFloatStateOf(10f) }
    var confirmDirectRpm by remember { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf<ControllerAction?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(missionInitiallyExpanded) {
        if (missionInitiallyExpanded) { missionExpanded = true; onMissionDetailsShown() }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            nowElapsed = SystemClock.elapsedRealtime()
            delay(1_000L)
        }
    }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(3_000L)
            feedback = null
        }
    }
    val inputUi = deriveControllerUiState(state, inputs.inputMode, source, failures, identity, lifecycle, updatedAt, nowElapsed)
    val missionUi = deriveMissionUiState(state, mission, upload, failures, inputUi.freshness == CockpitDataFreshness.LIVE || inputUi.freshness == CockpitDataFreshness.DELAYED)
    BackHandler(enabled = missionExpanded && missionUi.visible && confirmation == null && !confirmDirectRpm) { missionExpanded = false }
    fun missionActionOrOps(action: ControllerActionSpec) = if (!missionUi.visible && action.action == ControllerAction.OPEN_MISSION) ControllerActionSpec(ControllerAction.OPEN_OPS, "Open Ops") else action
    val ui = inputUi.copy(primaryAction = missionActionOrOps(inputUi.primaryAction), secondaryAction = missionActionOrOps(inputUi.secondaryAction))
    val execute: (ControllerAction) -> Unit = { action ->
        when (action) {
            ControllerAction.ARM -> AppRuntime.setArmed(true)
            ControllerAction.TAKEOFF -> AppRuntime.takeoff(takeoffHeight)
            ControllerAction.LAND -> AppRuntime.land()
            ControllerAction.CALIBRATE -> {
                AppRuntime.calibratePhoneSensors()
                feedback = "Neutral position saved"
            }
            ControllerAction.DISARM -> if (ui.onGround) AppRuntime.setArmed(false) else confirmation = action
            ControllerAction.TAKE_CONTROL, ControllerAction.TAKE_CONTROL_AND_LAND -> confirmation = action
            ControllerAction.OPEN_MISSION -> if (missionUi.visible) missionExpanded = true else onOpenOps()
            ControllerAction.OPEN_OPS -> onOpenOps()
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val landscape = maxWidth > maxHeight
            val controls: @Composable (Modifier) -> Unit = { bodyModifier ->
                Column(
                    bodyModifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MissionControlRow(mission, missionUi, missionExpanded, { missionExpanded = !missionExpanded }, onOpenSim, onOpenOps)
                    if (state.controlAuthority == ControlAuthority.GCS_MISSION) {
                        Text("Local inputs paused · GCS Mission has control", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        InputSelector(inputs.inputMode, ui.inputSelectionEnabled, state.armed) { mode ->
                            if (mode != inputs.inputMode) {
                                if (mode == ControllerInputMode.DIRECT_RPM) confirmDirectRpm = true
                                else {
                                    AppRuntime.setControllerInputMode(mode)
                                    feedback = "${mode.label} selected"
                                }
                            }
                        }
                        ControllerInputCard(ui, feedback, source, inputs, orientation, pilotInput, state.motors)
                        Disclosure("Calibration & control setup", setupExpanded, { setupExpanded = !setupExpanded }) {
                            if (inputs.inputMode == ControllerInputMode.PHONE_SENSORS) {
                                Text("Hold the phone comfortably and keep it still. Calibrate saves this position as neutral.", style = MaterialTheme.typography.bodyMedium)
                                OutlinedButton(
                                    onClick = { AppRuntime.calibratePhoneSensors(); feedback = "Neutral position saved" },
                                    enabled = ui.calibrationEnabled,
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("Calibrate neutral") }
                                if (!ui.calibrationEnabled) Text("Calibration requires a live sensor source, local authority and a disarmed vehicle.", style = MaterialTheme.typography.bodySmall)
                                ControlSlider("Yaw trim", inputs.manualYaw, signedPercent(inputs.manualYaw), -1f, 1f, ui.controlsEnabled, AppRuntime::setControllerManualYaw)
                                Text("Yaw trim: −100% to +100% added to phone rotation.", style = MaterialTheme.typography.bodySmall)
                                Text("Mapping: 30° roll/pitch · 45° yaw · 3° dead zone", style = MaterialTheme.typography.bodySmall)
                            } else if (inputs.inputMode == ControllerInputMode.CUSTOM_INPUT) {
                                OutlinedButton(onClick = {
                                    AppRuntime.setControllerManualRoll(0f)
                                    AppRuntime.setControllerManualPitch(0f)
                                    AppRuntime.setControllerManualYaw(0f)
                                    AppRuntime.setControllerThrottle(0.5f)
                                    feedback = "Inputs centered · throttle 50%"
                                }, enabled = ui.controlsEnabled, modifier = Modifier.fillMaxWidth()) { Text("Center inputs") }
                            }
                            Text("Takeoff height · metres above ground", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(5f, 10f, 20f).forEach { height ->
                                    FilterChip(selected = takeoffHeight == height, onClick = { takeoffHeight = height }, enabled = !state.armed && ui.inputSelectionEnabled, label = { Text("${height.toInt()} m") })
                                }
                            }
                            FlightModeSelector(state.mode, ui.flightModesEnabled, AppRuntime::setMode)
                            Text("Stabilize and Alt Hold accept attitude input. Loiter, RTL and Land use autopilot navigation. Input mode is separate from flight mode.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Disclosure("Learn about control", learningExpanded, { learningExpanded = !learningExpanded }) {
                        Text("Authority tells you who owns flight intent. A connected QGC peer does not by itself have control.", style = MaterialTheme.typography.bodyMedium)
                        Text("Phone tilt steers roll and pitch; rotation steers yaw. Calibrate while disarmed, then Arm and Takeoff. Select Custom input after disarming to leave Phone sensors.", style = MaterialTheme.typography.bodyMedium)
                        Text("Alt Hold: 50% throttle maintains the height target; raising or lowering it adjusts that target. Stabilize: throttle commands motor thrust.", style = MaterialTheme.typography.bodyMedium)
                        Text("GCS missions pause local inputs. Take control & land deliberately replaces the mission. Direct GCS commands can be followed by local phone input under the existing engine authority rules.", style = MaterialTheme.typography.bodyMedium)
                        Text("Direct RPM bypasses the autopilot and sends one speed to all four simulated motors. It is a motor test, not a flight control mode.", style = MaterialTheme.typography.bodyMedium)
                    }
                    AdvancedTestInputs(failures, testsExpanded, { testsExpanded = it }, enabled = ui.freshness == CockpitDataFreshness.LIVE || ui.freshness == CockpitDataFreshness.DELAYED)
                    DroneSoundLabCard(
                        settings = soundSettings, debugState = soundDebug, expanded = soundExpanded,
                        onExpandedChange = { soundExpanded = it },
                        onEnabledChange = AppRuntime::setDroneSoundEnabled,
                        onMasterVolumeChange = AppRuntime::setDroneSoundMasterVolume,
                        onPerMotorMixChange = AppRuntime::setDroneSoundPerMotorMix,
                        onRoughnessChange = AppRuntime::setDroneSoundRoughness,
                        onAlertsEnabledChange = AppRuntime::setDroneSoundAlertEnabled,
                        onTestModeChange = AppRuntime::setDroneSoundTestMode,
                        onTestRpmChange = AppRuntime::setDroneSoundTestRpm,
                        onProceduralEnabledChange = AppRuntime::setDroneSoundProceduralEnabled,
                        onSampleBedAmountChange = AppRuntime::setDroneSoundSampleBedAmount,
                        onBladeHarmonicsAmountChange = AppRuntime::setDroneSoundBladeHarmonicsAmount,
                        onPropWashAmountChange = AppRuntime::setDroneSoundPropWashAmount,
                        onMotorWhineAmountChange = AppRuntime::setDroneSoundMotorWhineAmount,
                        onBladeCountChange = AppRuntime::setDroneSoundBladeCount,
                        onAcousticProfileChange = AppRuntime::setDroneSoundAcousticProfile,
                        onSynthQualityChange = AppRuntime::setDroneSoundSynthQuality,
                        onShowAcousticTelemetryChange = AppRuntime::setDroneSoundShowAcousticTelemetry,
                        onReset = AppRuntime::resetDroneSoundSettings,
                    )
                }
            }
            Column(Modifier.fillMaxSize()) {
                ControllerStatus(state, ui, inputs.inputMode, identity.gcsConnected, failures.activeCount, landscape)
                if (landscape) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        controls(Modifier.weight(1f))
                        ControllerActionDock(ui, takeoffHeight, execute, Modifier.width(240.dp).align(Alignment.Bottom), stacked = true)
                    }
                } else {
                    controls(Modifier.weight(1f).fillMaxWidth())
                    ControllerActionDock(ui, takeoffHeight, execute)
                }
            }
        }
    }
    if (confirmDirectRpm) {
        AlertDialog(
            onDismissRequest = { confirmDirectRpm = false },
            title = { Text("Enable Direct RPM?") },
            text = { Text("This bypasses the autopilot and applies one RPM target to all four simulated motors. Start disarmed at 0 RPM. Use Stop motors to exit.") },
            confirmButton = {
                TextButton(onClick = {
                    if (ui.inputSelectionEnabled) {
                        AppRuntime.setControllerInputMode(ControllerInputMode.DIRECT_RPM)
                        feedback = "Direct RPM selected · target 0 RPM"
                    }
                    confirmDirectRpm = false
                }, enabled = ui.inputSelectionEnabled) { Text("Enable Direct RPM") }
            },
            dismissButton = { TextButton(onClick = { confirmDirectRpm = false }) { Text("Cancel") } },
        )
    }
    confirmation?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(if (action == ControllerAction.DISARM) "Stop motors in flight?" else "Replace GCS control?") },
            text = { Text(when (action) {
                ControllerAction.DISARM -> "Disarming cuts motor thrust immediately. The simulated vehicle may fall. Use Land for a controlled descent."
                ControllerAction.TAKE_CONTROL_AND_LAND -> "Local control will replace GCS intent and command a landing through the simulation engine."
                else -> "Hold the phone neutral. Local control will replace external intent with manual control."
            }) },
            confirmButton = {
                TextButton(onClick = {
                    if (ui.freshness == CockpitDataFreshness.LIVE || ui.freshness == CockpitDataFreshness.DELAYED) {
                        when (action) {
                            ControllerAction.DISARM -> AppRuntime.setArmed(false)
                            ControllerAction.TAKE_CONTROL_AND_LAND -> AppRuntime.land()
                            ControllerAction.TAKE_CONTROL -> AppRuntime.setMode(if (state.armed) FlightMode.ALT_HOLD else FlightMode.STABILIZE)
                            else -> Unit
                        }
                    }
                    confirmation = null
                }, enabled = ui.freshness == CockpitDataFreshness.LIVE || ui.freshness == CockpitDataFreshness.DELAYED) {
                    Text(if (action == ControllerAction.DISARM) "Disarm now" else "Take control")
                }
            },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } },
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ControllerStatus(state: DroneState, ui: ControllerUiState, inputMode: ControllerInputMode, gcsConnected: Boolean, warningCount: Int, compact: Boolean) {
    Column(Modifier.fillMaxWidth().screenSwipeArea().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (compact) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Control", style = MaterialTheme.typography.titleMedium)
                Text("Authority · ${state.controlAuthority.displayName}", style = MaterialTheme.typography.labelLarge)
                Text("${if (state.armed) "Armed" else "Disarmed"} · ${state.mode.displayName}", style = MaterialTheme.typography.labelLarge)
                Text("${inputMode.label} · Battery ${state.batteryRemainingPercent}%", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Control", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(if (ui.freshness == CockpitDataFreshness.UNAVAILABLE || ui.freshness == CockpitDataFreshness.STALE) "STATE NOT LIVE" else "Battery ${state.batteryRemainingPercent}%", style = MaterialTheme.typography.labelLarge)
            }
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusValue("AUTHORITY", if (ui.freshness == CockpitDataFreshness.UNAVAILABLE) "Unavailable" else state.controlAuthority.displayName, Modifier.weight(1.2f))
                    StatusValue("FLIGHT MODE", state.mode.displayName, Modifier.weight(1f))
                    StatusValue("VEHICLE", if (state.armed) "Armed" else "Disarmed", Modifier.weight(1f))
                }
            }
        }
        Text(
            if (ui.freshness == CockpitDataFreshness.UNAVAILABLE || ui.freshness == CockpitDataFreshness.STALE) "Last known state · ${ui.freshness.name.lowercase()}"
            else "GPS ${if (state.gpsFixType.toInt() >= 3) "3D fix" else "no fix"}  ·  QGC ${if (gcsConnected) "connected" else "waiting"}  ·  $warningCount issues",
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!compact) Text("Input · ${inputMode.label}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        ui.warning?.let { warning ->
            Surface(
                color = if (ui.criticalWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(),
            ) { Text(warning, Modifier.padding(10.dp), style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun StatusValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputSelector(mode: ControllerInputMode, enabled: Boolean, armed: Boolean, onSelect: (ControllerInputMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ControllerInputMode.entries.forEach { input ->
                FilterChip(selected = input == mode, onClick = { onSelect(input) }, enabled = enabled, label = { Text(input.label) })
            }
        }
        if (!enabled) Text(if (armed) "Input mode locked while armed" else "Input selection needs live state and local authority", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ControllerInputCard(
    ui: ControllerUiState,
    feedback: String?,
    source: OrientationSource,
    inputs: ControllerInputState,
    orientation: OrientationData,
    pilotInput: PilotInput,
    motors: List<MotorTelemetry>,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui.inputStatus, style = MaterialTheme.typography.labelLarge, color = if (ui.controlsEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(feedback ?: " ", Modifier.heightIn(min = 20.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            ui.blockedReason?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            when (inputs.inputMode) {
                ControllerInputMode.PHONE_SENSORS -> {
                    val config = remember { ControlConfig() }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("PHONE ATTITUDE", style = MaterialTheme.typography.labelSmall)
                        Text(source.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (source != OrientationSource.Unavailable && ui.freshness != CockpitDataFreshness.UNAVAILABLE && ui.freshness != CockpitDataFreshness.STALE) {
                        Box(Modifier.fillMaxWidth().screenSwipeArea(), contentAlignment = Alignment.Center) {
                            TiltVisualizer(
                                pilotInput.roll, pilotInput.pitch, config.deadzoneRad / config.maxRollAngleRad,
                                Modifier.size(160.dp).semantics { contentDescription = "Phone tilt preview. Roll ${angle(orientation.roll)}, pitch ${angle(orientation.pitch)}" },
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusValue("ROLL", angle(orientation.roll), Modifier.weight(1f))
                            StatusValue("PITCH", angle(orientation.pitch), Modifier.weight(1f))
                            StatusValue("YAW", angle(orientation.yaw), Modifier.weight(1f))
                        }
                    } else {
                        Text("Phone attitude unavailable", Modifier.padding(vertical = 36.dp), style = MaterialTheme.typography.titleMedium)
                    }
                    ControlSlider("Throttle", inputs.throttle, "${(inputs.throttle * 100).toInt()}%", enabled = ui.controlsEnabled, onValueChange = AppRuntime::setControllerThrottle)
                    Text("0–100% · 50% maintains height in Alt Hold", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ControllerInputMode.CUSTOM_INPUT -> {
                    Text("Manual inputs", style = MaterialTheme.typography.titleMedium)
                    ControlSlider("Throttle", inputs.throttle, "${(inputs.throttle * 100).toInt()}%", enabled = ui.controlsEnabled, onValueChange = AppRuntime::setControllerThrottle)
                    ControlSlider("Roll", inputs.manualRoll, signedPercent(inputs.manualRoll), -1f, 1f, ui.controlsEnabled, AppRuntime::setControllerManualRoll)
                    ControlSlider("Pitch", inputs.manualPitch, signedPercent(inputs.manualPitch), -1f, 1f, ui.controlsEnabled, AppRuntime::setControllerManualPitch)
                    ControlSlider("Yaw", inputs.manualYaw, signedPercent(inputs.manualYaw), -1f, 1f, ui.controlsEnabled, AppRuntime::setControllerManualYaw)
                    Text("Throttle 0–100% · attitude −100% to +100% · 0% is neutral", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ControllerInputMode.DIRECT_RPM -> {
                    Text("Direct motor command", style = MaterialTheme.typography.titleMedium)
                    Text("${inputs.directRpm.toInt()} RPM", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.tertiary)
                    ControlSlider("Target RPM", inputs.directRpm, "${inputs.directRpm.toInt()} RPM", 0f, ControllerInputState.MaxDirectRpm, ui.controlsEnabled, AppRuntime::setControllerDirectRpm)
                    Text("0–10,000 RPM · same command to all four motors", style = MaterialTheme.typography.labelSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        motors.forEachIndexed { index, motor -> StatusValue("M${index + 1}", "${motor.rpm.toInt()}", Modifier.weight(1f)) }
                    }
                    Text("Actual motor telemetry · RPM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ControllerActionDock(
    ui: ControllerUiState,
    height: Float,
    execute: (ControllerAction) -> Unit,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(ui.actionHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (stacked) {
                ActionButton(ui.primaryAction, height, execute, Modifier.fillMaxWidth())
                ActionButton(ui.secondaryAction, height, execute, Modifier.fillMaxWidth(), outlined = true)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton(ui.primaryAction, height, execute, Modifier.weight(1f))
                    ActionButton(ui.secondaryAction, height, execute, Modifier.weight(1f), outlined = true)
                }
            }
        }
    }
}

@Composable
private fun ActionButton(spec: ControllerActionSpec, height: Float, execute: (ControllerAction) -> Unit, modifier: Modifier, outlined: Boolean = false) {
    val label = if (spec.action == ControllerAction.TAKEOFF) "Takeoff · ${height.toInt()} m" else spec.label
    if (outlined) {
        OutlinedButton(onClick = { execute(spec.action) }, enabled = spec.enabled, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) { Text(label) }
    } else {
        Button(
            onClick = { execute(spec.action) }, enabled = spec.enabled, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (spec.action == ControllerAction.LAND || spec.action == ControllerAction.TAKE_CONTROL_AND_LAND) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary, contentColor = Color(0xFF081526)),
        ) { Text(label) }
    }
}

@Composable
private fun Disclosure(title: String, expanded: Boolean, onToggle: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("${if (expanded) "Hide" else "Show"} $title") }
        if (expanded) Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

private fun angle(radians: Float) = "%+.1f°".format(radians * 180f / PI.toFloat())
private fun signedPercent(value: Float) = "%+d%%".format((value * 100).toInt())
