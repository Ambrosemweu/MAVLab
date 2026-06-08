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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ascend.mavlab.core.common.AppRuntime
import com.ascend.mavlab.core.ui.components.ChartSeries
import com.ascend.mavlab.core.ui.components.RollingChart
import com.ascend.mavlab.core.ui.components.TelemetryCard
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.FlightMode
import kotlinx.coroutines.delay
import kotlin.math.PI

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val state by AppRuntime.state.collectAsState()
    val status by AppRuntime.status.collectAsState()
    val systemId by AppRuntime.systemId.collectAsState()
    val rollHistory = remember { mutableStateListOf<Float>() }
    val pitchHistory = remember { mutableStateListOf<Float>() }
    val yawHistory = remember { mutableStateListOf<Float>() }
    val altitudeHistory = remember { mutableStateListOf<Float>() }

    LaunchedEffect(Unit) {
        while (true) {
            val sample = AppRuntime.state.value
            rollHistory.push(degreesValue(sample.rollRadians))
            pitchHistory.push(degreesValue(sample.pitchRadians))
            yawHistory.push(degreesValue(sample.yawRadians))
            altitudeHistory.push(sample.altitudeAglMeters)
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
                text = "$status | System ID $systemId",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2,
            ) {
                TelemetryCard("Mode", state.mode.displayName, Modifier.weight(1f), accent = true)
                TelemetryCard("Armed", if (state.armed) "Yes" else "No", Modifier.weight(1f))
                TelemetryCard("Altitude", "%.1f m".format(state.altitudeAglMeters), Modifier.weight(1f), accent = true)
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
            }

            RollingChart(
                title = "Attitude: roll, pitch, yaw",
                series = listOf(
                    ChartSeries("Roll", rollHistory.toList(), Color(0xFF66D9C7)),
                    ChartSeries("Pitch", pitchHistory.toList(), Color(0xFF9DB7FF)),
                    ChartSeries("Yaw", yawHistory.toList(), Color(0xFFF1C27D)),
                ),
            )
            RollingChart(
                title = "Altitude AGL",
                series = listOf(
                    ChartSeries("Altitude", altitudeHistory.toList(), Color(0xFF66D9C7)),
                ),
            )

            HorizontalDivider()
            FlightControls(state)
            StatusRow("GPS", "%.6f, %.6f".format(state.latitudeDeg, state.longitudeDeg))
            StatusRow("Last inbound", state.lastInboundMessage)
            StatusRow("Last ACK", state.lastAck)
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
