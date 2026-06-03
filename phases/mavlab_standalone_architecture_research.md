# MAVLab — Standalone Architecture Research

**Goal:** Everything runs on the phone (Android) or in the browser (webapp). No Docker, no cloud servers, no Python bridge.  
**License:** MIT  
**GCS:** QGroundControl (Android or Desktop)  
**Date:** June 2026

---

## Table of Contents

1. [Architectural Shift — Why Standalone](#1-architectural-shift--why-standalone)
2. [The Core Problem — Running SITL on a Phone](#2-the-core-problem--running-sitl-on-a-phone)
3. [All Viable Approaches — Evaluated](#3-all-viable-approaches--evaluated)
4. [Recommended Architecture — Built-In Simulation Engine](#4-recommended-architecture--built-in-simulation-engine)
5. [Simulation Engine Design — Detailed](#5-simulation-engine-design--detailed)
6. [MAVLink Integration — The Phone IS the Drone](#6-mavlink-integration--the-phone-is-the-drone)
7. [QGroundControl Connection — How It Works](#7-qgroundcontrol-connection--how-it-works)
8. [Webapp Variant — Browser-Based Architecture](#8-webapp-variant--browser-based-architecture)
9. [Android App Architecture — Standalone](#9-android-app-architecture--standalone)
10. [Sensor Integration — Phone as Physical Interface](#10-sensor-integration--phone-as-physical-interface)
11. [3D Visualization](#11-3d-visualization)
12. [Education Modules — Offline-First](#12-education-modules--offline-first)
13. [Failure Injection — Built-In](#13-failure-injection--built-in)
14. [Licensing — MIT](#14-licensing--mit)
15. [Technology Stack Summary](#15-technology-stack-summary)
16. [Phased Build Plan](#16-phased-build-plan)
17. [Open Questions](#17-open-questions)
18. [References](#18-references)

---

## 1. Architectural Shift — Why Standalone

### 1.1 Previous Architecture (Rejected)

```text
Phone App → [WebSocket] → Python Bridge → [MAVLink UDP] → ArduPilot SITL (Docker/Cloud)
                                                                    ↓
                                                            QGroundControl
```

**Problems with this:**
- Requires a server (cloud cost, setup complexity)
- Requires Docker (heavy, intimidating for students)
- Requires internet connection (fragile in Africa)
- Multiple moving parts = more things break
- Not truly portable — can't learn on a bus with just your phone

### 1.2 New Architecture (Target)

```text
┌─────────────────────────────────────────┐
│          Android Phone / Browser         │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │         MAVLab App                 │  │
│  │                                    │  │
│  │  ┌──────────┐  ┌───────────────┐  │  │
│  │  │ Sim      │  │ Education     │  │  │
│  │  │ Engine   │  │ Dashboard     │  │  │
│  │  │ (physics │  │ (3D, charts,  │  │  │
│  │  │  + auto  │  │  lessons,     │  │  │
│  │  │  pilot)  │  │  MAVLink      │  │  │
│  │  │          │  │  explorer)    │  │  │
│  │  └────┬─────┘  └───────────────┘  │  │
│  │       │ MAVLink (loopback UDP)    │  │
│  │       │                            │  │
│  │  ┌────▼────────────────────────┐  │  │
│  │  │ MAVLink Server              │  │  │
│  │  │ Broadcasts to local network │  │  │
│  │  └────┬────────────────────────┘  │  │
│  └───────┼────────────────────────────┘  │
└──────────┼───────────────────────────────┘
           │ MAVLink UDP (Wi-Fi)
           ▼
  ┌──────────────────┐
  │ QGroundControl    │
  │ (Same phone,      │
  │  tablet, or PC)   │
  └──────────────────┘
```

**Benefits:**
- **Zero infrastructure** — no servers, no Docker, no internet needed
- **Fully offline** — works on a bus, in a village, anywhere
- **Single app** — install MAVLab, install QGC, done
- **MIT licensed** — maximum adoption, zero friction
- **Phone IS the drone** — physically tilt the phone to fly

---

## 2. The Core Problem — Running SITL on a Phone

### 2.1 Why ArduPilot SITL Can't Simply Run on Android

ArduPilot SITL is a full autopilot compiled as a native Linux binary (~200MB+ with dependencies). The challenges:

| Challenge | Severity | Detail |
|-----------|----------|--------|
| **Architecture** | Medium | SITL is x86-focused, phones are ARM64. But ArduPilot *can* compile for ARM (it runs on Pixhawk ARM boards and Raspberry Pi). |
| **Android != Linux** | High | Android uses Bionic libc (not glibc), has SELinux restrictions, W^X execution policies (Android 10+), and no standard POSIX environment. |
| **Binary execution** | High | Android blocks execution of binaries from writable directories. You must package them as `.so` files in `jniLibs/` and execute from the native library dir. |
| **Size** | Medium | Full ArduPilot build is large. But SITL-only could be trimmed. |
| **Process lifecycle** | High | Android kills background processes aggressively. A SITL binary as a subprocess is fragile. |
| **Dependencies** | High | SITL depends on many Linux libraries (pthreads, math, networking) that behave differently on Android. |

### 2.2 Verdict on Native ArduPilot SITL on Android

**Possible in theory, extremely fragile in practice.** Cross-compiling SITL for Android NDK is technically feasible (compile with NDK Clang, package as `.so`, execute via `ProcessBuilder`), but:

- It would be a maintenance nightmare (tracking ArduPilot updates + Android NDK changes)
- Android's process killing would make it unreliable
- The binary would be 50-100MB+ per architecture
- No one in the ArduPilot community has maintained this path
- It violates Android's design philosophy

**This approach is NOT recommended for production.**

---

## 3. All Viable Approaches — Evaluated

### 3.1 Approach Comparison

| # | Approach | Feasibility | Complexity | Educational Value | Offline | Recommended |
|---|---------|-------------|-----------|-------------------|---------|-------------|
| A | Cross-compile ArduPilot SITL for Android NDK | ⚠️ Fragile | 🔴 Extreme | ⭐⭐⭐⭐⭐ (real ArduPilot) | ✅ | ❌ |
| B | **Built-in simulation engine (Kotlin/JS) + MAVLink output** | ✅ Solid | 🟡 Medium | ⭐⭐⭐⭐ | ✅ | ✅ **YES** |
| C | Embed Hackflight (C++) via NDK/JNI | ✅ Feasible | 🟡 Medium | ⭐⭐⭐ | ✅ | ⚠️ Backup |
| D | Embed JSBSim via NDK/JNI | ✅ Feasible | 🔴 High | ⭐⭐⭐ (FW focus) | ✅ | ❌ Overkill |
| E | WebAssembly physics in browser | ✅ Solid | 🟡 Medium | ⭐⭐⭐⭐ | ✅ | ✅ For webapp |
| F | Pure mock MAVLink (no physics) | ✅ Easy | 🟢 Low | ⭐⭐ | ✅ | ❌ Not enough |

### 3.2 Why Approach B Wins

**Build our own quadcopter simulation engine in Kotlin (Android) / JavaScript (webapp) that:**

1. Simulates realistic quadcopter physics (6-DOF dynamics, gravity, thrust, drag)
2. Runs a simplified autopilot (PID controllers for stabilization + flight modes)
3. Outputs MAVLink telemetry on a UDP port
4. Accepts MAVLink commands (arm, takeoff, mode changes, manual control)
5. QGroundControl connects to it and thinks it's a real drone

**Why this is the right approach:**

- **Self-contained** — runs entirely in the app process, no subprocess hacking
- **Maintainable** — we own the code, it's Kotlin, not fighting ArduPilot's build system
- **Educational** — we can expose ALL internals (physics, PID gains, sensor model) to students
- **MAVLink compatible** — QGC connects normally, students learn real MAVLink
- **Accurate enough** — educational physics doesn't need ArduPilot-level fidelity. A well-tuned 6-DOF model with PID is sufficient to teach all the concepts
- **Phone sensors** — we can feed real phone sensor data into the simulation
- **Portable** — same physics engine works on Android (Kotlin) and browser (JS/WASM)

**What we lose vs. real SITL:**
- Not running actual ArduPilot code (but we teach the same concepts)
- Simplified EKF (but sufficient for education)
- Fewer flight modes initially (we implement what we need)
- No real ArduPilot parameter system (but we build our own for education)

**The tradeoff is worth it.** The goal is education, not flight certification.

---

## 4. Recommended Architecture — Built-In Simulation Engine

### 4.1 Complete System Architecture

```text
┌────────────────────────────────────────────────────────────┐
│                    MAVLab App (Android / Web)               │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Simulation Engine                        │  │
│  │                                                       │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐ │  │
│  │  │ Physics     │  │ Autopilot   │  │ Sensor       │ │  │
│  │  │ Model       │  │ (PID +      │  │ Model        │ │  │
│  │  │             │  │  flight     │  │              │ │  │
│  │  │ • 6-DOF     │  │  modes)     │  │ • GPS sim    │ │  │
│  │  │ • gravity   │  │             │  │ • IMU sim    │ │  │
│  │  │ • thrust    │  │ • Stabilize │  │ • Compass    │ │  │
│  │  │ • drag      │  │ • Alt Hold  │  │ • Barometer  │ │  │
│  │  │ • wind      │  │ • Loiter    │  │ • Noise      │ │  │
│  │  │ • inertia   │  │ • Guided    │  │              │ │  │
│  │  │             │  │ • Auto      │  │ OR           │ │  │
│  │  │             │  │ • RTL       │  │              │ │  │
│  │  │             │  │ • Land      │  │ • Real phone │ │  │
│  │  │             │  │             │  │   sensors    │ │  │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬───────┘ │  │
│  │         │                │                │          │  │
│  │         └────────────────┼────────────────┘          │  │
│  │                          │                            │  │
│  │                   ┌──────▼──────┐                     │  │
│  │                   │ Drone State │                     │  │
│  │                   │ (position,  │                     │  │
│  │                   │  attitude,  │                     │  │
│  │                   │  velocity,  │                     │  │
│  │                   │  battery,   │                     │  │
│  │                   │  mode, ...)│                     │  │
│  │                   └──────┬──────┘                     │  │
│  └──────────────────────────┼────────────────────────────┘  │
│                              │                               │
│         ┌────────────────────┼────────────────────┐         │
│         │                    │                    │         │
│    ┌────▼──────┐    ┌───────▼──────┐    ┌────────▼──────┐  │
│    │ MAVLink   │    │ Education    │    │ 3D           │  │
│    │ Server    │    │ Dashboard    │    │ Visualizer   │  │
│    │           │    │              │    │              │  │
│    │ UDP:14550 │    │ • Telemetry  │    │ • Drone      │  │
│    │ broadcast │    │ • MAVLink    │    │   model      │  │
│    │ to LAN    │    │   explorer   │    │ • Attitude   │  │
│    │           │    │ • PID tuner  │    │ • Path trail │  │
│    │           │    │ • Failure    │    │ • Waypoints  │  │
│    │           │    │   lab        │    │              │  │
│    │           │    │ • Lessons    │    │              │  │
│    └───────────┘    └──────────────┘    └──────────────┘  │
│                                                            │
└────────────────────────────────────────────────────────────┘
           │
           │ MAVLink UDP (loopback or Wi-Fi)
           ▼
  ┌──────────────────┐
  │ QGroundControl    │
  │ (connects to      │
  │  MAVLab's UDP     │
  │  port 14550)      │
  └──────────────────┘
```

### 4.2 How QGC Connects

1. MAVLab starts its built-in simulation engine
2. The engine runs at 50-100 Hz internally (physics updates)
3. A MAVLink server sends `HEARTBEAT` at 1 Hz + telemetry at negotiated rates
4. The MAVLink server binds to a known local UDP port and sends to `127.0.0.1:14550`, explicit QGC peer IPs, or a valid subnet broadcast address
5. QGroundControl (on the same phone, a tablet, or a PC on the same Wi-Fi) auto-detects the vehicle
6. QGC shows the simulated drone on its map, with all standard telemetry
7. Students can send commands from QGC (mode changes, missions, arm/disarm)
8. MAVLab receives the MAVLink commands and the autopilot processes them

**From QGC's perspective, MAVLab IS a real drone.** It speaks correct MAVLink, responds to commands, and streams proper telemetry. QGC doesn't know or care that the physics are simulated.

---

## 5. Simulation Engine Design — Detailed

### 5.1 Physics Model — 6-DOF Quadcopter

The simulation models a quadcopter using Newton-Euler equations of motion.

**State Vector:**
```text
Position:    [x, y, z]         (meters, NED frame)
Velocity:    [vx, vy, vz]     (m/s, NED frame)
Attitude:    [φ, θ, ψ]        (roll, pitch, yaw in radians)
             or quaternion [w, x, y, z]
Ang Velocity:[p, q, r]        (rad/s, body frame)
```

**Forces and Torques:**
```text
Thrust = Σ(motor_speed² × k_thrust)     → body Z axis (upward)
Gravity = mass × g                        → NED Z axis (downward)
Drag = -k_drag × velocity²               → opposing motion
Wind = [wx, wy, wz]                      → NED frame disturbance

Torque_roll  = (motor2² - motor4²) × L × k_thrust
Torque_pitch = (motor1² - motor3²) × L × k_thrust
Torque_yaw   = (motor1² + motor3² - motor2² - motor4²) × k_torque
```

**Integration:** 4th-order Runge-Kutta at 100 Hz

**Vehicle Parameters:**
```kotlin
data class QuadcopterParams(
    val mass: Float = 1.5f,            // kg
    val armLength: Float = 0.25f,       // meters
    val inertiaXX: Float = 0.0347f,     // kg·m²
    val inertiaYY: Float = 0.0347f,     // kg·m²
    val inertiaZZ: Float = 0.0977f,     // kg·m²
    val thrustCoeff: Float = 1.0e-5f,   // N/(rad/s)²
    val torqueCoeff: Float = 1.0e-7f,   // N·m/(rad/s)²
    val dragCoeff: Float = 0.1f,        // N·s/m
    val motorMaxRPM: Float = 10000f,
    val motorMinRPM: Float = 0f,
    val batteryCapacityMah: Float = 5000f,
    val batteryVoltageFull: Float = 12.6f,
    val batteryVoltageEmpty: Float = 10.0f,
)
```

### 5.2 Autopilot — Simplified PID-Based Flight Controller

The autopilot implements a cascaded PID controller structure:

```text
Position Controller (Loiter/Guided/Auto modes)
    ↓ desired velocity
Velocity Controller
    ↓ desired attitude
Attitude Controller
    ↓ desired angular rate
Rate Controller
    ↓ motor commands
Motor Mixer
    ↓ individual motor speeds
Physics Engine
```

**PID Controller (generic):**
```kotlin
class PIDController(
    var kP: Float = 1.0f,
    var kI: Float = 0.0f,
    var kD: Float = 0.0f,
    var iMax: Float = 100f,    // integral windup limit
    var outputMax: Float = 1f,
    var outputMin: Float = -1f,
) {
    private var integral = 0f
    private var previousError = 0f

    fun update(error: Float, dt: Float): Float {
        val p = kP * error
        integral = (integral + error * dt).coerceIn(-iMax, iMax)
        val i = kI * integral
        val d = if (dt > 0) kD * (error - previousError) / dt else 0f
        previousError = error
        return (p + i + d).coerceIn(outputMin, outputMax)
    }

    fun reset() { integral = 0f; previousError = 0f }
}
```

### 5.3 Flight Modes Implemented

| Mode | Behavior | PIDs Active | Complexity |
|------|----------|-------------|------------|
| **Stabilize** | Manual throttle, auto-level on release | Rate + Attitude | Low |
| **Alt Hold** | Auto throttle to hold altitude, manual tilt | Rate + Attitude + Altitude | Medium |
| **Loiter** | Hold position + altitude, manual override | Rate + Attitude + Altitude + Position | Medium |
| **Guided** | Fly to commanded GPS point | All | Medium |
| **Auto** | Follow waypoint mission | All + Mission engine | High |
| **RTL** | Return to launch point + land | All | Medium |
| **Land** | Descend at fixed rate + disarm | Rate + Attitude + Altitude | Low |

### 5.4 Simulation Loop

```kotlin
class SimulationEngine(
    private val params: QuadcopterParams = QuadcopterParams()
) {
    private val state = DroneState()
    private val autopilot = Autopilot(params)
    private val sensorModel = SensorModel()
    private val physicsModel = PhysicsModel(params)

    private val simRateHz = 100  // 100 Hz physics
    private val dtSeconds = 1.0f / simRateHz

    // Main loop — called by a coroutine at 100 Hz
    fun step(externalInput: ControlInput?) {
        // 1. Autopilot reads sensor model (possibly with noise)
        val sensorData = sensorModel.getSensorData(state)

        // 2. Autopilot computes motor commands based on mode + input
        val motorCommands = autopilot.update(
            sensorData = sensorData,
            externalInput = externalInput,
            dt = dtSeconds
        )

        // 3. Physics engine updates drone state
        physicsModel.update(state, motorCommands, dtSeconds)

        // 4. Update battery model
        state.batteryRemaining -= computeCurrentDraw(motorCommands) * dtSeconds

        // 5. Check failsafes
        autopilot.checkFailsafes(state)
    }
}
```

### 5.5 Sensor Model

The sensor model adds realistic noise to "perfect" physics state, teaching students about sensor limitations:

```kotlin
class SensorModel(
    var gpsNoiseMeters: Float = 0.5f,
    var gpsUpdateHz: Float = 5f,
    var imuNoiseGyro: Float = 0.01f,     // rad/s
    var imuNoiseAccel: Float = 0.1f,     // m/s²
    var compassNoiseDeg: Float = 2f,
    var barometerNoisePa: Float = 10f,
    var gpsEnabled: Boolean = true,
    var compassEnabled: Boolean = true,
) {
    fun getSensorData(trueState: DroneState): SensorData {
        return SensorData(
            gps = if (gpsEnabled) GpsData(
                lat = trueState.lat + gaussianNoise() * gpsNoiseMeters * 1e-7,
                lon = trueState.lon + gaussianNoise() * gpsNoiseMeters * 1e-7,
                alt = trueState.alt + gaussianNoise() * gpsNoiseMeters,
                fixType = 3,
                satellites = 12 + random.nextInt(-3, 3),
                eph = (gpsNoiseMeters * 100).toInt()
            ) else GpsData(fixType = 0, satellites = 0),

            imu = ImuData(
                gyroX = trueState.p + gaussianNoise() * imuNoiseGyro,
                gyroY = trueState.q + gaussianNoise() * imuNoiseGyro,
                gyroZ = trueState.r + gaussianNoise() * imuNoiseGyro,
                accelX = trueState.accelBodyX + gaussianNoise() * imuNoiseAccel,
                accelY = trueState.accelBodyY + gaussianNoise() * imuNoiseAccel,
                accelZ = trueState.accelBodyZ + gaussianNoise() * imuNoiseAccel,
            ),

            compass = if (compassEnabled)
                trueState.yaw + gaussianNoise() * Math.toRadians(compassNoiseDeg.toDouble()).toFloat()
            else Float.NaN,

            barometer = trueState.altMSL + gaussianNoise() * barometerNoisePa / 12f, // ~0.8m per 10Pa
        )
    }
}
```

---

## 6. MAVLink Integration — The Phone IS the Drone

### 6.1 MAVLink Server on Android

The app runs a MAVLink UDP server that broadcasts telemetry, making the phone appear as a drone on the network.

**Using mavlink-kotlin:**

```kotlin
class MavlinkServer(
    private val simulationEngine: SimulationEngine
) {
    private val systemId: UByte = loadStablePerInstallSystemId()
    private val componentId: UByte = 1u

    // Bind locally for command receive. 0.0.0.0 is a bind address only,
    // never a packet destination. Same-phone discovery sends to 127.0.0.1:14550.

    suspend fun startBroadcasting(scope: CoroutineScope) {
        // Heartbeat at 1 Hz
        scope.launch {
            while (isActive) {
                sendHeartbeat()
                delay(1000)
            }
        }

        // Attitude at 10 Hz
        scope.launch {
            while (isActive) {
                sendAttitude()
                delay(100)
            }
        }

        // Position at 5 Hz
        scope.launch {
            while (isActive) {
                sendGlobalPosition()
                sendGpsRaw()
                sendVfrHud()
                delay(200)
            }
        }

        // System status at 1 Hz
        scope.launch {
            while (isActive) {
                sendSysStatus()
                sendBatteryStatus()
                delay(1000)
            }
        }

        // Listen for incoming commands from QGC
        scope.launch {
            listenForCommands()
        }
    }

    private fun sendHeartbeat() {
        val state = simulationEngine.state
        send(Heartbeat(
            type = MavType.QUADROTOR,
            autopilot = MavAutopilot.ARDUPILOTMEGA,
            baseMode = buildBaseMode(state),
            customMode = getCustomMode(state.flightMode),
            systemStatus = if (state.armed) MavState.ACTIVE else MavState.STANDBY
        ))
    }

    private fun sendAttitude() {
        val state = simulationEngine.state
        send(Attitude(
            timeBootMs = state.uptimeMs.toUInt(),
            roll = state.roll,
            pitch = state.pitch,
            yaw = state.yaw,
            rollspeed = state.p,
            pitchspeed = state.q,
            yawspeed = state.r,
        ))
    }

    private fun sendGlobalPosition() {
        val state = simulationEngine.state
        send(GlobalPositionInt(
            timeBootMs = state.uptimeMs.toUInt(),
            lat = (state.lat * 1e7).toInt(),
            lon = (state.lon * 1e7).toInt(),
            alt = (state.altMSL * 1000).toInt(),
            relativeAlt = (state.altAGL * 1000).toInt(),
            vx = (state.vx * 100).toShort(),
            vy = (state.vy * 100).toShort(),
            vz = (state.vz * 100).toShort(),
            hdg = (Math.toDegrees(state.yaw.toDouble()) * 100).toUShort(),
        ))
    }

    // ... similar for VFR_HUD, GPS_RAW_INT, SYS_STATUS, BATTERY_STATUS

    private suspend fun listenForCommands() {
        // Listen on UDP for incoming MAVLink from QGC
        // Handle: SET_MODE, COMMAND_LONG (arm/disarm/takeoff/land),
        //         SET_MESSAGE_INTERVAL, PARAM_REQUEST_LIST, MANUAL_CONTROL,
        //         MISSION_ITEM_INT, etc.
        // Send COMMAND_ACK for supported commands.
    }
}
```

### 6.2 MAVLink Messages — What We Send

| Message | Rate | Fields | QGC Uses It For |
|---------|------|--------|-----------------|
| `HEARTBEAT` | 1 Hz | type, autopilot, base_mode, custom_mode, status | Vehicle detection, mode display |
| `ATTITUDE` | 10 Hz | roll, pitch, yaw, rates | Attitude indicator (HUD) |
| `GLOBAL_POSITION_INT` | 5 Hz | lat, lon, alt, velocity | Map position, altitude display |
| `GPS_RAW_INT` | 5 Hz | fix_type, lat, lon, satellites, eph | GPS status bar |
| `VFR_HUD` | 5 Hz | airspeed, groundspeed, heading, throttle, alt, climb | HUD instruments |
| `SYS_STATUS` | 1 Hz | sensors_health, battery_voltage, current, remaining | System health panel |
| `BATTERY_STATUS` | 1 Hz | voltages, current, energy, remaining | Battery widget |
| `STATUSTEXT` | Event | severity, text | Messages panel |

### 6.3 MAVLink Messages — What We Receive

| Message | From QGC | Our Response |
|---------|----------|-------------|
| `SET_MODE` | Mode button pressed | Change autopilot mode + `COMMAND_ACK` |
| `COMMAND_LONG` (ARM) | Arm button | Arm motors, start simulation + `COMMAND_ACK` |
| `COMMAND_LONG` (TAKEOFF) | Takeoff command | Switch to takeoff sequence + `COMMAND_ACK` |
| `COMMAND_LONG` (LAND) | Land command | Switch to land mode + `COMMAND_ACK` |
| `COMMAND_LONG` (SET_MESSAGE_INTERVAL) | QGC requests data rate | Adjust or deny rate + `COMMAND_ACK` |
| `MANUAL_CONTROL` | Joystick/controller | Feed to autopilot as pilot input |
| `MISSION_ITEM_INT` | Mission upload | Store waypoints in mission engine |
| `MISSION_REQUEST_LIST` | QGC requests mission | Send current mission |
| `PARAM_REQUEST_LIST` | QGC requests parameters | Send our simulation parameters |
| `PARAM_SET` | QGC changes a parameter | Update sim/autopilot parameter |

### 6.4 Custom MAVLink Mode Mapping

ArduPilot Copter uses `custom_mode` in the heartbeat to indicate flight mode. We mimic the same values so QGC displays correct mode names:

```kotlin
enum class FlightMode(val customMode: UInt, val displayName: String) {
    STABILIZE(0u, "Stabilize"),
    ALT_HOLD(2u, "Alt Hold"),
    LOITER(5u, "Loiter"),
    GUIDED(4u, "Guided"),
    AUTO(3u, "Auto"),
    RTL(6u, "RTL"),
    LAND(9u, "Land"),
}
```

These IDs match ArduCopter's mode numbering, so QGC's ArduPilot firmware plugin will display them correctly.

---

## 7. QGroundControl Connection — How It Works

### 7.1 Connection Flow

```text
1. Student opens MAVLab app → simulation starts
2. MAVLab broadcasts HEARTBEAT on UDP 14550
3. Student opens QGC on:
   a. Same phone (split-screen) — connects via localhost
   b. Same Wi-Fi tablet — connects via phone's IP
   c. Same Wi-Fi laptop — connects via phone's IP
4. QGC auto-detects the heartbeat → shows vehicle
5. Student interacts with QGC normally:
   - Sees map with drone position
   - Changes flight modes
   - Creates and uploads missions
   - Monitors telemetry
6. QGC sends commands → MAVLab receives → autopilot processes → physics updates → telemetry updates → QGC displays
```

### 7.2 Same-Device Connection (Android Split-Screen)

On Android, both MAVLab and QGC can run simultaneously in split-screen mode:

```text
┌──────────────────────┐
│      MAVLab          │  ← Top half: 3D viz + dashboard
│   (simulation +      │
│    education)         │
├──────────────────────┤
│   QGroundControl     │  ← Bottom half: map + telemetry
│   (real GCS)         │
└──────────────────────┘
```

MAVLab sends MAVLink to `127.0.0.1:14550`, QGC listens on that port. This works on a single phone with zero network required.

### 7.3 Multi-Device Connection (Classroom)

```text
Student Phone (MAVLab) ──── Wi-Fi ────► Instructor Laptop (QGC)
                                        or Instructor Tablet (QGC)
```

MAVLab sends to an explicit QGC IP or to a valid subnet broadcast address such as `192.168.1.255:14550`. `0.0.0.0` is only used for local binding. Multiple students can each run their own MAVLab instance with stable, different `system_id` values, and an instructor's QGC can see all of them on the map simultaneously.

---

## 8. Webapp Variant — Browser-Based Architecture

### 8.1 Technology Stack

| Component | Technology | Reason |
|-----------|-----------|--------|
| Physics Engine | **JavaScript/TypeScript** or **C++ via WebAssembly** | JS is simplest; WASM for performance if needed |
| MAVLink | Custom JS MAVLink encoder/decoder | Lightweight, no dependencies |
| 3D Visualization | **Three.js** | Industry standard for browser 3D |
| Charts | **Chart.js** or **uPlot** | Lightweight real-time charts |
| Framework | **Vanilla JS** or **Svelte** | Minimal, fast |
| Hosting | **GitHub Pages** (static) | Free, zero infrastructure |
| Offline | **Service Worker (PWA)** | Works offline once loaded |

### 8.2 Webapp Architecture

```text
┌───────────────────────────────────────────────────┐
│                  Browser (PWA)                     │
│                                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐│
│  │ Sim Engine   │  │ Three.js     │  │ Dashboard ││
│  │ (JS/WASM)    │→ │ 3D Visualizer│  │ (charts,  ││
│  │              │  │              │  │  MAVLink, ││
│  │ Physics +    │  │ Drone model  │  │  lessons) ││
│  │ Autopilot    │  │ rotates with │  │           ││
│  │              │  │ telemetry    │  │           ││
│  └──────┬───────┘  └──────────────┘  └──────────┘│
│         │                                          │
│  ┌──────▼──────────────────────────────────────┐  │
│  │ MAVLink + WebSocket Bridge (optional)        │  │
│  │                                              │  │
│  │ For local QGC connection:                    │  │
│  │ WebSocket ←→ UDP proxy (needs local helper)  │  │
│  │                                              │  │
│  │ OR: all-in-browser (no QGC needed)           │  │
│  └──────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────┘
```

### 8.3 Webapp Limitation — QGC Connection

Browsers **cannot** open raw UDP sockets. This means a pure webapp cannot directly broadcast MAVLink to QGC.

**Solutions:**

| Approach | Complexity | QGC Support |
|----------|-----------|-------------|
| A. All-in-browser (no QGC needed) | Low | ❌ But dashboard replaces key QGC features |
| B. WebSocket-to-UDP bridge (tiny local helper) | Medium | ✅ |
| C. WebRTC data channel | High | ❌ QGC doesn't support WebRTC |

**Recommendation for webapp:** Start with **Approach A** (self-contained browser experience). The webapp acts as both simulator AND dashboard. No QGC needed. If QGC integration is desired later, provide a small downloadable helper script (Python one-liner) that bridges WebSocket → UDP.

### 8.4 Webapp vs Android App — Summary

| Feature | Android App | Webapp (PWA) |
|---------|-------------|-------------|
| QGC integration | ✅ Native UDP | ⚠️ Needs helper for UDP |
| Phone sensors (tilt controller) | ✅ Native sensors | ✅ DeviceOrientation API |
| 3D visualization | ✅ SceneView/Filament | ✅ Three.js |
| Offline support | ✅ Always offline | ✅ PWA Service Worker |
| Installation | Play Store / APK | Bookmark / "Add to Home" |
| Performance | ⭐⭐⭐⭐⭐ Native | ⭐⭐⭐⭐ Good enough |
| Development speed | Medium | Fast |
| Distribution | Play Store review | Instant via URL |

**Strategy: Build Android app first (flagship, QGC integration). Build webapp second (wider reach, easier distribution).**

---

## 9. Android App Architecture — Standalone

### 9.1 Simplified Module Structure

```text
mavlab-android/
├── app/                        # Application entry point
├── core/
│   ├── ui/                     # Design system, theme
│   ├── mavlink/                # MAVLink encoding/decoding + UDP server
│   ├── sensors/                # Phone sensor abstraction
│   └── common/                 # Utilities
├── simulation/
│   ├── physics/                # 6-DOF physics engine
│   ├── autopilot/              # PID controllers + flight modes
│   ├── sensors/                # Simulated sensor model
│   ├── environment/            # Wind, gravity, failures
│   └── engine/                 # Main simulation loop orchestrator
├── feature/
│   ├── dashboard/              # Main telemetry dashboard
│   ├── controller/             # Phone tilt controller
│   ├── drone3d/                # 3D drone visualization
│   ├── mavlink-explorer/       # MAVLink message inspector
│   ├── pid-lab/                # Interactive PID tuning
│   ├── sensor-lab/             # Phone sensor visualization
│   ├── failure-lab/            # Failure injection UI
│   ├── mission-lab/            # Mission planning education
│   ├── flight-modes/           # Flight mode explainer
│   └── lessons/                # Guided lesson engine
└── build-logic/                # Gradle convention plugins
```

### 9.2 Key Dependencies (Simplified)

```kotlin
// No Python, no Docker, no server dependencies!
dependencies {
    // UI
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // DI
    implementation("com.google.dagger:hilt-android:2.51")

    // MAVLink
    implementation("com.divpundir.mavlink:definitions:2.12.0")
    implementation("com.divpundir.mavlink:connection-core:2.12.0")

    // 3D
    implementation("io.github.sceneview:sceneview:4.16.10")

    // Charts
    implementation("com.patrykandpatrick.vico:compose-m3:2.1.0")

    // That's it. No OkHttp, no Retrofit, no network dependencies.
}
```

### 9.3 No Network Required

The entire app runs with **zero network dependency**:
- Simulation engine: pure Kotlin computation
- MAVLink server: UDP on loopback (`127.0.0.1`) for same-device QGC
- 3D assets: bundled in APK
- Lessons: bundled Markdown/JSON in APK
- Phone sensors: local hardware

Wi-Fi is only needed if QGC runs on a *different* device.

---

## 10. Sensor Integration — Phone as Physical Interface

### 10.1 Two Sensor Modes

**Mode 1 — Phone as Controller (Default)**
Phone sensors provide control INPUT to the simulation. The sim's own sensor model provides simulated drone sensors.

```text
Phone tilt → ControlInput → Autopilot → Physics → Drone State → MAVLink
```

**Mode 2 — Phone as Drone Body (Advanced)**
Phone sensors REPLACE the simulated sensor model. The phone's real IMU/GPS acts as the drone's "real" sensors.

```text
Phone IMU/GPS → SensorData (real) → Autopilot → Physics → Drone State → MAVLink
```

Mode 2 is powerful for teaching because students can literally see what happens when they create "sensor interference" by waving a magnet near the phone's compass.

### 10.2 Controller Mapping

```text
Phone Orientation          →  Autopilot Input
──────────────────────────────────────────────
Pitch forward (tilt away)  →  Drone pitches forward (fly forward)
Roll left (tilt left)      →  Drone rolls left (fly left)
Yaw (rotate phone CW)     →  Drone yaws clockwise
Throttle (on-screen slider)→  Climb / descend

Deadzone: ±3° to avoid noise-induced drift
Expo curve: x^1.5 for finer control near center
Max input: ±30° mapped to full stick deflection
```

---

## 11. 3D Visualization

Same as previous research — **SceneView + Filament** for Android, **Three.js** for webapp.

Key addition: since the physics engine is in-app, we have direct access to all internal state. The 3D visualizer can show:

- Individual motor thrust vectors (colored arrows)
- PID controller outputs as visual overlays
- Sensor readings vs. true state (show the "gap")
- Wind vector arrow
- GPS uncertainty ring that grows/shrinks with noise settings
- Propeller speed proportional to actual motor commands

This is impossible with external SITL — we'd only have MAVLink telemetry. With built-in simulation, we have **everything**.

---

## 12. Education Modules — Offline-First

All education content is bundled in the APK:

| Module | Content Type | Interactive Elements |
|--------|-------------|---------------------|
| **Sensor Lab** | Phone sensor viz + simulated sensor viz | Toggle noise, disable sensors, compare real vs sim |
| **MAVLink Explorer** | Live MAVLink message feed | Tap to expand, field explanations, filter by type |
| **PID Lab** | PID tuner with live response | Sliders for P/I/D, target vs actual graph, oscillation visualization |
| **Flight Mode Lab** | Mode descriptions + live testing | Switch modes, see behavior change, see which PIDs activate |
| **Failure Lab** | Failure injection toggles | Disable GPS, add wind, drain battery, kill motor |
| **Mission Lab** | Waypoint editor + execution viz | Create mission on map, upload to sim, watch execution |
| **Lesson Engine** | Step-by-step guided lessons | "Now try arming the drone" → checks → "Great! Now change mode to Guided" |

All content is Markdown/JSON bundled in `assets/`. No internet needed.

---

## 13. Failure Injection — Built-In

Since we own the simulation engine, failure injection is trivial:

```kotlin
class FailureInjector {
    var gpsEnabled = true
    var gpsNoiseMultiplier = 1.0f        // 1.0 = normal, 5.0 = noisy
    var compassEnabled = true
    var compassInterference = 0f          // degrees of static offset
    var windSpeed = 0f                    // m/s
    var windDirection = 0f                // degrees
    var windGusts = 0f                    // m/s variance
    var batteryDrainMultiplier = 1.0f     // 1.0 = normal, 10.0 = fast drain
    var motorFailureMask = 0              // bitmask: bit 0 = motor 1, etc.
    var vibrationLevel = 0f               // m/s² added noise
    var payloadMassKg = 0f               // additional mass on drone

    fun applyToSensorModel(model: SensorModel) { /* ... */ }
    fun applyToPhysicsModel(model: PhysicsModel) { /* ... */ }
}
```

The Failure Lab UI provides toggle switches and sliders for each failure, with real-time visualization of the effect.

---

## 14. Licensing — MIT

### 14.1 Why MIT

You want:
- Maximum adoption and community contribution
- No friction for educational institutions
- Simple, well-understood, universally accepted
- Compatible with every dependency in the stack

MIT License gives all of this. It allows:
- ✅ Anyone to use, modify, distribute
- ✅ Commercial use (but you're okay with that now)
- ✅ No copyleft obligations
- ✅ Compatible with GPL, Apache, LGPL — everything

### 14.2 License Compatibility

| Dependency | License | Compatible with MIT? |
|-----------|---------|---------------------|
| mavlink-kotlin | Apache 2.0 | ✅ |
| SceneView | Apache 2.0 | ✅ |
| Vico | Apache 2.0 | ✅ |
| Jetpack Compose | Apache 2.0 | ✅ |
| Hilt | Apache 2.0 | ✅ |
| Three.js | MIT | ✅ |
| QGroundControl | LGPL-3.0 / Apache 2.0 | ✅ (we don't modify or link QGC) |

No conflicts. MIT is the simplest path.

### 14.3 LICENSE File

```
MIT License

Copyright (c) 2026 Ascend

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 15. Technology Stack Summary

```text
┌─────────────────────────────────────────────────────────────┐
│                   ANDROID APP (Flagship)                     │
│                                                              │
│  Language:        Kotlin 2.1                                 │
│  UI:              Jetpack Compose + Material 3               │
│  Architecture:    Clean Architecture + MVVM                  │
│  DI:              Hilt                                       │
│  Simulation:      Custom 6-DOF physics + PID autopilot       │
│  MAVLink:         mavlink-kotlin + custom UDP server         │
│  3D:              SceneView (Filament)                       │
│  Charts:          Vico                                       │
│  Sensors:         Android SensorManager                      │
│  Network:         UDP only (MAVLink broadcast, NO internet)  │
│  Min SDK:         API 26 (Android 8.0)                       │
│  License:         MIT                                        │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   WEBAPP (Secondary)                         │
│                                                              │
│  Language:        JavaScript/TypeScript                       │
│  Framework:       Vanilla or Svelte                           │
│  Simulation:      Same physics, ported to JS (or shared KMP)│
│  3D:              Three.js                                   │
│  Charts:          Chart.js or uPlot                          │
│  Sensors:         DeviceOrientation API                      │
│  Hosting:         GitHub Pages (free, static)                │
│  Offline:         PWA with Service Worker                    │
│  License:         MIT                                        │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   EXTERNAL (Not built by us)                 │
│                                                              │
│  GCS:             QGroundControl (Android / Desktop)         │
│  Connection:      MAVLink UDP (auto-detected by QGC)         │
│  No servers. No Docker. No cloud. No bridge.                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 16. Phased Build Plan

### Phase 0 — Project Skeleton + Guardrails (Week 1)

**Goal:** Create a buildable Android project skeleton with the correct package/module boundaries and written protocol guardrails.

| Task | Output |
|------|--------|
| Create Android project with Compose | Buildable empty app shell |
| Add package skeleton for UI, MAVLink, simulation, sensors, lessons, settings | Clear implementation seams |
| Add placeholder navigation tabs | Dashboard, Controller, 3D View, Labs, Settings |
| Add placeholder foreground service | Service boundary exists, no MAVLink yet |
| Write `protocol_guardrails.md` and `test_matrix.md` | Risk rules captured before implementation |
| **Milestone: Project builds and installs with no protocol behavior yet** | ✅ Foundation ready |

### Phase 1 — QGC/MAVLink Protocol Proof (Week 2)

**Goal:** Prove QGroundControl can detect MAVLab, send commands, receive ACKs, and reconnect reliably.

| Task | Output |
|------|--------|
| Implement bound UDP socket model | Same-phone and LAN paths are explicit |
| Send HEARTBEAT and core telemetry | QGC detects ArduPilot-like copter |
| Listen for QGC commands | Arm/disarm/mode/takeoff/land requests received |
| Send `COMMAND_ACK` for supported commands | QGC UI does not hang or retry blindly |
| Handle `SET_MESSAGE_INTERVAL` and minimal params | QGC startup and parameter refresh complete |
| Generate stable per-install system ID | Classroom devices do not collide |
| **Milestone: QGC detects, commands, ACKs, disconnects, and reconnects** | ✅ Protocol validated |

### Phase 2 — Physics Engine (Week 3-5)

**Goal:** Drone responds to physics. Gravity pulls it down, thrust pushes it up.

| Task | Output |
|------|--------|
| Add deterministic JVM physics tests | Unit/sign/stability bugs caught early |
| Implement 6-DOF physics model | Drone falls under gravity |
| Implement motor mixer (quad X) | Thrust produces forces/torques |
| Implement rate PID controller | Drone stabilizes angular rates |
| Implement attitude PID controller | Drone auto-levels |
| Implement altitude PID controller | Drone holds altitude |
| Implement Stabilize + Alt Hold modes | First playable modes |
| Send physics state as MAVLink telemetry | QGC shows realistic movement |
| **Milestone: Arm → Takeoff → Hover in QGC** | ✅ Core simulation working |

### Phase 3 — Phone Controller + Dashboard (Week 6-8)

**Goal:** Control the sim with phone tilt. See telemetry on dashboard.

| Task | Output |
|------|--------|
| Implement phone sensor reading (orientation) | Tilt data flowing |
| Map tilt → control input → autopilot | Phone controls drone |
| Build telemetry dashboard UI | Roll/pitch/yaw/alt/speed/battery display |
| Add Vico charts for attitude + altitude | Rolling time-series graphs |
| Implement Loiter mode (position hold) | GPS-based hold |
| **Milestone: Tilt phone → drone flies → QGC shows movement** | ✅ Flagship demo |

### Phase 4 — 3D + Education (Week 9-12)

**Goal:** Visual, educational, impressive.

| Task | Output |
|------|--------|
| Integrate SceneView 3D drone model | Drone rotates with telemetry |
| Build MAVLink Explorer UI | Live message inspector with explanations |
| Build PID Lab | Interactive P/I/D sliders with live response graph |
| Build Sensor Lab | Phone sensors vs simulated sensors side-by-side |
| Implement Flight Mode Lab | Mode picker with behavior descriptions |
| Implement Guided + RTL modes | Navigate to point, return home |

### Phase 5 — Failure Lab + Missions (Week 13-16)

**Goal:** Teach safety and autonomy.

| Task | Output |
|------|--------|
| Build Failure Lab UI | Toggle GPS, wind, battery, motor failures |
| Implement failure effects in physics/sensors | Visible degradation |
| Implement Auto mode + mission engine | Follow uploaded waypoints |
| Implement mission upload from QGC | QGC uploads mission → sim executes |
| Build Mission Lab UI | Waypoint visualization + execution monitoring |

### Phase 6 — Polish + Release (Week 17-20)

**Goal:** Production-quality release.

| Task | Output |
|------|--------|
| Onboarding + connection guide | First-time user experience |
| Lesson engine + bundled lessons | Guided 7-day curriculum |
| Performance optimization | 60 FPS 3D on mid-range phones |
| Design polish | Material 3, animations, dark theme |
| Google Play Store listing | Screenshots, description, video |
| GitHub release + MIT license | Public repository |
| **Milestone: v1.0 on Play Store** | ✅ Product launched |

---

## 17. Open Questions

| Question | Options | Recommendation |
|----------|---------|----------------|
| Should the simulation engine be Kotlin-only or Kotlin + C++ (NDK)? | Pure Kotlin is simpler; C++ via NDK would be faster | **Kotlin-only** — phones are fast enough for 100 Hz educational physics |
| Should we use Kotlin Multiplatform for sharing sim code with webapp? | KMP can target JS + Android | **Defer** — build Android first in pure Kotlin, port to JS later. KMP adds complexity now |
| Same-phone QGC: split-screen or PiP? | Split-screen works on all phones; PiP is advanced | **Split-screen** — universally supported on Android 8+ |
| Should we publish on F-Droid too? | F-Droid = open-source Android store | **Yes** — aligns with MIT, reaches open-source community |
| How realistic should the physics be? | Simple (spring-damper) vs Full (Newton-Euler) | **Full Newton-Euler** — it's not that much harder and teaches correctly |
| Should we eventually add ArduPilot SITL as an optional backend? | Power users could connect to real SITL on a laptop | **Yes, as Phase 6+** — but core product must work standalone |

---

## 18. References

### Physics & Simulation
1. Quadcopter Dynamics & Control — https://arxiv.org/abs/1602.02622
2. Newton-Euler Quadrotor Modeling — https://sal.aalto.fi/publications/pdf-files/eluu11_public.pdf
3. Hackflight (minimal C++ autopilot) — https://github.com/simondlevy/Hackflight
4. JSBSim Flight Dynamics — https://github.com/JSBSim-Team/jsbsim

### MAVLink
5. MAVLink Protocol — https://mavlink.io/en/
6. MAVLink Common Messages — https://mavlink.io/en/messages/common.html
7. mavlink-kotlin — https://github.com/divyanshupundir/mavlink-kotlin
8. ArduCopter Flight Modes — https://ardupilot.org/copter/docs/flight-modes.html

### Android
9. Jetpack Compose — https://developer.android.com/compose
10. Android Sensor Overview — https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview
11. SceneView Android — https://github.com/SceneView/sceneview-android
12. Vico Charts — https://github.com/patrykandpatrick/vico

### Web
13. Three.js — https://threejs.org/
14. WebAssembly — https://webassembly.org/
15. DeviceOrientation API — https://developer.mozilla.org/en-US/docs/Web/API/DeviceOrientationEvent

### QGroundControl
16. QGroundControl — https://qgroundcontrol.com/
17. QGroundControl Android — https://play.google.com/store/apps/details?id=org.mavlink.qgroundcontrol

---

*This document supersedes the previous cloud/Docker architecture. The standalone approach is simpler, more portable, and better for the target market.*
