# Final UI/UX verification — 2026-10-11

## Outcome

**Automated gates and the scoped functional phone checks passed. Merge readiness is held for the Control large-text landscape clipping finding and audio-shutdown crash triage below.** This is a working-tree verification record, not release acceptance or a live QGroundControl qualification.

No production code was changed during this verification. Documentation/index corrections and this record were added. Nothing was committed, pushed, tagged, published or merged.

## Identity

- Branch: `feature/mavlab-uiux-refinements`.
- Base HEAD: `7948d64403fcb0768b41d9b761e78885f930f3f6`, with uncommitted UI/UX implementation. HEAD alone does not identify the tested implementation.
- App: MAVLab `1.5.3`, version code `18`, debug.
- APK: `mavlab-android/app/build/outputs/apk/debug/MAVlab.apk`.
- SHA-256: `f19b66f09809db09f0ad2e0b3068ad589a3edb645f1f185e5edbf2e5ed0013af`.
- Installed package update time: `2026-10-11 18:49:24`, phone local time UTC+03. Host artifact matches the artifact installed during the preceding onboarding refinement session; this verification did not reinstall the production APK. Test APK was reinstalled.
- Device: Samsung SM-G970U, serial `R58M83HFYYY`, Android 12; physical display 1080×2280, density 480.
- Tester: Codex automation with Ambrose performing physical tilts and reporting cross-screen response; date `2026-10-11`, Africa/Nairobi.
- QGC version: not identified; no controlled live QGC scenario executed. An existing peer's heartbeat was observed, but its presence does not establish interoperability or authority.

## Automated evidence

```bash
cd mavlab-android
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --max-workers=2 --console=plain
```

- **BUILD SUCCESSFUL in 21 seconds** (incremental). [Gradle log](evidence/gradle-gate.txt).
- **253 unit tests; zero failures, errors or skips**. [Result totals](evidence/unit-test-totals.txt).
- Lint: **zero errors, 34 warnings**, retained rather than silently described as warning-free. [Lint output](evidence/lint.txt).

Full phone instrumentation command:

```bash
adb -s R58M83HFYYY shell am instrument -w -r com.ascend.mavlab.test/androidx.test.runner.AndroidJUnitRunner
```

The first run had 27 passes and one onboarding chart-test timeout. The tester launched MainActivity while instrumentation was still running, contaminating the run; it is not counted as accepted evidence. [Interrupted run](evidence/phone-suite-interrupted.txt).

After force-stopping the disarmed, non-recording app, the isolated repeat completed **OK (28 tests), 27.469 seconds**, without competing inputs. [Accepted full-suite log](evidence/phone-suite-final.txt). Coverage comprises 15 guarded-navigation/pager tests, two launch-theme tests, one page-clipping test, nine onboarding tests and one GCS-indicator test. This validates those test assertions, not every possible device or accessibility configuration.

## Physical-phone functional checks

### Phone Sensors across Cockpit, Control and SIM — passed

Calibrated neutral, armed through Control, then requested a 10 m takeoff. Engine mode changed from Stabilize to Alt Hold, with Controller authority and armed state. Ambrose tilted left/right and forward/back, switching between all three surfaces, and replied:

> Responds on all three; phone still on Control

Read-only follow-up showed SIM foreground, 10 m altitude, nonzero horizontal speed/position and motor response. The difference between the reported and observed selected tab was handled by explicitly selecting Control before landing. Land produced engine Land mode and paused manual input, followed by ground arrival and automatic disarm/Idle. No extra explicit Disarm press is claimed. [Observed response](evidence/physical-tilt-observation.txt), [landing](evidence/phone-sensors-landing.txt), [touchdown](evidence/phone-sensors-touchdown.txt).

No automatic recording was created: the saved-folder listing remained identical to the 75-folder pre-flight baseline through the three input-mode checks. This is local-flight default-off evidence, not a repeat of AUTO-mission recording qualification.

### Custom Input — passed, scoped

Selected while disarmed, armed and took off to 10 m. A horizontal drag changed Roll from zero to **+21%** without navigating away. Switching to SIM retained engine response: nonzero horizontal motion, authoritative position and motor telemetry. Land completed with automatic disarm; shared input reset restored Phone Sensors. [Command](evidence/custom-input-roll-command.txt), [SIM response](evidence/custom-input-sim-response.txt), [landed/reset state](evidence/custom-input-landed.txt).

