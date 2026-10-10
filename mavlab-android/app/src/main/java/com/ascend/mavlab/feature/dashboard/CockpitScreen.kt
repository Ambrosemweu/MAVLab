package com.ascend.mavlab.feature.dashboard

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.mavlab.core.common.AppRuntime
import com.ascend.mavlab.core.mavlink.MavlinkIdentityStatus
import com.ascend.mavlab.core.ui.components.ChartSeries
import com.ascend.mavlab.core.ui.components.ChartLineStyle
import com.ascend.mavlab.core.ui.components.ChartAxis
import com.ascend.mavlab.core.ui.components.ChartAxisSpec
import com.ascend.mavlab.core.ui.components.RollingChart
import com.ascend.mavlab.core.ui.components.RollingChartSampleCapacity
import com.ascend.mavlab.core.ui.components.RollingChartSampleIntervalMs
import com.ascend.mavlab.feature.drone3d.AltitudeInstrument
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.mission.MissionProgress
import com.ascend.mavlab.simulation.mission.MissionUploadStatus
import com.ascend.mavlab.simulation.recording.FlightRecordingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI

@Composable
fun CockpitScreen(
    modifier: Modifier = Modifier,
    onOpenController: () -> Unit = {},
    onOpenSim: () -> Unit = {},
    onOpenMission: () -> Unit = {},
    onOpenOps: () -> Unit = {},
) {
    val state by AppRuntime.state.collectAsStateWithLifecycle()
    val mission by AppRuntime.missionProgress.collectAsStateWithLifecycle()
    val uploadStatus by AppRuntime.missionUploadStatus.collectAsStateWithLifecycle()
    val recording by AppRuntime.recordingStatus.collectAsStateWithLifecycle()
    val identityStatus by AppRuntime.mavlinkIdentityStatus.collectAsStateWithLifecycle()
    val failures by AppRuntime.failures.collectAsStateWithLifecycle()
    val controllerInput by AppRuntime.controllerInputState.collectAsStateWithLifecycle()
    val phoneOrientation by AppRuntime.phoneSensorOrientation.collectAsStateWithLifecycle()
    val phoneSensorSource by AppRuntime.phoneSensorSource.collectAsStateWithLifecycle()
    val runtimeLifecycle by AppRuntime.runtimeLifecycle.collectAsStateWithLifecycle()
    val stateUpdatedAt by AppRuntime.stateUpdatedAtElapsedRealtimeMs.collectAsStateWithLifecycle()
    val mavlinkStatus by AppRuntime.status.collectAsStateWithLifecycle()
    val systemId by AppRuntime.systemId.collectAsStateWithLifecycle()

    var nowElapsedMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var nowEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var telemetryExpanded by remember { mutableStateOf(false) }
    var confirmTakeoverAndLand by remember { mutableStateOf(false) }
    val rollHistory = remember { mutableStateListOf<Float>() }
    val pitchHistory = remember { mutableStateListOf<Float>() }
    val yawHistory = remember { mutableStateListOf<Float>() }
    val altitudeHistory = remember { mutableStateListOf<Float>() }

    LaunchedEffect(Unit) {
        while (isActive) {
            nowElapsedMs = SystemClock.elapsedRealtime()
            nowEpochMs = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    LaunchedEffect(telemetryExpanded) {
        if (!telemetryExpanded) return@LaunchedEffect
        // Each expansion starts a continuous window; do not join old traces
        // across a period when the dropdown was closed and not sampling.
        rollHistory.clear()
        pitchHistory.clear()
        yawHistory.clear()
        altitudeHistory.clear()
        while (isActive) {
            val sample = AppRuntime.state.value
            rollHistory.push(degreesValue(sample.rollRadians))
            pitchHistory.push(degreesValue(sample.pitchRadians))
            yawHistory.push(degreesValue(sample.yawRadians))
            altitudeHistory.push(sample.altitudeAglMeters)
            delay(RollingChartSampleIntervalMs)
        }
    }

    val uiState = deriveCockpitUiState(
        state = state,
        mission = mission,
        uploadStatus = uploadStatus,
        recording = recording,
        failures = failures,
        identity = identityStatus,
        runtimeLifecycle = runtimeLifecycle,
        stateUpdatedAtElapsedRealtimeMs = stateUpdatedAt,
        nowElapsedRealtimeMs = nowElapsedMs,
        nowEpochMs = nowEpochMs,
        controllerInputMode = controllerInput.inputMode,
    )

    val executeAction: (CockpitAction) -> Unit = { action ->
        when (action) {
            CockpitAction.ARM -> AppRuntime.setArmed(true)
            CockpitAction.DISARM -> AppRuntime.setArmed(false)
            CockpitAction.TAKEOFF -> AppRuntime.takeoff(DefaultTakeoffAltitudeMeters)
            CockpitAction.LAND -> AppRuntime.land()
            CockpitAction.TAKE_CONTROL_AND_LAND -> confirmTakeoverAndLand = true
            CockpitAction.OPEN_CONTROLLER,
            CockpitAction.NEW_FLIGHT,
            -> onOpenController()
            CockpitAction.OPEN_MISSION -> onOpenMission()
            CockpitAction.OPEN_SIM -> onOpenSim()
            CockpitAction.OPEN_OPS,
            CockpitAction.REVIEW_FLIGHT,
            -> onOpenOps()
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                CockpitHeader(uiState = uiState, recording = recording)
            }
            item {
                CockpitTruthStrip(uiState)
            }
            uiState.alert?.let { alert ->
                item { CockpitAlertCard(alert) }
            }
            item {
                CockpitHero(
                    state = state,
                    uiState = uiState,
                    attitude = resolveCockpitInstrumentAttitude(
                        state, phoneOrientation, uiState.freshness, phoneSensorSource,
                    ),
                )
            }
            item {
                CockpitVitalRail(state = state)
            }
            item {
                CockpitTaskCard(uiState.task, uiState.taskActions, executeAction)
            }
            item {
                CockpitSystemLine(uiState)
            }
            item {
                CockpitActionDock(
                    primary = uiState.primaryAction,
                    secondary = uiState.secondaryAction,
                    alert = uiState.alert,
                    onAction = executeAction,
                )
            }
            item {
                OutlinedButton(
                    onClick = { telemetryExpanded = !telemetryExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (telemetryExpanded) "Hide telemetry & details" else "Telemetry & details")
                }
            }
            if (telemetryExpanded) {
                item {
                    RollingChart(
                        title = "Flight telemetry",
                        series = listOf(
                            ChartSeries("Altitude AGL", altitudeHistory.toList(), CockpitAmber, ChartAxis.LEFT),
                            ChartSeries("Roll", rollHistory.toList(), CockpitTeal, ChartAxis.RIGHT),
                            ChartSeries("Pitch", pitchHistory.toList(), CockpitBlue, ChartAxis.RIGHT, ChartLineStyle.DASHED),
                            ChartSeries("Yaw", yawHistory.toList(), CockpitPurple, ChartAxis.RIGHT, ChartLineStyle.DOTTED),
                        ),
                        leftAxis = ChartAxisSpec("Altitude", "m", false, CockpitAmber),
                        rightAxis = ChartAxisSpec("Attitude", "°", true, CockpitText),
                    )
                }
                item {
                    CockpitDetailCard(
                        state = state,
                        mission = mission,
                        uploadStatus = uploadStatus,
                        recording = recording,
                        identityStatus = identityStatus,
                        failures = failures,
                        mavlinkStatus = mavlinkStatus,
                        systemId = systemId,
                    )
                }
            }
        }
    }

    if (confirmTakeoverAndLand) {
        AlertDialog(
            onDismissRequest = { confirmTakeoverAndLand = false },
            title = { Text("Take local control and land?") },
            text = {
                Text(
                    "This ends external control authority, switches MAVLab to local control, and commands LAND.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmTakeoverAndLand = false
                        AppRuntime.land()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Take control & land")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmTakeoverAndLand = false }) {
                    Text("Keep external control")
                }
            },
        )
    }
}

@Composable
private fun CockpitHeader(
    uiState: CockpitUiState,
    recording: FlightRecordingStatus,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = uiState.eyebrow,
                style = MaterialTheme.typography.labelSmall,
                color = cockpitStateColor(uiState),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Cockpit",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (recording.active) {
            Surface(
                color = CockpitRed.copy(alpha = 0.16f),
                contentColor = CockpitRed,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = "● REC",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun CockpitTruthStrip(uiState: CockpitUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TruthCell("CONTROL", uiState.authorityLabel, Modifier.weight(1f), CockpitTeal)
        TruthCell("MODE", uiState.modeLabel, Modifier.weight(1f), CockpitBlue)
        TruthCell("VEHICLE", uiState.vehicleLabel, Modifier.weight(1f), cockpitStateColor(uiState))
    }
}

@Composable
private fun TruthCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CockpitAlertCard(alert: CockpitAlert) {
    val color = when (alert.severity) {
        CockpitAlertSeverity.ADVISORY -> CockpitBlue
        CockpitAlertSeverity.CAUTION -> CockpitAmber
        CockpitAlertSeverity.CRITICAL -> CockpitRed
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.42f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = when (alert.severity) {
                    CockpitAlertSeverity.ADVISORY -> "i"
                    CockpitAlertSeverity.CAUTION -> "!"
                    CockpitAlertSeverity.CRITICAL -> "×"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(alert.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    alert.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                )
            }
        }
    }
}

@Composable
private fun CockpitHero(
    state: DroneState,
    uiState: CockpitUiState,
    attitude: CockpitInstrumentAttitude,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AltitudeInstrument(
                    altitudeMeters = state.altitudeAglMeters,
                    verticalSpeedMetersPerSecond = state.verticalSpeedMS,
                    yawRadians = attitude.yawRadians,
                    rollRadians = attitude.rollRadians,
                    pitchRadians = attitude.pitchRadians,
                    armed = state.armed,
                )
                Text(
                    if (attitude.phonePreview) "PHONE ATTITUDE" else "SIM ATTITUDE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeroValue("ALTITUDE AGL", "%.1f m".format(state.altitudeAglMeters), cockpitStateColor(uiState))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HeroValue(
                        "GROUND SPEED",
                        "%.1f m/s".format(state.groundSpeedMS),
                        CockpitText,
                        Modifier.weight(1f),
                        compact = true,
                    )
                    HeroValue(
                        "VERTICAL",
                        "%+.1f m/s".format(state.verticalSpeedMS),
                        CockpitTeal,
                        Modifier.weight(1f),
                        compact = true,
                    )
                }
                LinearProgressIndicator(
                    progress = { state.batteryRemainingPercent.toInt().coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = batteryColor(state),
                    trackColor = MaterialTheme.colorScheme.surface,
                )
                Text(
                    text = "BATTERY  ${state.batteryRemainingPercent.toInt()}% · " +
                        "%.1f V".format(state.batteryVoltageMv.toInt() / 1_000f),
                    style = MaterialTheme.typography.labelSmall,
                    color = batteryColor(state),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun HeroValue(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CockpitVitalRail(state: DroneState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        VitalCell("ALT AGL", "%.1f m".format(state.altitudeAglMeters), Modifier.weight(1f))
        VitalCell("SPEED", "%.1f m/s".format(state.groundSpeedMS), Modifier.weight(1f))
        VitalCell("BATTERY", "${state.batteryRemainingPercent.toInt()}%", Modifier.weight(1f), batteryColor(state))
        VitalCell(
            "GPS",
            if (state.gpsFixType.toInt() >= 3) "3D · ${state.gpsSatellites}" else "NO FIX",
            Modifier.weight(1f),
            if (state.gpsFixType.toInt() >= 3) CockpitBlue else CockpitRed,
        )
    }
}

@Composable
private fun VitalCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = CockpitText,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        shape = RoundedCornerShape(9.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CockpitTaskCard(
    task: CockpitTaskPresentation,
    actions: List<CockpitActionSpec>,
    onAction: (CockpitAction) -> Unit,
) {
    if (task.compactMission) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onAction(CockpitAction.OPEN_MISSION) }, modifier = Modifier.weight(1f)) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(task.title, style = MaterialTheme.typography.labelLarge, color = CockpitBlue)
                        Text(task.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                actions.filter { it.action != CockpitAction.OPEN_MISSION }.take(1).forEach { action ->
                    TextButton(onClick = { onAction(action.action) }, enabled = action.enabled) { Text(action.label) }
                }
            }
        }
        return
    }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(task.title.uppercase(), style = MaterialTheme.typography.labelLarge, color = CockpitBlue, fontWeight = FontWeight.Bold)
                Text(task.trailing, style = MaterialTheme.typography.labelSmall, color = CockpitTeal, fontWeight = FontWeight.Bold)
            }
            task.progress?.let {
                LinearProgressIndicator(
                    progress = { it },
                    modifier = Modifier.fillMaxWidth(),
                    color = CockpitBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            Text(task.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actions.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.forEach { action ->
                        TextButton(onClick = { onAction(action.action) }) {
                            Text(action.label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CockpitSystemLine(uiState: CockpitUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "● ${uiState.systemStatus}",
            style = MaterialTheme.typography.labelSmall,
            color = if (uiState.alert == null) CockpitTeal else cockpitStateColor(uiState),
        )
        Text(
            text = uiState.linkStatus,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CockpitActionDock(
    primary: CockpitActionSpec?,
    secondary: CockpitActionSpec?,
    alert: CockpitAlert?,
    onAction: (CockpitAction) -> Unit,
) {
    if (primary == null && secondary == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        primary?.let { action ->
            Button(
                onClick = { onAction(action.action) },
                modifier = Modifier.weight(1f),
                enabled = action.enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryActionColor(action.action, alert),
                ),
            ) {
                Text(action.label, maxLines = 1)
            }
        }
        secondary?.let { action ->
            OutlinedButton(
                onClick = { onAction(action.action) },
                modifier = Modifier.weight(1f),
                enabled = action.enabled,
            ) {
                Text(action.label, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CockpitDetailCard(
    state: DroneState,
    mission: MissionProgress,
    uploadStatus: MissionUploadStatus,
    recording: FlightRecordingStatus,
    identityStatus: MavlinkIdentityStatus,
    failures: FailureState,
    mavlinkStatus: String,
    systemId: Int,
) {
    val details = listOf(
        "Heading" to "${state.headingDegrees}°",
        "Attitude" to "R ${degrees(state.rollRadians)} · P ${degrees(state.pitchRadians)} · Y ${degrees(state.yawRadians)}",
        "Home" to distanceFromHomeLabel(state),
        "Motors" to rpmSummary(state),
        "Upload" to uploadStatus.displayText.removePrefix("Upload: "),
        "MAVLink" to "${mavlinkStatusLabel(mavlinkStatus, identityStatus)} · Vehicle SYSID $systemId",
        "Recording" to recording.displayText,
        "Failures" to failureStatusLabel(failures),
        "Last inbound" to state.lastInboundMessage,
        "Last ACK" to state.lastAck,
    )
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Engineering detail", style = MaterialTheme.typography.titleSmall)
            HorizontalDivider()
            details.forEach { (label, value) ->
                DetailRow(label, value)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            modifier = Modifier.weight(0.34f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            value,
            modifier = Modifier.weight(0.66f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun cockpitStateColor(uiState: CockpitUiState): Color {
    return when {
        uiState.alert?.severity == CockpitAlertSeverity.CRITICAL -> CockpitRed
        uiState.alert?.severity == CockpitAlertSeverity.CAUTION -> CockpitAmber
        uiState.freshness != CockpitDataFreshness.LIVE -> CockpitAmber
        uiState.flightPhase == CockpitFlightPhase.ARMED_GROUND -> CockpitAmber
        else -> CockpitTeal
    }
}

@Composable
private fun primaryActionColor(action: CockpitAction, alert: CockpitAlert?): Color {
    return when {
        action == CockpitAction.TAKE_CONTROL_AND_LAND -> MaterialTheme.colorScheme.error
        action == CockpitAction.LAND && alert?.severity == CockpitAlertSeverity.CRITICAL -> MaterialTheme.colorScheme.error
        action == CockpitAction.LAND -> CockpitAmber
        action == CockpitAction.DISARM -> CockpitTeal
        else -> CockpitBlue
    }
}

@Composable
private fun batteryColor(state: DroneState): Color {
    return when {
        state.batteryRemainingPercent.toInt() <= 15 -> CockpitRed
        state.batteryRemainingPercent.toInt() <= 30 -> CockpitAmber
        else -> CockpitTeal
    }
}

private fun MutableList<Float>.push(value: Float, limit: Int = RollingChartSampleCapacity) {
    add(value)
    while (size > limit) removeAt(0)
}

private fun degrees(radians: Float): String = "%.1f°".format(degreesValue(radians))

private fun degreesValue(radians: Float): Float = radians * 180f / PI.toFloat()

private fun rpmSummary(state: DroneState): String {
    return state.motors.joinToString(" / ") { "%.0f".format(it.rpm) }
}

private fun failureStatusLabel(failures: FailureState): String {
    return if (failures.activeCount == 0) "Clear" else "${failures.activeCount} active"
}

private val CockpitTeal = Color(0xFF2DD4BF)
private val CockpitBlue = Color(0xFF60A5FA)
private val CockpitAmber = Color(0xFFF5B942)
private val CockpitPurple = Color(0xFFC4B5FD)
private val CockpitRed = Color(0xFFFF5D5D)
private val CockpitText = Color(0xFFE7EEF9)
private const val DefaultTakeoffAltitudeMeters = 10f
