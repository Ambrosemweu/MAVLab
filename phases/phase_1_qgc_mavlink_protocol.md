# Phase 1 — QGC/MAVLink Protocol Proof

**Timeline:** Week 2  
**Depends on:** Phase 0 (project skeleton)  
**Produces:** A minimal Android app that proves QGroundControl can detect MAVLab, send commands to it, receive acknowledgements, reconnect reliably, and treat it as an ArduPilot-like copter.

---

## 1. Goal

Build the absolute minimum protocol implementation that:

1. Sends MAVLink `HEARTBEAT` messages over UDP port 14550
2. Sends basic telemetry (`ATTITUDE`, `GLOBAL_POSITION_INT`, `VFR_HUD`, `SYS_STATUS`)
3. Listens for QGroundControl commands on a known UDP socket
4. Sends `COMMAND_ACK` for every supported command
5. Responds to stream-rate and parameter-list requests enough that QGC does not hang
6. QGroundControl (on the same phone in split-screen, or on another device on the same Wi-Fi) auto-detects the vehicle and can arm, disarm, change mode, take off, and land

**This phase proves the riskiest architecture assumption.** If QGC cannot reliably talk to MAVLab in both directions, do not move to physics, UI polish, 3D, lessons, or missions.

---

## 2. Success Criteria

- [ ] Starts from the Phase 0 project skeleton
- [ ] QGroundControl (Android, same device, split-screen) detects a vehicle within 3 seconds of app launch
- [ ] QGroundControl Desktop on the same Wi-Fi detects the same vehicle
- [ ] QGC shows the vehicle as "ArduPilot Copter" (not "Generic" or "Unknown")
- [ ] QGC attitude indicator (HUD) reflects the roll/pitch/yaw values sent by the app
- [ ] QGC map shows the drone at a fixed GPS location (e.g., Nairobi: -1.2921, 36.8219)
- [ ] QGC displays altitude, groundspeed, heading, and battery percentage
- [ ] QGC arm/disarm sends a command and receives `COMMAND_ACK`
- [ ] QGC mode changes update MAVLab state and receive `COMMAND_ACK`
- [ ] QGC takeoff/land commands update MAVLab state and receive `COMMAND_ACK`
- [ ] QGC `SET_MESSAGE_INTERVAL` requests are accepted or explicitly denied with `COMMAND_ACK`
- [ ] QGC parameter-list request receives a minimal parameter set
- [ ] Same-phone mode uses a clearly defined local socket model
- [ ] LAN mode uses explicit peer IP or valid subnet broadcast, never `0.0.0.0` as a destination
- [ ] The MAVLink system ID is stable per install and visible in settings
- [ ] Two MAVLab devices on the same Wi-Fi use different system IDs
- [ ] The app has a simple UI showing "Simulation Running" and the current values being sent
- [ ] No crashes, no ANR, stable for 30+ minutes in split-screen
- [ ] QGC can disconnect/reconnect without restarting MAVLab

---

## 3. Architecture Overview

```text
┌────────────────────────────────────────────────────┐
│                   MAVLab App                        │
│                                                     │
│  ┌───────────────────┐   ┌───────────────────────┐ │
│  │  SimulationService │   │  MainActivity          │ │
│  │  (Foreground)      │   │  (Compose UI)          │ │
│  │                    │   │                         │ │
│  │  • DroneState      │──►│  • Status display      │ │
│  │  • MavlinkServer   │   │  • Roll/pitch/yaw      │ │
│  │  • Coroutine loops │   │  • GPS coords          │ │
│  └────────┬───────────┘   │  • Battery %           │ │
│           │               └───────────────────────┘ │
│           │ UDP broadcast                           │
└───────────┼─────────────────────────────────────────┘
            │ port 14550
            ▼
   ┌──────────────────┐
   │ QGroundControl    │
   │ (auto-detects)    │
   └──────────────────┘
```

---

## 4. Project Setup

### 4.1 Use The Phase 0 Skeleton

