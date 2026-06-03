# MAVLab Android

MAVLab is a standalone, offline-first drone education simulator for Android.

This repository is currently at **Phase 0: Project Skeleton + Architecture Guardrails**. The Android app intentionally contains no MAVLink, UDP, QGroundControl, physics, sensor, 3D, mission, failure, or lesson behavior yet.

## Phase 0 Scope

Phase 0 provides:

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

## What Comes Next

Phase 1 validates the riskiest assumption: QGroundControl must detect MAVLab, send commands, receive `COMMAND_ACK`, refresh minimal params, reconnect cleanly, and work in same-phone and LAN modes.

Do not add physics, 3D, lessons, or controller behavior until Phase 1 passes.

## Build

The project expects Android SDK 35 and Java 17.

```sh
./gradlew lintDebug testDebugUnitTest assembleDebug
```

If `gradle/wrapper/gradle-wrapper.jar` is missing, regenerate the wrapper with Gradle 8.10.2:

```sh
gradle wrapper --gradle-version 8.10.2
```
