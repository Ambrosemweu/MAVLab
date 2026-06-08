package com.ascend.mavlab.feature.controller

import android.content.Context
import android.hardware.SensorManager
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ascend.mavlab.core.common.AppRuntime
import com.ascend.mavlab.core.sensors.OrientationData
import com.ascend.mavlab.core.sensors.OrientationSource
import com.ascend.mavlab.core.sensors.PhoneSensorRepository
import com.ascend.mavlab.core.sensors.SensorCalibration
import com.ascend.mavlab.simulation.autopilot.PilotInput
import com.ascend.mavlab.simulation.engine.FlightMode
import kotlin.math.PI

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControllerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state by AppRuntime.state.collectAsState()
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val repository = remember(sensorManager) { PhoneSensorRepository(sensorManager) }
    val source = remember(repository) { repository.activeSource() }
    val calibration = remember { SensorCalibration() }
    val mapper = remember { ControlMapper() }
    val config = remember { ControlConfig() }

    var tiltEnabled by remember { mutableStateOf(source != OrientationSource.Unavailable) }
    var rawOrientation by remember { mutableStateOf(OrientationData(source = source)) }
    var calibratedOrientation by remember { mutableStateOf(OrientationData(source = source)) }
    var pilotInput by remember { mutableStateOf(PilotInput(throttle = 0.5f)) }
    var throttle by remember { mutableFloatStateOf(0.5f) }
    var manualRoll by remember { mutableFloatStateOf(0f) }
    var manualPitch by remember { mutableFloatStateOf(0f) }
    var manualYaw by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(tiltEnabled, throttle, manualYaw, source) {
        if (!tiltEnabled || source == OrientationSource.Unavailable) return@LaunchedEffect
        repository.orientationFlow().collect { raw ->
            rawOrientation = raw
            val calibrated = calibration.apply(raw)
            calibratedOrientation = calibrated
            pilotInput = mapper.map(calibrated, throttle, manualYaw)
            AppRuntime.setPilotInput(pilotInput)
        }
    }

    LaunchedEffect(tiltEnabled, throttle, manualRoll, manualPitch, manualYaw) {
        if (tiltEnabled && source != OrientationSource.Unavailable) return@LaunchedEffect
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
                text = "Sensor: ${source.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Tilt input", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = tiltEnabled,
                    onCheckedChange = { tiltEnabled = it && source != OrientationSource.Unavailable },
                    enabled = source != OrientationSource.Unavailable,
                )
            }

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
                }
            }

            ControlSlider("Throttle", throttle, " ${(throttle * 100).toInt()}%") {
                throttle = it
                AppRuntime.setPilotInput(pilotInput.copy(throttle = it))
            }

            if (!tiltEnabled || source == OrientationSource.Unavailable) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Manual fallback", style = MaterialTheme.typography.titleMedium)
                        ControlSlider("Roll", manualRoll, normalizedLabel(manualRoll), -1f, 1f) { manualRoll = it }
                        ControlSlider("Pitch", manualPitch, normalizedLabel(manualPitch), -1f, 1f) { manualPitch = it }
                        ControlSlider("Yaw", manualYaw, normalizedLabel(manualYaw), -1f, 1f) { manualYaw = it }
                    }
                }
            }

            HorizontalDivider()
            FlightModeSelector(selected = state.mode, onSelected = AppRuntime::setMode)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { AppRuntime.setArmed(!state.armed) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.armed) "Disarm" else "Arm")
                }
                OutlinedButton(
                    onClick = { AppRuntime.takeoff(10f) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Takeoff")
                }
                OutlinedButton(
                    onClick = AppRuntime::land,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Land")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OutlinedButton(onClick = { calibration.calibrate(rawOrientation) }) {
                    Text("Calibrate")
                }
                Text(
                    text = "${state.mode.displayName} | ${if (state.armed) "Armed" else "Disarmed"} | ${"%.1f".format(state.altitudeAglMeters)} m",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlightModeSelector(
    selected: FlightMode,
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
        )
    }
}

private fun degrees(radians: Float): String {
    return "%.1f deg".format(radians * 180f / PI.toFloat())
}

private fun normalizedLabel(value: Float): String = "%+.2f".format(value)
