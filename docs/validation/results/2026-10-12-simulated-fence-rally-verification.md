# Simulated fence/rally — working-tree verification

Execution: 2026-10-11 through 2026-10-12, Africa/Nairobi; final review at 00:05 on October 12.
Tester: Codex, automated host and ADB/instrumentation checks.
Application: MAVLab 1.5.3, version code 18, debug working-tree build on `feature/mavlab-uiux-refinements`.
Base commit: `de8b8e9e5d83235aea69d5b4bb7dff6ec95caada`. The changes are uncommitted, not released.
Device: Samsung SM-G970U, Android 12, serial `R58M83HFYYY`.
QGC: Daily was running on the laptop; its exact build version was not verified. Fresh-session upload and live QGC flight acceptance remain pending.
APK SHA-256: `6844dc2e487e48dd2fc76d4d9ff6bb193883cd9acb724c8fc42a3dffdb455680`.

## Host gate

Executed from `mavlab-android` with source frozen during the final run:

```bash
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --max-workers=2 --console=plain
```

Result: BUILD SUCCESSFUL, 7m 15s. JUnit XML totals: 307 tests, zero failures, errors or skips. Debug application and instrumentation APKs assembled. Lint: zero errors, 34 warnings, including obsolete Compose custom-check APIs which caused those library checks to be skipped. A successful lint task therefore does not establish execution of every Compose library detector. No dependency upgrade or lint suppression was added for this feature.

Focused coverage includes geometry/configuration/codec validation, atomic disarmed plan/origin changes, prearm refusal, minimum-altitude takeoff/landing semantics, report-only and actionable breaches, recovery authority/reverse paths, motor override clearing, eligible return selection, MSL conversion, GPS-loss fallback, ground RTL never launching, terminal mission RTL, manual phone input eligibility, typed upload ownership/retries/duplicates, retained plans after failed transfers, and snapshot-consistent download.

## Installation and user-data preservation

User authorized installation and isolated phone tests. `adb install -r` succeeded for both APKs. Package inspection shows version 1.5.3/code 18 and lastUpdateTime `2026-10-11 23:59:24`. The installed `base.apk` SHA-256 matched the built application artifact. No uninstall, application-data clear or recording deletion was performed.

The four existing preference files matched before and after installation/tests:

| File | SHA-256 |
|---|---|
| `drone_sound.xml` | `0e97e42ed75e576fae3870afb195c5068a50e19d4ad35c57d040954c353a73c1` |
| `mavlab_app.xml` | `48014cc7bf6aea19f98bcacb7cf0a57b0459d5657d65fa3ee52803bcb3e8d3fb` |
| `mavlab_mission.xml` | `3325d2a819fdd8062c2cdc48a09b995c9b012915bcdf88b1cf9742a7f057c793` |
| `mavlab_recording.xml` | `8f5370e242a2465a7fad950f2254065de4e931b0f823fa4de502513992e1a734` |

The saved-flight directory retained 388 files. Its sorted per-file SHA-256 manifest digest matched before/after: `55f852a28fd8322b0194c8d1b3bfc601d606bb8edc833094ebd225d5d825b215`. This checks recorded file contents, not just file count. Main-app launch created the new independent `mavlab_safety.xml` with empty safety lists and enforcement disabled; it did not replace the existing navigation mission or enable recording.

## Isolated Android checks

```bash
adb -s R58M83HFYYY shell am instrument -w -r -e class com.ascend.mavlab.core.mavlink.MavlinkProtocolDeviceTest com.ascend.mavlab.test/androidx.test.runner.AndroidJUnitRunner
```

Result: `OK (7 tests)`, 0.244 seconds, instrumentation code -1 (normal completion).

| Check | Result |
|---|---|
| Batched compressed navigation upload and repeated final ACK without loading twice | PASS |
| Exhausted upload retains the accepted navigation mission | PASS |
| Unsupported mission type cannot erase navigation | PASS |
| Typed fence/rally upload, download and clear preserve navigation | PASS |
| Unsupported message request receives a targeted MAVLink 2 ACK | PASS |
| Engine flies to an eligible rally destination, lands and disarms | PASS |
| Fence breach drives return, landing and disarming | PASS |

These tests used isolated simulated engines and loopback sockets, not the user's AppRuntime mission or QGC. Deterministic engine stepping is Android execution evidence, not a measured real-time flight-performance test. No automated arm, takeoff, mission replacement or recording command was sent to the main application.

## Main-app observation

The updated main application opened successfully (cold launch reported 2122 ms). Cockpit showed READY/DISARMED, IDLE authority, STABILIZE, zero altitude and a validated QGC heartbeat. Ops showed disarmed state, recording stopped, no active injected conditions, and the new Flight boundaries & rally card. The card expanded and vertically scrolled to expose its explanation and controls. It showed zero fence/rally items, zero breaches, no return destination, an unchecked enabled fence switch, and a disabled clear button for the empty plans. No actual fence enable/clear or flight action was exercised on the user's runtime. No AndroidRuntime crash was observed in the bounded launch check.

A connected heartbeat is not mission-transfer acceptance. QGC needs a fresh capability handshake and a controlled re-upload check against this installed artifact. Supported nonempty fence/rally uploads and actual main-app QGC recovery flights have not been manually accepted in this record.

## Impact matrix and remaining acceptance

| Area | Evidence and limits |
|---|---|
| Product surfaces | Cockpit/Ops observed on phone; Control recovery state covered by host tests. SIM observes engine state but was not visually exercised during a recovery flight. Onboarding unchanged. |
| Control paths | Isolated engine/protocol tests cover mission/direct navigation and safety authority; disabled-fence manual phone eligibility covered by unit tests. No new physical phone-tilt flight check or live QGC flight check. |
| Runtime states | Foreground launch and idle QGC heartbeat observed. Background handoff, explicit stop/reconnect and restored safety preferences after main-process recreation were not exercised here; codec/restore logic has host coverage. Existing lifecycle policy is unchanged. |
| Simulation | Fence enforcement, return selection, terminal RTL, landing/disarm, GPS failure and mission progression covered by deterministic tests. No obstacle-aware routing, terrain or SmartRTL claim. |
| Protocol | Typed transactions, telemetry/capabilities and ACK behavior have host and isolated Android evidence. Fresh live QGC upload/download remains pending; real-aircraft compatibility is out of scope. |
| Reverse states | Disarm/landing, disable/manual recovery and typed/all-store clears tested on isolated engines. Main-app clear confirmation and armed disable interaction not manually exercised. |
| Evidence | 307 host tests, seven Android tests, successful lint/build, installed artifact hash and preserved data. No release or comprehensive hardware qualification. |
| Documentation | User flight-boundary/controller/mission/Ops guidance, MAVLink contract, glossary, authority, simulation, runtime lifecycle and decision 0013 reviewed/updated. |

User workflow: [flight boundaries and rally points](../../user/flight-boundaries.md). Canonical limits: [MAVLink contract](../../internals/mavlink-contract.md#simulated-fence-and-rally-contract-working-tree). No commit, push, merge, tag or release was performed.
