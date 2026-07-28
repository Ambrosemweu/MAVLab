# MAVLab v1.5.2 Release Notes

MAVLab v1.5.2 is a UI and documentation release. The first-run onboarding has been rebuilt from the ground up as native Jetpack Compose, and the "how do I connect a ground station?" guidance has been corrected across the app and docs. There are no protocol, physics, or autopilot changes in this release.

---

## What's New

### 1. Onboarding rebuilt natively in Compose
The previous onboarding shipped baked screenshots that squashed or letterboxed on real devices (a 360×780 raster never matches a phone's safe area). It has been replaced with a native, flex-layout flow that adapts to any screen height — no more stretching.

- **Reuses the real app components** so the instruments are pixel-accurate: the attitude instrument (`AltitudeInstrument`), telemetry cards, the tilt crosshair (`TiltVisualizer`), and the Controller's throttle / yaw-trim sliders (`ControlSlider`).
- **Hand-drawn visuals** faithfully port the design frames: the drone glyph, the ground-station link diagram, the arm/takeoff scene, the mission-waypoint plot, the failure preview, and the telemetry log panel.
- The baked `onb_*.png` assets are removed.

### 2. "Connect a ground station" (not just QGroundControl)
The connect screen is generalized: MAVLab broadcasts MAVLink for **QGroundControl or any GCS** to discover, and the phone/desktop illustration now reads as a device streaming to a ground station.

- The **Open QGroundControl** button now links to the official QGC Android [download-and-install docs](https://docs.qgroundcontrol.com/master/en/qgc-user-guide/getting_started/download_and_install.html#android), since QGroundControl for Android is not on the Play Store.

### 3. Connection guidance corrected: no split-screen required
You do **not** need Android split-screen to connect a GCS on the same device. MAVLab runs as a foreground service holding wake + Wi-Fi locks (since v1.5.1), so its MAVLink stream keeps flowing in the background — just keep MAVLab running and open your GCS on `127.0.0.1:14550`. Split-screen is now described only as an optional way to view both apps at once. This was corrected across the onboarding copy, in-app lessons/settings, and every doc (READMEs, setup guide, first-run issues, teacher guide, demo script, acceptance spec, guidelines, and the paper).

### 4. Copy
- The final onboarding button now reads **"Start flying"**.

---

## Upgrade Instructions
1. Download `MAVLab-v1.5.2.apk` from the GitHub Releases page and install it over the existing app, or build from source:
   ```bash
   cd mavlab-android
   GRADLE_USER_HOME="$PWD/.gradle" ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/MAVlab.apk
   ```
2. New installs land in the rebuilt onboarding automatically. To replay it, use Ops → Replay tour.
