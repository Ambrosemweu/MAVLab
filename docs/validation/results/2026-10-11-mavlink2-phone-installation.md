# MAVLink 2 update — installation and isolated phone checks

Date/time: 2026-10-11, Africa/Nairobi, installation at 23:07:24; checks completed before 23:08:13.
Tester: Codex automated ADB/instrumentation checks.
Device: Samsung SM-G970U, serial `R58M83HFYYY`, Android 12 (read from the device during this run).
Application: MAVLab 1.5.3, version code 18, uncommitted working-tree build on `feature/mavlab-uiux-refinements`, base commit `de8b8e9e5d83235aea69d5b4bb7dff6ec95caada`.
Input APK SHA-256: `6b085193941ca3c0716012782127de1b1c938f65c35e1969b6dd12685b5ba956`.
QGC: Daily was open on the laptop; exact build version and live interoperability were not verified in this execution.

## Installation

User authorized installation. `adb install -r` returned `Success` for the verified application APK and the instrumentation APK. Package inspection confirmed lastUpdateTime `2026-10-11 23:07:24`, versionName 1.5.3 and versionCode 18. No uninstall, app-data clear or preference change was performed.

The pre-upgrade preference-hash command failed due to shell quoting, so there is no pre-upgrade hash comparison. Immediately after installation the four preference files (audio, application, mission, recording) existed and the flights directory contained 388 files. Their hashes before and after isolated instrumentation matched. This establishes that the tests did not alter those preferences, not an independently measured before/after upgrade comparison of every user file.

## Isolated Android protocol checks

Command:

```bash
adb -s R58M83HFYYY shell am instrument -w -r -e class com.ascend.mavlab.core.mavlink.MavlinkProtocolDeviceTest com.ascend.mavlab.test/androidx.test.runner.AndroidJUnitRunner
```

Result: `OK (4 tests)`, time 0.14 seconds, instrumentation code -1 (normal success).

| Check | Result |
|---|---|
| Batched compressed four-item upload including CHANGE_SPEED and repeated final ACK without reloading the engine mission | PASS |
| Timed-out upload retains the previously loaded mission | PASS |
| Unsupported mission type cannot clear the navigation mission | PASS |
| Unsupported message request returns a targeted MAVLink 2 ACK | PASS |

These checks used a separate, non-running simulation engine and loopback sockets. They did not contact QGC, arm the application vehicle, change flight authority, upload a mission into the application runtime, or start recording. They establish Android execution of the protocol paths, not real QGC mission-transfer acceptance.

## Deferred launch/QGC check

The phone disconnected from ADB after the tests. The subsequent installed-file hash query and MainActivity launch both returned `device not found`. Therefore this record does not claim successful updated-app launch, live QGC telemetry reception, or live QGC upload/download. Those checks require reconnection/opening MAVLab and a controlled, disarmed QGC session. No flight command was sent.

## Impact review

Protocol, mission-engine loading/preservation and upload reverse-state cases have the isolated device evidence above. Product-surface rendering, foreground/background handoff, app stop/reconnect and actual GCS direct/mission control remain unverified for this artifact. Local/manual sensors, physics, recording and onboarding were not changed or exercised. Host evidence and canonical documentation review are recorded in [host verification](2026-10-11-mavlink2-host-verification.md).

No commit, push, merge or release was performed.

## Follow-up after USB reconnection (same execution)

The phone returned to ADB at 23:08:51. A SHA-256 query of the installed `base.apk` matched the input artifact exactly. MainActivity launch returned `Status: ok`, `LaunchState: WARM`, `TotalTime: 1959`; activity inspection confirmed MAVLab resumed, PID 31047. SimulationService was foreground with startRequested=true.

At 23:09:01 the application started its real UDP endpoint (SYSID 1, COMPID 1, local port 14551). It received two QGC SET_MESSAGE_INTERVAL commands (511) and continuous GCS heartbeats from the laptop. From 23:08:50 through 23:09:56, the QGC journal contained no new message-request timeout warnings, unlike the repeated warnings before installation. This is a limited live connection/ACK observation; it is not a fresh-session initialization or mission-upload acceptance test. Legacy REQUEST_DATA_STREAM packets remain unsupported under the documented fixed-rate telemetry policy.

Preference hashes remained unchanged after launch. No automated flight, mission replacement, recording or QGC settings command was sent. Live QGC mission upload/download and flight qualification remain deferred.

A subsequent screenshot confirmed Cockpit rendered READY / DISARMED, IDLE authority, STABILIZE mode, 0.0 m AGL and QGC CONNECTED. The connection indicator is heartbeat evidence only, not mission interoperability. The saved-flight file count remained 388. The temporary phone screenshot was removed after transfer; its local diagnostic copy is `/tmp/mavlab-protocol-jYXTH2/installed-phone.png` (ephemeral, not durable release evidence).
