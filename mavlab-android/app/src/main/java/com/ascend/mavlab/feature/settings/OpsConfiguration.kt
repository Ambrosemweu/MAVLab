package com.ascend.mavlab.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.ascend.mavlab.core.common.AppRuntime
import com.ascend.mavlab.core.ui.navigation.screenSwipeExcluded
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.SimLocation
import com.ascend.mavlab.simulation.mission.MissionProgress
import java.util.Locale

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun SimLocationCard(
    state: DroneState,
    mission: MissionProgress,
    selectedLocation: SimLocation,
) {
    var locationExpanded by rememberSaveable { mutableStateOf(false) }
    var customLatitude by remember(selectedLocation) {
        mutableStateOf("%.6f".format(Locale.US, selectedLocation.latitudeDeg))
    }
    var customLongitude by remember(selectedLocation) {
        mutableStateOf("%.6f".format(Locale.US, selectedLocation.longitudeDeg))
    }
    var customAltitude by remember(selectedLocation) {
        mutableStateOf("%.0f".format(Locale.US, selectedLocation.altitudeMslMeters))
    }
    var pendingLocation by remember { mutableStateOf<SimLocation?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val canChange = !state.armed
    val parsedCustom = remember(customLatitude, customLongitude, customAltitude) {
        val latitude = customLatitude.toDoubleOrNull()
        val longitude = customLongitude.toDoubleOrNull()
        val altitude = customAltitude.toFloatOrNull()
        if (latitude != null && latitude.isFinite() && latitude in -90.0..90.0 &&
            longitude != null && longitude.isFinite() && longitude in -180.0..180.0 &&
            altitude != null && altitude.isFinite()) {
            SimLocation.custom(latitude, longitude, altitude)
        } else {
            null
        }
    }

    pendingLocation?.let { location ->
        AlertDialog(onDismissRequest = { pendingLocation = null }, title = { Text("Change simulation origin?") },
            text = { Text("Sets the GPS origin to ${location.label}, clears navigation/fence/rally plans and disables fencing. The vehicle must remain disarmed.") },
            confirmButton = { TextButton(onClick = {
                pendingLocation = null
                feedback = if (AppRuntime.setSimLocation(location)) "Simulation origin updated" else "Origin unchanged: disarm before changing it."
            }) { Text("Apply origin") } },
            dismissButton = { TextButton(onClick = { pendingLocation = null }) { Text("Cancel") } })
    }
    ElevatedCard(modifier = Modifier.fillMaxWidth().screenSwipeExcluded()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("GCS start location", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "%.6f, %.6f @ %.0f m MSL".format(
                            state.latitudeDeg,
                            state.longitudeDeg,
                            state.altitudeMslMeters,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
                OutlinedButton(modifier = Modifier.screenSwipeExcluded(cancelClickOnHorizontalDrag = true), onClick = { locationExpanded = !locationExpanded }) {
                    Icon(
                        imageVector = if (locationExpanded) Icons.Filled.KeyboardArrowUp
                        else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (locationExpanded) "Collapse GCS start location" else "Expand GCS start location",
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (locationExpanded) "Hide" else "Show")
                }
            }
            Text(
                text = buildString {
                    append("Active: ${selectedLocation.label}")
                    if (mission.loaded) {
                        append(" | Mission loaded")
                    }
                    if (state.armed) {
                        append(" | Disarm to change")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
            )
            feedback?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            AnimatedVisibility(
                visible = locationExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "QGC sees this as the vehicle GPS position. Change it while disarmed before uploading a mission.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().screenSwipeExcluded(cancelClickOnHorizontalDrag = true),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SimLocation.presets.forEach { location ->
                            FilterChip(
                                selected = selectedLocation.id == location.id,
                                enabled = canChange,
                                onClick = { pendingLocation = location },
                                label = { Text(location.label) },
                            )
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LocationTextField(
                            label = "Latitude",
                            value = customLatitude,
                            onValueChange = { customLatitude = it },
                            enabled = canChange,
                            modifier = Modifier.weight(1f),
                        )
                        LocationTextField(
                            label = "Longitude",
                            value = customLongitude,
                            onValueChange = { customLongitude = it },
                            enabled = canChange,
                            modifier = Modifier.weight(1f),
                        )
                        LocationTextField(
                            label = "MSL m",
                            value = customAltitude,
                            onValueChange = { customAltitude = it },
                            enabled = canChange,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Button(
                        onClick = { pendingLocation = parsedCustom },
                        enabled = canChange && parsedCustom != null,
                        modifier = Modifier.fillMaxWidth().screenSwipeExcluded(cancelClickOnHorizontalDrag = true),
                    ) {
                        Text("Apply custom location")
                    }
                    val note = buildString {
                        if (mission.loaded) {
                            append("Changing location clears the current mission so QGC can upload a route for the new area.")
                        }
                        if (state.armed) {
                            if (isNotEmpty()) append("\n")
                            append("Disarm before changing the simulated GPS origin.")
                        }
                    }
                    if (note.isNotBlank()) {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
