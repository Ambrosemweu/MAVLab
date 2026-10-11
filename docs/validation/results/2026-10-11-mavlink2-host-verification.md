# MAVLink 2 and mission recovery — host verification

Execution date: 2026-10-11, Africa/Nairobi (completed 22:12)
Tester: Codex automated host checks
Branch: `feature/mavlab-uiux-refinements`
Base commit: `de8b8e9e5d83235aea69d5b4bb7dff6ec95caada`; changes are uncommitted working-tree behavior.
Application: MAVLab 1.5.3, version code 18 (no release/version increment).
APK SHA-256: `6b085193941ca3c0716012782127de1b1c938f65c35e1969b6dd12685b5ba956`.

## Diagnostic evidence

The open laptop application identified itself as QGC Daily in its screenshot/logs. Its logs show MAVLink 1 traffic detection and repeated command/message-request timeouts. Exact Daily build version was not retrieved; no live interoperability PASS is claimed here. MAVLab's previous outbound frame builder used 0xFE while its capabilities included MAVLINK2. Phone logs confirm receipt of QGC COMMAND_LONG requests over the LAN.

The earlier regular-QGC telemetry log `2026-10-11 20-25-48.tlog` contains a six-item upload beginning at 20:23:41 local time: request/item exchanges for sequences 0–3, followed by no recorded request for sequence 4 or mission ACK. Sequence 3 is CHANGE_SPEED, command 178, frame MISSION (2). This locates the stalled exchange but does not independently establish whether a datagram/request was lost or ignored. The updated decoder/retry path addresses confirmed implementation gaps without claiming that historical root cause is proven.

## Completed host gate

From `mavlab-android/`:

```bash
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --max-workers=2 --console=plain
```

Final run: BUILD SUCCESSFUL. 277 unit tests; zero failures, errors or skipped tests. Lint and debug/application-test APK assembly passed. `git diff --check` passed.

New unit coverage includes independent pymavlink 2.4.50 heartbeat bytes/CRC, v1 compatibility, compressed command/parameter/mission payloads, batched mixed-version frames, bad CRC and incomplete data, signed/unknown incompatibility rejection, 24-bit ID separation, sequence wrap, targeted ACKs, engine-derived landed state, missing-item retry bounds, progress-based expiration, gap/duplicate behavior and uploader ownership. Common-dialect base lengths and CRC extras were independently compared against pymavlink definitions. Pymavlink was installed only in a temporary validation environment, not added to the product.

Four isolated Android tests were compiled for batched upload/duplicate final ACK, unsupported mission-type preservation, timeout preservation and unsupported-message ACKs. They have **not been run on the phone** in this record.

## Impact matrix

| Area | Applicability and evidence |
|---|---|
| Product surfaces | Ops upload diagnostics and contextual mission state affected through existing engine publication; no layout/onboarding/navigation changes. On-device presentation pending. |
| Control paths | GCS command decoding/ACK and mission transfer changed; local/manual/sensor eligibility unchanged. Full host regression suite passed; no new flight-command device checks. |
| Runtime states | Endpoint receive polling/stop cleanup changed; foreground/background/service policy unchanged. Device lifecycle/reconnect checks pending. |
| Simulation | Mission loads still enter PhysicsSimulationEngine only after complete validated upload; previous mission is retained on failure. Execution/physics/autopilot unchanged; isolated Android evidence pending. |
| Protocol | v2 output, CRC-checked v1/v2 subset input, zero restoration, datagram batches, ACK routing, extended state and mission recovery covered by host tests/source review. Live Daily acceptance pending. |
| Reverse states | Upload abort/clear/stop and duplicate completion handling changed. No flight, recording or failure/reset action performed during this host verification. |
| Evidence | 277 unit tests, lint, app and instrumentation APK build PASS; real-device and QGC acceptance NOT RUN. |
| Documentation | Reviewed/updated canonical `docs/internals/mavlink-contract.md` and `docs/user/mission.md`. No new public product term; glossary update not required. |

## Deferred device and QGC acceptance

Connected device observed: Samsung SM-G970U, serial `R58M83HFYYY`, Android 12 from the preceding diagnostic session. The phone still has the earlier APK (last update 19:38); this new APK has **not been installed**. Installation is waiting for confirmation that the simulated vehicle is disarmed, recording is stopped and no real aircraft is connected to QGC.

After confirmation: install while retaining app data; run the four isolated protocol tests; establish a fresh QGC Daily connection and verify telemetry/ACKs; upload/download a known disarmed mission including the speed item; verify errors preserve the previous mission and stop/reconnect recovery. Record exact QGC version, installed artifact, device/Android, tester, times and evidence in a separate dated execution record. Do not treat host or synthetic checks as live-QGC, flight or release qualification.

No commit, push, merge or release was performed.
