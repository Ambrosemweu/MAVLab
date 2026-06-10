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
    fun appendTelemetry(state: DroneState, timestampMs: Long = clockMs()) {
        val directory = activeDirectory ?: return
        telemetryFile(directory).appendText(telemetryRow(timestampMs, state))
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
        appendEvent(FlightEvent(endedAtMs, "recording_stopped", reason))
        val closed = session.copy(endedAtMs = endedAtMs)
        writeManifest(closed)
        activeSession = null
        activeDirectory = null
        mutableStatus.value = FlightRecordingStatus(active = false, lastSession = closed)
        return closed
    }

    @Synchronized
    fun activeSession(): FlightSession? = activeSession

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

    private fun telemetryRow(timestampMs: Long, state: DroneState): String {
        return listOf(
            timestampMs.toString(),
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
            state.groundSpeedMS.toString(),
            state.verticalSpeedMS.toString(),
            state.batteryVoltageMv.toString(),
            state.batteryCurrentCa.toString(),
            state.batteryRemainingPercent.toString(),
            state.throttlePercent.toString(),
            state.motors.joinToString(";") { "%.1f".format(it.rpm) },
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

    private companion object {
        val SessionIdFormatter: DateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneOffset.UTC)

        const val TelemetryHeader =
            "timestampMs,uptimeMs,armed,mode,controlAuthority,latitudeDeg,longitudeDeg,northMeters,eastMeters," +
                "altitudeAglMeters,rollRadians,pitchRadians,yawRadians,groundSpeedMS,verticalSpeedMS," +
                "batteryVoltageMv,batteryCurrentCa,batteryRemainingPercent,throttlePercent,motorRpm\n"
    }
}
