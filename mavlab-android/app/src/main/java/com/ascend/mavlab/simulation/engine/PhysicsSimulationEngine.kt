package com.ascend.mavlab.simulation.engine

import com.ascend.mavlab.simulation.autopilot.Autopilot
import com.ascend.mavlab.simulation.autopilot.MissionEngine
import com.ascend.mavlab.simulation.autopilot.PilotInput
import com.ascend.mavlab.simulation.autopilot.PositionController
import com.ascend.mavlab.simulation.failures.FailureInjector
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.physics.EnvironmentModel
import com.ascend.mavlab.simulation.physics.PhysicsModel
import com.ascend.mavlab.simulation.physics.QuadcopterParams
import com.ascend.mavlab.simulation.mission.MissionItem
import com.ascend.mavlab.simulation.mission.MissionProgress
import com.ascend.mavlab.simulation.physics.Vector3
import kotlin.math.cos
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class PhysicsSimulationEngine(
    private val params: QuadcopterParams = QuadcopterParams(),
    private val physics: PhysicsModel = PhysicsModel(params),
    private val autopilot: Autopilot = Autopilot(params),
    private val positionController: PositionController = PositionController(params),
    val failureInjector: FailureInjector = FailureInjector(),
    val missionEngine: MissionEngine = MissionEngine(),
) : SimulationEngine {
    private val mutableState = MutableStateFlow(DroneState())
    override val state: StateFlow<DroneState> = mutableState.asStateFlow()
    val failures: StateFlow<FailureState> = failureInjector.state
    val missionProgress: StateFlow<MissionProgress> = missionEngine.progress

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private var startedAtMs = System.currentTimeMillis()
    private var lastTickNs = 0L
    private var batteryUsedWh = 0f
    private var homeNorthMeters = 0f
    private var homeEastMeters = 0f
    private var homeAltitudeMeters = 10f
    private var loiterNorthMeters = 0f
    private var loiterEastMeters = 0f
    private var guidedNorthMeters = 0f
    private var guidedEastMeters = 0f
    private var guidedAltitudeMeters = 10f
    @Volatile
    private var pilotInput = PilotInput(throttle = params.hoverThrottle)

    override fun start() {
        if (job != null) return
        startedAtMs = System.currentTimeMillis() - mutableState.value.uptimeMs.toLong()
        lastTickNs = System.nanoTime()
        job = scope.launch {
            while (isActive) {
                tick()
                delay(TICK_MS)
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    fun setArmed(armed: Boolean) {
        autopilot.setArmed(armed, mutableState.value)
        if (armed) {
            val current = mutableState.value
            homeNorthMeters = current.northMeters
            homeEastMeters = current.eastMeters
            homeAltitudeMeters = current.altitudeAglMeters.coerceAtLeast(8f)
            captureLoiterTarget(current)
        }
        mutableState.value = mutableState.value.copy(armed = autopilot.armed)
    }

    fun setMode(mode: FlightMode) {
        if (mode == FlightMode.LOITER) {
            captureLoiterTarget(mutableState.value)
        }
        autopilot.setMode(mode, mutableState.value)
        mutableState.value = mutableState.value.copy(mode = mode)
    }

    fun takeoff(targetAltitudeM: Float) {
        autopilot.takeoff(mutableState.value, targetAltitudeM)
        mutableState.value = mutableState.value.copy(
            armed = autopilot.armed,
            mode = autopilot.mode,
        )
    }

    fun land() {
        autopilot.land()
        mutableState.value = mutableState.value.copy(mode = FlightMode.LAND)
    }

    fun setPilotInput(input: PilotInput) {
        pilotInput = input.copy(
            roll = input.roll.coerceIn(-1f, 1f),
            pitch = input.pitch.coerceIn(-1f, 1f),
            throttle = input.throttle.coerceIn(0f, 1f),
            yaw = input.yaw.coerceIn(-1f, 1f),
        )
    }

    fun setGuidedTarget(latitudeDeg: Double, longitudeDeg: Double, altitudeAglMeters: Float) {
        val current = mutableState.value
        guidedNorthMeters = current.northMeters + ((latitudeDeg - current.latitudeDeg) * METERS_PER_LAT_DEG).toFloat()
        guidedEastMeters = current.eastMeters + ((longitudeDeg - current.longitudeDeg) * lonMetersPerDeg(current)).toFloat()
        guidedAltitudeMeters = altitudeAglMeters.coerceAtLeast(0f)
        autopilot.setTargetAltitude(guidedAltitudeMeters)
        autopilot.setMode(FlightMode.GUIDED, current)
        mutableState.value = current.copy(mode = FlightMode.GUIDED)
        noteAck("GUIDED TARGET")
    }

    fun setGuidedOffset(northMeters: Float, eastMeters: Float, altitudeAglMeters: Float) {
        val current = mutableState.value
        guidedNorthMeters = current.northMeters + northMeters
        guidedEastMeters = current.eastMeters + eastMeters
        guidedAltitudeMeters = altitudeAglMeters.coerceAtLeast(0f)
        autopilot.setTargetAltitude(guidedAltitudeMeters)
        autopilot.setMode(FlightMode.GUIDED, current)
        mutableState.value = current.copy(mode = FlightMode.GUIDED)
        noteAck("GUIDED OFFSET")
    }

    fun loadMission(items: List<MissionItem>) {
        missionEngine.load(items)
        missionEngine.progress.value.activeTarget?.let { autopilot.setTargetAltitude(it.altitudeAglMeters) }
        noteAck("MISSION ${items.size}")
    }

    fun loadDemoMission() {
        val current = mutableState.value
        loadMission(
            listOf(
                missionItem(current, sequence = 0, north = 8f, east = 0f, altitude = 8f),
                missionItem(current, sequence = 1, north = 8f, east = 8f, altitude = 10f),
                missionItem(current, sequence = 2, north = 0f, east = 8f, altitude = 10f),
                missionItem(current, sequence = 3, north = 0f, east = 0f, altitude = 6f),
            ),
        )
    }

    fun clearMission() {
        missionEngine.clear()
        noteAck("MISSION CLEAR")
    }

    fun noteInbound(message: String) {
        mutableState.value = mutableState.value.copy(lastInboundMessage = message)
    }

    fun noteAck(message: String) {
        mutableState.value = mutableState.value.copy(lastAck = message)
    }

    private fun tick() {
        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastTickNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastTickNs = nowNs

        val current = applyFailsafes(mutableState.value)
        val failures = failureInjector.state.value
        val navigationInput = activePilotInput(current, dt)
        val output = autopilot.computeMotorOutput(current, navigationInput, dt)
        val failedMotorSpeeds = failureInjector.applyMotorFailures(output.speeds)
        val environment = EnvironmentModel(windNedMS = failureInjector.windVectorNed())
        val physicsState = physics.step(current, failedMotorSpeeds, dt, environment)
        val batteryState = updateBattery(physicsState, output.throttle, dt, failures)
        val nowMs = System.currentTimeMillis()

        mutableState.value = applySensorTelemetry(
            batteryState.copy(
                armed = autopilot.armed,
                mode = autopilot.mode,
                uptimeMs = (nowMs - startedAtMs).coerceAtLeast(0L).toUInt(),
                throttlePercent = autopilot.throttlePercent(),
            ),
            failures,
        )
    }

    private fun activePilotInput(state: DroneState, dt: Float): PilotInput {
        return when {
            !state.armed -> PilotInput(throttle = 0f)
            autopilot.mode == FlightMode.LOITER -> {
                autopilot.setTargetAltitude(autopilot.targetAltitudeM.coerceAtLeast(state.altitudeAglMeters))
                positionController.computePilotInput(
                    state = state,
                    targetNorthMeters = loiterNorthMeters,
                    targetEastMeters = loiterEastMeters,
                    targetAltitudeMeters = autopilot.targetAltitudeM,
                    dt = dt,
                )
            }
            autopilot.mode == FlightMode.GUIDED -> {
                autopilot.setTargetAltitude(guidedAltitudeMeters)
                positionController.computePilotInput(
                    state = state,
                    targetNorthMeters = guidedNorthMeters,
                    targetEastMeters = guidedEastMeters,
                    targetAltitudeMeters = guidedAltitudeMeters,
                    dt = dt,
                )
            }
            autopilot.mode == FlightMode.AUTO -> {
                val progress = missionEngine.update(state)
                val target = progress.activeTarget
                if (target == null) {
                    captureLoiterTarget(state)
                    autopilot.setMode(FlightMode.LOITER, state)
                    PilotInput(throttle = 0.5f)
                } else {
                    val localTarget = localTargetFor(state, target)
                    autopilot.setTargetAltitude(target.altitudeAglMeters)
                    positionController.computePilotInput(
                        state = state,
                        targetNorthMeters = localTarget.x,
                        targetEastMeters = localTarget.y,
                        targetAltitudeMeters = target.altitudeAglMeters,
                        dt = dt,
                    )
                }
            }
            autopilot.mode == FlightMode.RTL -> {
                autopilot.setTargetAltitude(max(homeAltitudeMeters, 8f))
                positionController.computePilotInput(
                    state = state,
                    targetNorthMeters = homeNorthMeters,
                    targetEastMeters = homeEastMeters,
                    targetAltitudeMeters = autopilot.targetAltitudeM,
                    dt = dt,
                )
            }
            else -> pilotInput
        }
    }

    private fun applyFailsafes(state: DroneState): DroneState {
        val failures = failureInjector.state.value
        if (!failures.gpsEnabled &&
            (autopilot.mode == FlightMode.LOITER || autopilot.mode == FlightMode.GUIDED || autopilot.mode == FlightMode.AUTO)
        ) {
            autopilot.setMode(FlightMode.ALT_HOLD, state)
            noteAck("GPS FAILSAFE")
            return state.copy(mode = FlightMode.ALT_HOLD)
        }
        if (state.batteryRemainingPercent.toInt() <= 20 && autopilot.armed && autopilot.mode != FlightMode.RTL) {
            autopilot.setMode(FlightMode.RTL, state)
            noteAck("BATTERY RTL")
            return state.copy(mode = FlightMode.RTL)
        }
        return state
    }

    private fun applySensorTelemetry(state: DroneState, failures: FailureState): DroneState {
        val noisyNorth = if (failures.gpsEnabled) {
            state.northMeters + randomSigned(failures.gpsNoiseMultiplier - 1f) * 0.35f
        } else {
            state.northMeters
        }
        val noisyEast = if (failures.gpsEnabled) {
            state.eastMeters + randomSigned(failures.gpsNoiseMultiplier - 1f) * 0.35f
        } else {
            state.eastMeters
        }
        val northNoise = noisyNorth - state.northMeters
        val eastNoise = noisyEast - state.eastMeters
        val headingOffset = if (failures.compassEnabled) failures.compassOffsetDeg else 0f
        return state.copy(
            latitudeDeg = if (failures.gpsEnabled) state.latitudeDeg + northNoise / METERS_PER_LAT_DEG else state.latitudeDeg,
            longitudeDeg = if (failures.gpsEnabled) state.longitudeDeg + eastNoise / lonMetersPerDeg(state) else state.longitudeDeg,
            gpsFixType = if (failures.gpsEnabled) 3u else 0u,
            gpsSatellites = if (failures.gpsEnabled) 12u else 0u,
            headingDegrees = (((state.headingDegrees + headingOffset).toInt() % 360 + 360) % 360).toShort(),
            lastInboundMessage = when {
                !failures.gpsEnabled -> "GPS unavailable"
                failures.hasMotorFailure -> "Motor failure mask ${failures.motorFailureMask}"
                failures.windSpeedMs > 0f -> "Wind %.1f m/s".format(failures.windSpeedMs)
                else -> state.lastInboundMessage
            },
        )
    }

    private fun updateBattery(
        state: DroneState,
        throttle: Float,
        dt: Float,
        failures: FailureState,
    ): DroneState {
        val payloadFactor = 1f + failures.payloadMassKg * 0.18f
        val currentA = if (autopilot.armed) {
            (params.hoverCurrentDrawA * (0.25f + throttle * 1.5f) * failures.batteryDrainMultiplier * payloadFactor)
                .coerceAtLeast(0f)
        } else {
            0.15f
        }
        val previousRemaining = state.batteryRemainingPercent.toInt().coerceIn(0, 100) / 100f
        val voltage = params.batteryVoltageEmpty +
            (params.batteryVoltageFull - params.batteryVoltageEmpty) * previousRemaining
        batteryUsedWh = (batteryUsedWh + voltage * currentA * dt / 3600f)
            .coerceIn(0f, params.batteryCapacityWh)
        val remaining = (1f - batteryUsedWh / params.batteryCapacityWh).coerceIn(0f, 1f)
        return state.copy(
            armed = autopilot.armed,
            batteryVoltageMv = (params.batteryVoltageEmpty +
                (params.batteryVoltageFull - params.batteryVoltageEmpty) * remaining)
                .times(1000f)
                .roundToInt()
                .coerceIn(0, UShort.MAX_VALUE.toInt())
                .toUInt()
                .toUShort(),
            batteryCurrentCa = (currentA * 100f)
                .roundToInt()
                .coerceIn(0, Short.MAX_VALUE.toInt())
                .toShort(),
            batteryRemainingPercent = (remaining * 100f)
                .roundToInt()
                .coerceIn(0, 100)
                .toByte(),
        )
    }

    private fun captureLoiterTarget(state: DroneState) {
        loiterNorthMeters = state.northMeters
        loiterEastMeters = state.eastMeters
    }

    private fun localTargetFor(state: DroneState, item: MissionItem): Vector3 {
        val north = state.northMeters + ((item.latitudeDeg - state.latitudeDeg) * METERS_PER_LAT_DEG).toFloat()
        val east = state.eastMeters + ((item.longitudeDeg - state.longitudeDeg) * lonMetersPerDeg(state)).toFloat()
        return Vector3(x = north, y = east, z = item.altitudeAglMeters)
    }

    private fun missionItem(
        state: DroneState,
        sequence: Int,
        north: Float,
        east: Float,
        altitude: Float,
    ): MissionItem {
        return MissionItem(
            sequence = sequence,
            command = com.ascend.mavlab.simulation.mission.MissionCommand.WAYPOINT,
            latitudeDeg = state.latitudeDeg + north / METERS_PER_LAT_DEG,
            longitudeDeg = state.longitudeDeg + east / lonMetersPerDeg(state),
            altitudeAglMeters = altitude,
        )
    }

    private fun lonMetersPerDeg(state: DroneState): Double {
        return METERS_PER_LAT_DEG * max(0.2, cos(Math.toRadians(state.latitudeDeg)))
    }

    private fun randomSigned(scale: Float): Float {
        if (scale <= 0f) return 0f
        return ((Math.random().toFloat() * 2f) - 1f) * scale
    }

    private companion object {
        const val TICK_MS = 10L
        const val METERS_PER_LAT_DEG = 111_320.0
    }
}
