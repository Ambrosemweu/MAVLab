# MAVLab

MAVLab is an Android-first drone education simulator. It runs offline on a phone and teaches drone systems through live simulation, MAVLink telemetry, phone control, missions, failures, 3D visualization, and guided lessons.

Active implementation target: `mavlab-android/`.

## Features

- Fixed-rate quadcopter physics and PID autopilot
- MAVLink UDP telemetry for QGroundControl
- Phone tilt controller with manual fallback
- Telemetry dashboard with charts
- 3D drone view using a bundled GLB model
- Failure Lab for GPS, compass, wind, motors, battery, and payload
- Mission Lab for demo waypoint missions and Guided commands
- Seven guided lessons
- First-launch onboarding
- GitHub Actions CI scaffolding
- Release metadata for Play Store preparation

## Quick Start

```bash
cd mavlab-android
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew lintDebug testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

To start the local Android emulator helper:

```bash
./scripts/run-android-emulator.sh
```

## Documentation

- [Setup guide](docs/setup_guide.md)
- [Teacher guide](docs/teacher_guide.md)
- [Architecture](docs/architecture.md)
- [Android project README](mavlab-android/README.md)
- [Phase roadmap](phases/)

## License

MIT. See [LICENSE](LICENSE).
