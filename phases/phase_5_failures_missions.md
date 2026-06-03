# Phase 5 — Failure Lab + Mission Engine

**Timeline:** Week 13–16  
**Depends on:** Phase 0–4  
**Produces:** Failure injection system, Auto mode with waypoint missions, mission upload from QGC, Guided mode (fly-to-point).

---

## 1. Goal

1. **Failure Lab:** Students inject realistic failures (GPS loss, wind, motor death, battery drain, compass interference) and observe the drone's response — teaching failsafe design and safety thinking
2. **Guided Mode:** Command the drone to fly to a specific GPS point
3. **Auto Mode + Mission Engine:** The drone follows a sequence of waypoints
4. **Loiter Mode:** Position hold using GPS (deferred from Phase 2)
5. **QGC Mission Upload:** QGC can upload a mission to MAVLab's sim; the drone executes it

---

## 2. Success Criteria

- [ ] Failure Lab UI has toggle/slider for each failure type
- [ ] Disabling GPS → drone can't hold position, mode changes to Alt Hold, QGC shows GPS warning
- [ ] Increasing wind → drone drifts, autopilot fights to hold position, visible in QGC
- [ ] Motor failure → drone becomes unstable, loses altitude
- [ ] Battery drain → when battery hits 20%, RTL auto-triggers (failsafe)
- [ ] Compass interference → heading drifts, position hold degrades
- [ ] Guided mode: send a GPS point → drone flies to it
- [ ] Auto mode: load a mission with 3+ waypoints → drone visits each sequentially
- [ ] QGC mission upload works: plan mission in QGC → upload → drone executes
- [ ] QGC mission download works: drone's current mission readable by QGC
- [ ] Loiter mode holds position within ±2m of target (with normal GPS noise)

---

## 3. New Files

```text
com/ascend/mavlab/
├── simulation/
│   ├── failures/
│   │   ├── FailureInjector.kt        # Master failure controller
│   │   └── FailureScenario.kt        # Pre-built failure scenarios
│   ├── autopilot/
│   │   ├── PositionController.kt     # XY position PID (for Loiter/Guided/Auto)
│   │   ├── NavigationController.kt   # Navigate to waypoint logic
│   │   └── MissionEngine.kt          # Waypoint sequencer
│   └── mission/
│       ├── Mission.kt                # Mission data structure
│       ├── MissionItem.kt            # Single waypoint
│       └── MissionStorage.kt         # In-memory mission storage
├── mavlink/
│   └── MavlinkCommandHandler.kt      # Handle MAVLink mission protocol
├── feature/
│   ├── failure_lab/
│   │   ├── FailureLabScreen.kt       # Failure injection UI
│   │   ├── FailureLabViewModel.kt
│   │   ├── FailureToggle.kt          # Toggle + slider for each failure
│   │   └── FailureScenarioCard.kt    # Pre-built scenario selector
│   └── mission_lab/
│       ├── MissionLabScreen.kt       # Mission visualization
│       ├── MissionLabViewModel.kt
│       └── WaypointList.kt           # List of waypoints with status
```

---

## 4. Failure Injection System

### 4.1 FailureInjector.kt

