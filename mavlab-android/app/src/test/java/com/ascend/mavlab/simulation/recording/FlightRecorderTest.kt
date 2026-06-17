package com.ascend.mavlab.simulation.recording

import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.mission.MissionCommand
import com.ascend.mavlab.simulation.mission.MissionItem
import com.ascend.mavlab.simulation.mission.MissionProgress
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FlightRecorderTest {
    @Test
    fun startsSessionAndCreatesDirectoryFiles() {
        withRecorder { recorder, baseDir ->
            val session = recorder.startSession("unit test", sessionId = "test-session")
            val directory = baseDir.resolve("mavlab/flights/test-session")

            assertEquals("test-session", session.id)
            assertTrue(directory.isDirectory)
            assertTrue(directory.resolve("manifest.json").isFile)
            assertTrue(directory.resolve("telemetry.csv").isFile)
            assertTrue(directory.resolve("events.jsonl").isFile)
        }
    }

    @Test
    fun writesTelemetryHeaderAndRow() {
        withRecorder { recorder, baseDir ->
            recorder.startSession("unit test", sessionId = "test-session")
            recorder.appendTelemetry(
                state = DroneState(armed = true, northMeters = 3f),
                timestampMs = 2000L,
                activeWaypoint = 2,
                failureFlags = "motor_failure"
            )

            val lines = baseDir.resolve("mavlab/flights/test-session/telemetry.csv").readLines()
            assertEquals(2, lines.size)
            assertTrue(lines.first().startsWith("timestampMs,session_id,uptimeMs,armed,mode"))
            assertTrue(lines[1].startsWith("2000,test-session,0,true,STABILIZE"))
            assertTrue(lines[1].contains(",2,motor_failure"))
        }
    }

    @Test
    fun appendsEventJsonLine() {
        withRecorder { recorder, baseDir ->
            recorder.startSession("unit test", sessionId = "test-session")
            recorder.appendEvent(FlightEvent(2100L, "mode_changed", "Auto"))

            val events = baseDir.resolve("mavlab/flights/test-session/events.jsonl").readLines()
            assertTrue(events.any { it.contains("\"type\": \"mode_changed\"") })
            assertTrue(events.any { it.contains("\"message\": \"Auto\"") })
        }
    }

    @Test
    fun savesMissionSnapshot() {
        withRecorder { recorder, baseDir ->
            recorder.startSession("unit test", sessionId = "test-session")
            recorder.saveMissionSnapshot(
                MissionProgress(
                    items = listOf(
                        MissionItem(
                            sequence = 0,
                            command = MissionCommand.WAYPOINT,
                            latitudeDeg = -1.2921,
                            longitudeDeg = 36.8219,
                            altitudeAglMeters = 12f,
                        ),
                    ),
                    complete = false,
                    activeTarget = null,
                ),
            )

            val mission = baseDir.resolve("mavlab/flights/test-session/mission.json").readText()
            assertTrue(mission.contains("\"items\""))
            assertTrue(mission.contains("\"command\": \"WAYPOINT\""))
        }
    }

    @Test
    fun closesManifest() {
        withRecorder { recorder, baseDir ->
            recorder.startSession("unit test", sessionId = "test-session")

            val closed = recorder.closeSession("done", endedAtMs = 3000L)
            val manifest = baseDir.resolve("mavlab/flights/test-session/manifest.json").readText()

            assertNotNull(closed)
            assertEquals(3000L, closed.endedAtMs)
            assertTrue(manifest.contains("\"endedAtMs\": 3000"))
            assertEquals(false, recorder.status.value.active)
            assertEquals("Last log: mavlab/flights/test-session", recorder.status.value.displayText)
        }
    }

    @Test
    fun initializesWithLatestExistingSession() {
        val baseDir = Files.createTempDirectory("mavlab-flight-recorder-test").toFile()
        try {
            val recorder = FlightRecorder(baseDir, clockMs = { 1000L })
            recorder.startSession("first", sessionId = "20260610-100000-000")
            recorder.closeSession("done", endedAtMs = 2000L)

            val restored = FlightRecorder(baseDir, clockMs = { 3000L })

            assertEquals(false, restored.status.value.active)
            assertEquals("20260610-100000-000", restored.status.value.lastSession?.id)
            assertEquals(2000L, restored.status.value.lastSession?.endedAtMs)
        } finally {
            baseDir.deleteRecursively()
        }
    }

    @Test
    fun reportsSessionListingNewestFirst() {
        val baseDir = Files.createTempDirectory("mavlab-flight-recorder-test").toFile()
        try {
            val recorder = FlightRecorder(baseDir, clockMs = { 1000L })
            
            // Create session 1
            recorder.startSession("session 1", sessionId = "20260610-100000-000")
            recorder.closeSession("done 1", endedAtMs = 2000L)
            
            // Create session 2
            val recorderSecond = FlightRecorder(baseDir, clockMs = { 3000L })
            recorderSecond.startSession("session 2", sessionId = "20260610-110000-000")
            recorderSecond.closeSession("done 2", endedAtMs = 4000L)

            val sessions = recorderSecond.listSessions()
            assertEquals(2, sessions.size)
            assertEquals("20260610-110000-000", sessions[0].id)
            assertEquals("20260610-100000-000", sessions[1].id)
        } finally {
            baseDir.deleteRecursively()
        }
    }

    @Test
    fun generatesMarkdownReportOnClose() {
        withRecorder { recorder, baseDir ->
            recorder.startSession("unit test", sessionId = "test-session")
            
            // Append telemetry to ensure max altitude / battery ranges are parsed
            recorder.appendTelemetry(
                state = DroneState(armed = true, altitudeAglMeters = 5.5f, batteryRemainingPercent = 90),
                timestampMs = 1500L
            )
            recorder.appendTelemetry(
                state = DroneState(armed = true, altitudeAglMeters = 10.2f, batteryRemainingPercent = 85),
                timestampMs = 2000L
            )
            recorder.appendTelemetry(
                state = DroneState(armed = true, altitudeAglMeters = 2.0f, batteryRemainingPercent = 80),
                timestampMs = 2500L
            )

            // Inject events to check safety notes and timeline
            recorder.appendEvent(FlightEvent(1600L, "failure_motor", "motor 1 failure"))
            recorder.appendEvent(FlightEvent(1700L, "mode_changed", "RTL"))

            recorder.closeSession("done", endedAtMs = 3000L)

            val reportFile = baseDir.resolve("mavlab/flights/test-session/report.md")
            assertTrue(reportFile.isFile)

            val content = reportFile.readText()
            assertTrue(content.contains("# MAVLab Flight Report"))
            assertTrue(content.contains("Session ID:** test-session"))
            assertTrue(content.contains("Max altitude:** 10.2 m AGL"))
            assertTrue(content.contains("Battery start:** 90%"))
            assertTrue(content.contains("Battery end:** 80%"))
            assertTrue(content.contains("Motor failure was injected"))
            assertTrue(content.contains("mode_changed"))
            assertTrue(content.contains("Student / Operator Notes"))
        }
    }

    private fun withRecorder(block: (FlightRecorder, java.io.File) -> Unit) {
        val baseDir = Files.createTempDirectory("mavlab-flight-recorder-test").toFile()
        try {
            val recorder = FlightRecorder(baseDir, clockMs = { 1000L })
            block(recorder, baseDir)
        } finally {
            baseDir.deleteRecursively()
        }
    }
}
