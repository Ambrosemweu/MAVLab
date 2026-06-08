package com.ascend.mavlab.core.common

import android.content.Context
import android.provider.Settings
import com.ascend.mavlab.core.mavlink.MavlinkSocketConfig
import com.ascend.mavlab.core.mavlink.MavlinkUdpServer
import com.ascend.mavlab.simulation.autopilot.PilotInput
import com.ascend.mavlab.simulation.engine.FlightMode
import com.ascend.mavlab.simulation.engine.PhysicsSimulationEngine
import com.ascend.mavlab.simulation.failures.FailureScenario
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.mission.MissionProgress
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppRuntime {
    private val simLoop = PhysicsSimulationEngine()
    private val fallbackStatus = MutableStateFlow("Stopped")
    private val mutableSystemId = MutableStateFlow(1)
    private var mavlinkServer: MavlinkUdpServer? = null

    val state = simLoop.state
    val failures: StateFlow<FailureState> = simLoop.failures
    val missionProgress: StateFlow<MissionProgress> = simLoop.missionProgress
    val status: StateFlow<String>
        get() = mavlinkServer?.status ?: fallbackStatus.asStateFlow()
    val systemId: StateFlow<Int> = mutableSystemId.asStateFlow()

    fun start(context: Context) {
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
    }

    fun stop() {
        mavlinkServer?.stopNow()
        simLoop.stop()
    }

    fun setArmed(armed: Boolean) {
        simLoop.setArmed(armed)
    }

    fun takeoff(targetAltitudeM: Float = 10f) {
        simLoop.takeoff(targetAltitudeM)
    }

    fun land() {
        simLoop.land()
    }

    fun setMode(mode: FlightMode) {
        simLoop.setMode(mode)
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
        simLoop.setArmed(true)
        simLoop.setMode(FlightMode.AUTO)
    }

    fun sendGuidedOffset(northMeters: Float, eastMeters: Float, altitudeAglMeters: Float) {
        simLoop.setArmed(true)
        simLoop.setGuidedOffset(northMeters, eastMeters, altitudeAglMeters)
    }

    private fun stableSystemId(context: Context): Int {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ).orEmpty()
        return (androidId.hashCode().absoluteValue % 250) + 1
    }
}
