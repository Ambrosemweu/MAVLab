# Final findings fixes — 2026-10-11

## Outcome

The two findings from [final UI/UX verification](../2026-10-11-final-uiux-verification/README.md) are addressed in the working tree and installed on the physical phone:

1. Control no longer clips Calibrate at 150% text in landscape; the portrait vehicle status no longer splits “Disarmed” across a word break.
2. Procedural audio shutdown no longer disposes an AudioTrack underneath its render worker. Track ownership, restart serialization and deterministic cancellation tests address the observed use-after-release race.

**261 unit tests, all 34 phone instrumentation tests, lint and debug/test assembly passed.** The original layout/crash findings and earlier evidence remain unchanged in their historical record. Nothing was committed, pushed, tagged, published or merged during this fix.

## Build and device identity

- Branch: `feature/mavlab-uiux-refinements`.
- Base HEAD: `7948d64403fcb0768b41d9b761e78885f930f3f6`; tested code includes uncommitted work. This is not the published v1.5.3 artifact.
- App: MAVLab `1.5.3` / version code `18`, debug.
- Host APK: `mavlab-android/app/build/outputs/apk/debug/MAVlab.apk`.
- SHA-256: `e5b90d65815d36cba902a8ae6011b9d568f09a6b22e022bccad0750a730628a1`.
- Installation: both application and instrumentation APKs returned Success with `adb install -r`; application data retained.
- Installed update time: `2026-10-11 19:38:01` UTC+03. Installed base APK checksum independently matches the host artifact. [Installed checksum](evidence/installed-apk-hash.txt).
- Device: Samsung SM-G970U, serial `R58M83HFYYY`, Android 12; 1080×2280 physical display, density 480.
- Tester: Codex; date `2026-10-11`, Africa/Nairobi. Ambrose confirmed disarmed, recording stopped and phone untouched before testing.
- QGC version: not identified; no controlled live QGC test. An existing heartbeat peer remained present, without changing local authority.

## Control layout change and checks

The previous 240 dp landscape side dock stacked a multi-line hint and both buttons without accounting for enlarged text. The scrolling controls could not reveal content clipped in the separate dock.

`ControllerContentLayout` keeps the side dock only when font scale is at most 1.15 and the content viewport is at least 240 dp tall. Larger text or shorter viewports use the full-width bottom action row, separate from the scrolling controls and above navigation. Portrait status fields flow at natural widths instead of forcing equal narrow columns. Availability, actions, confirmations and engine commands are unchanged.

Five new Compose device tests verify:

- both large-text landscape actions are fully visible above navigation and clickable;
- a short normal-text landscape viewport uses the bottom dock;
- ordinary landscape still supports the side dock;
- large portrait text keeps Disarmed on one line;
- a warning and long external-takeover action remain accessible.

These assertions compare clipped/unclipped action bounds and invoke callbacks without commanding the simulation. They are not a substitute for the actual-app check:

- At **1.5 font scale in landscape**, both Arm and Calibrate were fully visible. Calibrate was tapped through its observed enabled bounds without arming the vehicle. The subsequent dump does not capture the transient feedback, so feedback visibility is not claimed here; callback execution is covered by the Compose test. [Actual screen](evidence/control-landscape-large-text.png), [hierarchy](evidence/control-landscape-large-text.txt), [after tap](evidence/calibrate-large-landscape-feedback.txt).
- At **1.5 in portrait**, status items wrapped as whole items and Disarmed remained intact; both actions stayed visible. [Actual screen](evidence/control-portrait-large-text.png).

These two screenshots were visually inspected. Other font scales, TalkBack, Android versions and multi-window layouts are not exhaustively qualified by this record.

## Audio ownership change and checks

The old stop path canceled a coroutine without waiting for its in-progress native call, then release immediately disposed the shared AudioTrack. It also replaced the shared track on restart without giving the previous session sole cleanup ownership.

Each new render session now creates, plays, writes, queries and closes its own output on its worker. Stop/release only request cancellation; the worker's finally block pauses, flushes and releases the track. A mutex prevents restarted rendering from overlapping the old cleanup or shared buffer/oscillators. Session identity prevents obsolete work from publishing a newer session's running status. Release is terminal for that instance and cancels its scope. No UI-thread native wait/join was added.