Do not recreate the project in this phase. Start from the `mavlab-android/` skeleton created in Phase 0 and fill in the existing `core/mavlink`, `service`, and simple dashboard placeholders.

### 4.2 Protocol Implementation Targets

```text
mavlab-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/ascend/mavlab/
│   │   │   ├── MavLabApp.kt                  # Application class
│   │   │   ├── MainActivity.kt               # Single activity
│   │   │   ├── ui/
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Theme.kt              # Material 3 theme
│   │   │   │   │   ├── Color.kt              # Color definitions
│   │   │   │   │   └── Type.kt               # Typography
│   │   │   │   └── screens/
│   │   │   │       └── HomeScreen.kt          # Main status screen
│   │   │   ├── simulation/
│   │   │   │   ├── DroneState.kt              # Drone state data class
│   │   │   │   └── SimpleSimLoop.kt           # Minimal sim (oscillate values)
│   │   │   ├── mavlink/
│   │   │   │   ├── MavlinkUdpServer.kt        # UDP broadcaster
│   │   │   │   └── MavlinkMessageBuilder.kt   # Builds MAVLink messages
│   │   │   └── service/
│   │   │       └── SimulationService.kt        # Foreground service
│   │   ├── AndroidManifest.xml
│   │   └── res/
│   │       ├── values/strings.xml
│   │       └── drawable/ (app icon)
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts                           # Root build file
├── settings.gradle.kts
├── gradle.properties
└── gradle/
    └── libs.versions.toml                     # Version catalog
```

### 4.3 Dependencies

**`gradle/libs.versions.toml`:**
```toml
[versions]
kotlin = "2.1.0"
agp = "8.7.0"
compose-bom = "2025.05.00"
activity-compose = "1.9.0"
lifecycle = "2.8.0"
mavlink-kotlin = "2.12.0"
coroutines = "1.9.0"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }

# Lifecycle
lifecycle-runtime = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }

# MAVLink
mavlink-definitions = { group = "com.divpundir.mavlink", name = "definitions", version.ref = "mavlink-kotlin" }
mavlink-connection-core = { group = "com.divpundir.mavlink", name = "connection-core", version.ref = "mavlink-kotlin" }

# Coroutines
coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

**`app/build.gradle.kts`:**
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.ascend.mavlab"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ascend.mavlab"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)

    // MAVLink
    implementation(libs.mavlink.definitions)
    implementation(libs.mavlink.connection.core)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
}
```

---

## 5. Core Implementation — Detailed

### 5.1 DroneState.kt

This holds all state that the simulated drone broadcasts. In Phase 1, we hardcode or slowly oscillate values to prove the pipeline works.

```kotlin
package com.ascend.mavlab.simulation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the complete state of the simulated drone.
 * In Phase 1, values are hardcoded or slowly oscillated.
 * In Phase 2+, the physics engine updates this at 100 Hz.
 */
data class DroneState(
    // Attitude (radians)
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,

    // Angular rates (rad/s)
    val rollSpeed: Float = 0f,
    val pitchSpeed: Float = 0f,
    val yawSpeed: Float = 0f,

    // Position (WGS84 decimal degrees + meters)
    val latitude: Double = -1.2921,    // Nairobi
    val longitude: Double = 36.8219,
    val altitudeMSL: Float = 1680f,    // Nairobi elevation ~1680m
    val altitudeAGL: Float = 10f,      // 10m above ground

    // Velocity (m/s, NED frame)
    val vx: Float = 0f,
    val vy: Float = 0f,
    val vz: Float = 0f,

    // Flight info
    val groundSpeed: Float = 0f,
    val heading: Int = 90,             // degrees, 0-359
    val throttle: Int = 50,            // percent, 0-100

    // Battery
    val batteryVoltage: Float = 12.4f, // volts
    val batteryCurrent: Float = 5.0f,  // amps
    val batteryRemaining: Int = 85,    // percent

    // System
    val armed: Boolean = false,
    val flightMode: FlightMode = FlightMode.STABILIZE,
    val uptimeMs: Long = 0L,

    // GPS
    val gpsFix: Int = 3,               // 3 = 3D fix
    val satelliteCount: Int = 12,
    val gpsEph: Int = 120,             // HDOP * 100
)

enum class FlightMode(val customModeId: UInt, val displayName: String) {
    STABILIZE(0u, "Stabilize"),
    ALT_HOLD(2u, "Alt Hold"),
    AUTO(3u, "Auto"),
    GUIDED(4u, "Guided"),
    LOITER(5u, "Loiter"),
    RTL(6u, "RTL"),
    LAND(9u, "Land"),
}
```

