package com.ascend.mavlab.simulation.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SimpleSimLoop : SimulationEngine {
    private val mutableState = MutableStateFlow(DroneState())
    override val state: StateFlow<DroneState> = mutableState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private val startedAtMs = System.currentTimeMillis()

    override fun start() {
        if (job != null) return
        job = scope.launch {
            while (isActive) {
                tick()
                delay(100)
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    fun setArmed(armed: Boolean) {
        mutableState.value = mutableState.value.copy(armed = armed)
    }

    fun setMode(mode: FlightMode) {
        mutableState.value = mutableState.value.copy(mode = mode)
    }

    fun noteInbound(message: String) {
        mutableState.value = mutableState.value.copy(lastInboundMessage = message)
    }

    fun noteAck(message: String) {
        mutableState.value = mutableState.value.copy(lastAck = message)
    }

    private fun tick() {
        val nowMs = System.currentTimeMillis()
        val elapsedS = (nowMs - startedAtMs) / 1000.0
        val phase = (elapsedS * 2.0 * PI / 8.0).toFloat()
        val roll = 0.18f * sin(phase)
        val pitch = 0.12f * sin(phase * 0.7f)
        val yaw = 1.57f + 0.08f * sin(phase * 0.35f)
        val altitude = 10f + 1.2f * sin(phase * 0.5f)
        val battery = (85 - (elapsedS / 60.0).toInt()).coerceIn(30, 85).toByte()

        mutableState.value = mutableState.value.copy(
            uptimeMs = (nowMs - startedAtMs).toUInt(),
            rollRadians = roll,
            pitchRadians = pitch,
            yawRadians = yaw,
            rollSpeedRadS = 0.02f * sin(phase),
            pitchSpeedRadS = 0.02f * sin(phase * 0.7f),
            yawSpeedRadS = 0.01f * sin(phase * 0.35f),
            altitudeAglMeters = altitude,
            altitudeMslMeters = 1805f + altitude,
            groundSpeedMS = if (mutableState.value.armed) 1.5f else 0f,
            verticalSpeedMS = 0.1f * sin(phase * 0.5f),
            headingDegrees = 90,
            batteryRemainingPercent = battery,
        )
    }
}
