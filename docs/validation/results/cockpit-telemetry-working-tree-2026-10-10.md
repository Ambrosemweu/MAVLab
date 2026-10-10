# Cockpit telemetry readability — 2026-10-10

Scope: Working-tree UI validation on `feature/mavlab-uiux-upgrade`; this is not a release qualification.

## Build identity and evidence

- App: MAVLab 1.5.3, version code 18, debug build.
- Base commit: `703b7f90d717ff386ee9a232bbc5a51a38af3ca0`, with uncommitted UI/UX and phone-control changes.
- APK SHA-256: `4a66442944bc1ee89904645e7ae1193c98aed9bac24c29f647e0617fbe19335d`.
- Device: Samsung SM-G970U, Android 12, serial `R58M83HFYYY`, portrait.
- Tester: Codex, through Gradle and ADB; date 2026-10-10.
- QGroundControl: not used; protocol compatibility is outside this UI check.
- [Installed chart screenshot](evidence/cockpit-telemetry-2026-10-10.png).

## Results and impact

| Row | Scope and verification |
| --- | --- |
| Product surfaces | Cockpit telemetry dropdown. Device screenshot and UI hierarchy show separate Altitude above ground and Vehicle attitude charts, named line keys, latest values, metre/degree labels, and 10 s ago / 5 s / Now labels. Solid/dashed/dotted keys match the attitude traces, including coincident zero values. Other surfaces are not changed. |
| Control paths | Not applicable: charts read DroneState; phone/manual input and GCS authority are unchanged. |
| Runtime states | Foreground, disarmed ground state checked on the phone. Histories remain bounded and sample only with the dropdown expanded; reopening clears the old window. Background and process recreation were not re-exercised. |
| Simulation | No engine, physics or autopilot changes. Altitude remains AGL in metres; roll/pitch/yaw are converted from engine radians to displayed degrees. |
| Protocol | Not applicable: no MAVLink changes or live QGC check. |
| Reverse states | Dropdown expands and retains its Hide telemetry & details control; hidden sampling is cancelled by the existing effect lifecycle. Flight reverse actions are unchanged. |
| Evidence | Full testDebugUnitTest, lintDebug and assembleDebug gate succeeded: 179 tests, zero failures/errors/skips. Four new tests check signed attitude coordinates, zero-ground altitude scaling, stationary/non-finite samples, and correct ten-second placement of short/full histories. APK installed with adb install -r, then launched. Android crash buffer was empty during the bounded UI check. |
| Documentation | Cockpit user guide explains how to read the traces, units, time window and simulated-vehicle source. ADR-0004 records the chart presentation and sampling rules. |

The device check does not qualify flight, QGC, large-font layouts, landscape, or release behavior. Existing canonical armed phone-sensor control remains protected by its regression tests and ADR-0005.
