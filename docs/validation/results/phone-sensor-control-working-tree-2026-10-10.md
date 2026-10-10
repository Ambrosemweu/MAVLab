# Armed phone-sensor control — 2026-10-10

Scope: Working-tree validation on `feature/mavlab-uiux-upgrade`; this is not a release qualification.

## Build identity

- App: MAVLab 1.5.3, version code 18, debug build.
- Base commit: `703b7f90d717ff386ee9a232bbc5a51a38af3ca0`, with uncommitted Cockpit and phone-control changes.
- APK SHA-256: `235868d6652b38f6f600c602f6149e5f2ffe5636df45368e11c413ea04e0393f`.
- Device: Samsung SM-G970U, Android 12, serial `R58M83HFYYY`.
- Testers: Codex for automated checks, installation and Android diagnostics; Ambrose for physical phone tilt and SIM observation.
- QGroundControl: not used in this device check; no QGC version or interoperability result is claimed.

## Results

- `GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest lintDebug assembleDebug --console=plain`: successful; 175 tests, zero failures, errors or skips.
- The prior local-takeoff reproduction produced zero simulated roll despite nonzero pilot input. Regression tests now verify motor response immediately after local arming; roll, pitch and yaw response after local takeoff; stale navigation/RPM recovery; ALT_HOLD preservation; disarm motor cutoff; and GCS takeoff retaining GUIDED.
- Two AppRuntime tests exercise the actual local Arm and Takeoff facade and verify Phone Sensors remains selected with Controller authority.
- `adb install -r` succeeded, preserving app data. The installed package reports version 1.5.3/code 18 and update time 2026-10-10 07:49:44 (device clock).
- MAVLab launched with an active `PhoneSensorRepository` Game Rotation Vector registration. The Android crash buffer was empty during this bounded check.
- Ambrose followed Phone Sensors → Calibrate → Arm → Takeoff → SIM → gentle phone tilt and confirmed: “Yes, it responds.” This confirms the reported still-3D-drone issue is resolved in this installed build.

## Limits

The device check covers foreground local phone-sensor control and the SIM view. GCS takeoff separation is verified by unit tests here. Live QGC behavior, background handoff, process recreation, missions, failure injection and release qualification were not re-exercised on the device in this check. The impact matrix and canonical contract are recorded in [ADR-0005](../../internals/decisions/0005-canonical-phone-sensor-control.md).
