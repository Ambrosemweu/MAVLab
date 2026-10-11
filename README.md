# MAVLab by Ascend Labs

A phone-first, offline-capable drone simulator and digital twin. Learn attitude, control, telemetry, missions and failure recovery on Android before taking on ROS, Gazebo, ArduPilot/PX4 SITL, Docker or a multi-process simulator stack.

Physics, autopilot, visualization and the MAVLink endpoint run on the phone. QGroundControl is optional for the core learning loop; no aircraft, cloud service or external simulation server is required.

**Latest release:** [**v1.6.0**](https://github.com/Labs-Ascend/MAVLab/releases/tag/v1.6.0), Android version code **19**, published on 12 October 2026 (Africa/Nairobi). See the [release notes](docs/v1_6_0_release_notes.md) and [publication verification](docs/validation/results/v1.6.0-2026-10-12-publication.md). Stable signing and final-artifact phone/QGC qualification remain explicitly deferred.

## Four connected views

| Cockpit | Control | SIM | Ops |
|---|---|---|---|
| [![Cockpit — vehicle state and telemetry](assets/media/2026-10/screenshots/cockpit.jpg)](docs/user/cockpit.md) | [![Control — local input and flight actions](assets/media/2026-10/screenshots/control.jpg)](docs/user/controller.md) | [![SIM — engine-driven digital twin](assets/media/2026-10/screenshots/sim.jpg)](docs/user/sim.md) | [![Ops — health, connectivity and setup](assets/media/2026-10/screenshots/ops.jpg)](docs/user/ops.md) |

Missions span these views: concise Cockpit progress, expandable Control actions, received-route geometry in SIM and exact upload evidence in Ops. There is no separate Mission tab, built-in sample route or onboard mission planner.

## Demos

### MAVLab on-device

Local controls, live telemetry/charts and the 3D digital twin, with original recorded sound.

<a href="assets/media/2026-10/mavlab-on-device.mp4"><img src="assets/media/2026-10/mavlab-on-device-poster.jpg" alt="Play the on-device MAVLab demo" width="280"></a>

[Watch/download MP4](assets/media/2026-10/mavlab-on-device.mp4) · 84 seconds · 60 fps.

### MAVLab + QGroundControl

QGC plan editing and simulated AUTO flight, with the MAVLab phone view and its audio alongside it.

[![Play the QGC and MAVLab demo](assets/media/2026-10/mavlab-qgc-poster.jpg)](assets/media/2026-10/mavlab-qgc-demo.mp4)

[Watch/download MP4](assets/media/2026-10/mavlab-qgc-demo.mp4) · 118 seconds · 1080p. The clips are approximately aligned; the recording ends during RTL, before touchdown. It does not demonstrate fence/rally acceptance or every protocol capability.

[Media gallery, captions and provenance](assets/media/2026-10/README.md).

<details>
<summary>Earlier v1.5 demos</summary>

On-device: https://github.com/user-attachments/assets/31fe9b06-06e0-4d55-aa34-e33151d50802

QGC: https://github.com/user-attachments/assets/1b7cdebd-bcb9-48b3-9dc3-ca9344e6c6aa

</details>

## What v1.6.0 adds

- **A focused interface:** Cockpit, Control, SIM and Ops; contextual missions; a nine-page onboarding tour ending with **Start flying**.
- **Fluid navigation:** continuous guarded horizontal swipes, compact labelled tabs and optional full-screen display. Sliders, route gestures and the 3D camera retain their own input.
- **Engine-owned control:** calibrated Phone Sensors, Custom Input and deliberate Direct RPM. Foreground phone steering continues across Cockpit, Control and SIM; GCS missions and fence recovery remain distinct authorities.
- **Readable telemetry and SIM:** one combined altitude/attitude chart, a centered starting camera and inspection lighting that keeps the model visible when orbiting.
- **Ops health and recovery:** explicit GCS link state, incidents, bounded transition history, diagnostics and failure exercises.
- **Opt-in recording:** recording is **off by default**, including AUTO missions. Begin explicitly in Ops or enable recording on arm while disarmed; stop/save, review and export sessions.
- **MAVLink 2 and resilient transfers:** unsigned MAVLink 2 output, CRC-checked supported input, targeted ACKs, missing-item retries and preservation of accepted plans after failed replacement uploads.
- **Simulated fences and rally returns:** separate safety-plan stores, explicit fence enablement, engine-driven breach recovery, eligible rally/home RTL, landing and disarming. See [flight boundaries](docs/user/flight-boundaries.md).

The simulator uses flat ground and straight-line return control. It does not implement terrain/obstacle routing, SmartRTL, MAVLink signing, full ArduPilot equivalence or real-aircraft safety qualification. Timed loiter and waypoint pause semantics remain limited. Implemented, partial, no-op and unsupported behavior is documented in the [MAVLink contract](docs/internals/mavlink-contract.md).

## Start learning

**Signing warning:** v1.6.0 is distributed as a locally verified debug build. It cannot update the official v1.5.3 APK in place because their certificates differ. Export recordings/save QGC plans and read [upgrade guidance](docs/user/upgrading.md) before deciding to change installations. Uninstalling erases local app data; no automatic migration is provided. Stable release signing is the [next-release priority](docs/operations/release-signing-next.md).

### Published APK

Download [MAVLab-v1.6.0.apk](https://github.com/Labs-Ascend/MAVLab/releases/download/v1.6.0/MAVLab-v1.6.0.apk) from the [published release](https://github.com/Labs-Ascend/MAVLab/releases/tag/v1.6.0), which also includes sound-enabled demos, screenshots and `SHA256SUMS.txt`. Android may ask you to allow installation from your file manager or browser. Read the signing warning above before changing an existing installation.

### Build v1.6.0

Requires Java 17 and Android SDK 35; minimum Android version is 8.0 (API 26).

```bash
cd mavlab-android
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease --console=plain
adb install -r app/build/outputs/apk/debug/MAVlab.apk
```

The downloadable APK path is a debug-signed build. The release AAB needs production signing and Play Console qualification before Play distribution; it is not directly installable. See the [release runbook](docs/operations/release.md).

### First local flight

1. Complete or skip onboarding; **Start flying** opens Cockpit without arming.
2. In Control, choose Phone Sensors and calibrate while grounded.
3. **Arm → Takeoff**; observe the same simulated vehicle in Cockpit and SIM.
4. **Land → Disarm**. Disarming cuts motor thrust; do not use it as normal airborne landing.
5. For a saved session, begin recording in Ops before flight. Recording never starts merely because you opened the app or uploaded a mission.

## Connect QGroundControl

- **Same phone:** open MAVLab, then switch to QGC within the one-minute handoff window. Telemetry targets `127.0.0.1:14550`; split-screen is optional.
- **Desktop over LAN:** use the same trusted Wi-Fi network/subnet. Client isolation and a firewall blocking UDP 14550 can prevent discovery.
- **Identity:** MAVLab currently uses SYSID 1 / COMPID 1; QGC normally uses SYSID 255. Do not place multiple same-ID simulators or real aircraft on the test network.
- **Continuity:** a validated GCS heartbeat, armed flight or active mission retains the background runtime. Phone input suspends in the background; motor audio follows retained simulation. Idle/disconnected runtime stops after the handoff policy expires.
- **Uploads:** keep the simulated vehicle disarmed while diagnosing connection and plan transfers. After upgrading, save your plan and reconnect QGC to refresh capabilities. Upload navigation, fence and rally lists independently; uploading a fence does not enable it.

GCS connection is not control authority or proof of successful command/mission delivery. See [setup](docs/setup_guide.md), [troubleshooting](docs/known_first_run_issues.md) and [mission operation](docs/user/mission.md).

## Documentation and evidence

- [Documentation home](docs/README.md), [user guides](docs/user/README.md) and [teacher guide](docs/teacher_guide.md).
- [Architecture](docs/internals/overview.md), [control authority](docs/internals/control-authority.md), [runtime lifecycle](docs/internals/runtime-lifecycle.md) and [MAVLink contract](docs/internals/mavlink-contract.md).
- [Contributor guide](AGENTS.md), [Android implementation](mavlab-android/README.md), [release runbook](docs/operations/release.md) and [v1.6.0 readiness](docs/operations/v1.6.0-readiness.md).
- [Validation records](docs/validation/results/README.md). Prior build/device passes are not automatically acceptance of the new versioned artifact.
- Published release notes: [v1.6.0](docs/v1_6_0_release_notes.md), [v1.5.3](docs/v1_5_3_release_notes.md), [v1.5.2](docs/v1_5_2_release_notes.md), [v1.5.1](docs/v1_5_1_release_notes.md) and [v1.5](docs/v1_5_release_notes.md).

## Technical paper

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.21319694.svg)](https://doi.org/10.5281/zenodo.21319694)

**MAVLab: A Phone-First Drone Digital-Twin Simulator for Flattening the Drone-Systems Learning Curve** — Kioko & Kaira, Ascend Labs, 2026. [DOI](https://doi.org/10.5281/zenodo.21319694) · [Markdown source](docs/paper/paper.md) · [How to cite](CITATION.cff).

The paper describes its evaluated release snapshot. MAVLab v1.6.0 does not silently inherit those acceptance results or revise the published research claims.

## License

Apache License 2.0. See [LICENSE](LICENSE).
