package com.ascend.mavlab.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ascend.mavlab.core.common.AppRuntime

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val state by AppRuntime.state.collectAsState()
    val status by AppRuntime.status.collectAsState()
    val systemId by AppRuntime.systemId.collectAsState()

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "MAVLab Protocol",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            HorizontalDivider()
            StatusRow("System ID", systemId.toString())
            StatusRow("Mode", state.mode.displayName)
            StatusRow("Armed", if (state.armed) "Yes" else "No")
            StatusRow("GPS", "${state.latitudeDeg}, ${state.longitudeDeg}")
            StatusRow("Altitude AGL", "%.1f m".format(state.altitudeAglMeters))
            StatusRow("Roll", "%.2f rad".format(state.rollRadians))
            StatusRow("Pitch", "%.2f rad".format(state.pitchRadians))
            StatusRow("Yaw", "%.2f rad".format(state.yawRadians))
            StatusRow("Battery", "${state.batteryRemainingPercent}%")
            HorizontalDivider()
            StatusRow("Last inbound", state.lastInboundMessage)
            StatusRow("Last ACK", state.lastAck)
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
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