This is one roll/flight/reset scenario, not exhaustive slider, axis, yaw or all-control-mode qualification.

### Direct RPM — passed, scoped

Entry confirmation Cancel was exercised, followed by deliberate Enable. Disarmed state showed zero RPM with target adjustment disabled. After Arm, a target of **3173 RPM** produced approximately **3174 RPM on all four motors** in SIM. The simulated vehicle stayed on the ground. Stop motors returned Idle/Disarmed/Phone Sensors. [Zero state](evidence/direct-rpm-zero.txt), [target](evidence/direct-rpm-command.txt), [engine response](evidence/direct-rpm-sim-response.txt), [stop/reset](evidence/direct-rpm-stopped.txt).

External GCS authority suspension was not newly exercised on the phone; existing automated regressions passed. It remains a separate live integration check.

### Ops GPS recovery — passed

Injected GPS Signal Lost through confirmation. Control showed no fix and one issue; Ops exposed the navigation incident. Reset simulated failures required confirmation. Subsequent Ops showed no active issues/zero faults and Control showed GPS 3D fix/zero issues. Battery was not falsely recharged by reset. [Incident](evidence/gps-incident-ops.txt), [Ops recovery](evidence/gps-recovered-ops.txt), [Control recovery](evidence/gps-recovered-control.txt).

### Manual recording, review and export — passed, scoped

Automatic recording remained off. Explicit Begin created `20261011-161139-550`; Stop and save closed it after about five seconds. Review opened the generated report with IDLE/STABILIZE, zero ground motion, no mission and no injected failures. Its timeline described explicit Ops recording start/stop. [Review](evidence/session-review.txt), [screenshot](evidence/session-review.png), [manifest](evidence/manual-session-manifest.json).

Export opened Android's chooser with **five items**. The chooser was closed without selecting a recipient or sending data. This verifies export handoff, not successful delivery to a third-party app. [Chooser](evidence/session-export-chooser.txt).

### Runtime Stop/Start and recording closure — passed, disarmed scope

Began `20261011-161232-857`, then confirmed Stop simulation while disarmed. Ops showed unavailable/stale engine state, suspended sensor monitoring, stopped audio and recording, and a red GCS-disconnected indicator. The session manifest has a non-null end time; its event records **recording_stopped — runtime stopped**. All five session files exist. [Manifest](evidence/runtime-stop-session-manifest.json), [events](evidence/runtime-stop-session-events.jsonl), [stopped header](evidence/runtime-stopped-ops-header.png).

Start simulation restored live publication and sensor monitoring without resuming recording. The existing peer subsequently revalidated. [Restart state](evidence/runtime-restarted.txt). This is not a test of armed stop, background retention, AUTO handoff or proven QGC command delivery.

## Layout finding requiring follow-up

At the user's original **0.8 font scale**, Control's landscape action buttons were visible. [Landscape screenshot](evidence/control-landscape.png).

At **1.5 font scale in landscape**, the multi-line hint and stacked buttons exceed the available Control action-dock height. The secondary **Calibrate** label/button is visibly truncated above the bottom navigation. A vertical gesture in that dock did not reveal the full button. [Screenshot](evidence/control-landscape-large-text.png), [hierarchy](evidence/control-landscape-large-text.txt), [after attempted dock scroll](evidence/control-landscape-large-text-dock-scroll.png).

Source review: `ControllerScreen.kt` composes a fixed-width 240 dp landscape dock with stacked hint/actions and no independent vertical overflow treatment. The controls body is scrollable, but the dock is not. A content-aware compact arrangement or safely scrollable dock should preserve both actions at large text; a focused short-height/large-font regression test is needed. No fix was implemented during this verification-only task.

At 1.5 font scale in portrait, both actions remained visible, but the three-column status display split **Disarmed** across an awkward word break. [Portrait screenshot](evidence/control-portrait-large-text.png). Treat this as additional large-text polish rather than a passed premium-layout claim.

Ops' scrollable layout wrapped large text in the observed portrait and landscape viewports; the observed landscape health summary and navigation remained readable. [Ops landscape](evidence/ops-landscape-large-text.png). This is a spot check, not exhaustive expanded-card accessibility validation.

