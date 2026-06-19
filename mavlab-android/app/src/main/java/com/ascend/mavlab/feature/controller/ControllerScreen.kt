package com.ascend.mavlab.feature.controller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ascend.mavlab.core.common.AppRuntime
import kotlinx.coroutines.delay
import com.ascend.mavlab.core.sensors.OrientationData
import com.ascend.mavlab.core.sensors.OrientationSource
import com.ascend.mavlab.simulation.autopilot.PilotInput
import com.ascend.mavlab.simulation.audio.DroneAcousticProfile
import com.ascend.mavlab.simulation.audio.DroneSoundDebugState
import com.ascend.mavlab.simulation.audio.DroneSoundSettings
import com.ascend.mavlab.simulation.audio.DroneSynthQuality
import com.ascend.mavlab.simulation.engine.ControlAuthority
import com.ascend.mavlab.simulation.engine.FlightMode
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.engine.MotorTelemetry
import kotlin.math.PI

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControllerScreen(modifier: Modifier = Modifier) {
    val state by AppRuntime.state.collectAsState()
    val failures by AppRuntime.failures.collectAsState()
    val source by AppRuntime.phoneSensorSource.collectAsState()
    val calibratedOrientation by AppRuntime.phoneSensorOrientation.collectAsState()
    val phoneSensorPilotInput by AppRuntime.phoneSensorPilotInput.collectAsState()
    val soundSettings by AppRuntime.soundSettings.collectAsState()
    val soundDebugState by AppRuntime.soundDebugState.collectAsState()
    val config = remember { ControlConfig() }
    val sensorAvailable = source != OrientationSource.Unavailable
    val inputPaused = state.controlAuthority == ControlAuthority.GCS_MISSION

    var inputMode by remember {
        mutableStateOf(
            if (sensorAvailable) ControllerInputMode.PHONE_SENSORS else ControllerInputMode.CUSTOM_INPUT,
        )
    }
    var pilotInput by remember { mutableStateOf(PilotInput(throttle = 0.5f)) }
    var throttle by remember { mutableFloatStateOf(0.5f) }
    var manualRoll by remember { mutableFloatStateOf(0f) }
    var manualPitch by remember { mutableFloatStateOf(0f) }
    var manualYaw by remember { mutableFloatStateOf(0f) }
    var directRpm by remember { mutableFloatStateOf(0f) }
    var advancedExpanded by remember { mutableStateOf(false) }
    var soundLabExpanded by remember { mutableStateOf(false) }
    var isDetectingArmedRpm by remember { mutableStateOf(false) }
    var wasArmed by remember { mutableStateOf(state.armed) }

    // Detect arming transitions or disarming when in DIRECT_RPM mode
    LaunchedEffect(state.armed, inputMode) {
        if (inputMode == ControllerInputMode.DIRECT_RPM) {
            if (state.armed) {
                if (!wasArmed) {
                    // Just armed: trigger detection
                    isDetectingArmedRpm = true
                }
            } else {
                // Disarmed: reset
                directRpm = 0f
                isDetectingArmedRpm = false
            }
        }
        wasArmed = state.armed
    }

    // Initialize/detect RPM on switching to DIRECT_RPM mode
    LaunchedEffect(inputMode) {
        if (inputMode == ControllerInputMode.DIRECT_RPM) {
            if (state.armed) {
                val currentRpm = state.motors.map { it.rpm }.average().toFloat()
                if (currentRpm > 100f) {
                    directRpm = currentRpm
                } else {
                    // Try to detect it dynamically if it spins up shortly
                    isDetectingArmedRpm = true
                }
            } else {
                directRpm = 0f
            }
        }
    }

    // Monitor motor speeds while detecting arming RPM
    LaunchedEffect(state.motors, isDetectingArmedRpm) {
        if (isDetectingArmedRpm) {
            val currentRpm = state.motors.map { it.rpm }.average().toFloat()
            if (currentRpm > 100f) {
                directRpm = currentRpm
                isDetectingArmedRpm = false
            }
        }
    }

    // Safety timeout for arming detection (e.g. if armed in Stabilize mode with 0 throttle)
    LaunchedEffect(isDetectingArmedRpm) {
        if (isDetectingArmedRpm) {
            delay(500L)
            if (isDetectingArmedRpm) {
                isDetectingArmedRpm = false
            }
        }
    }

    // Apply the speed override
    LaunchedEffect(inputMode, inputPaused, directRpm, isDetectingArmedRpm) {
        if (inputMode == ControllerInputMode.DIRECT_RPM && !inputPaused && !isDetectingArmedRpm) {
            AppRuntime.setMotorSpeedOverrideRpm(directRpm)
        } else {
            AppRuntime.setMotorSpeedOverrideRpm(null)
        }
    }

    LaunchedEffect(inputMode, inputPaused, throttle, manualYaw, sensorAvailable) {
        val enabled = inputMode == ControllerInputMode.PHONE_SENSORS && sensorAvailable && !inputPaused
        AppRuntime.setPhoneSensorThrottle(throttle)
        AppRuntime.setPhoneSensorYawTrim(manualYaw)
        AppRuntime.setPhoneSensorControlEnabled(enabled)
        if (enabled) {
            pilotInput = phoneSensorPilotInput
        }
    }

    LaunchedEffect(inputMode, inputPaused, throttle, manualRoll, manualPitch, manualYaw) {
        if (inputMode != ControllerInputMode.CUSTOM_INPUT || inputPaused) return@LaunchedEffect
        AppRuntime.setPhoneSensorControlEnabled(false)
        pilotInput = PilotInput(
            roll = manualRoll,
            pitch = manualPitch,
            throttle = throttle,
            yaw = manualYaw,
        )
        AppRuntime.setPilotInput(pilotInput)
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Controller", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Local/manual control for the MAVLab digital twin. QGC missions take authority while AUTO is active.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Text(
                text = "Sensor: ${source.label}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (inputPaused) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("GCS Mission active", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Manual Controller inputs are paused until mission control is released.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                        )
                    }
                }
            }

            InputModeSelector(
                selected = inputMode,
                sensorAvailable = sensorAvailable,
                onSelected = { inputMode = it },
            )

            AdvancedTestInputs(
                failures = failures,
                expanded = advancedExpanded,
                onExpandedChange = { advancedExpanded = it },
            )

            DroneSoundLabCard(
                settings = soundSettings,
                debugState = soundDebugState,
                expanded = soundLabExpanded,
                onExpandedChange = { soundLabExpanded = it },
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

            when (inputMode) {
                ControllerInputMode.PHONE_SENSORS -> PhoneSensorControls(
                    pilotInput = phoneSensorPilotInput,
                    calibratedOrientation = calibratedOrientation,
                    config = config,
                    throttle = throttle,
                    manualYaw = manualYaw,
                    inputPaused = inputPaused,
                    onThrottleChange = {
                        throttle = it
                        AppRuntime.setPhoneSensorThrottle(it)
                    },
                    onYawChange = {
                        manualYaw = it
                        AppRuntime.setPhoneSensorYawTrim(it)
                    },
                )
                ControllerInputMode.CUSTOM_INPUT -> CustomInputControls(
                    throttle = throttle,
                    roll = manualRoll,
                    pitch = manualPitch,
                    yaw = manualYaw,
                    inputPaused = inputPaused,
                    onThrottleChange = { throttle = it },
                    onRollChange = { manualRoll = it },
                    onPitchChange = { manualPitch = it },
                    onYawChange = { manualYaw = it },
                )
                ControllerInputMode.DIRECT_RPM -> DirectRpmControls(
                    rpm = directRpm,
                    motorTelemetry = state.motors,
                    inputPaused = inputPaused,
                    onRpmChange = { directRpm = it },
                )
            }

            HorizontalDivider()
            FlightModeSelector(
                selected = state.mode,
                enabled = !inputPaused,
                onSelected = AppRuntime::setMode,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { AppRuntime.setArmed(!state.armed) },
                    modifier = Modifier.weight(1f),
                    enabled = !inputPaused,
                ) {
                    Text(if (state.armed) "Disarm" else "Arm")
                }
                OutlinedButton(
                    onClick = { AppRuntime.takeoff(10f) },
                    modifier = Modifier.weight(1f),
                    enabled = !inputPaused,
                ) {
                    Text("Takeoff")
                }
                OutlinedButton(
                    onClick = AppRuntime::land,
                    modifier = Modifier.weight(1f),
                    enabled = !inputPaused,
                ) {
                    Text("Land")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (inputMode == ControllerInputMode.PHONE_SENSORS) {
                    OutlinedButton(
                        onClick = AppRuntime::calibratePhoneSensors,
                        enabled = sensorAvailable && !inputPaused,
                    ) {
                        Text("Calibrate")
                    }
                }
                Text(
                    text = "${state.controlAuthority.displayName} | ${state.mode.displayName} | ${if (state.armed) "Armed" else "Disarmed"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DroneSoundLabCard(
    settings: DroneSoundSettings,
    debugState: DroneSoundDebugState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onMasterVolumeChange: (Float) -> Unit,
    onPerMotorMixChange: (Float) -> Unit,
    onRoughnessChange: (Float) -> Unit,
    onAlertsEnabledChange: (Boolean) -> Unit,
    onTestModeChange: (Boolean) -> Unit,
    onTestRpmChange: (Float) -> Unit,
    onProceduralEnabledChange: (Boolean) -> Unit,
    onSampleBedAmountChange: (Float) -> Unit,
    onBladeHarmonicsAmountChange: (Float) -> Unit,
    onPropWashAmountChange: (Float) -> Unit,
    onMotorWhineAmountChange: (Float) -> Unit,
    onBladeCountChange: (Int) -> Unit,
    onAcousticProfileChange: (String) -> Unit,
    onSynthQualityChange: (DroneSynthQuality) -> Unit,
    onShowAcousticTelemetryChange: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    var advancedAcousticsExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (expanded) "Hide Advanced sound inputs" else "Advanced sound inputs")
        }
        if (!expanded) return@Column

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sound test inputs", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Simulation-driven motor sound. Tune the acoustic response while using Controller or Direct RPM mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SwitchRow(
                label = "Sound enabled",
                checked = settings.enabled,
                onCheckedChange = onEnabledChange,
            )
            ControlSlider(
                label = "Master volume",
                value = settings.masterVolume,
                valueLabel = "${(settings.masterVolume * 100f).toInt()}%",
                onValueChange = onMasterVolumeChange,
            )
            ControlSlider(
                label = "Per-motor mix",
                value = settings.perMotorMix,
                valueLabel = "${(settings.perMotorMix * 100f).toInt()}%",
                onValueChange = onPerMotorMixChange,
            )
            ControlSlider(
                label = "Roughness",
                value = settings.roughness,
                valueLabel = "${(settings.roughness * 100f).toInt()}%",
                onValueChange = onRoughnessChange,
            )
            SwitchRow(
                label = "Alerts enabled",
                checked = settings.alertsEnabled,
                onCheckedChange = onAlertsEnabledChange,
            )
            SwitchRow(
                label = "Sound test mode",
                checked = settings.testMode,
                onCheckedChange = onTestModeChange,
            )
            Text(
                text = "Audio-only test. Does not affect simulation state.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ControlSlider(
                label = "Test RPM",
                value = settings.testRpm,
                valueLabel = "${settings.testRpm.toInt()} RPM",
                min = 0f,
                max = DroneSoundSettings.MaxReferenceRpm,
                enabled = settings.testMode,
                onValueChange = onTestRpmChange,
            )
            AdvancedAcousticsControls(
                settings = settings,
                expanded = advancedAcousticsExpanded,
                onExpandedChange = { advancedAcousticsExpanded = it },
                onProceduralEnabledChange = onProceduralEnabledChange,
                onSampleBedAmountChange = onSampleBedAmountChange,
                onBladeHarmonicsAmountChange = onBladeHarmonicsAmountChange,
                onPropWashAmountChange = onPropWashAmountChange,
                onMotorWhineAmountChange = onMotorWhineAmountChange,
                onBladeCountChange = onBladeCountChange,
                onAcousticProfileChange = onAcousticProfileChange,
                onSynthQualityChange = onSynthQualityChange,
                onShowAcousticTelemetryChange = onShowAcousticTelemetryChange,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SoundReadout("Avg RPM", debugState.averageRpm.toInt().toString())
                SoundReadout("RPM spread", "${debugState.rpmSpreadPercent.toInt()}%")
                SoundReadout("Sound rate", "%.2fx".format(debugState.averagePlaybackRate))
                SoundReadout("Roughness", "${(debugState.roughness * 100f).toInt()}%")
                SoundReadout("Active motors", "${debugState.activeMotorCount}/4")
                SoundReadout("Alert", debugState.alertLabel)
            }
            if (settings.showAcousticTelemetry) {
                AcousticTelemetryReadouts(debugState)
            }
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset sound")
            }
        }
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancedAcousticsControls(
    settings: DroneSoundSettings,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onProceduralEnabledChange: (Boolean) -> Unit,
    onSampleBedAmountChange: (Float) -> Unit,
    onBladeHarmonicsAmountChange: (Float) -> Unit,
    onPropWashAmountChange: (Float) -> Unit,
    onMotorWhineAmountChange: (Float) -> Unit,
    onBladeCountChange: (Int) -> Unit,
    onAcousticProfileChange: (String) -> Unit,
    onSynthQualityChange: (DroneSynthQuality) -> Unit,
    onShowAcousticTelemetryChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (expanded) "Hide Advanced acoustics" else "Advanced acoustics")
        }
        if (!expanded) return@Column

        Text(
            text = "Blade-pass frequency = blade count x RPM / 60. MAVLab uses this to synthesize rotor harmonics above the sample loop.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SwitchRow(
            label = "Procedural layer",
            checked = settings.proceduralEnabled,
            onCheckedChange = onProceduralEnabledChange,
        )
        ControlSlider(
            label = "Sample bed",
            value = settings.sampleBedAmount,
            valueLabel = "${(settings.sampleBedAmount * 100f).toInt()}%",
            onValueChange = onSampleBedAmountChange,
        )
        ControlSlider(
            label = "Blade harmonics",
            value = settings.bladeHarmonicsAmount,
            valueLabel = "${(settings.bladeHarmonicsAmount * 100f).toInt()}%",
            onValueChange = onBladeHarmonicsAmountChange,
        )
        ControlSlider(
            label = "Prop wash",
            value = settings.propWashAmount,
            valueLabel = "${(settings.propWashAmount * 100f).toInt()}%",
            onValueChange = onPropWashAmountChange,
        )
        ControlSlider(
            label = "Motor whine",
            value = settings.motorWhineAmount,
            valueLabel = "${(settings.motorWhineAmount * 100f).toInt()}%",
            onValueChange = onMotorWhineAmountChange,
        )
        Text("Blade count", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(2, 3, 4).forEach { count ->
                FilterChip(
                    selected = settings.bladeCount == count,
                    onClick = { onBladeCountChange(count) },
                    label = { Text(count.toString()) },
                )
            }
        }
        Text("Acoustic profile", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DroneAcousticProfile.all.forEach { profile ->
                FilterChip(
                    selected = settings.acousticProfileId == profile.id,
                    onClick = { onAcousticProfileChange(profile.id) },
                    label = { Text(profile.label) },
                )
            }
        }
        Text("Synth quality", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DroneSynthQuality.entries.forEach { quality ->
                FilterChip(
                    selected = settings.synthQuality == quality,
                    onClick = { onSynthQualityChange(quality) },
                    label = { Text(quality.label) },
                )
            }
        }
        SwitchRow(
            label = "Show acoustic telemetry",
            checked = settings.showAcousticTelemetry,
            onCheckedChange = onShowAcousticTelemetryChange,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AcousticTelemetryReadouts(debugState: DroneSoundDebugState) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SoundReadout("Blade-pass frequency", "${debugState.averageBladePassHz.toInt()} Hz")
        SoundReadout("BPF M1/M2/M3/M4", debugState.motorBladePassLabel)
        SoundReadout("Harmonics", debugState.harmonicCount.toString())
        SoundReadout("Prop wash", "${debugState.propWashPercent.toInt()}%")
        SoundReadout("Motor whine", "${debugState.motorWhinePercent.toInt()}%")
        SoundReadout("Synth", debugState.synthStatus)
        SoundReadout("Sample rate", "${debugState.sampleRateHz} Hz")
        SoundReadout("Buffer", "${debugState.bufferFrames} frames")
        SoundReadout("Underruns", debugState.underrunCount.toString())
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SoundReadout(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputModeSelector(
    selected: ControllerInputMode,
    sensorAvailable: Boolean,
    onSelected: (ControllerInputMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Input mode", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ControllerInputMode.entries.forEach { mode ->
                FilterChip(
                    selected = selected == mode,
                    onClick = { onSelected(mode) },
                    enabled = mode != ControllerInputMode.PHONE_SENSORS || sensorAvailable,
                    label = { Text(mode.label) },
                )
            }
        }
    }
}