### 5.2 SimpleSimLoop.kt

For Phase 1, this slowly oscillates values so QGC's HUD moves visibly, proving real-time telemetry works.

```kotlin
package com.ascend.mavlab.simulation

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin

/**
 * Phase 1 protocol simulation: slowly oscillates roll/pitch/yaw and altitude
 * to prove the MAVLink pipeline works visually in QGC.
 *
 * Replaced by the real physics engine in Phase 2.
 */
class SimpleSimLoop {
    private val _state = MutableStateFlow(DroneState())
    val state: StateFlow<DroneState> = _state.asStateFlow()

    private var running = false
    private var startTime = 0L

    fun start(scope: CoroutineScope) {
        running = true
        startTime = System.currentTimeMillis()

        scope.launch(Dispatchers.Default) {
            while (running) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                val uptimeMs = System.currentTimeMillis() - startTime

                _state.value = _state.value.copy(
                    // Gentle oscillation so QGC HUD shows movement
                    roll = (sin(elapsed * 0.5) * 0.15).toFloat(),       // ±8.6°
                    pitch = (sin(elapsed * 0.3) * 0.10).toFloat(),      // ±5.7°
                    yaw = ((elapsed * 0.1) % (2 * Math.PI)).toFloat(),  // slow rotation

                    // Altitude bobbing
                    altitudeAGL = 10f + (sin(elapsed * 0.2) * 2).toFloat(),

                    // Heading follows yaw
                    heading = ((Math.toDegrees(((elapsed * 0.1) % (2 * Math.PI)))) .toInt() + 360) % 360,

                    // Slowly drain battery
                    batteryRemaining = (85 - (elapsed * 0.1).toInt()).coerceAtLeast(0),
                    batteryVoltage = 12.4f - (elapsed * 0.002f).toFloat().coerceAtMost(2f),

                    uptimeMs = uptimeMs,
                    armed = true,
                )

                delay(50) // 20 Hz state updates
            }
        }
    }

    fun stop() {
        running = false
    }
}
```

### 5.3 MavlinkUdpServer.kt

This is the critical component — it takes `DroneState` and broadcasts MAVLink messages over UDP so QGC can detect the vehicle.

