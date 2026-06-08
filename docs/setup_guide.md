# MAVLab Setup Guide

## Install

1. Install MAVLab on an Android 8.0+ phone.
2. Install QGroundControl if you want ground-station integration.
3. Open MAVLab and complete onboarding.
4. Use the Lessons tab for the guided curriculum.

## QGroundControl

For same-device use, run MAVLab and QGroundControl in split-screen. MAVLab broadcasts MAVLink telemetry over UDP and QGC should discover it automatically.

For desktop QGC, put the Android phone and desktop on the same Wi-Fi network. MAVLab broadcasts to common LAN destinations and UDP port `14550`.

## Build From Source

```bash
cd mavlab-android
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew lintDebug testDebugUnitTest assembleDebug
```

Install the debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Troubleshooting

- If QGC does not connect, restart QGC and MAVLab, then confirm both apps are on the same network.
- If the phone controller is unavailable, use manual fallback sliders in the Controller tab.
- If the 3D model does not render smoothly, continue using Dashboard and Lessons; the simulator keeps running.
