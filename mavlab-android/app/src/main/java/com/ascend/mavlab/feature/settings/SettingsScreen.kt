package com.ascend.mavlab.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ascend.mavlab.core.common.AppRuntime
import com.ascend.mavlab.simulation.recording.FlightRecordingStatus
import com.ascend.mavlab.simulation.recording.FlightSession
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val state by AppRuntime.state.collectAsState()
    val mission by AppRuntime.missionProgress.collectAsState()
    val uploadStatus by AppRuntime.missionUploadStatus.collectAsState()
    val recording by AppRuntime.recordingStatus.collectAsState()
    val status by AppRuntime.status.collectAsState()
    val systemId by AppRuntime.systemId.collectAsState()
    val identityStatus by AppRuntime.mavlinkIdentityStatus.collectAsState()

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Ops", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "MAVLink, QGC, recording, and runtime diagnostics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            InfoCard(
                title = "MAVLink",
                body = buildString {
                    append("Status: $status")
                    append("\nVehicle SYSID: $systemId")
                    append("\nVehicle COMPID: ${identityStatus.vehicleComponentId}")
                    append("\nExpected QGC SYSID: ${identityStatus.recommendedGcsSystemId}")
                    append("\nLast GCS SYSID: ${identityStatus.lastGcsSystemId ?: "none"}")
                    append("\nLast GCS COMPID: ${identityStatus.lastGcsComponentId ?: "none"}")
                    append("\nIdentity: ${identityStatus.healthLabel}")
                    if (identityStatus.message.isNotBlank()) {
                        append("\n${identityStatus.message}")
                    }
                },
            )
            InfoCard(
                title = "GCS diagnostics",
                body = "Last inbound: ${state.lastInboundMessage}\nLast ACK: ${state.lastAck}\n${uploadStatus.displayText}\nMission: ${mission.items.size} items, active ${mission.activeTarget?.sequence?.plus(1) ?: "none"}",
            )
            InfoCard(
                title = "Flight logs",
                body = flightLogBody(recording),
            )
            InfoCard(
                title = "Troubleshooting",
                body = "Recommended QGC setup: set QGC MAVLink System ID to 255, keep MAVLab Vehicle SYSID at 1, keep QGC UDP on 14550, and restart QGC or reconnect the UDP link after changing System ID. Do not set QGC to the same system ID as MAVLab.",
            )
            InfoCard(
                title = "Release QA",
                body = "Run onboarding, QGC discovery, mission upload, demo mission, GPS loss, and QGC split-screen before tagging a release.",
            )
        }
    }
}

private fun flightLogBody(recording: FlightRecordingStatus): String {
    val session = recording.currentSession ?: recording.lastSession
    return if (session == null) {
        "No local flight logs yet.\nPath: app-private files/mavlab/flights"
    } else {
        buildString {
            append(if (recording.active) "Status: Active" else "Status: Last session")
            append("\nSession ID: ${session.id}")
            append("\nStarted: ${formatTimestamp(session.startedAtMs)}")
            append("\nEnded: ${endedLabel(session)}")
            append("\nPath: ${session.directoryPath}")
        }
    }
}

private fun endedLabel(session: FlightSession): String {
    return session.endedAtMs?.let(::formatTimestamp) ?: "Active"
}

private fun formatTimestamp(timestampMs: Long): String {
    return OpsTimeFormatter.format(Instant.ofEpochMilli(timestampMs))
}

@Composable
private fun InfoCard(title: String, body: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
            )
        }
    }
}

private val OpsTimeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
    .withZone(ZoneOffset.UTC)