```kotlin
package com.ascend.mavlab.mavlink

import com.ascend.mavlab.simulation.DroneState
import com.ascend.mavlab.simulation.FlightMode
import com.divpundir.mavlink.api.MavEnumValue
import com.divpundir.mavlink.definitions.common.*
import com.divpundir.mavlink.definitions.minimal.Heartbeat
import com.divpundir.mavlink.definitions.minimal.MavAutopilot
import com.divpundir.mavlink.definitions.minimal.MavModeFlag
import com.divpundir.mavlink.definitions.minimal.MavState
import com.divpundir.mavlink.definitions.minimal.MavType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class MavlinkSocketConfig(
    val localBindPort: Int = 14550,
    val sameDeviceHost: String = "127.0.0.1",
    val sameDeviceQgcPort: Int = 14550,
    val lanDestinations: List<UdpDestination> = emptyList(),
    val systemId: Int,
    val componentId: Int = 1,
)

data class UdpDestination(
    val host: String,
    val port: Int = 14550,
)

/**
 * Sends and receives MAVLink over one known UDP socket.
 *
 * Important:
 *   - 0.0.0.0 is a bind address only. Never use it as a packet destination.
 *   - Same-device mode sends discovery traffic to 127.0.0.1:14550.
 *   - LAN mode uses either an explicit QGC IP or a real subnet broadcast address.
 *   - Commands from QGC must be read from the same bound socket so replies can be ACKed.
 *
 * System ID must be stable per install and visible in settings. A hardcoded
 * system ID breaks classroom use when many student phones are on the same Wi-Fi.
 */
class MavlinkUdpServer(
    private val stateFlow: StateFlow<DroneState>,
    private val config: MavlinkSocketConfig,
) {
    private var socket: DatagramSocket? = null
    private var running = false
    private val messageBuilder = MavlinkMessageBuilder(config.systemId, config.componentId)

    fun start(scope: CoroutineScope) {
        running = true
        socket = DatagramSocket(config.localBindPort)
        socket?.broadcast = true

        // Heartbeat — 1 Hz (REQUIRED for QGC detection)
        scope.launch(Dispatchers.IO) {
            while (running) {
                val state = stateFlow.value
                val bytes = messageBuilder.buildHeartbeat(state)
                sendToAll(bytes)
                delay(1000)
            }
        }

        // Attitude — 10 Hz
        scope.launch(Dispatchers.IO) {
            while (running) {
                val state = stateFlow.value
                val bytes = messageBuilder.buildAttitude(state)
                sendToAll(bytes)
                delay(100)
            }
        }

        // Position + VFR HUD — 4 Hz
        scope.launch(Dispatchers.IO) {
            while (running) {
                val state = stateFlow.value
                sendToAll(messageBuilder.buildGlobalPositionInt(state))
                sendToAll(messageBuilder.buildGpsRawInt(state))
                sendToAll(messageBuilder.buildVfrHud(state))
                delay(250)
            }
        }

        // System status + battery — 1 Hz
        scope.launch(Dispatchers.IO) {
            while (running) {
                val state = stateFlow.value
                sendToAll(messageBuilder.buildSysStatus(state))
                delay(1000)
            }
        }

        // Command listener — required for arm/disarm, mode changes, takeoff, land, params, rates
        scope.launch(Dispatchers.IO) {
            listenForQgcPackets()
        }
    }

    fun stop() {
        running = false
        socket?.close()
        socket = null
    }

    private fun sendToAll(data: ByteArray) {
        try {
            val sock = socket ?: return

            // Send to loopback (same-device QGC)
            val loopbackPacket = DatagramPacket(
                data, data.size,
                InetAddress.getByName(config.sameDeviceHost), config.sameDeviceQgcPort
            )
            sock.send(loopbackPacket)

            // LAN mode: send to explicit QGC IP or valid subnet broadcast, never 0.0.0.0.
            config.lanDestinations.forEach { destination ->
                val packet = DatagramPacket(
                    data, data.size,
                    InetAddress.getByName(destination.host), destination.port
                )
                sock.send(packet)
            }
        } catch (e: Exception) {
            // Log but don't crash — network errors are expected
            e.printStackTrace()
        }
    }

    private suspend fun listenForQgcPackets() {
        // Parse inbound MAVLink packets and reply with COMMAND_ACK for supported commands.
        // At minimum handle COMMAND_LONG, SET_MODE, SET_MESSAGE_INTERVAL,
        // PARAM_REQUEST_LIST, and PARAM_SET.
    }
}
```

### 5.4 MavlinkMessageBuilder.kt

Builds raw MAVLink v2 byte arrays from `DroneState`. This uses `mavlink-kotlin` for type-safe message construction, then serializes to bytes for UDP.

