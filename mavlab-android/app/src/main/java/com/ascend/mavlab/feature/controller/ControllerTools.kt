package com.ascend.mavlab.feature.controller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ascend.mavlab.core.common.AppRuntime
import com.ascend.mavlab.simulation.audio.DroneAcousticProfile
import com.ascend.mavlab.simulation.audio.DroneSoundDebugState
import com.ascend.mavlab.simulation.audio.DroneSoundSettings
import com.ascend.mavlab.simulation.audio.DroneSynthQuality
import com.ascend.mavlab.simulation.engine.FlightMode
import com.ascend.mavlab.simulation.failures.FailureState

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DroneSoundLabCard(
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
internal fun FlightModeSelector(
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
internal fun ControlSlider(
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
internal fun AdvancedTestInputs(
    failures: FailureState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (expanded) "Hide advanced test inputs" else "Advanced test inputs")
        }
        if (!expanded) return@Column

        if (!enabled) Text("Test inputs unavailable while simulation state is not live.", style = MaterialTheme.typography.bodySmall)

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
                            enabled = enabled,
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
                        enabled = enabled,
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
                            enabled = enabled,
                            label = { Text(preset.label) },
                        )
                    }
                }
                OutlinedButton(
                    onClick = AppRuntime::resetFailures,
                    enabled = enabled,
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
