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
            recorder.appendTelemetry(DroneState(armed = true, northMeters = 3f), timestampMs = 2000L)

            val lines = baseDir.resolve("mavlab/flights/test-session/telemetry.csv").readLines()
            assertEquals(2, lines.size)
            assertTrue(lines.first().startsWith("timestampMs,uptimeMs,armed,mode"))
            assertTrue(lines[1].startsWith("2000,0,true,STABILIZE"))
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