```kotlin
package com.ascend.mavlab.mavlink

import com.ascend.mavlab.simulation.DroneState
import com.ascend.mavlab.simulation.FlightMode
import com.divpundir.mavlink.api.*
import com.divpundir.mavlink.definitions.common.*
import com.divpundir.mavlink.definitions.minimal.*
import com.divpundir.mavlink.serialization.MavDataEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Constructs MAVLink v2 messages from DroneState.
 *
 * IMPORTANT: QGC identifies this as an ArduPilot Copter vehicle because:
 *   - HEARTBEAT.autopilot = MAV_AUTOPILOT_ARDUPILOTMEGA
 *   - HEARTBEAT.type = MAV_TYPE_QUADROTOR
 *   - HEARTBEAT.custom_mode matches ArduCopter mode IDs
 *
 * This causes QGC to load the ArduPilot firmware plugin,
 * which displays correct flight mode names.
 *
 * Each build* method returns raw bytes ready for UDP transmission.
 * The developer implementing this should use mavlink-kotlin's
 * serialization API to convert MavMessage objects to MAVLink v2 frames.
 *
 * MAVLink v2 frame structure:
 *   [0xFD] [len] [incompat_flags] [compat_flags] [seq] [sysid] [compid]
 *   [msgid_low] [msgid_mid] [msgid_high] [payload...] [crc_low] [crc_high]
 */
class MavlinkMessageBuilder(
    private val systemId: Int,
    private val componentId: Int,
) {
    private var sequenceNumber: UByte = 0u

    /**
     * Build HEARTBEAT (msg id 0) — tells QGC "I am a quadrotor running ArduPilot"
     *
     * CRITICAL: This is the message that makes QGC detect the vehicle.
     * Without this, nothing works.
     */
    fun buildHeartbeat(state: DroneState): ByteArray {
        val msg = Heartbeat(
            type = MavEnumValue.of(MavType.MAV_TYPE_QUADROTOR),
            autopilot = MavEnumValue.of(MavAutopilot.MAV_AUTOPILOT_ARDUPILOTMEGA),
            baseMode = MavEnumValue.of(
                if (state.armed) {
                    MavModeFlag.MAV_MODE_FLAG_SAFETY_ARMED or
                    MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED
                } else {
                    MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED
                }
            ),
            customMode = state.flightMode.customModeId,
            systemStatus = MavEnumValue.of(
                if (state.armed) MavState.MAV_STATE_ACTIVE
                else MavState.MAV_STATE_STANDBY
            ),
            mavlinkVersion = 3u,
        )
        return serializeMessage(msg)
    }

    /**
     * Build ATTITUDE (msg id 30) — roll, pitch, yaw for QGC's HUD
     */
    fun buildAttitude(state: DroneState): ByteArray {
        val msg = Attitude(
            timeBootMs = state.uptimeMs.toUInt(),
            roll = state.roll,
            pitch = state.pitch,
            yaw = state.yaw,
            rollspeed = state.rollSpeed,
            pitchspeed = state.pitchSpeed,
            yawspeed = state.yawSpeed,
        )
        return serializeMessage(msg)
    }

    /**
     * Build GLOBAL_POSITION_INT (msg id 33) — position on QGC's map
     *
     * lat/lon are in degE7 (degrees × 10^7)
     * alt is in mm
     * velocity is in cm/s
     */
    fun buildGlobalPositionInt(state: DroneState): ByteArray {
        val msg = GlobalPositionInt(
            timeBootMs = state.uptimeMs.toUInt(),
            lat = (state.latitude * 1e7).toInt(),
            lon = (state.longitude * 1e7).toInt(),
            alt = (state.altitudeMSL * 1000).toInt(),
            relativeAlt = (state.altitudeAGL * 1000).toInt(),
            vx = (state.vx * 100).toInt().toShort(),
            vy = (state.vy * 100).toInt().toShort(),
            vz = (state.vz * 100).toInt().toShort(),
            hdg = (state.heading * 100).toUShort(),
        )
        return serializeMessage(msg)
    }

    /**
     * Build GPS_RAW_INT (msg id 24) — GPS status for QGC's GPS indicator
     */
    fun buildGpsRawInt(state: DroneState): ByteArray {
        val msg = GpsRawInt(
            timeUsec = (state.uptimeMs * 1000).toULong(),
            fixType = MavEnumValue.of(GpsFixType.entries[state.gpsFix]),
            lat = (state.latitude * 1e7).toInt(),
            lon = (state.longitude * 1e7).toInt(),
            alt = (state.altitudeMSL * 1000).toInt(),
            eph = state.gpsEph.toUShort(),
            epv = 200u.toUShort(),
            vel = (state.groundSpeed * 100).toUInt().toUShort(),
            cog = (state.heading * 100).toUShort(),
            satellitesVisible = state.satelliteCount.toUByte(),
        )
        return serializeMessage(msg)
    }

    /**
     * Build VFR_HUD (msg id 74) — airspeed, groundspeed, heading, throttle
     * These populate QGC's primary HUD instruments.
     */
    fun buildVfrHud(state: DroneState): ByteArray {
        val msg = VfrHud(
            airspeed = state.groundSpeed,
            groundspeed = state.groundSpeed,
            heading = state.heading.toShort(),
            throttle = state.throttle.toUShort(),
            alt = state.altitudeMSL + state.altitudeAGL,
            climb = -state.vz, // NED: negative vz = climbing
        )
        return serializeMessage(msg)
    }

    /**
     * Build SYS_STATUS (msg id 1) — battery voltage and system health
     */
    fun buildSysStatus(state: DroneState): ByteArray {
        val msg = SysStatus(
            onboardControlSensorsPresent = MavEnumValue.of(0u),
            onboardControlSensorsEnabled = MavEnumValue.of(0u),
            onboardControlSensorsHealth = MavEnumValue.of(0u),
            load = 500u.toUShort(), // 50% CPU
            voltageBattery = (state.batteryVoltage * 1000).toUShort(), // mV
            currentBattery = (state.batteryCurrent * 100).toShort(),   // cA
            batteryRemaining = state.batteryRemaining.toByte(),
            dropRateComm = 0u.toUShort(),
            errorsComm = 0u.toUShort(),
            errorsCount1 = 0u.toUShort(),
            errorsCount2 = 0u.toUShort(),
            errorsCount3 = 0u.toUShort(),
            errorsCount4 = 0u.toUShort(),
        )
        return serializeMessage(msg)
    }

    /**
     * Serialize a MavMessage to MAVLink v2 wire bytes.
     *
     * NOTE: The exact serialization approach depends on the mavlink-kotlin
     * API version. The developer should:
     *
     * 1. Use MavFrame or equivalent to wrap the message with system/component IDs
     * 2. Serialize to MAVLink v2 format (0xFD header)
     * 3. Include CRC (mavlink-kotlin handles this)
     * 4. Return raw bytes suitable for DatagramPacket
     *
     * If mavlink-kotlin doesn't provide a simple serialize-to-bytes,
     * use its MavRawFrame or MavDataEncoder directly.
     * Alternatively, use a MavConnection with a ByteArrayOutputStream.
     */
    private fun serializeMessage(message: MavMessage<*>): ByteArray {
        // Implementation depends on mavlink-kotlin version.
        // The developer should consult:
        //   https://github.com/divyanshupundir/mavlink-kotlin
        //
        // Key approach: create a MavFrame.V2 with the message,
        // then call its serialize() method.
        //
        // Pseudocode:
        //   val frame = MavFrame.V2(
        //       sequence = sequenceNumber++,
        //       systemId = systemId.toUByte(),
        //       componentId = componentId.toUByte(),
        //       message = message
        //   )
        //   return frame.serialize()

        TODO("Implement using mavlink-kotlin serialization API")
    }
}
```