@Composable
private fun PhoneSensorControls(
    pilotInput: PilotInput,
    calibratedOrientation: OrientationData,
    config: ControlConfig,
    throttle: Float,
    manualYaw: Float,
    inputPaused: Boolean,
    onThrottleChange: (Float) -> Unit,
    onYawChange: (Float) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TiltVisualizer(
                rollNormalized = pilotInput.roll,
                pitchNormalized = pilotInput.pitch,
                deadzoneRadius = config.deadzoneRad / config.maxRollAngleRad,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Roll ${degrees(calibratedOrientation.roll)}")
                Text("Pitch ${degrees(calibratedOrientation.pitch)}")
                Text("Yaw ${degrees(calibratedOrientation.yaw)}")
            }
            ControlSlider(
                label = "Throttle",
                value = throttle,
                valueLabel = " ${(throttle * 100).toInt()}%",
                enabled = !inputPaused,
                onValueChange = onThrottleChange,
            )
            ControlSlider(
                label = "Yaw trim",
                value = manualYaw,
                valueLabel = normalizedLabel(manualYaw),
                min = -1f,
                max = 1f,
                enabled = !inputPaused,
                onValueChange = onYawChange,
            )
        }
    }
}

@Composable
private fun CustomInputControls(
    throttle: Float,
    roll: Float,
    pitch: Float,
    yaw: Float,
    inputPaused: Boolean,
    onThrottleChange: (Float) -> Unit,
    onRollChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onYawChange: (Float) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Custom input", style = MaterialTheme.typography.titleMedium)
            ControlSlider("Throttle", throttle, " ${(throttle * 100).toInt()}%", enabled = !inputPaused, onValueChange = onThrottleChange)
            ControlSlider("Roll", roll, normalizedLabel(roll), -1f, 1f, !inputPaused, onRollChange)
            ControlSlider("Pitch", pitch, normalizedLabel(pitch), -1f, 1f, !inputPaused, onPitchChange)
            ControlSlider("Yaw", yaw, normalizedLabel(yaw), -1f, 1f, !inputPaused, onYawChange)
        }
    }
}

