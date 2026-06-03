# Phase 0 — Project Skeleton + Architecture Guardrails

**Timeline:** Week 1  
**Depends on:** Nothing  
**Produces:** A buildable Android project skeleton with the intended package/module structure, placeholder screens, empty simulation/protocol seams, CI-ready Gradle files, and written guardrails for the later QGC/MAVLink work.

---

## 1. Goal

Create the project structure before implementing behavior.

Phase 0 should not prove MAVLink, physics, QGC, controller input, 3D, missions, or lessons. It should only make the project build, install, and expose the boundaries that later phases fill in.

This avoids mixing architecture setup with protocol risk. Phase 1 becomes the focused QGroundControl/MAVLink proof.

---

## 2. Success Criteria

- [ ] Android project exists under `mavlab-android/`
- [ ] Package namespace is `com.ascend.mavlab`
- [ ] App installs and opens on Android 8.0+ (API 26+)
- [ ] Compose shell renders without crashes
- [ ] Bottom navigation placeholders exist for Dashboard, Controller, 3D View, Labs, and Settings
- [ ] Empty package/module skeleton exists for UI, MAVLink, simulation, phone sensors, lessons, and app settings
- [ ] Foreground service placeholder exists but does not yet broadcast MAVLink
- [ ] No QGC, UDP, physics, or sensor behavior is implemented in this phase
- [ ] `./gradlew lintDebug testDebugUnitTest assembleDebug` can run
- [ ] README explains that Phase 0 is scaffold-only and points to Phase 1 for protocol validation

---

## 3. Directory Structure

```text
mavlab-android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/ascend/mavlab/
│       │   ├── MavLabApp.kt
│       │   ├── MainActivity.kt
│       │   ├── service/
│       │   │   └── SimulationService.kt          # Placeholder only
│       │   ├── core/
│       │   │   ├── common/
│       │   │   ├── ui/
│       │   │   │   ├── theme/
│       │   │   │   └── components/
│       │   │   ├── mavlink/
│       │   │   │   ├── MavlinkEndpoint.kt        # Interface only
│       │   │   │   └── MavlinkSocketConfig.kt    # Data class only
│       │   │   ├── sensors/
│       │   │   │   └── PhoneSensorSource.kt      # Interface only
│       │   │   └── settings/
│       │   │       └── AppSettings.kt            # Defaults only
│       │   ├── simulation/
│       │   │   ├── engine/
│       │   │   │   └── SimulationEngine.kt       # Interface only
│       │   │   ├── physics/
│       │   │   ├── autopilot/
│       │   │   ├── sensors/
│       │   │   ├── failures/
│       │   │   └── mission/
│       │   └── feature/
│       │       ├── dashboard/
│       │       ├── controller/
│       │       ├── drone3d/
│       │       ├── labs/
│       │       ├── lessons/
│       │       ├── settings/
│       │       └── navigation/
│       └── res/
│           ├── values/
│           └── drawable/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
├── docs/
│   ├── architecture.md
│   ├── protocol_guardrails.md
│   └── test_matrix.md
├── README.md
└── LICENSE
```

---

## 4. Required Guardrail Docs

### 4.1 `docs/protocol_guardrails.md`

Record the rules that Phase 1 must follow:

- `0.0.0.0` is only a bind address, never a broadcast destination.
- Same-phone mode must define exactly how MAVLab and QGC share UDP ports.
- LAN mode must use explicit peer IP or a valid subnet broadcast address.
- MAVLab must listen for QGC commands on a known socket.
- Phase 1 must send `COMMAND_ACK` for supported commands.
- Each app install must use a stable, user-visible MAVLink system ID.
- `system_id = 1` is allowed only as a temporary development default.

### 4.2 `docs/test_matrix.md`

Define the test matrix before implementation:

- Android 8, 11, 13, and 15
- Same-phone QGC split-screen
- Desktop QGC on same Wi-Fi
- Wi-Fi off/on
- App background/restore
- Screen rotation
- Screen off/on
- Two MAVLab devices on the same network
- Low-end Android device for sensor fallback

---

## 5. Placeholder Interfaces

Do not implement behavior yet. Only create seams.

```kotlin
interface SimulationEngine {
    val state: StateFlow<DroneState>
    fun start()
    fun stop()
}
```

```kotlin
interface MavlinkEndpoint {
    suspend fun start()
    suspend fun stop()
}
```

```kotlin
data class MavlinkSocketConfig(
    val localBindPort: Int = 14550,
    val sameDeviceHost: String = "127.0.0.1",
    val sameDeviceQgcPort: Int = 14550,
    val systemId: Int,
    val componentId: Int = 1,
)
```

The `systemId` must come from settings in Phase 1, not remain hardcoded forever.

---

## 6. What Not To Build In Phase 0

- No MAVLink encoding/decoding
- No UDP broadcasting
- No QGC connection
- No physics loop
- No PID controllers
- No phone sensor mapping
- No charts
- No 3D rendering
- No failure injection
- No mission protocol
- No lesson engine

Phase 0 is complete when the app skeleton is boring, buildable, and ready for Phase 1 to validate the riskiest protocol assumptions.
