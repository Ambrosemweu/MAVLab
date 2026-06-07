# MAVLab Android

MAVLab is a standalone, offline-first drone education simulator for Android.

This repository is currently at **Phase 1: QGC/MAVLink Protocol Proof**. The app sends minimal MAVLink telemetry, listens for basic QGroundControl commands, sends `COMMAND_ACK`, returns a small parameter list, and shows live protocol-demo state in the dashboard.

## Implemented Scope

Phase 0 provided:

- Buildable Android project structure under `mavlab-android/`
- Package namespace `com.ascend.mavlab`
- Compose shell with placeholder tabs:
  - Dashboard
  - Controller
  - 3D View
  - Labs
  - Settings
- Placeholder foreground service boundary
- Empty seams for MAVLink, simulation, phone sensors, settings, and lessons
- Protocol guardrails for Phase 1
- Test matrix for later validation

Phase 1 adds:

- Foreground service runtime
- Shared protocol-demo state loop
- Minimal MAVLink v2 framing and checksum generation
- `HEARTBEAT`, `ATTITUDE`, `GLOBAL_POSITION_INT`, `GPS_RAW_INT`, `VFR_HUD`, `SYS_STATUS`, and `BATTERY_STATUS`
- `COMMAND_ACK` for arm/disarm, takeoff, land, mode, and message interval requests
- Minimal `PARAM_VALUE` responses for QGC parameter refresh
- Same-device UDP target and LAN broadcast discovery
- Stable device-derived MAVLink system ID displayed in the dashboard

## What Comes Next

Next is real-device validation with QGroundControl on the same phone and on a desktop over the same Wi-Fi. Do not add physics, 3D, lessons, or controller behavior until that protocol check passes.

## Build

The project expects Android SDK 35 and Java 17.

```sh
./gradlew lintDebug testDebugUnitTest assembleDebug
```

If `gradle/wrapper/gradle-wrapper.jar` is missing, regenerate the wrapper with Gradle 8.10.2:

```sh
gradle wrapper --gradle-version 8.10.2
```