@Composable
private fun DirectRpmControls(
    rpm: Float,
    motorTelemetry: List<MotorTelemetry>,
    inputPaused: Boolean,
    onRpmChange: (Float) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Direct Motor RPM Control", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Bypass the autopilot and set the spin rate of all 4 motors directly. Useful for motor bench testing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ControlSlider(
                label = "Target RPM",
                value = rpm,
                valueLabel = "${rpm.toInt()} RPM",
                min = 0f,
                max = 10000f,
                enabled = !inputPaused,
                onValueChange = onRpmChange
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text("Motor Status (Telemetry)", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                motorTelemetry.forEachIndexed { index, motor ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("M${index + 1}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${motor.rpm.toInt()}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (motor.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Text("RPM", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlightModeSelector(
    selected: FlightMode,
    enabled: Boolean,
    onSelected: (FlightMode) -> Unit,
) {
    val modes = listOf(
        FlightMode.STABILIZE,
        FlightMode.ALT_HOLD,
        FlightMode.LOITER,
        FlightMode.RTL,
        FlightMode.LAND,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Flight mode", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            modes.forEach { mode ->
                FilterChip(
                    selected = selected == mode,
                    onClick = { onSelected(mode) },
                    enabled = enabled,
                    label = { Text(mode.displayName) },
                )
            }
        }
    }
}

@Composable
private fun ControlSlider(
    label: String,
    value: Float,
    valueLabel: String,
    min: Float = 0f,
    max: Float = 1f,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(valueLabel, style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            enabled = enabled,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancedTestInputs(
    failures: FailureState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (expanded) "Hide advanced test inputs" else "Advanced test inputs")
        }
        if (!expanded) return@Column

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Motor failure", style = MaterialTheme.typography.bodyLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (0..3).forEach { index ->
                        val failed = failures.motorFailureMask and (1 shl index) != 0
                        FilterChip(
                            selected = failed,
                            onClick = { AppRuntime.setMotorFailed(index, !failed) },
                            label = { Text("Motor ${index + 1}") },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("GPS available", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = failures.gpsEnabled,
                        onCheckedChange = AppRuntime::setGpsEnabled,
                    )
                }
                Text("Wind preset", style = MaterialTheme.typography.bodyLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WindPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = windPresetMatches(failures, preset),
                            onClick = { applyWindPreset(preset) },
                            label = { Text(preset.label) },
                        )
                    }
                }
                OutlinedButton(
                    onClick = AppRuntime::resetFailures,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reset perturbations")
                }
            }
        }
    }
}

private enum class WindPreset(
    val label: String,
    val speedMs: Float,
    val gustsMs: Float,
) {
    NONE("None", 0f, 0f),
    LIGHT("Light", 3f, 0.8f),
    STRONG("Strong", 8f, 2f),
}

private fun applyWindPreset(preset: WindPreset) {
    AppRuntime.setWindSpeedMs(preset.speedMs)
    AppRuntime.setWindDirectionDeg(90f)
    AppRuntime.setWindGustsMs(preset.gustsMs)
}

private fun windPresetMatches(failures: FailureState, preset: WindPreset): Boolean {
    return failures.windSpeedMs == preset.speedMs && failures.windGustsMs == preset.gustsMs
}

private fun degrees(radians: Float): String {
    return "%.1f deg".format(radians * 180f / PI.toFloat())
}

private fun normalizedLabel(value: Float): String = "%+.2f".format(value)