Native output uses nonblocking writes with cancellation checks and handles zero/partial transfers by retaining the unwritten samples. This follows the [Android AudioTrack write contract](https://developer.android.com/reference/android/media/AudioTrack). Runtime output errors publish fallback status and still dispose native resources. A narrow fake-output boundary makes the critical native-call interleaving deterministic in unit tests without introducing a second audio or simulation owner.

Eight new unit tests cover held underrun-count reads during release, immediate restart ordering/obsolete status, partial and zero writes, unavailable output, play/read/write failures, cleanup after pause failure, and terminal/idempotent release. All eight passed. [Unit results](evidence/audio-unit-tests.xml).

One new physical-device test creates **31 real Android AudioTracks**, exercising 30 stop/restart cycles followed by final release. It waits for every track to return to the released/uninitialized state and verifies stopped status/no fallback. It uses silent output to test lifecycle safely; this is not an acoustic quality or audible latency measurement.

In the actual app, **two disarmed Ops Stop/Start cycles** completed with recording stopped, audio stopped on Stop and retained with the running runtime on Start. [Stopped](evidence/runtime-stopped-2.txt), [restarted](evidence/runtime-started-2.txt). No additional recording was created.

The crash buffer queried since the new install contains no entries. [Post-install crash check](evidence/crash-since-install.txt), [exit history](evidence/process-exit-history.txt). This is a bounded observation, not proof that every crash class is eliminated. The older native model-loader aborts from the preceding verification record were not reproduced or investigated in this two-finding fix; no general renderer crash-free claim is made.

## Automated gate

```bash
cd mavlab-android
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --max-workers=2 --console=plain
```

**BUILD SUCCESSFUL in 2m 9s; 261 unit tests, zero failures/errors/skips; lint zero errors and 34 warnings.** [Gradle](evidence/gradle-gate.txt), [unit totals](evidence/unit-test-totals.txt), [lint](evidence/lint.txt). An initial test-compilation typo in the new bounds assertion was corrected before this accepted gate; it was not a production runtime failure.

```bash
adb -s R58M83HFYYY shell am instrument -w -r com.ascend.mavlab.test/androidx.test.runner.AndroidJUnitRunner
```

**OK (34 tests), 34.668 seconds**, with no concurrent UI input. This includes the previous 28 tests, five Control layout tests and the native audio-lifecycle test. [Full phone suite](evidence/phone-suite.txt).

## Preservation and handoff

Before/after comparisons passed byte-for-byte for the four preference-file hashes, the 77 saved-recording folder names and original system settings. Existing recordings, mission data, recording opt-in, sound settings and onboarding/display preferences were preserved. The two explicit test recordings from the previous verification remain; this turn added none.

- [Preferences before](evidence/preferences-before.txt) / [after](evidence/preferences-after.txt).
- [Recordings before](evidence/recordings-before.txt) / [after](evidence/recordings-after.txt).
- [Display settings before](evidence/settings-before.txt) / [after](evidence/settings-after.txt): font scale `0.8`, user rotation `0`, accelerometer rotation `0`.
- App left open on Control, Idle/Disarmed/Stabilize/Phone Sensors, zero active issues; recording stopped. [Handoff](evidence/handoff-control.txt).

## Impact matrix

| Row | Scope and evidence |
| --- | --- |
| Product surfaces | Control responsive action/status layout changed; five new Compose tests and actual 150% portrait/landscape views passed. Other surfaces/onboarding retained and covered by the full existing device suite. |
| Control paths | Action callbacks, sensor eligibility and GCS authority rules unchanged. Button callbacks tested without engine commands; existing unit regressions passed. No new physical armed-tilt or live GCS qualification claimed. |
| Runtime states | Audio stop/restart/release ownership changed; eight deterministic unit tests, 31 native tracks and two disarmed foreground Ops cycles passed. Background retention/service policy unchanged and not requalified. |
| Simulation | Physics/autopilot, failures, battery and mission progression unchanged; presentation/audio observe existing engine state. Existing unit regressions passed. |
| Protocol | Not changed. No command, capability, identity or mission-protocol change; existing unit regressions passed, no live QGC qualification. |
| Reverse states | Control availability/confirmations unchanged. Audio Stop/Start/release and actual disarmed runtime Stop/Start verified; no new recording or flight initiated. |
| Evidence | 261 unit tests, lint/build, checksum-matched device install, isolated 34-test suite, actual-app screenshots/callback and two runtime cycles, bounded crash-log check. |
| Documentation | Control user guide, ADR-0006 layout and runtime audio ownership updated; validation index links this immutable result. No persisted format, retention policy, protocol contract or glossary change. |

The two scoped findings are closed for this artifact. Broader live QGC/LAN/mission/background/accessibility/device and performance deferrals in the previous record remain explicit. Commit/push/remote CI/PR/merge are subsequent, separately authorized work; this record is not release acceptance.
