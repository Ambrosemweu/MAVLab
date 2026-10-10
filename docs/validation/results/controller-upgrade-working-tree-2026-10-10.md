# Controller upgrade checkpoint — 2026-10-10

## Identity and scope

- Source commit: `126def1dcd8776d078b1f9956b1de80a3b420dc2` on `feature/mavlab-uiux-refinements`.
- MAVLab 1.5.3/code 18; final debug APK SHA-256: `1fdc0b9d45d42f3ed94cf456174c7d482d01d0fdc3d77d44d956b364e6bb159d`.
- Tester: Codex; date: 2026-10-10, Africa/Nairobi (UTC+03).
- Device for the earlier installation: Samsung SM-G970U, Android 12, serial `R58M83HFYYY`.
- QGroundControl was not used; no QGC build, live endpoint or interoperability outcome applies.
- The user explicitly deferred final phone installation/interaction checks after USB disconnected.
- The user also requested review of the approved Mission Consolidation Decision and updated consolidated specification, then explicitly chose **Handle consolidation next**. This checkpoint therefore retains Controller and the five-tab shell. The future Control name, four-tab navigation, contextual mission row and distributed mission presentation are not implemented by this checkpoint.

## Automated result

```bash
cd mavlab-android
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest lintDebug assembleDebug --console=plain
```

Result: BUILD SUCCESSFUL in 1m 43s; **193 tests, zero failures, errors or skips**. Lint and debug assembly passed. [Final Gradle log](evidence/controller-gradle-gate-2026-10-10.log).

The gate ran against the pre-commit implementation tree. Afterwards, one trailing blank line at the end of ControllerTools.kt was removed for the Git whitespace check; no executable code changed. The source commit identifies the committed implementation. The validation checkpoint changes only documentation and the captured build log.

Thirteen added tests cover nine Controller presentation scenarios, three runtime integration scenarios, and Direct RPM authority gating. The runtime integration tests step the actual engine and assert custom roll/pitch/altitude response and Direct RPM motor speed without composing Controller. The existing canonical Phone Sensors tests remain in the full gate.

## Device result and limitation

An earlier Controller working build was installed with `adb install -r`, preserving app data. Installation returned **Success**, and package metadata reported version 1.5.3/code 18 and update time **2026-10-10 09:08:35**. That earlier APK checksum was `f2672b82c1d8deb7a9760ceec9db3dfb35272c17a8b5bb88858e3d65128313d2`.

The earlier APK predates final feedback-layout/stale-test-input polish and explicit Direct RPM suspension under GCS_DIRECT. It is **not** the final APK identified above. Installation of a subsequent build was interrupted by USB disappearance. The user chose to finish checks later.

No final-build portrait, landscape, large-font, calibration, flight-action or sensor/SIM scenario is marked passed on the device. No screenshots or hierarchies from unrelated foreground applications are retained as MAVLab evidence.

## Impact matrix

| Row | Scope and verification |
| --- | --- |
| Product surfaces | Controller gains persistent operational status and contextual action dock, input-mode/calibration locks, clear input availability, units, progressive tools and a compact landscape composition. Unit/build evidence; final visual/device checks pending. Cockpit/SIM/Mission/Ops remain existing surfaces; shell callbacks now link Controller recovery to Mission/Ops. |
| Control paths | Canonical foreground Phone Sensors acceptance/mapping is unchanged and existing engine/facade tests pass. Custom Input application moves to AppRuntime; deterministic runtime tests prove physical roll/pitch/altitude response. Direct RPM also moves to runtime; motor-speed and external-authority gating tests pass. Live GCS workflows pending. |
| Runtime states | Shared advanced input no longer depends on Controller composition. Runtime observation suspends Direct RPM under external authority. Foreground integration is unit tested; background retention, service lifecycle and process restoration are unchanged and were not requalified on hardware. |
| Simulation | Fresh Custom Input arm restores a manual mode through the existing armForLocalInput engine path. Physics equations, authoritative DroneState publication, autopilot navigation, failures and battery are otherwise unchanged. No UI-only vehicle movement. |
| Protocol | MAVLink parsing, ACKs, identity, advertised capabilities and mission protocol are unchanged. Direct RPM respects observed GCS mission/direct authority. Existing protocol tests run in the full suite; no live QGC claim. |
| Reverse states | Contextual Arm/Takeoff/Land/Disarm, landed disarm, motor stop, stale-state recovery and deliberate external landing takeover are covered by presentation/runtime tests. In-flight disarm and Direct RPM entry have confirmation UI. Final manual execution pending. |
| Evidence | 193 passing JVM tests, lint, assembly, APK checksum and Git whitespace review. Earlier-build installation only; final installation and physical-device interaction deferred by user. |
| Documentation | Controller user guidance, runtime lifecycle, control-authority and simulation contracts reviewed; ADR-0006 records the layout, shared input ownership and unchanged sensor invariant. Mission consolidation is explicitly the next phase. |

## Updated specification review

The new approved Mission Consolidation Decision and the updated consolidated specification supersede the older five-surface design direction for the next implementation phase. The related Controller/Cockpit redesign plans, MAVLab Surface Directions and Target Experience Flows still contain standalone Mission and demo-route references; those are earlier design proposals, not the new navigation requirement.

The reviewed direction is Cockpit, Control, SIM and Ops; conditional mission status/actions in Control; concise awareness in Cockpit; actual received-route geometry in SIM; and protocol/upload evidence in Ops. Mission authoring stays in QGC. Sample routes and normal-product Load route UI are removed in that phase, while deterministic fixtures may remain internal.

A live-code finding for that later phase: MissionPersistence currently stores mission items without source/provenance, and the earlier user-facing demo path saves into the same storage. Legacy stored data therefore cannot automatically be described as a verified received QGC mission. Its provenance/migration requires explicit review before claiming the new real-route guardrail is satisfied. No persistence format or mission behavior changed in this Controller checkpoint.

The user explicitly chose to handle consolidation next, so this review did not change the current navigation, Controller title, SIM route presentation or Ops mission UI.

## Remaining device checks

1. Install the final identified APK without clearing application data.
2. Check portrait, landscape, large text, pinned warnings/actions and scrolling controls.
3. Phone Sensors: calibrate neutral, arm, take off, verify physical tilt response in SIM, land and disarm.
4. Custom Input: verify normalized sliders, cross-surface engine response and arm/disarm reset.
5. Direct RPM: verify confirmation/cancel, zero target, motor response, cross-surface persistence, external-authority suspension and Stop motors exit.
6. With isolated QGC, exercise received mission authority and confirmed local landing recovery; record QGC build and endpoint evidence separately.

This is a development checkpoint, not a release or a claim that all UI/UX phases or device scenarios are complete.
