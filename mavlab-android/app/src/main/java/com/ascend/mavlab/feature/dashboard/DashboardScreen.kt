package com.ascend.mavlab.feature.dashboard

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ascend.mavlab.core.common.AppRuntime
import com.ascend.mavlab.core.mavlink.MavlinkIdentityStatus
import com.ascend.mavlab.core.ui.components.ChartSeries
import com.ascend.mavlab.core.ui.components.RollingChart
import com.ascend.mavlab.core.ui.components.TelemetryCard
import com.ascend.mavlab.feature.drone3d.AltitudeInstrument
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.FlightMode
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.mission.MissionProgress
import com.ascend.mavlab.simulation.mission.MissionUploadStatus
import kotlinx.coroutines.delay
import kotlin.math.PI

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val state by AppRuntime.state.collectAsState()
    val status by AppRuntime.status.collectAsState()
    val systemId by AppRuntime.systemId.collectAsState()
    val mission by AppRuntime.missionProgress.collectAsState()
    val uploadStatus by AppRuntime.missionUploadStatus.collectAsState()
    val recording by AppRuntime.recordingStatus.collectAsState()
    val identityStatus by AppRuntime.mavlinkIdentityStatus.collectAsState()
    val failures by AppRuntime.failures.collectAsState()
    val sensorSource by AppRuntime.phoneSensorSource.collectAsState()
    val sensorOrientation by AppRuntime.phoneSensorOrientation.collectAsState()
    val usePhoneAttitude = shouldUsePhoneAttitudeForCockpit(sensorSource, state, mission)
    val rollHistory = remember { mutableStateListOf<Float>() }
    val pitchHistory = remember { mutableStateListOf<Float>() }
    val yawHistory = remember { mutableStateListOf<Float>() }

    LaunchedEffect(Unit) {
        while (true) {
            val sample = AppRuntime.state.value
            rollHistory.push(degreesValue(sample.rollRadians))
            pitchHistory.push(degreesValue(sample.pitchRadians))
            yawHistory.push(degreesValue(sample.yawRadians))
            delay(100)
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Flight Cockpit", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Live operations for the phone-based drone digital twin and training platform.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Text(
                text = "MAVLink: $status | Vehicle System ID $systemId",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            CockpitStatusStrip(
                state = state,
                mavlinkLabel = mavlinkStatusLabel(status, identityStatus),
                failures = failures,
            )
            PrimaryInstruments(
                state = state,
                mission = mission,
                yawRadians = if (usePhoneAttitude) sensorOrientation.yaw else state.yawRadians,
                rollRadians = if (usePhoneAttitude) sensorOrientation.roll else state.rollRadians,
                pitchRadians = if (usePhoneAttitude) sensorOrientation.pitch else state.pitchRadians,
            )
            MissionConnectionStrip(
                mission = mission,
                uploadStatus = uploadStatus,
                identityStatus = identityStatus,
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2,
            ) {
                TelemetryCard("Mode", state.mode.displayName, Modifier.weight(1f), accent = true)
                TelemetryCard("Control", state.controlAuthority.displayName, Modifier.weight(1f), accent = true)
                TelemetryCard("Recording", if (recording.active) "Active" else "Idle", Modifier.weight(1f), accent = recording.active)
                TelemetryCard("Failures", failureStatusLabel(failures), Modifier.weight(1f), accent = failures.activeCount > 0)
                TelemetryCard("Armed", if (state.armed) "Yes" else "No", Modifier.weight(1f))
                TelemetryCard("Ground speed", "%.2f m/s".format(state.groundSpeedMS), Modifier.weight(1f))
                TelemetryCard("Vertical speed", "%.2f m/s".format(state.verticalSpeedMS), Modifier.weight(1f))
                TelemetryCard("Heading", "${state.headingDegrees} deg", Modifier.weight(1f))
                TelemetryCard("Battery", "${state.batteryRemainingPercent}%", Modifier.weight(1f), accent = true)
                TelemetryCard("Voltage", "%.2f V".format(state.batteryVoltageMv.toInt() / 1000f), Modifier.weight(1f))
                TelemetryCard("GPS", gpsLabel(state), Modifier.weight(1f))
                TelemetryCard("Satellites", state.gpsSatellites.toString(), Modifier.weight(1f))
                TelemetryCard("Roll", degrees(state.rollRadians), Modifier.weight(1f))
                TelemetryCard("Pitch", degrees(state.pitchRadians), Modifier.weight(1f))
                TelemetryCard("Yaw", degrees(state.yawRadians), Modifier.weight(1f))
                TelemetryCard("Throttle", "${state.throttlePercent}%", Modifier.weight(1f))
                TelemetryCard("Home distance", distanceFromHomeLabel(state), Modifier.weight(1f))
                TelemetryCard("Mission", missionProgressLabel(mission), Modifier.weight(1f))
                TelemetryCard("RPM", rpmSummary(state), Modifier.weight(1f), accent = true)
            }

            RollingChart(
                title = "Attitude: roll, pitch, yaw",
                series = listOf(
                    ChartSeries("Roll", rollHistory.toList(), Color(0xFF66D9C7)),
                    ChartSeries("Pitch", pitchHistory.toList(), Color(0xFF9DB7FF)),
                    ChartSeries("Yaw", yawHistory.toList(), Color(0xFFF1C27D)),
                ),
            )

            HorizontalDivider()
            FlightControls(state)
            StatusRow("GPS", "%.6f, %.6f".format(state.latitudeDeg, state.longitudeDeg))
            StatusRow("Recording", recording.displayText)
            StatusRow("Last inbound", state.lastInboundMessage)
            StatusRow("Last ACK", state.lastAck)
        }
    }
}

