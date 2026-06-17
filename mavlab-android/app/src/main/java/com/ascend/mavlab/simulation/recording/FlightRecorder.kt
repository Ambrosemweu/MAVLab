package com.ascend.mavlab.simulation.recording

import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.mission.MissionProgress
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FlightRecorder(
    baseFilesDir: File,
    private val clockMs: () -> Long = System::currentTimeMillis,
) {
    private val flightsRoot = File(baseFilesDir, "mavlab/flights")
    private val mutableStatus = MutableStateFlow(FlightRecordingStatus(lastSession = latestSession()))
    val status: StateFlow<FlightRecordingStatus> = mutableStatus.asStateFlow()

    private var activeSession: FlightSession? = null
    private var activeDirectory: File? = null

    @Synchronized
    fun startSession(reason: String, sessionId: String = newSessionId()): FlightSession {
        activeSession?.let { return it }
        val now = clockMs()
        val directory = File(flightsRoot, sessionId).also { it.mkdirs() }
        val session = FlightSession(
            id = sessionId,
            startedAtMs = now,
            directoryPath = directory.absolutePath,
        )
        activeDirectory = directory
        activeSession = session
        telemetryFile(directory).writeText(TelemetryHeader)
        writeManifest(session)
        appendEvent(FlightEvent(now, "recording_started", reason))
        mutableStatus.value = FlightRecordingStatus(active = true, currentSession = session)
        return session
    }

    @Synchronized
    fun appendTelemetry(
        state: DroneState,
        timestampMs: Long = clockMs(),
        activeWaypoint: Int = -1,
        failureFlags: String = "",
    ) {
        val directory = activeDirectory ?: return
        val session = activeSession ?: return
        telemetryFile(directory).appendText(telemetryRow(timestampMs, session.id, state, activeWaypoint, failureFlags))
    }

    @Synchronized
    fun appendEvent(event: FlightEvent) {
        val directory = activeDirectory ?: return
        eventsFile(directory).appendText(
            buildString {
                append("{")
                appendJsonField("timestampMs", event.timestampMs.toString(), quoted = false)
                append(",")
                appendJsonField("type", event.type)
                append(",")
                appendJsonField("message", event.message)
                append("}\n")
            },
        )
    }

    @Synchronized
    fun saveMissionSnapshot(progress: MissionProgress) {
        val directory = activeDirectory ?: return
        missionFile(directory).writeText(missionJson(progress))
    }

    @Synchronized
    fun closeSession(reason: String, endedAtMs: Long = clockMs()): FlightSession? {
        val session = activeSession ?: return null
        val directory = activeDirectory ?: return null
        appendEvent(FlightEvent(endedAtMs, "recording_stopped", reason))
        val closed = session.copy(endedAtMs = endedAtMs)
        writeManifest(closed)
        generateReport(directory, closed)
        activeSession = null
        activeDirectory = null
        mutableStatus.value = FlightRecordingStatus(active = false, lastSession = closed)
        return closed
    }

    @Synchronized
    fun activeSession(): FlightSession? = activeSession

    fun listSessions(): List<FlightSession> {
        return flightsRoot
            .listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.name }
            ?.mapNotNull { directory -> readSessionManifest(directory) }
            ?: emptyList()
    }

    fun sessionDirectory(sessionId: String): File? {
        val directory = File(flightsRoot, sessionId)
        return if (directory.isDirectory) directory else null
    }

    // --- Report generation ---

    private fun generateReport(directory: File, session: FlightSession) {
        val report = buildString {
            append("# MAVLab Flight Report\n\n")
            appendReportSummary(directory, session)
            appendReportTimeline(directory)
            appendReportSafetyNotes(directory)
            append("## Student / Operator Notes\n\n")
            append("_Add observations, lessons learned, or debrief notes here._\n\n")
        }
        reportFile(directory).writeText(report)
    }

    private fun StringBuilder.appendReportSummary(directory: File, session: FlightSession) {
        val durationMs = (session.endedAtMs ?: session.startedAtMs) - session.startedAtMs
        val events = readEvents(directory)
        val telemetryStats = readTelemetryStats(directory)

        append("## Summary\n\n")
        append("- **Session ID:** ${session.id}\n")
        append("- **Date:** ${ReportTimeFormatter.format(Instant.ofEpochMilli(session.startedAtMs))}\n")
        append("- **Duration:** ${formatDuration(durationMs)}\n")

        val authorities = events
            .filter { it.contains("\"authority_changed\"") }
            .mapNotNull { extractJsonValue(it, "message") }
        if (authorities.isNotEmpty()) {
            append("- **Control authority used:** ${authorities.distinct().joinToString(", ")}\n")
        }

        val missionFile = missionFile(directory)
        if (missionFile.isFile) {
            val missionText = missionFile.readText()
            val itemCount = "\"sequence\"".toRegex().findAll(missionText).count()
            val complete = extractJsonValue(missionText, "complete") == "true"
            append("- **Mission status:** ${if (complete) "Completed" else "Not completed"} ($itemCount items)\n")
        } else {
            append("- **Mission status:** No mission loaded\n")
        }

        append("- **Max altitude:** ${telemetryStats.maxAltitude} m AGL\n")
        append("- **Battery start:** ${telemetryStats.batteryStart}%\n")
        append("- **Battery end:** ${telemetryStats.batteryEnd}%\n")

        val failureEvents = events.filter { it.contains("\"failure_") && !it.contains("\"failure_reset\"") }
        if (failureEvents.isNotEmpty()) {
            val failureTypes = failureEvents.mapNotNull { extractJsonValue(it, "type") }.distinct()
            append("- **Failures injected:** ${failureTypes.joinToString(", ") { it.removePrefix("failure_") }}\n")
        } else {
            append("- **Failures injected:** None\n")
        }

        append("\n")
    }

    private fun StringBuilder.appendReportTimeline(directory: File) {
        val events = readEvents(directory)
        append("## Timeline\n\n")
        if (events.isEmpty()) {
            append("_No events recorded._\n\n")
            return
        }
        events.forEach { line ->
            val timestampMs = extractJsonValue(line, "timestampMs")?.toLongOrNull()
            val type = extractJsonValue(line, "type") ?: "unknown"
            val message = extractJsonValue(line, "message") ?: ""
            val time = timestampMs?.let { ReportTimeFormatter.format(Instant.ofEpochMilli(it)) } ?: "?"
            append("- **$time** — `$type` $message\n")
        }
        append("\n")
    }

    private fun StringBuilder.appendReportSafetyNotes(directory: File) {
        val events = readEvents(directory)
        val safetyNotes = mutableListOf<String>()

        if (events.any { it.contains("failure_gps_loss") || it.contains("failure_manual_gps") }) {
            safetyNotes.add("GPS was lost during flight — practice altitude hold recovery before relying on position modes.")
        }
        if (events.any { it.contains("failure_motor") }) {
            safetyNotes.add("Motor failure was injected — remember that small quadcopters have limited motor redundancy.")
        }
        if (events.any { it.contains("failure_low_battery") || it.contains("failure_critical_battery") || it.contains("failure_manual_battery") }) {
            safetyNotes.add("Battery drain was elevated — always plan return-to-launch with sufficient reserve.")
        }
        if (events.any { it.contains("failure_wind") || it.contains("failure_manual_wind") }) {
            safetyNotes.add("Wind conditions were simulated — increase standoff from obstacles in windy conditions.")
        }
        if (events.any { it.contains("failure_compass") }) {
            safetyNotes.add("Compass interference was present — heading errors degrade navigation accuracy.")
        }
        if (events.any { it.contains("failure_lost_link") }) {
            safetyNotes.add("Link loss was simulated — ensure failsafe behavior is configured before flight.")
        }

        append("## Safety Notes\n\n")
        if (safetyNotes.isEmpty()) {
            append("_No safety concerns observed during this session._\n\n")
        } else {
            safetyNotes.forEach { append("- $it\n") }
            append("\n")
        }
    }

    // --- Telemetry stats helper ---

    private data class TelemetryStats(
        val maxAltitude: String = "0.0",
        val batteryStart: String = "?",
        val batteryEnd: String = "?",
    )

    private fun readTelemetryStats(directory: File): TelemetryStats {
        val file = telemetryFile(directory)
        if (!file.isFile) return TelemetryStats()
        val lines = file.readLines()
        if (lines.size < 2) return TelemetryStats()

        // Header: find column indices
        val header = lines[0].split(",")
        val altIndex = header.indexOf("altitudeAglMeters")
        val batteryIndex = header.indexOf("batteryRemainingPercent")
        if (altIndex < 0 && batteryIndex < 0) return TelemetryStats()

        val dataLines = lines.drop(1).filter { it.isNotBlank() }
        if (dataLines.isEmpty()) return TelemetryStats()

        var maxAlt = 0f
        var batteryStart: String? = null
        var batteryEnd: String? = null

        dataLines.forEach { line ->
            val cols = line.split(",")
            if (altIndex in cols.indices) {
                cols[altIndex].toFloatOrNull()?.let { if (it > maxAlt) maxAlt = it }
            }
            if (batteryIndex in cols.indices) {
                val battery = cols[batteryIndex]
                if (batteryStart == null) batteryStart = battery
                batteryEnd = battery
            }
        }

        return TelemetryStats(
            maxAltitude = "%.1f".format(maxAlt),
            batteryStart = batteryStart ?: "?",
            batteryEnd = batteryEnd ?: "?",
        )
    }

    // --- Event reading helper ---

    private fun readEvents(directory: File): List<String> {
        val file = eventsFile(directory)
        return if (file.isFile) file.readLines().filter { it.isNotBlank() } else emptyList()
    }

    // --- Manifest and file helpers ---

    private fun writeManifest(session: FlightSession) {
        val directory = activeDirectory ?: File(session.directoryPath)
        manifestFile(directory).writeText(
            buildString {
                append("{\n")
                appendJsonLine("id", session.id)
                append(",\n")
                appendJsonLine("startedAtMs", session.startedAtMs.toString(), quoted = false)
                append(",\n")
                if (session.endedAtMs == null) {
                    append("  \"endedAtMs\": null,\n")
                } else {
                    appendJsonLine("endedAtMs", session.endedAtMs.toString(), quoted = false)
                    append(",\n")
                }
                appendJsonLine("directoryPath", session.directoryPath)
                append("\n}\n")
            },
        )
    }

    private fun telemetryRow(
        timestampMs: Long,
        sessionId: String,
        state: DroneState,
        activeWaypoint: Int,
        failureFlags: String,
    ): String {
        return listOf(
            timestampMs.toString(),
            sessionId,
            state.uptimeMs.toString(),
            state.armed.toString(),
            state.mode.name,
            state.controlAuthority.name,
            state.latitudeDeg.toString(),
            state.longitudeDeg.toString(),
            state.northMeters.toString(),
            state.eastMeters.toString(),
            state.altitudeAglMeters.toString(),
            state.rollRadians.toString(),
            state.pitchRadians.toString(),
            state.yawRadians.toString(),
            state.headingDegrees.toString(),
            state.groundSpeedMS.toString(),
            state.verticalSpeedMS.toString(),
            state.batteryVoltageMv.toString(),
            state.batteryCurrentCa.toString(),
            state.batteryRemainingPercent.toString(),
            state.throttlePercent.toString(),
            state.gpsFixType.toString(),
            state.motors.joinToString(";") { "%.1f".format(it.rpm) },
            activeWaypoint.toString(),
            failureFlags,
        ).joinToString(",") + "\n"
    }

    private fun missionJson(progress: MissionProgress): String {
        return buildString {
            append("{\n")
            appendJsonLine("currentIndex", progress.currentIndex.toString(), quoted = false)
            append(",\n")
            appendJsonLine("complete", progress.complete.toString(), quoted = false)
            append(",\n")
            append("  \"items\": [\n")
            progress.items.forEachIndexed { index, item ->
                append("    {")
                appendJsonField("sequence", item.sequence.toString(), quoted = false)
                append(",")
                appendJsonField("command", item.command.name)
                append(",")
                appendJsonField("latitudeDeg", item.latitudeDeg.toString(), quoted = false)
                append(",")
                appendJsonField("longitudeDeg", item.longitudeDeg.toString(), quoted = false)
                append(",")
                appendJsonField("altitudeAglMeters", item.altitudeAglMeters.toString(), quoted = false)
                append(",")
                appendJsonField("acceptanceRadiusMeters", item.acceptanceRadiusMeters.toString(), quoted = false)
                append(",")
                appendJsonField("autocontinue", item.autocontinue.toString(), quoted = false)
                append("}")
                if (index != progress.items.lastIndex) append(",")
                append("\n")
            }
            append("  ]\n")
            append("}\n")
        }
    }

    private fun newSessionId(): String {
        return SessionIdFormatter.format(Instant.ofEpochMilli(clockMs()))
    }

    private fun manifestFile(directory: File): File = File(directory, "manifest.json")
    private fun telemetryFile(directory: File): File = File(directory, "telemetry.csv")
    private fun eventsFile(directory: File): File = File(directory, "events.jsonl")
    private fun missionFile(directory: File): File = File(directory, "mission.json")
    private fun reportFile(directory: File): File = File(directory, "report.md")

    private fun latestSession(): FlightSession? {
        return flightsRoot
            .listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.name }
            ?.mapNotNull { directory -> readSessionManifest(directory) }
            ?.firstOrNull()
    }

    private fun readSessionManifest(directory: File): FlightSession? {
        val manifest = manifestFile(directory)
        if (!manifest.isFile) return null
        val text = manifest.readText()
        val id = stringField(text, "id") ?: directory.name
        val startedAtMs = longField(text, "startedAtMs") ?: return null
        return FlightSession(
            id = id,
            startedAtMs = startedAtMs,
            endedAtMs = longField(text, "endedAtMs"),
            directoryPath = stringField(text, "directoryPath") ?: directory.absolutePath,
        )
    }

    private fun extractJsonValue(json: String, name: String): String? {
        // Handles both quoted and unquoted values
        return Regex("\"${Regex.escape(name)}\"\\s*:\\s*\"([^\"]*)\"")
            .find(json)?.groupValues?.get(1)
            ?: Regex("\"${Regex.escape(name)}\"\\s*:\\s*(\\S+)")
                .find(json)?.groupValues?.get(1)?.trim(',', '}')
    }

    private fun stringField(json: String, name: String): String? {
        return Regex("\"${Regex.escape(name)}\"\\s*:\\s*\"([^\"]*)\"")
            .find(json)
            ?.groupValues
            ?.get(1)
    }

    private fun longField(json: String, name: String): Long? {
        val raw = Regex("\"${Regex.escape(name)}\"\\s*:\\s*(null|-?\\d+)")
            .find(json)
            ?.groupValues
            ?.get(1)
        return raw?.takeUnless { it == "null" }?.toLongOrNull()
    }

    private fun StringBuilder.appendJsonLine(name: String, value: String, quoted: Boolean = true) {
        append("  ")
        appendJsonField(name, value, quoted)
    }

    private fun StringBuilder.appendJsonField(name: String, value: String, quoted: Boolean = true) {
        append("\"")
        append(jsonEscape(name))
        append("\": ")
        if (quoted) {
            append("\"")
            append(jsonEscape(value))
            append("\"")
        } else {
            append(value)
        }
    }

    private fun jsonEscape(value: String): String {
        return buildString {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    private companion object {
        val SessionIdFormatter: DateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneOffset.UTC)

        val ReportTimeFormatter: DateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC)

        const val TelemetryHeader =
            "timestampMs,session_id,uptimeMs,armed,mode,controlAuthority,latitudeDeg,longitudeDeg,northMeters,eastMeters," +
                "altitudeAglMeters,rollRadians,pitchRadians,yawRadians,headingDegrees,groundSpeedMS,verticalSpeedMS," +
                "batteryVoltageMv,batteryCurrentCa,batteryRemainingPercent,throttlePercent,gpsFixType,motorRpm," +
                "activeWaypoint,failureFlags\n"
    }
}
