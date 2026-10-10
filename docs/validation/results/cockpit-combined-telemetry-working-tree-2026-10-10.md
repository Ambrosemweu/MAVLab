# Combined Cockpit telemetry — 2026-10-10

Scope: Working-tree UI validation on `feature/mavlab-uiux-upgrade`, following the user's request to combine the two telemetry graphs. This is not a release qualification.

## Build identity

- App: MAVLab 1.5.3, version code 18, debug build.
- Base commit: `703b7f90d717ff386ee9a232bbc5a51a38af3ca0`, with uncommitted UI/UX and phone-control changes.
- APK: `mavlab-android/app/build/outputs/apk/debug/MAVlab.apk`.
- APK SHA-256: `a323141616a6358be62757a8c642d294d4975d6e544bda40ad3fc1ef3adf71d7`.
- Tester: Codex; date 2026-10-10.
- Device/Android/QGroundControl versions: not applicable to this execution; the user reported the phone disconnected. This build has not been installed or visually checked on a device.

## Results and impact

| Row | Scope and verification |
| --- | --- |
| Product surfaces | Cockpit replaces two chart cards with one Flight telemetry chart and shared time axis. Four line keys retain names, values and units. Altitude uses amber and the labelled left metre axis; roll/pitch/yaw use teal/blue/purple and the labelled right degree axis. Device layout review remains pending. |
| Control paths | Not applicable: charts observe DroneState; sensor mapping, local/manual control and GCS authority are unchanged. |
| Runtime states | No runtime-policy change. The bounded expanded-only sampling and fresh window on reopening remain in place. Device foreground, background and recreation were not tested in this execution. |
| Simulation | No engine/physics/autopilot change. A regression test verifies altitude and attitude are scaled independently, including a 10,000 m altitude with attitude limited to ±90°. The same physical values can occupy the same plot height but are read against their respective units. |
| Protocol | Not applicable: no MAVLink changes or QGC exercise. |
| Reverse states | Expand/hide telemetry behavior and all flight actions are unchanged. |
| Evidence | `GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest lintDebug assembleDebug --console=plain` succeeded: 180 tests, zero failures/errors/skips. Existing signed-attitude, ground-altitude, invalid-sample and ten-second-window tests also passed. `git diff --check` was clean. |
| Documentation | Cockpit user guide and ADR-0004 now describe the combined chart, independent axes and distinct yaw colour. Prior two-chart screenshots remain historical evidence of their earlier build. |

Canonical armed phone-sensor control remains unchanged and covered by the full regression suite. Installation and visual review await a connected phone.