```kotlin
package com.ascend.mavlab.simulation.failures

import com.ascend.mavlab.simulation.physics.PhysicsModel
import com.ascend.mavlab.simulation.sensors.SensorModel
import com.ascend.mavlab.simulation.physics.Vector3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

/**
 * Central controller for all failure injection.
 *
 * Each failure parameter is observable via StateFlow so the UI can reflect it.
 * The SimulationEngine reads these values each physics step.
 */
class FailureInjector {

    // --- GPS Failures ---
    val gpsEnabled = MutableStateFlow(true)
    val gpsNoiseMultiplier = MutableStateFlow(1.0f)  // 1.0 = normal, 5.0 = very noisy
    // GPS glitch: sudden position jump
    val gpsGlitchOffsetMeters = MutableStateFlow(Vector3.ZERO)

    // --- Compass Failures ---
    val compassEnabled = MutableStateFlow(true)
    val compassOffsetDeg = MutableStateFlow(0f)  // static heading offset (interference)

    // --- Wind ---
    val windSpeedMs = MutableStateFlow(0f)
    val windDirectionDeg = MutableStateFlow(0f)  // 0 = from North
    val windGustsMs = MutableStateFlow(0f)       // random gust amplitude

    // --- Motor Failures ---
    // Bitmask: bit 0 = motor 1, bit 1 = motor 2, etc.
    // 1 = failed (0% output), 0 = working
    val motorFailureMask = MutableStateFlow(0)

    // --- Battery ---
    val batteryDrainMultiplier = MutableStateFlow(1.0f)  // 1.0 = normal, 10.0 = fast drain

    // --- Vibration ---
    val vibrationLevel = MutableStateFlow(0f)  // m/s² added to IMU

    // --- Payload ---
    val payloadMassKg = MutableStateFlow(0f)   // additional mass

    /**
     * Apply failures to the sensor model before autopilot reads sensors.
     */
    fun applySensorFailures(sensorModel: SensorModel) {
        sensorModel.gpsEnabled = gpsEnabled.value
        sensorModel.gpsNoiseMeters = 0.5f * gpsNoiseMultiplier.value
        sensorModel.compassEnabled = compassEnabled.value
        sensorModel.compassNoiseDeg = 2f + compassOffsetDeg.value
    }

    /**
     * Get current wind vector in NED frame.
     */
    fun getWindVector(): Vector3 {
        val dir = Math.toRadians(windDirectionDeg.value.toDouble())
        val speed = windSpeedMs.value
        val gust = windGustsMs.value * (Math.random().toFloat() * 2 - 1)
        val totalSpeed = speed + gust

        return Vector3(
            x = -(totalSpeed * cos(dir)).toFloat(),  // North component
            y = -(totalSpeed * sin(dir)).toFloat(),  // East component
            z = 0f
        )
    }

    /**
     * Apply motor failures to motor speed array.
     * Failed motors produce 0 thrust.
     */
    fun applyMotorFailures(motorSpeeds: FloatArray): FloatArray {
        val mask = motorFailureMask.value
        return FloatArray(4) { i ->
            if (mask and (1 shl i) != 0) 0f else motorSpeeds[i]
        }
    }

    /**
     * Get effective mass including payload.
     */
    fun getEffectiveMass(baseMass: Float): Float {
        return baseMass + payloadMassKg.value
    }

    /**
     * Reset all failures to normal.
     */
    fun resetAll() {
        gpsEnabled.value = true
        gpsNoiseMultiplier.value = 1f
        gpsGlitchOffsetMeters.value = Vector3.ZERO
        compassEnabled.value = true
        compassOffsetDeg.value = 0f
        windSpeedMs.value = 0f
        windDirectionDeg.value = 0f
        windGustsMs.value = 0f
        motorFailureMask.value = 0
        batteryDrainMultiplier.value = 1f
        vibrationLevel.value = 0f
        payloadMassKg.value = 0f
    }
}
```

### 4.2 Pre-Built Failure Scenarios