@Composable
private fun CockpitStatusStrip(
    state: DroneState,
    mavlinkLabel: String,
    failures: FailureState,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusPill(
            label = "Authority",
            value = state.controlAuthority.displayName,
            color = MaterialTheme.colorScheme.primary,
        )
        StatusPill(
            label = "Mode",
            value = state.mode.displayName,
            color = MaterialTheme.colorScheme.tertiary,
        )
        StatusPill(
            label = "Armed",
            value = armedLabel(state),
            color = if (state.armed) WarningAmber else SafeGreen,
        )
        StatusPill(
            label = "Battery",
            value = batteryLabel(state),
            color = batteryColor(state),
        )
        StatusPill(
            label = "GPS",
            value = gpsStatusLabel(state),
            color = if (state.gpsFixType.toInt() >= 3) SafeGreen else WarningAmber,
        )
        StatusPill(
            label = "MAVLink",
            value = mavlinkLabel,
            color = if (mavlinkLabel == "QGC connected") SafeGreen else MaterialTheme.colorScheme.outline,
        )
        StatusPill(
            label = "Failures",
            value = failureStatusLabel(failures),
            color = if (failures.activeCount > 0) CriticalRed else SafeGreen,
        )
    }
}

@Composable
private fun StatusPill(
    label: String,
    value: String,
    color: Color,
) {
    Surface(
        color = color.copy(alpha = 0.16f),
        contentColor = color,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PrimaryInstruments(
    state: DroneState,
    mission: MissionProgress,
    yawRadians: Float,
    rollRadians: Float,
    pitchRadians: Float,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CockpitAltitudeInstrument(
            state = state,
            yawRadians = yawRadians,
            rollRadians = rollRadians,
            pitchRadians = pitchRadians,
            modifier = Modifier.weight(1.2f),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SpeedBatteryPanel(state)
            MissionFocusPanel(mission)
        }
    }
}

@Composable
private fun CockpitAltitudeInstrument(
    state: DroneState,
    yawRadians: Float,
    rollRadians: Float,
    pitchRadians: Float,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            AltitudeInstrument(
                altitudeMeters = state.altitudeAglMeters,
                verticalSpeedMetersPerSecond = state.verticalSpeedMS,
                yawRadians = yawRadians,
                rollRadians = rollRadians,
                pitchRadians = pitchRadians,
                armed = state.armed,
            )
        }
    }
}

@Composable
private fun SpeedBatteryPanel(state: DroneState) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Ground speed", style = MaterialTheme.typography.labelMedium)
                Text("%.2f m/s".format(state.groundSpeedMS), style = MaterialTheme.typography.titleMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Home", style = MaterialTheme.typography.labelMedium)
                Text(distanceFromHomeLabel(state), style = MaterialTheme.typography.titleMedium)
            }
            LinearProgressIndicator(
                progress = { state.batteryRemainingPercent.toInt().coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = batteryColor(state),
            )
            Text(batteryLabel(state), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun MissionFocusPanel(mission: MissionProgress) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Current objective", style = MaterialTheme.typography.labelMedium)
            Text(
                missionFocusLabel(mission),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(missionProgressLabel(mission), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun MissionConnectionStrip(
    mission: MissionProgress,
    uploadStatus: MissionUploadStatus,
    identityStatus: MavlinkIdentityStatus,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Mission and connection", style = MaterialTheme.typography.titleSmall)
            StatusRow("Mission", "${missionFocusLabel(mission)} | ${missionProgressLabel(mission)}")
            StatusRow("Upload", uploadStatus.displayText)
            StatusRow(
                "QGC",
                if (identityStatus.gcsConnected) {
                    "Connected SYSID ${identityStatus.lastGcsSystemId ?: "unknown"}"
                } else {
                    "Waiting for heartbeat"
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlightControls(state: DroneState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Flight controls", style = MaterialTheme.typography.titleMedium)
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
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                FlightMode.STABILIZE,
                FlightMode.ALT_HOLD,
                FlightMode.LOITER,
                FlightMode.RTL,
                FlightMode.LAND,
            ).forEach { mode ->
                FilterChip(
                    selected = state.mode == mode,
                    onClick = { AppRuntime.setMode(mode) },
                    label = { Text(mode.displayName) },
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun MutableList<Float>.push(value: Float, limit: Int = 120) {
    add(value)
    while (size > limit) removeAt(0)
}

private fun gpsLabel(state: DroneState): String {
    return if (state.gpsFixType.toInt() >= 3) "3D fix" else "No fix"
}

private fun degrees(radians: Float): String = "%.1f deg".format(degreesValue(radians))

private fun degreesValue(radians: Float): Float = radians * 180f / PI.toFloat()

private fun rpmSummary(state: DroneState): String {
    return state.motors.joinToString(" / ") { "%.0f".format(it.rpm) }
}

private fun failureStatusLabel(failures: FailureState): String {
    return if (failures.activeCount == 0) "Clear" else "${failures.activeCount} active"
}

@Composable
private fun batteryColor(state: DroneState): Color {
    val percent = state.batteryRemainingPercent.toInt()
    return when {
        percent <= 15 -> CriticalRed
        percent <= 30 -> WarningAmber
        else -> SafeGreen
    }
}

private val SafeGreen = Color(0xFF2EAD66)
private val WarningAmber = Color(0xFFE6A100)
private val CriticalRed = Color(0xFFD64A4A)
