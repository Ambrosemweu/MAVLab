package com.ascend.mavlab.feature.mission

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
import androidx.compose.material3.LinearProgressIndicator
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
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.mission.MissionItem
import com.ascend.mavlab.simulation.mission.MissionProgress
import com.ascend.mavlab.simulation.mission.MissionUploadStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionScreen(modifier: Modifier = Modifier) {
    val droneState by AppRuntime.state.collectAsState()
    val mission by AppRuntime.missionProgress.collectAsState()
    val uploadStatus by AppRuntime.missionUploadStatus.collectAsState()
    val failures by AppRuntime.failures.collectAsState()

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
            MissionFailureWarningsCard(failures = failures)
            MissionControls()
            MissionMonitoringCard(droneState = droneState, mission = mission)
            WaypointList(droneState = droneState, mission = mission)
            QgcWorkflowCard(uploadStatus = uploadStatus, mission = mission)
        }
    }
}

@Composable
private fun MissionFailureWarningsCard(failures: FailureState) {
    val warnings = missionFailureWarnings(failures)
    if (warnings.isEmpty()) return
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Mission safety warnings", style = MaterialTheme.typography.titleMedium)
            warnings.forEach { warning ->
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
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
@OptIn(ExperimentalLayoutApi::class)
private fun MissionStatusCard(
    droneState: DroneState,
    mission: MissionProgress,
    uploadStatus: MissionUploadStatus,
) {
    val progressPercent = missionProgressPercent(mission)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(missionRunStatus(droneState, mission), style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { progressPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MissionMetric("Progress", "${mission.completedCount}/${mission.items.size.coerceAtLeast(1)} reached")
                MissionMetric("Complete", "$progressPercent%")
                MissionMetric("Mode", droneState.mode.displayName)
                MissionMetric("Authority", droneState.controlAuthority.displayName)
            }
            Text(
                text = uploadStatus.displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = "Objective: ${missionObjectiveLabel(mission)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MissionMonitoringCard(
    droneState: DroneState,
    mission: MissionProgress,
) {
    val distance = missionDistanceToActiveMeters(droneState, mission)
    val targetSpeed = missionTargetSpeedMetersPerSecond(mission)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Mission monitor", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MissionMetric("Next distance", distance?.let { "%.1f m".format(it) } ?: "No target")
                MissionMetric("Ground speed", "%.2f m/s".format(droneState.groundSpeedMS))
                MissionMetric("Target speed", targetSpeed?.let { "%.1f m/s".format(it) } ?: "Default")
                MissionMetric("ETA", missionEtaLabel(distance, droneState, targetSpeed))
                MissionMetric("Vehicle", "N %.1f / E %.1f".format(droneState.northMeters, droneState.eastMeters))
                MissionMetric("Altitude", "%.1f m AGL".format(droneState.altitudeAglMeters))
            }
            Text(
                text = if (mission.loaded) {
                    "Replay/export hook: available from Ops after recording or export is enabled."
                } else {
                    "Upload a QGC mission or load a demo route to populate replay/export context."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
            )
        }
    }
}

@Composable
private fun MissionMetric(
    label: String,
    value: String,
) {
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

@Composable
private fun WaypointList(
    droneState: DroneState,
    mission: MissionProgress,
) {
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
                WaypointRow(item = item, droneState = droneState, mission = mission)
            }
        }
    }
}

@Composable
private fun WaypointRow(
    item: MissionItem,
    droneState: DroneState,
    mission: MissionProgress,
) {
    val status = waypointStatusLabel(item, mission)
    val distance = distanceToItemMeters(droneState, item)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "#${item.sequence + 1} ${item.command.name} - $status",
            style = if (status == "Active") {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = if (status == "Active") {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        Text(
            text = "${waypointDetailLabel(item)} | %.1f m from vehicle".format(distance),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun QgcWorkflowCard(
    uploadStatus: MissionUploadStatus,
    mission: MissionProgress,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("QGroundControl workflow", style = MaterialTheme.typography.titleMedium)
            Text("Upload: ${uploadStatus.displayText.removePrefix("Upload: ")}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (mission.loaded) {
                    "Route visible in-app. AUTO will publish MISSION_CURRENT and reached items while flying."
                } else {
                    "Waiting for MISSION_COUNT from QGC or a demo route."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Reconnect note: uploaded missions persist locally until Clear is pressed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
            )
        }
    }
}