**Developer note:** The `serializeMessage` method is intentionally left as a TODO because the exact API depends on the mavlink-kotlin version used. The developer should read the mavlink-kotlin README and use the appropriate serialization API. If `mavlink-kotlin`'s API makes raw byte serialization difficult, an alternative is to manually construct MAVLink v2 frames using the message's `serializeV2()` or `encode()` methods.

### 5.5 SimulationService.kt

Android kills background processes. To keep the simulation running when the user switches to QGC in split-screen, we use a Foreground Service.

```kotlin
package com.ascend.mavlab.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ascend.mavlab.MainActivity
import com.ascend.mavlab.R
import com.ascend.mavlab.mavlink.MavlinkUdpServer
import com.ascend.mavlab.simulation.SimpleSimLoop
import kotlinx.coroutines.*

/**
 * Foreground Service that keeps the simulation + MAVLink broadcast alive
 * even when the user switches to QGC in split-screen.
 *
 * Without this, Android will kill the simulation within seconds
 * of the app going to the background.
 */
class SimulationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var simLoop: SimpleSimLoop
    private lateinit var mavlinkServer: MavlinkUdpServer

    companion object {
        const val CHANNEL_ID = "mavlab_simulation"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        simLoop = SimpleSimLoop()
        mavlinkServer = MavlinkUdpServer(simLoop.state)

        simLoop.start(scope)
        mavlinkServer.start(scope)

        return START_STICKY
    }

    override fun onDestroy() {
        simLoop.stop()
        mavlinkServer.stop()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "MAVLab Simulation",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the drone simulation running"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MAVLab Simulation Active")
            .setContentText("Broadcasting MAVLink on UDP 14550")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
```

