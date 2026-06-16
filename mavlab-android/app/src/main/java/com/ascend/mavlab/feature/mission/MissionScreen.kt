package com.ascend.mavlab.feature.mission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ascend.mavlab.core.common.AppRuntime
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.mission.MissionItem
import com.ascend.mavlab.simulation.mission.MissionProgress
import com.ascend.mavlab.simulation.mission.MissionUploadStatus

@Composable
fun MissionScreen(modifier: Modifier = Modifier) {
    val droneState by AppRuntime.state.collectAsState()
    val mission by AppRuntime.missionProgress.collectAsState()
    val uploadStatus by AppRuntime.missionUploadStatus.collectAsState()

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Mission", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Autonomous route execution for QGroundControl uploads, demo missions, and waypoint progress.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )

            MissionStatusCard(droneState = droneState, mission = mission, uploadStatus = uploadStatus)
            MissionControls()
            WaypointList(mission = mission)
        }
    }
}

@Composable
private fun MissionControls() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = AppRuntime::loadDemoMission,
            modifier = Modifier.weight(1f),
        ) {
            Text("Load route")
        }
        OutlinedButton(
            onClick = AppRuntime::startAutoMission,
            modifier = Modifier.weight(1f),
        ) {
            Text("Start Auto")
        }
        OutlinedButton(
            onClick = AppRuntime::clearMission,
            modifier = Modifier.weight(1f),
        ) {
            Text("Clear")
        }
    }
}

@Composable
private fun MissionStatusCard(
    droneState: DroneState,
    mission: MissionProgress,
    uploadStatus: MissionUploadStatus,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (mission.loaded) {
                    "Mission loaded"
                } else {
                    "No mission loaded"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (mission.loaded) {
                    "Progress: ${mission.completedCount}/${mission.items.size}"
                } else {
                    "Progress: none"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("Mode: ${droneState.mode.displayName}", style = MaterialTheme.typography.bodyMedium)
            Text("Control: ${droneState.controlAuthority.displayName}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = uploadStatus.displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = mission.activeTarget?.let {
                    "Active target: waypoint ${it.sequence + 1}, %.1f m AGL".format(it.altitudeAglMeters)
                } ?: "Active target: none",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Vehicle: N %.1f m, E %.1f m, Alt %.1f m".format(
                    droneState.northMeters,
                    droneState.eastMeters,
                    droneState.altitudeAglMeters,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
            )
        }
    }
}

@Composable
private fun WaypointList(mission: MissionProgress) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Waypoints", style = MaterialTheme.typography.titleMedium)
            if (!mission.loaded) {
                Text(
                    text = "Load a demo mission or upload one from QGroundControl.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                return@Column
            }

            mission.items.forEach { item ->
                WaypointRow(item = item, mission = mission)
            }
        }
    }
}

@Composable
private fun WaypointRow(
    item: MissionItem,
    mission: MissionProgress,
) {
    val status = when {
        mission.complete || item.sequence < mission.currentIndex -> "Reached"
        item.sequence == mission.currentIndex -> "Active"
        else -> "Queued"
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "#${item.sequence + 1} ${item.command.name} - $status",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "%.6f, %.6f | %.1f m AGL".format(
                item.latitudeDeg,
                item.longitudeDeg,
                item.altitudeAglMeters,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
    }
}
