# MAVLab Android

MAVLab by Ascend Labs is a phone-based drone digital twin and training platform
for Android.

## Current Scope

The v1.5 app is organized around five MVP surfaces:

- Cockpit: live operations, telemetry, safety state, and MAVLink status
- Controller: local/manual control with phone sensors and fallback inputs
- Mission: QGroundControl uploads, autonomous route execution, and waypoint progress
- SIM: 3D physical behavior visualization
- Ops: diagnostics, logs, export staging, QGC setup, and release checks

Current implementation includes:

- Android Compose shell and foreground simulation service
- MAVLink v2 UDP telemetry and QGroundControl command handling
- Quadcopter physics, motor mixing, PID stabilization, takeoff, landing, battery model
- Phone tilt controller with calibration and manual fallback controls
- Telemetry dashboard cards and rolling charts
- SceneView-based 3D drone model tab
- Failure, mission, and lesson code retained for reuse under the v1.5 surfaces
- First-launch onboarding
- CI and release metadata scaffolding

## Build

The project expects Android SDK 35 and Java 17.

```bash
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew lintDebug testDebugUnitTest assembleDebug
```

Install on a connected phone:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## QGroundControl

MAVLab broadcasts MAVLink telemetry over UDP and tracks the last peer that sends inbound MAVLink messages. QGC can be used on the same phone in split-screen or from a desktop on the same Wi-Fi network.

## Release

GitHub Actions workflows live in the repository root under `.github/workflows/`. Play Store draft metadata lives under `fastlane/metadata/android/en-US/`.