```kotlin
package com.ascend.mavlab.simulation.failures

/**
 * Curated failure scenarios for structured learning.
 * Each scenario configures the FailureInjector to a specific state
 * and provides educational context.
 */
data class FailureScenario(
    val id: String,
    val title: String,
    val description: String,
    val learningGoal: String,
    val whatToObserve: List<String>,
    val apply: (FailureInjector) -> Unit,
)

val FAILURE_SCENARIOS = listOf(
    FailureScenario(
        id = "gps_loss",
        title = "GPS Signal Lost",
        description = "Simulates flying under a bridge or near jamming.",
        learningGoal = "Understand how the autopilot degrades when GPS is unavailable.",
        whatToObserve = listOf(
            "Mode automatically changes from Loiter to Alt Hold",
            "Drone can no longer hold position",
            "QGC shows GPS warning icon",
            "The drone still holds altitude (barometer is independent)",
        ),
        apply = { it.gpsEnabled.value = false }
    ),
    FailureScenario(
        id = "gps_drift",
        title = "GPS Drift / Multipath",
        description = "GPS signal bouncing off buildings causes position errors.",
        learningGoal = "Learn why GPS accuracy matters for autonomous flight.",
        whatToObserve = listOf(
            "Drone position wanders even in Loiter mode",
            "QGC shows the drone jittering on the map",
            "HDOP value increases in GPS_RAW_INT",
        ),
        apply = { it.gpsNoiseMultiplier.value = 5f }
    ),
    FailureScenario(
        id = "windy_day",
        title = "Strong Wind (8 m/s)",
        description = "Constant wind from the east, like flying near the coast.",
        learningGoal = "See how the autopilot compensates for external disturbances.",
        whatToObserve = listOf(
            "Drone tilts into the wind to hold position",
            "In Stabilize mode, drone drifts downwind rapidly",
            "In Loiter mode, autopilot fights the wind (visible tilt)",
            "Battery drains faster due to higher throttle",
        ),
        apply = {
            it.windSpeedMs.value = 8f
            it.windDirectionDeg.value = 90f  // from East
            it.windGustsMs.value = 2f
        }
    ),
    FailureScenario(
        id = "motor_failure",
        title = "Motor 3 Failure",
        description = "One motor stops producing thrust mid-flight.",
        learningGoal = "Understand why quadcopters have limited redundancy.",
        whatToObserve = listOf(
            "Drone becomes unstable, tilts toward failed motor",
            "Autopilot can partially compensate but performance degrades",
            "Altitude drops as total thrust decreases",
            "This is why hexacopters are preferred for professional use",
        ),
        apply = { it.motorFailureMask.value = 0b0100 } // Motor 3
    ),
    FailureScenario(
        id = "battery_low",
        title = "Battery Critical (Fast Drain)",
        description = "Battery drains 10x faster than normal.",
        learningGoal = "Understand battery failsafe behavior.",
        whatToObserve = listOf(
            "Battery percentage drops rapidly in QGC",
            "At 30%: 'Battery Low' warning text",
            "At 20%: RTL failsafe triggers automatically",
            "Drone returns to home and lands",
        ),
        apply = { it.batteryDrainMultiplier.value = 10f }
    ),
    FailureScenario(
        id = "compass_interference",
        title = "Compass Interference",
        description = "Large metal structure nearby disrupts magnetometer.",
        learningGoal = "Learn why compass calibration matters.",
        whatToObserve = listOf(
            "Heading display in QGC drifts from true heading",
            "In Loiter mode, drone may fly in circles ('toilet bowl')",
            "Yaw commands don't go where expected",
        ),
        apply = {
            it.compassOffsetDeg.value = 45f  // 45° static offset
        }
    ),
    FailureScenario(
        id = "heavy_payload",
        title = "Heavy Payload (1 kg extra)",
        description = "Picking up a package adds weight.",
        learningGoal = "Understand thrust-to-weight ratio.",
        whatToObserve = listOf(
            "Drone needs higher throttle to hover",
            "Climb rate is reduced",
            "Battery drains faster",
            "Maximum tilt angle has less effect on speed",
        ),
        apply = { it.payloadMassKg.value = 1.0f }
    ),
)
```

### 4.3 Failure Lab Screen

```text
┌──────────────────────────────────────────┐
│  Failure Lab                             │
│  ─────────────────────────────────────── │
│  Quick Scenarios:                        │
│  [GPS Lost] [Windy Day] [Motor Fail]     │
│  [Battery Low] [Compass Error] [Payload] │
│  ─────────────────────────────────────── │
│  Manual Controls:                        │
│                                          │
│  GPS Enabled         [ON ●────]          │
│  GPS Noise           [──●─────] 1.0x     │
│  Compass Enabled     [ON ●────]          │
│  Compass Offset      [●──────] 0°        │
│  Wind Speed          [●──────] 0 m/s     │
│  Wind Direction      [────●──] 90°       │
│  Wind Gusts          [●──────] 0 m/s     │
│  Motor 1             [ON ●────]          │
│  Motor 2             [ON ●────]          │
│  Motor 3             [ON ●────]          │
│  Motor 4             [ON ●────]          │
│  Battery Drain       [──●────] 1.0x      │
│  Payload Mass        [●──────] 0 kg      │
│  ─────────────────────────────────────── │
│  [RESET ALL]                             │
└──────────────────────────────────────────┘
```