## Historical crash triage requiring follow-up

Crash-buffer inspection found an audio exception at **18:11:44**, before the final APK's 18:49:24 install:

`IllegalStateException: Unable to retrieve AudioTrack pointer for getUnderrunCount()` in `ProceduralDroneSynth.kt:80`.

Current source still reads the active track's underrun count in its render coroutine, while `release()` cancels the job and immediately releases the track without waiting for render completion. This is consistent with an audio-track use-after-release race; source inspection is not a reproduction or a confirmed complete root cause. It should be addressed or explicitly dispositioned before merge. No such crash occurred during the accepted test run and subsequent phone checks, and the older exception is not attributed to the final APK without evidence.

The same buffer includes 15:34/15:36 native model-loader aborts, also predating the final install. Their applicability to the current artifact is unqualified. Retain historical evidence rather than assert a crash-free product. [Crash buffer](evidence/crash-buffer-historical.txt), [exit history](evidence/process-exit-history.txt).

## Preservation and handoff

- Original 75 saved-recording folders remain; exactly two explicit verification recordings were added and retained, yielding 77. No user recordings were deleted.
- Checked app, recording and mission preference files have byte-identical SHA-256 hashes to the pre-check baseline. [Final hashes](evidence/final-preference-hashes.txt). No data clear or mission mutation was performed.
- Original system settings were restored: font scale `0.8`, user rotation `0`, accelerometer rotation `0`. [Settings](evidence/final-display-settings.txt).
- App left open on Control: Idle, Disarmed, Stabilize, Phone Sensors, zero active issues; recording stopped. [Handoff state](evidence/final-control-state.txt).

## Impact matrix

| Row | Applicable scope and verified result |
| --- | --- |
| Product surfaces | Cockpit/Control/SIM cross-screen physical sensors; Control advanced input/actions; Ops recovery/runtime/recordings; onboarding/pager/launch assertions through all 28 instrumentation tests. Contextual mission workflows not rerun. Large-text Control finding remains open. |
| Control paths | Phone Sensors physically confirmed on three surfaces; one Custom Input roll scenario and Direct RPM target/stop verified against engine telemetry. Live GCS direct and mission scenarios deferred. |
| Runtime states | Foreground armed local flight and disarmed Stop/Start verified. Background handoff, validated GCS flight, AUTO, armed stop and process recreation not newly qualified. |
| Simulation | Actual takeoff, motion, landing/disarm, motor targets and GPS injection/reset observed. No physics/autopilot model change or exhaustive battery/failure qualification. |
| Protocol | Existing heartbeat indicator disconnected/revalidated through runtime Stop/Start. Full unit gate includes protocol regressions. No controlled live QGC/LAN mission or capability qualification. |
| Reverse states | Arm/takeoff/land/automatic disarm, Direct RPM enable/cancel/stop, GPS inject/reset, Begin/Stop-save, runtime Stop/Start and recording closure observed. External-authority landing takeover not rerun. |
| Evidence | 253 unit tests, lint/debug/test assembly, isolated 28-test device run, user-confirmed physical tilt, hierarchies/screenshots/manifests. Interrupted run and historical crashes retained separately. |
| Documentation | Release checklist now has four destinations/contextual missions; controller swipe wording and validation index corrected. No new runtime, protocol or persistence decision implemented; existing ADRs/user docs reviewed. |

## Remaining qualification boundaries

This record closes the scoped deferred physical sensor, advanced-input ground/local-flight, GPS recovery, manual recording/review/export chooser and disarmed runtime Stop/Start checks from earlier checkpoints. It does not overwrite those historical records or convert their other deferrals into passes.

Still separate: controlled QGC build/endpoint mission upload, rejection-detail recovery scrolling, mission completion/local takeover, GCS advanced-input suspension, LAN delivery, armed/background lifecycle, recording opt-in persistence repeat on this final artifact, SIM route pan/pinch, TalkBack, Android 15, other navigation modes, multi-window and broader device/performance qualification. Existing historical or automated evidence is not silently promoted to fresh phone acceptance.

Before recommending merge: resolve/retest the large-text action dock and triage the audio shutdown race. Remote CI/PR/merge is a subsequent explicitly requested step.