### 5.6 AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- UDP broadcasting requires network access -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

    <!-- Foreground service permission (Android 9+) -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />

    <application
        android:name=".MavLabApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="MAVLab"
        android:supportsRtl="true"
        android:theme="@style/Theme.MAVLab">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="MAVLab"
            android:configChanges="orientation|screenSize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.SimulationService"
            android:foregroundServiceType="connectedDevice"
            android:exported="false" />
    </application>
</manifest>
```

### 5.7 MainActivity.kt

```kotlin
package com.ascend.mavlab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ascend.mavlab.service.SimulationService
import com.ascend.mavlab.ui.screens.HomeScreen
import com.ascend.mavlab.ui.theme.MavLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start simulation service
        val serviceIntent = Intent(this, SimulationService::class.java)
        startForegroundService(serviceIntent)

        setContent {
            MavLabTheme {
                HomeScreen()
            }
        }
    }
}
```

### 5.8 HomeScreen.kt

A simple status screen showing current simulation values.

```kotlin
package com.ascend.mavlab.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MAVLab") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Status indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("●", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp)
                    Column {
                        Text(
                            "Simulation Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Broadcasting MAVLink on UDP 14550",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // Instructions card
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Connect QGroundControl",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("1. Open QGroundControl on this device or another on the same Wi-Fi")
                    Text("2. QGC will auto-detect the simulated drone")
                    Text("3. You should see a quadcopter on the map near Nairobi")
                    Text("4. The attitude indicator should show gentle movement")
                }
            }

            // TODO: In later phases, this screen will show live
            // telemetry values, sensor readings, and controls.
            // For Phase 1, the static instructions are sufficient
            // to validate the architecture.
        }
    }
}
```

---

## 6. Testing Plan

### 6.1 Manual Testing Steps

1. **Build and install** the app on an Android device (API 26+)
2. **Verify notification** — a persistent notification "MAVLab Simulation Active" should appear
3. **Open QGroundControl** on the same device (split-screen) or on another device on the same Wi-Fi
4. **Check QGC vehicle detection** — within 3 seconds, QGC should show a vehicle connected
5. **Check vehicle type** — QGC should identify it as "ArduPilot" copter
6. **Check HUD** — the attitude indicator should show gentle oscillation (roll/pitch)
7. **Check map** — the drone should appear near Nairobi (-1.2921, 36.8219)
8. **Check telemetry** — altitude, speed, battery should display in QGC
9. **Arm/disarm test** — arm and disarm from QGC; MAVLab must send `COMMAND_ACK`
10. **Mode test** — switch Stabilize, Alt Hold, Guided, RTL, and Land; MAVLab must send `COMMAND_ACK`
11. **Takeoff/land test** — send takeoff and land from QGC; MAVLab must send `COMMAND_ACK`
12. **Parameter test** — refresh parameters in QGC; MAVLab must return a minimal parameter list
13. **Message-rate test** — QGC `SET_MESSAGE_INTERVAL` requests are handled or explicitly denied
14. **Reconnect test** — restart QGC while MAVLab keeps running; QGC reconnects
15. **MAVLab restart test** — restart MAVLab while QGC keeps running; QGC reconnects
16. **Stability test** — leave running for 30 minutes, verify no crash or disconnect
17. **Lifecycle test** — rotate screen, background/restore, screen off/on, and return to split-screen
18. **Two-device identity test** — run two MAVLab phones on one Wi-Fi network; QGC sees distinct vehicles

### 6.2 What Success Looks Like

When everything works, QGC will show:
- A quadcopter icon on the map in Nairobi
- An attitude indicator gently rocking back and forth
- Flight mode: "Stabilize"
- Battery: slowly decreasing from 85%
- Altitude: ~10m AGL, gently bobbing
- GPS: 3D fix, 12 satellites
- Armed status indicator

### 6.3 Common Failure Modes

| Problem | Likely Cause | Fix |
|---------|-------------|-----|
| QGC doesn't detect vehicle | Heartbeat not sending, wrong port, or wrong MAVLink frame format | Check UDP actually sends bytes, verify MAVLink v2 framing |
| QGC shows "Generic" not "ArduPilot" | `autopilot` field wrong in HEARTBEAT | Must be `MAV_AUTOPILOT_ARDUPILOTMEGA` |
| QGC shows mode as "Unknown" | `custom_mode` doesn't match ArduCopter values | Use the exact mode IDs from `FlightMode` enum |
| HUD doesn't move | ATTITUDE message not sending or values always zero | Check the oscillation math in SimpleSimLoop |
| App crashes on background | Service not properly foregrounded | Ensure foreground notification is created before starting work |
| Arm button appears stuck | Missing or malformed `COMMAND_ACK` | ACK every supported command and log unsupported commands |
| Parameter refresh hangs | No response to `PARAM_REQUEST_LIST` | Send a minimal parameter list before moving to physics |
| Multiple phones collapse into one vehicle | Duplicate MAVLink system IDs | Generate stable per-install system IDs |
| No connection on Wi-Fi | Broadcast blocked by router | Try explicit QGC IP or valid subnet broadcast instead of generic broadcast |

---

## 7. Notes for the Developer

1. **mavlink-kotlin version:** The exact API for serializing messages to bytes may vary. Check the library's README and test folder for examples. If the library doesn't expose a convenient `serialize()` method, you may need to use its `MavConnection` with a `ByteArrayOutputStream` to capture the bytes.

2. **Don't over-engineer:** Phase 1 is intentionally simple. No physics, no 3D, no lessons. Just prove QGC protocol behavior.

3. **Foreground service type:** On Android 14 (API 34)+, you must declare `foregroundServiceType="connectedDevice"` in the manifest AND pass it in `startForeground()`. Without this, the service will crash.

4. **UDP on loopback:** Android allows UDP on `127.0.0.1` without any special permissions. This is how same-device QGC communication works.

5. **Next phase dependency:** Phase 2 (Physics Engine) replaces `SimpleSimLoop` with a real 6-DOF physics model. Everything else in Phase 1 (MavlinkUdpServer, service, protocol handling, UI shell) carries forward.

---

## 8. Definition of Done

Phase 1 is complete when:
- [x] Android app builds and installs
- [x] QGC detects the vehicle
- [x] QGC shows correct vehicle type (ArduPilot Copter)
- [x] QGC HUD shows moving attitude
- [x] QGC map shows drone at Nairobi coordinates
- [x] QGC displays battery, altitude, speed
- [x] QGC arm/disarm/mode/takeoff/land commands receive `COMMAND_ACK`
- [x] QGC parameter refresh completes with a minimal parameter list
- [x] QGC reconnects after either QGC or MAVLab restarts
- [x] Two MAVLab devices appear as distinct vehicles
- [x] App is stable for 30+ minutes in split-screen with QGC
- [x] Code is committed to Git with MIT license