---

## 5. Mission Engine

### 5.1 Data Structures

```kotlin
package com.ascend.mavlab.simulation.mission

/**
 * A mission is a sequence of waypoints the drone follows in Auto mode.
 */
data class Mission(
    val items: List<MissionItem> = emptyList(),
    var currentIndex: Int = 0,
) {
    val currentItem: MissionItem? get() = items.getOrNull(currentIndex)
    val isComplete: Boolean get() = currentIndex >= items.size
    fun advance() { currentIndex++ }
    fun reset() { currentIndex = 0 }
}

data class MissionItem(
    val sequence: Int,           // 0-indexed sequence number
    val command: MissionCommand,
    val latitude: Double,        // WGS84 degrees
    val longitude: Double,
    val altitude: Float,         // meters AGL
    val param1: Float = 0f,      // command-specific
    val param2: Float = 0f,
    val param3: Float = 0f,
    val param4: Float = 0f,
    val autocontinue: Boolean = true,
)

enum class MissionCommand(val mavCmdId: Int) {
    WAYPOINT(16),       // MAV_CMD_NAV_WAYPOINT: fly to lat/lon/alt
    TAKEOFF(22),        // MAV_CMD_NAV_TAKEOFF: takeoff to altitude
    LAND(21),           // MAV_CMD_NAV_LAND: land at lat/lon
    RTL(20),            // MAV_CMD_NAV_RETURN_TO_LAUNCH
    LOITER_TIME(19),    // MAV_CMD_NAV_LOITER_TIME: hold for N seconds
}
```

### 5.2 MissionEngine.kt

```kotlin
package com.ascend.mavlab.simulation.autopilot

import com.ascend.mavlab.simulation.DroneState
import com.ascend.mavlab.simulation.mission.*
import kotlin.math.*

/**
 * Executes a mission by feeding waypoints to the NavigationController.
 *
 * Logic for each step:
 *   1. Get current mission item
 *   2. Compute distance to waypoint
 *   3. If within acceptance radius (2m) → advance to next item
 *   4. If last item → mission complete, switch to Loiter
 *
 * The MissionEngine provides a target position to the Autopilot.
 * The Autopilot's Guided/Auto mode uses the PositionController
 * to navigate toward the target.
 */
class MissionEngine {
    var mission: Mission = Mission()
        private set

    var targetLat: Double = 0.0
        private set
    var targetLon: Double = 0.0
        private set
    var targetAlt: Float = 0f
        private set

    val isActive: Boolean get() = !mission.isComplete
    val currentWaypointIndex: Int get() = mission.currentIndex

    fun loadMission(newMission: Mission) {
        mission = newMission
        if (mission.items.isNotEmpty()) {
            updateTarget(mission.items[0])
        }
    }

    /**
     * Called every autopilot cycle to check if the current waypoint is reached.
     * @return true if mission is still active, false if complete
     */
    fun update(currentState: DroneState): Boolean {
        val item = mission.currentItem ?: return false

        val distance = haversineDistance(
            currentState.latitude, currentState.longitude,
            item.latitude, item.longitude
        )
        val altError = abs(currentState.altitudeAGL - item.altitude)

        val acceptanceRadius = 2.0  // meters
        val altAcceptance = 1.0f    // meters

        if (distance < acceptanceRadius && altError < altAcceptance) {
            mission.advance()
            val nextItem = mission.currentItem
            if (nextItem != null) {
                updateTarget(nextItem)
            }
            return !mission.isComplete
        }

        return true
    }

    private fun updateTarget(item: MissionItem) {
        targetLat = item.latitude
        targetLon = item.longitude
        targetAlt = item.altitude
    }

    /**
     * Haversine distance between two GPS points in meters.
     */
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
```

### 5.3 PositionController.kt — For Loiter/Guided/Auto

