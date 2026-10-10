# Ops upgrade and opt-in recording checkpoint — 2026-10-10

## Identity and scope

- Implementation commit: `51aeb15c5fc954cda0d4a5fd6e8c371238f3f09c` on `feature/mavlab-uiux-refinements`.
- MAVLab 1.5.3/code 18; final debug APK SHA-256: `988baaccc9db0e82bc159c11d4343620cea71cd40473a7f027b71b28cfbcd09b`.
- Device: Samsung SM-G970U, Android 12; tester: Codex; date: 2026-10-10, Africa/Nairobi (UTC+03).
- Scope: [ADR-0008](../../internals/decisions/0008-ops-health-and-recording.md), Ops health/recovery, diagnostics, recording/review and observed transitions. Recording defaults to off, including local flights and AUTO missions; users explicitly begin recording or opt into automatic capture in Ops.
- This is a partial development checkpoint. The user requested **“Finish phone checks later.”** Remaining device checks are deferred; this record does not certify complete Ops, accessibility or release acceptance. SIM redesign remains a later phase.
- QGroundControl was not used; no QGC version or interoperability result applies. Mission protocol checks used an isolated synthetic GCS peer, SYSID 255/COMPID 190, through an ADB shell Toybox netcat bridge to MAVLab's loopback UDP port 14551. No emulator or real-aircraft endpoint was used.

## Automated result and installation

```bash
cd mavlab-android
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest lintDebug assembleDebug --max-workers=2 --console=plain
```

**BUILD SUCCESSFUL in 2m 31s; 229 tests, zero failures, errors or skips.** Lint and debug assembly passed. [Gradle log](evidence/ops-gradle-gate-2026-10-10.log); [per-suite result summary](evidence/ops-unit-results-2026-10-10.json).

The gate built the implementation tree before its commit. No executable behavior changed afterward; one trailing blank line in OpsConfiguration was removed before committing. This validation commit adds documentation and captured evidence only.

Focused tests cover bounded transition history, stable incident timestamps, truthful recovery, neutral peer absence, actual versus injected link faults, exact upload rejection, filters, recording default-off/opt-in/stop/resume behavior, explicit ground sessions, final mission report state, sampled path and report timestamps. Existing authority, sensor, engine and lifecycle regressions remain in the full suite.

`adb install -r` returned Success and retained application data. [Install log](evidence/ops-phone-install-2026-10-10.log). The SHA-256 above identifies the host APK; the installed package's checksum was not independently checked during this checkpoint. No version, tag or published release was changed.

## Final APK physical-phone observations

The following observations apply to the final APK identified above. [Build-annotated machine-readable results](evidence/ops-phone-results-2026-10-10.json).

1. Ops displayed the runtime/vehicle/authority/recording health summary. [Health](evidence/ops-health-2026-10-10.png).
2. GPS injection required confirmation. Cockpit then showed GPS no-fix from the same engine state, and Ops displayed the active GPS incident. Reset was confirmed; this record does not claim a subsequent final-build observation of the recovered fix. [Active GPS incident](evidence/ops-gps-incident-2026-10-10.png).
3. A controlled five-item mission upload returned mission ACK 0. Confirmed Start AUTO established armed AUTO with GCS Mission authority. Ops showed **Recording stopped**. Saved-session folder count remained **75 before AUTO → 75 during AUTO**, providing evidence that mission arming did not start recording by default. [AUTO with recording stopped](evidence/ops-auto-recording-off-2026-10-10.png).
4. Take control & land was confirmed. MAVLab subsequently lost foreground focus, so final-build landing completion was not checked.
5. A controlled replacement containing unsupported command 999 returned mission ACK 3 while retaining the prior loaded five items. The final rejection presentation and whole-section recovery reveal were not checked on the phone before the user deferred further checks.

## Earlier default-off build observations

These checks ran on the earlier working APK with SHA-256 `e310a9b42eab4683ac4e29db14155fabcfe7f2774ce5d20a1dcd1cf321d519c1`, also version 1.5.3/code 18. The final build changed the recovery scroll target to the whole expandable card; recording logic was unchanged. These observations are not described as final-APK checks.

- Ops showed automatic recording off. [Off setting](evidence/ops-recording-off-2026-10-10.png).
- Explicit opt-in saved the setting, and a cold process restart restored it as enabled. [Enabled setting](evidence/ops-recording-opt-in-2026-10-10.png).
- Arming after opt-in started a session. Deliberate Stop and save paused automatic capture during that armed flight.
- After disarming, automatic capture was switched off and the saved preference was confirmed false. A controlled clear returned mission ACK 0.

## Cleanup and deferred checks

Original mission preferences were restored byte-for-byte from the pre-check backup. Original display settings were retained. Existing saved recordings were preserved, and automatic recording was left off. MAVLab was left stopped after cleanup; it can be reopened for later checks.

Remaining phone checks include final whole-section recovery scrolling and rejection details, final GPS recovery and landing completion, runtime Stop/Start and recording closure, manual Begin/review/export on the final APK, larger text and landscape layout. The persisted opt-in checks above can be repeated on the final APK when resuming. Live QGC, LAN delivery, background handoff and broader device/accessibility qualification require separate evidence.

## Impact matrix

| Row | Scope and verification |
| --- | --- |
| Product surfaces | Ops reordered around health, active incidents, peer/mission evidence, runtime, exercises, recordings, history and setup. Ops-to-Control recovery uses the existing shell; other surfaces remain uncomposed. Final phone health/GPS/AUTO observations; remaining layout checks deferred. SIM redesign is outside scope. |
| Control paths | Recording observes local/manual and GCS mission state without changing authority. Foreground Phone Sensors eligibility remains canonical. Existing regressions passed; no new physical armed-tilt qualification is claimed. |
| Runtime states | Ops observation is process-owned, sampled on the existing monitor; history is bounded and process-local. Earlier-build cold restart verified saved recording opt-in. Service retention/foreground policy is unchanged; final Stop/Start and background checks remain pending. |
| Simulation | Incidents derive from authoritative engine/failure/mission state. Reset does not fabricate restored GPS or recharge battery. Focused observer tests and final phone GPS injection/AUTO state provide evidence. Physics and autopilot models are unchanged. |
| Protocol | Existing identity/upload evidence is presented without adding capabilities or granting authority from peer presence. Synthetic Android-loopback upload/rejection ACKs were observed. No live QGC or outbound delivery qualification is claimed. |
| Reverse states | Recording opt-in/out, Begin/Stop-save and temporary armed pause have focused tests; opt-in/out and Stop/save were checked on the earlier default-off APK. GPS inject/reset and local landing confirmation were exercised on the final APK; final completion/reveal checks are deferred. Runtime stop wording accurately describes frozen state rather than landing/disarm. |
| Evidence | 229 passing JVM tests, lint/build, successful installation, named-device screenshots and build-separated protocol/settings results. Partial phone scope is explicit; no emulator or full release acceptance is asserted. |
| Documentation | Ops user guide, architecture/overview, surface definition, lifecycle review and ADR-0008 updated. The new Boolean preference defaults false when absent, including existing installs; session file formats remain retained. No new glossary term or protocol contract change was needed. |
