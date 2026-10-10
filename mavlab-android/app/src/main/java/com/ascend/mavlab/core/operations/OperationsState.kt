package com.ascend.mavlab.core.operations

import com.ascend.mavlab.core.common.RuntimeLifecycleState
import com.ascend.mavlab.core.mavlink.MavlinkIdentityStatus
import com.ascend.mavlab.simulation.engine.ControlAuthority
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.mission.MissionProgress
import com.ascend.mavlab.simulation.mission.MissionUploadPhase
import com.ascend.mavlab.simulation.mission.MissionUploadStatus
import kotlin.math.abs

enum class OpsSeverity(val label: String) { INFO("Info"), CAUTION("Caution"), WARNING("Warning"), CRITICAL("Critical") }
enum class OpsComponent(val label: String) { RUNTIME("Runtime"), CONTROL("Control"), NAVIGATION("Navigation"), LINK("Link"), MISSION("Mission"), VEHICLE("Vehicle"), RECORDING("Recording") }
enum class OpsEventState(val label: String) { CHANGED("Changed"), ACTIVE("Active"), RECOVERED("Recovered") }
enum class OpsRecovery { CONTROL, RESET_FAILURES, RESET_BATTERY, QGC_SETUP, REUPLOAD, RESTART }

data class OpsIncident(
    val id: String,
    val title: String,
    val consequence: String,
    val severity: OpsSeverity,
    val component: OpsComponent,
    val recovery: OpsRecovery,
    val actionLabel: String,
    val observedAtMs: Long = 0,
)

data class OpsEvent(
    val id: Long,
    val timestampMs: Long,
    val severity: OpsSeverity,
    val component: OpsComponent,
    val state: OpsEventState,
    val message: String,
)

data class OperationsState(
    val incidents: List<OpsIncident> = emptyList(),
    val events: List<OpsEvent> = emptyList(),
    val runtime: RuntimeLifecycleState = RuntimeLifecycleState.STOPPED,
    val appVisible: Boolean = true,
    val stateFresh: Boolean = false,
    val recording: Boolean = false,
)

data class OpsObservation(
    val vehicle: DroneState,
    val failures: FailureState,
    val identity: MavlinkIdentityStatus,
    val mission: MissionProgress,
    val upload: MissionUploadStatus,
    val runtime: RuntimeLifecycleState,
    val appVisible: Boolean,
    val stateFresh: Boolean,
    val recording: Boolean,
    val recordingError: String? = null,
    val automaticRecordingEnabled: Boolean = false,
)

/** Process-local history of observed transitions, not raw high-frequency telemetry or an audit log. */
class OperationsObserver(private val capacity: Int = 200) {
    init { require(capacity > 0) }
    private var previousValues: Map<String, Pair<OpsComponent, String>> = emptyMap()
    private var latest = OperationsState()
    private var nextId = 0L

    @Synchronized
    fun update(observation: OpsObservation, nowMs: Long): OperationsState {
        val oldIncidents = latest.incidents.associateBy { it.id }
        val incidents = detectIncidents(observation).map { incident ->
            incident.copy(observedAtMs = oldIncidents[incident.id]?.observedAtMs ?: nowMs)
        }.sortedByDescending { it.severity.ordinal }
        val events = latest.events.toMutableList()
        val values = transitionValues(observation)
        values.forEach { (key, value) ->
            val previous = previousValues[key]?.second
            if (previous != null && previous != value.second) {
                events += OpsEvent(++nextId, nowMs, OpsSeverity.INFO, value.first, OpsEventState.CHANGED,
                    "$key: $previous → ${value.second}")
            }
        }
        incidents.filter { it.id !in oldIncidents }.forEach {
            events += OpsEvent(++nextId, nowMs, it.severity, it.component, OpsEventState.ACTIVE, it.title)
        }
        val currentIds = incidents.map { it.id }.toSet()
        oldIncidents.values.filter { it.id !in currentIds }.forEach {
            events += OpsEvent(++nextId, nowMs, OpsSeverity.INFO, it.component, OpsEventState.RECOVERED,
                "${it.title} — condition no longer active in observed state")
        }
        previousValues = values
        latest = OperationsState(incidents, events.takeLast(capacity), observation.runtime,
            observation.appVisible, observation.stateFresh, observation.recording)
        return latest
    }
}