```kotlin
package com.ascend.mavlab.simulation.autopilot

import com.ascend.mavlab.simulation.DroneState
import kotlin.math.*

/**
 * Position controller for Loiter, Guided, and Auto modes.
 *
 * Outer loop in the control cascade:
 *   Position error → desired velocity → desired attitude → rate PID → motors
 *
 * @param positionPID Controls desired velocity from position error
 * @param velocityPID Controls desired attitude from velocity error (optional second layer)
 */
class PositionController {
    val northPID = PIDController(kP = 0.8f, kI = 0.1f, kD = 0.3f, iMax = 2f, outputMin = -5f, outputMax = 5f)
    val eastPID = PIDController(kP = 0.8f, kI = 0.1f, kD = 0.3f, iMax = 2f, outputMin = -5f, outputMax = 5f)

    /**
     * Given a target GPS position, compute desired pitch and roll angles.
     *
     * @return Pair(desiredPitch, desiredRoll) in radians
     */
    fun computeDesiredAttitude(
        currentState: DroneState,
        targetLat: Double,
        targetLon: Double,
        dt: Float,
        maxTilt: Float = 0.35f  // ~20°
    ): Pair<Float, Float> {
        // Convert lat/lon error to meters (NED)
        val northError = ((targetLat - currentState.latitude) * 111320).toFloat()
        val eastError = ((targetLon - currentState.longitude) *
            111320 * cos(Math.toRadians(currentState.latitude))).toFloat()

        // Position PID → desired velocity
        val desiredVelN = northPID.update(northError, dt)
        val desiredVelE = eastPID.update(eastError, dt)

        // Velocity → desired tilt angle
        // Simplified: desired pitch ≈ -velocity_north / g, desired roll ≈ velocity_east / g
        // (this is a linear approximation valid for small angles)
        val desiredPitch = -(desiredVelN / 9.81f).coerceIn(-maxTilt, maxTilt)
        val desiredRoll = (desiredVelE / 9.81f).coerceIn(-maxTilt, maxTilt)

        // Rotate by yaw to convert from NED to body frame
        val cosYaw = cos(currentState.yaw)
        val sinYaw = sin(currentState.yaw)
        val bodyPitch = desiredPitch * cosYaw + desiredRoll * sinYaw
        val bodyRoll = -desiredPitch * sinYaw + desiredRoll * cosYaw

        return Pair(bodyPitch, bodyRoll)
    }

    fun reset() {
        northPID.reset()
        eastPID.reset()
    }
}
```

### 5.4 MAVLink Mission Protocol Handler

QGC uses the MAVLink mission protocol to upload/download missions. The handler must implement:

```text
MISSION UPLOAD (QGC → MAVLab):
  1. QGC sends MISSION_COUNT (number of items)
  2. MAVLab responds MISSION_REQUEST_INT (request item 0)
  3. QGC sends MISSION_ITEM_INT (item 0 data)
  4. MAVLab responds MISSION_REQUEST_INT (request item 1)
  5. ... repeat until all items received
  6. MAVLab sends MISSION_ACK (success)

MISSION DOWNLOAD (MAVLab → QGC):
  1. QGC sends MISSION_REQUEST_LIST
  2. MAVLab responds MISSION_COUNT
  3. QGC sends MISSION_REQUEST_INT (request item 0)
  4. MAVLab responds MISSION_ITEM_INT (item 0)
  5. ... repeat until all items sent

MISSION PROGRESS:
  - MAVLab sends MISSION_CURRENT (current waypoint index) at 1 Hz
  - MAVLab sends MISSION_ITEM_REACHED when a waypoint is reached
```

---

## 6. Autopilot Integration

Update the `Autopilot.kt` from Phase 2 to add Loiter, Guided, and Auto modes:

