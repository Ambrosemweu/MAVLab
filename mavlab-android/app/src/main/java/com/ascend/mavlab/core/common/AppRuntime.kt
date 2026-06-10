package com.ascend.mavlab.core.common

import android.content.Context
import android.provider.Settings
import com.ascend.mavlab.core.mavlink.MavlinkSocketConfig
import com.ascend.mavlab.core.mavlink.MavlinkUdpServer
import com.ascend.mavlab.simulation.autopilot.PilotInput
import com.ascend.mavlab.simulation.engine.ControlAuthority
import com.ascend.mavlab.simulation.engine.FlightMode
import com.ascend.mavlab.simulation.engine.PhysicsSimulationEngine
import com.ascend.mavlab.simulation.failures.FailureScenario
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.mission.MissionProgress
import com.ascend.mavlab.simulation.mission.MissionUploadStatus
import com.ascend.mavlab.simulation.recording.FlightEvent
import com.ascend.mavlab.simulation.recording.FlightRecorder
import com.ascend.mavlab.simulation.recording.FlightRecordingStatus
import kotlin.math.absoluteValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object AppRuntime {
    private val simLoop = PhysicsSimulationEngine()
    private val fallbackStatus = MutableStateFlow("Stopped")
    private val mutableSystemId = MutableStateFlow(1)
    private val mutableRecordingStatus = MutableStateFlow(FlightRecordingStatus())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var mavlinkServer: MavlinkUdpServer? = null
    private var flightRecorder: FlightRecorder? = null
    private var recordingJob: Job? = null

    val state = simLoop.state
    val failures: StateFlow<FailureState> = simLoop.failures
    val missionProgress: StateFlow<MissionProgress> = simLoop.missionProgress
    val missionUploadStatus: StateFlow<MissionUploadStatus> = simLoop.missionUploadStatus
    val recordingStatus: StateFlow<FlightRecordingStatus> = mutableRecordingStatus.asStateFlow()
    val status: StateFlow<String>
        get() = mavlinkServer?.status ?: fallbackStatus.asStateFlow()
    val systemId: StateFlow<Int> = mutableSystemId.asStateFlow()

    fun start(context: Context) {
        if (flightRecorder == null) {
            flightRecorder = FlightRecorder(context.applicationContext.filesDir)
        }
        if (mavlinkServer == null) {
            val id = stableSystemId(context)
            mutableSystemId.value = id
            mavlinkServer = MavlinkUdpServer(
                simLoop = simLoop,
                config = MavlinkSocketConfig(systemId = id),
            )
        }
        simLoop.start()
        mavlinkServer?.start(context.applicationContext)
        startRecordingMonitor()
    }

    fun stop() {
        mavlinkServer?.stopNow()
        recordingJob?.cancel()
        recordingJob = null
        flightRecorder?.closeSession("runtime stopped")
        syncRecordingStatus()
        simLoop.stop()
    }

    fun setArmed(armed: Boolean) {
        simLoop.setArmed(armed, ControlAuthority.CONTROLLER)
    }

    fun takeoff(targetAltitudeM: Float = 10f) {
        simLoop.takeoff(targetAltitudeM, ControlAuthority.CONTROLLER)
    }

    fun land() {
        simLoop.land(ControlAuthority.CONTROLLER)
    }

    fun setMode(mode: FlightMode) {
        simLoop.setMode(mode, ControlAuthority.CONTROLLER)
    }

    fun setPilotInput(input: PilotInput) {
        simLoop.setPilotInput(input)
    }

    fun applyFailureScenario(scenario: FailureScenario) {
        simLoop.failureInjector.applyScenario(scenario)
    }

    fun resetFailures() {
        simLoop.failureInjector.resetAll()
    }

    fun setGpsEnabled(enabled: Boolean) {
        simLoop.failureInjector.setGpsEnabled(enabled)
    }

    fun setGpsNoiseMultiplier(value: Float) {
        simLoop.failureInjector.setGpsNoiseMultiplier(value)
    }

    fun setCompassEnabled(enabled: Boolean) {
        simLoop.failureInjector.setCompassEnabled(enabled)
    }

    fun setCompassOffsetDeg(value: Float) {
        simLoop.failureInjector.setCompassOffsetDeg(value)
    }

    fun setWindSpeedMs(value: Float) {
        simLoop.failureInjector.setWindSpeedMs(value)
    }

    fun setWindDirectionDeg(value: Float) {
        simLoop.failureInjector.setWindDirectionDeg(value)
    }

    fun setWindGustsMs(value: Float) {
        simLoop.failureInjector.setWindGustsMs(value)
    }

    fun setMotorFailed(index: Int, failed: Boolean) {
        simLoop.failureInjector.setMotorFailed(index, failed)
    }

    fun setBatteryDrainMultiplier(value: Float) {
        simLoop.failureInjector.setBatteryDrainMultiplier(value)
    }

    fun setPayloadMassKg(value: Float) {
        simLoop.failureInjector.setPayloadMassKg(value)
    }

    fun loadDemoMission() {
        simLoop.loadDemoMission()
    }

    fun clearMission() {
        simLoop.clearMission()
    }

    fun startAutoMission() {
        if (!simLoop.missionProgress.value.loaded) {
            simLoop.noteAck("MISSION_START NO MISSION")
            return
        }
        simLoop.setArmed(true, ControlAuthority.GCS_MISSION)
        simLoop.setMode(FlightMode.AUTO, ControlAuthority.GCS_MISSION)
    }

    fun sendGuidedOffset(northMeters: Float, eastMeters: Float, altitudeAglMeters: Float) {
        simLoop.setArmed(true, ControlAuthority.CONTROLLER)
        simLoop.setGuidedOffset(northMeters, eastMeters, altitudeAglMeters, ControlAuthority.CONTROLLER)
    }

    private fun stableSystemId(context: Context): Int {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ).orEmpty()
        return (androidId.hashCode().absoluteValue % 250) + 1
    }

    private fun startRecordingMonitor() {
        if (recordingJob != null) return
        recordingJob = scope.launch {
            var previousArmed = state.value.armed
            var previousMode = state.value.mode
            var previousAuthority = state.value.controlAuthority
            var previousMissionSignature = missionSignature(missionProgress.value)

            while (isActive) {
                val recorder = flightRecorder
                if (recorder == null) {
                    delay(RecordingSampleMs)
                    continue
                }

                val current = state.value
                val mission = missionProgress.value
                val currentMissionSignature = missionSignature(mission)
                val sessionActive = recorder.activeSession() != null

                if (current.armed && !sessionActive) {
                    recorder.startSession(recordingStartReason(current))
                    recorder.saveMissionSnapshot(mission)
                }

                if (recorder.activeSession() != null) {
                    if (!previousArmed && current.armed) {
                        recorder.appendEvent(FlightEvent(System.currentTimeMillis(), "vehicle_armed", current.controlAuthority.displayName))
                    }
                    if (current.mode != previousMode) {
                        recorder.appendEvent(FlightEvent(System.currentTimeMillis(), "mode_changed", current.mode.displayName))
                    }
                    if (current.controlAuthority != previousAuthority) {
                        recorder.appendEvent(FlightEvent(System.currentTimeMillis(), "authority_changed", current.controlAuthority.displayName))
                    }
                    if (mission.loaded && currentMissionSignature != previousMissionSignature) {
                        recorder.saveMissionSnapshot(mission)
                        recorder.appendEvent(FlightEvent(System.currentTimeMillis(), "mission_snapshot", "${mission.items.size} items"))
                    }
                    recorder.appendTelemetry(current)
                }

                if (previousArmed && !current.armed) {
                    recorder.closeSession("vehicle disarmed")
                }

                previousArmed = current.armed
                previousMode = current.mode
                previousAuthority = current.controlAuthority
                previousMissionSignature = currentMissionSignature
                syncRecordingStatus()
                delay(RecordingSampleMs)
            }
        }
    }

    private fun syncRecordingStatus() {
        flightRecorder?.let { mutableRecordingStatus.value = it.status.value }
    }

    private fun recordingStartReason(state: com.ascend.mavlab.simulation.engine.DroneState): String {
        return when {
            state.controlAuthority == ControlAuthority.GCS_MISSION -> "GCS mission"
            state.mode == FlightMode.GUIDED -> "takeoff or guided command"
            else -> "vehicle armed"
        }
    }

    private fun missionSignature(mission: MissionProgress): String {
        return mission.items.joinToString("|") { item ->
            "${item.sequence}:${item.command}:${item.latitudeDeg}:${item.longitudeDeg}:${item.altitudeAglMeters}"
        } + ":${mission.currentIndex}:${mission.complete}"
    }

    private const val RecordingSampleMs = 200L
}