fun filterOpsEvents(
    events: List<OpsEvent>, severity: OpsSeverity? = null, component: OpsComponent? = null,
    state: OpsEventState? = null, sinceMs: Long? = null,
): List<OpsEvent> = events.asReversed().filter {
    (severity == null || it.severity == severity) && (component == null || it.component == component) &&
        (state == null || it.state == state) && (sinceMs == null || it.timestampMs >= sinceMs)
}

private fun transitionValues(o: OpsObservation): Map<String, Pair<OpsComponent, String>> = linkedMapOf(
    "Runtime" to (OpsComponent.RUNTIME to o.runtime.name),
    "App visibility" to (OpsComponent.RUNTIME to if (o.appVisible) "Foreground" else "Background"),
    "State freshness" to (OpsComponent.RUNTIME to if (o.stateFresh) "Live" else "Unavailable or stale"),
    "Control authority" to (OpsComponent.CONTROL to o.vehicle.controlAuthority.displayName),
    "Armed state" to (OpsComponent.CONTROL to if (o.vehicle.armed) "Armed" else "Disarmed"),
    "Flight mode" to (OpsComponent.CONTROL to o.vehicle.mode.displayName),
    "GPS fix" to (OpsComponent.NAVIGATION to if (o.vehicle.gpsFixType >= 3u) "3D fix" else "No 3D fix"),
    "GCS heartbeat" to (OpsComponent.LINK to if (o.identity.gcsConnected) "Validated" else if (o.identity.lastGcsHeartbeatAtMs != null) "Unvalidated or expired" else "None detected"),
    "GCS identity" to (OpsComponent.LINK to "${o.identity.lastGcsSystemId ?: "none"}/${o.identity.lastGcsComponentId ?: "none"}"),
    "Identity conflict" to (OpsComponent.LINK to if (o.identity.identityConflict) "Conflict" else "No conflict"),
    "Mission upload" to (OpsComponent.MISSION to (o.upload.phase.name + (o.upload.lastError?.let { ": $it" } ?: ""))),
    "Mission state" to (OpsComponent.MISSION to when {
        !o.mission.loaded -> "No mission"
        o.mission.complete -> "Completed"
        o.vehicle.armed && o.vehicle.controlAuthority == ControlAuthority.GCS_MISSION -> "AUTO active"
        o.mission.received -> "Received, not executing"
        else -> "Stored source unverified"
    }),
    "Recording" to (OpsComponent.RECORDING to if (o.recording) "Active" else "Stopped"),
    "Automatic recording" to (OpsComponent.RECORDING to if (o.automaticRecordingEnabled) "Enabled" else "Off"),
)