```kotlin
// Add to Autopilot.computeMotorSpeeds() switch statement:

FlightMode.LOITER -> {
    // Hold current position using GPS
    val (desPitch, desRoll) = positionController.computeDesiredAttitude(
        state, loiterTargetLat, loiterTargetLon, dt
    )
    desiredRoll = desRoll + pilotRoll * params.maxTiltAngle * 0.5f  // pilot override
    desiredPitch = desPitch + pilotPitch * params.maxTiltAngle * 0.5f
    desiredYawRate = pilotYaw * 1.5f
    val altError = targetAltitude - state.altitudeAGL
    throttleCommand = 0.5f + altPID.update(altError, dt) * 0.3f
}

FlightMode.GUIDED -> {
    // Fly to commanded position
    val (desPitch, desRoll) = positionController.computeDesiredAttitude(
        state, guidedTargetLat, guidedTargetLon, dt
    )
    desiredRoll = desRoll
    desiredPitch = desPitch
    desiredYawRate = 0f
    val altError = guidedTargetAlt - state.altitudeAGL
    throttleCommand = 0.5f + altPID.update(altError, dt) * 0.3f
}

FlightMode.AUTO -> {
    // Follow mission
    if (missionEngine.isActive) {
        missionEngine.update(state)
        val (desPitch, desRoll) = positionController.computeDesiredAttitude(
            state, missionEngine.targetLat, missionEngine.targetLon, dt
        )
        desiredRoll = desRoll
        desiredPitch = desPitch
        desiredYawRate = 0f
        val altError = missionEngine.targetAlt - state.altitudeAGL
        throttleCommand = 0.5f + altPID.update(altError, dt) * 0.3f
    } else {
        // Mission complete → switch to Loiter
        setMode(FlightMode.LOITER, state)
    }
}
```

---

## 7. Failsafe System

```kotlin
/**
 * Add to SimulationEngine.step():
 *
 * Check failsafes after each physics step:
 *   - Battery < 20% → trigger RTL
 *   - GPS lost in Loiter/Auto → downgrade to Alt Hold
 *   - Motor failure detected → STATUSTEXT warning
 */
fun checkFailsafes(state: DroneState) {
    // Battery failsafe
    if (state.batteryRemaining <= 20 && autopilot.mode != FlightMode.RTL
        && autopilot.mode != FlightMode.LAND) {
        autopilot.setMode(FlightMode.RTL, state)
        sendStatusText("Battery low! RTL triggered", severity = 2) // MAV_SEVERITY_CRITICAL
    }

    // GPS failsafe
    if (!failureInjector.gpsEnabled.value) {
        if (autopilot.mode == FlightMode.LOITER || autopilot.mode == FlightMode.AUTO) {
            autopilot.setMode(FlightMode.ALT_HOLD, state)
            sendStatusText("GPS lost! Switching to Alt Hold", severity = 3) // MAV_SEVERITY_ERROR
        }
    }
}
```

---

## 8. Testing Plan

### Failure Lab
1. Toggle GPS off → mode changes to Alt Hold, QGC shows warning
2. Set wind to 8 m/s → visible drone drift in QGC, drone tilts to compensate
3. Disable motor 3 → drone tilts, loses altitude
4. Set battery drain to 10x → RTL triggers at 20%
5. Set compass offset to 45° → heading drift visible in QGC
6. Add 1 kg payload → hover throttle increases
7. Reset all → all values return to normal

### Missions
1. Create 3-waypoint mission in QGC → upload → drone follows them
2. Mission progress shown in QGC (current waypoint highlighted)
3. Mission completes → drone switches to Loiter
4. Download mission from MAVLab → QGC displays it correctly

### Loiter + Guided
1. Switch to Loiter → drone holds position within ±2m
2. Send Guided command with target point → drone flies there
3. Arrive at target → drone holds position

---

## 9. Definition of Done

- [x] Failure Lab UI with toggles/sliders for all failure types
- [x] 7 pre-built failure scenarios with educational descriptions
- [x] GPS loss triggers mode downgrade
- [x] Battery failsafe triggers RTL at 20%
- [x] Wind affects drone behavior realistically
- [x] Motor failure causes instability
- [x] Loiter mode holds position using GPS
- [x] Guided mode navigates to commanded point
- [x] Auto mode follows waypoint mission
- [x] QGC mission upload/download works
- [x] All prior phase functionality intact