private fun detectIncidents(o: OpsObservation): List<OpsIncident> = buildList {
    fun issue(id: String, title: String, consequence: String, severity: OpsSeverity, component: OpsComponent,
              recovery: OpsRecovery = OpsRecovery.RESET_FAILURES, action: String = "Reset simulated failures") {
        add(OpsIncident(id, title, consequence, severity, component, recovery, action))
    }
    val f = o.failures
    if (o.runtime == RuntimeLifecycleState.ERROR) issue("runtime", "Simulation runtime failed", "State and controls may be unavailable.", OpsSeverity.CRITICAL, OpsComponent.RUNTIME, OpsRecovery.RESTART, "Restart simulation")
    else if (o.runtime == RuntimeLifecycleState.RUNNING && !o.stateFresh) issue("stale", "Simulation state is stale", "Current position and health cannot be verified. Review Control before acting.", OpsSeverity.WARNING, OpsComponent.RUNTIME, OpsRecovery.CONTROL, "Open Control")
    if (o.identity.identityConflict) issue("identity", "MAVLink identity conflict", o.identity.message.ifBlank { "Vehicle and GCS must use different system IDs." }, OpsSeverity.CRITICAL, OpsComponent.LINK, OpsRecovery.QGC_SETUP, "Review QGC setup")
    if (!f.gpsEnabled || o.vehicle.gpsFixType < 3u) issue("gps", "GPS position guidance unavailable", "Position modes and mission guidance are degraded. Reset an injected fault or recover in Control.", OpsSeverity.WARNING, OpsComponent.NAVIGATION, if (!f.gpsEnabled) OpsRecovery.RESET_FAILURES else OpsRecovery.CONTROL, if (!f.gpsEnabled) "Reset simulated failures" else "Open Control")
    if (f.gpsNoiseMultiplier > 1.05f) issue("gps_noise", "GPS drift injected", "Position estimates are noisy; route tracking is less reliable.", OpsSeverity.WARNING, OpsComponent.NAVIGATION)
    if (!f.compassEnabled || abs(f.compassOffsetDeg) > 0.5f) issue("compass", "Compass guidance degraded", "Heading may be unavailable or biased; autonomous guidance is affected.", OpsSeverity.WARNING, OpsComponent.NAVIGATION)
    if (f.hasMotorFailure) issue("motor", "Motor failure injected", "Reduced thrust can destabilize the simulated vehicle. Landing controls remain in Control.", OpsSeverity.CRITICAL, OpsComponent.VEHICLE)
    if (f.windSpeedMs > 0.05f || f.windGustsMs > 0.05f) issue("wind", "Wind scenario active", "Drift and tracking demand increase with wind and gusts.", OpsSeverity.CAUTION, OpsComponent.VEHICLE)
    if (f.batteryDrainMultiplier > 1.05f) issue("drain", "Elevated battery drain injected", "Resetting the scenario restores normal drain; it does not recharge the battery.", OpsSeverity.WARNING, OpsComponent.VEHICLE)
    if (o.vehicle.batteryRemainingPercent <= 30) issue("battery", if (o.vehicle.batteryRemainingPercent <= 15) "Battery critical" else "Battery low", "Land before recharging. Battery reset is available while disarmed.", if (o.vehicle.batteryRemainingPercent <= 15) OpsSeverity.CRITICAL else OpsSeverity.WARNING, OpsComponent.VEHICLE, if (o.vehicle.armed) OpsRecovery.CONTROL else OpsRecovery.RESET_BATTERY, if (o.vehicle.armed) "Open landing controls" else "Recharge simulated battery")
    if (f.payloadMassKg > 0.05f) issue("payload", "Extra payload active", "Thrust margin is reduced by the added simulated mass.", OpsSeverity.CAUTION, OpsComponent.VEHICLE)
    if (f.lostLinkActive) issue("lost_link", "Lost-link scenario active", "This injected fault is separate from the actual GCS heartbeat. Current autonomous state is retained.", OpsSeverity.CRITICAL, OpsComponent.LINK)
    if (!o.identity.gcsConnected && o.identity.lastGcsHeartbeatAtMs != null && o.vehicle.armed &&
        o.vehicle.controlAuthority in listOf(ControlAuthority.GCS_DIRECT, ControlAuthority.GCS_MISSION)) issue("gcs_expired", "GCS heartbeat is not validated", "The engine retains its current authority and flight intent. Reconnect the GCS or take local control and land.", OpsSeverity.WARNING, OpsComponent.LINK, OpsRecovery.CONTROL, "Open recovery controls")
    if (abs(f.barometerOffsetMeters) > 0.05f) issue("barometer", "Barometer bias injected", "Reported altitude carries a sensor offset; low-level guidance is harder to trust.", OpsSeverity.WARNING, OpsComponent.NAVIGATION)
    if (f.unsafeMissionReserveActive) issue("reserve", "Unsafe mission reserve flagged", "Local Start AUTO is blocked until the simulated reserve fault is cleared.", OpsSeverity.CRITICAL, OpsComponent.MISSION)
    if (o.upload.phase == MissionUploadPhase.REJECTED) issue("upload", "Mission upload rejected", o.upload.lastError ?: "Inspect upload diagnostics. The previous loaded mission is retained.", OpsSeverity.WARNING, OpsComponent.MISSION, OpsRecovery.REUPLOAD, "Inspect upload rejection")
    if (o.mission.loaded && !o.mission.received) issue("source", "Stored mission source is unverified", "Re-upload from QGC to enable route display and local Start AUTO. Stored items are retained.", OpsSeverity.WARNING, OpsComponent.MISSION, OpsRecovery.REUPLOAD, "Review mission source")
    o.recordingError?.let { issue("recording", "Recording action failed", it, OpsSeverity.WARNING, OpsComponent.RECORDING, OpsRecovery.CONTROL, "Review recording controls") }
}
