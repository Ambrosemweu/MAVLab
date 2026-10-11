# Refreshed onboarding — working-tree checkpoint

Date: 2026-10-11, Africa/Nairobi. Tester: Codex. Branch `feature/mavlab-uiux-refinements`, baseline commit `7948d64403fcb0768b41d9b761e78885f930f3f6` plus uncommitted changes, including preceding SIM camera and Control/Ops presentation work. Version 1.5.3/code 18. No commit, push, tag or release was made.

## Implementation

The user requested direct Android implementation following the existing visual styling. The canonical `designs/mavlab-onboarding.pen` was read, not modified; its dark navy, blue-accented progress/pill controls and vector illustration direction is retained.

Nine pages now explain the offline simulated drone, shared state, four surfaces, optional GCS, calibration/phone steering before first flight, Arm/Takeoff/Land/Disarm, contextual received missions, failure recovery and opt-in recording. All illustrations are labelled previews; tutorial sliders affect only local demo values. Surface cards are a two-by-two grid instead of a competing horizontal carousel. A misleading always-linked GCS chip is replaced by labelled link-state examples, the no-op fault reset button is removed, and recording is no longer claimed for every flight.

The existing guarded pager/motion mechanism is reused for short swipes and continuous finger-following transitions. Demo sliders and the QGC download action are explicitly excluded from page swiping. Buttons share the motion target; a saveable committed step restores after recreation. Step/Skip and the action dock remain outside vertically scrollable page content. Android Back pages backwards except on page one. The shell passes existing accessibility/keyboard swipe eligibility; completion/Skip retain the existing preference and now open Cockpit without issuing flight commands. Existing completed users are not forced through the tour.

See [onboarding guidance](../../../user/onboarding.md) and [ADR-0012](../../../internals/decisions/0012-onboarding-paging-and-preview-truth.md).

## Passing host evidence

- [Final gate](evidence/gradle-gate.txt): `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --max-workers=2 --console=plain`, successful in 3m 9s.
- Unit XML totals: **251 tests, zero failures/errors/skips**. [Nine onboarding tests](evidence/onboarding-unit-tests.xml) include calibration ordering, safe first-flight copy, optional GCS, preview labels and recording opt-in. Existing [gesture unit coverage](evidence/swipe-gesture-unit-tests.xml) exercises the reused gesture policy.
- Final [lint report](evidence/lint.txt) and `git diff --check` retained/checked. The Android test APK builds; building it is not evidence that its revised tests passed on a phone.
- Final app APK: `mavlab-android/app/build/outputs/apk/debug/MAVlab.apk`, SHA-256 `17ee137e81379572af4f45c500ad34a58b8d9ea279b427477295a5b6db45aa1d`. This final APK was **not installed** before the phone disconnected.

## Phone attempt, failures and deferral

The user authorized tests on Samsung SM_G970U (`R58M83HFYYY`), Android 12, 1080 × 2280, 480 dpi, font scale 0.8, full-screen enabled. The first native user-scrollable pager implementation and test package installed successfully; its APK hash was not recorded before the build output was replaced. This installed attempt is not the final APK above.

[Initial onboarding phone tests](evidence/onboarding-phone-tests-initial.txt): **6 passed, 2 failed, 8 total** in 12.709 seconds. Button/Android Back, boundaries/completion, Skip, saved-page restoration, disabled swipe and short-display/large-text checks passed in that initial implementation. The short held swipe returned to page one, and a demo throttle drag changed pages. Those failures motivated replacing native pager gesture ownership with the app's existing guarded gesture detector and explicit slider exclusions. The [initial build](evidence/initial-native-pager-build.txt) also passed, but does not override the failing phone evidence.

Before a final install/retest, the user reported: “The phone is disconected”. Phone interactions stopped; no emulator was started. Actual-app navigation reached Ops/About while preparing replay, but the final nine-screen walkthrough, completion preference/replay landing, revised gesture dispatch and full **27-test** Android suite remain **deferred** until reconnection. The final implementation has no phone PASS claim, and the initial 6 passes are not substituted for final validation. No live-QGC, browser-download, TalkBack operation, reduced-motion device setting, landscape, armed-flight or background qualification is claimed.

## Final installation after reconnection

At the user's request, the final APK with the SHA-256 above was installed on the same Samsung SM_G970U (`R58M83HFYYY`) using `adb install -r`; installation returned `Success`, preserving existing app data. Android reports version 1.5.3/code 18 and update time **2026-10-11 18:31:55 +03:00**. `am start -W` returned `Status: ok`, a cold launch of `com.ascend.mavlab/.MainActivity` in 1840 ms; the resumed activity was confirmed. A [launch screenshot](evidence/final-installed-launch.png) shows Cockpit, disarmed, with the app's navigation visible.

This verifies installation and startup only. The revised onboarding phone tests, nine-screen visual/replay walkthrough and combined 27-test Android suite remain pending; no flight, recording or protocol qualification was performed during installation.

Subsequent CTA/chart refinement and focused onboarding retesting are recorded in the [later checkpoint](../2026-10-11-onboarding-chart-refinement/README.md): all nine updated onboarding phone tests passed, and actual Ops replay/final-page layout was checked. The full cross-surface suite remains deferred. Earlier failures and deferrals above are historical evidence, not replaced with retrospective PASS claims.

## Impact matrix

- **Product surfaces:** onboarding refreshed; shell completion selects Cockpit. Ops replay remains available. Existing Control/SIM/Ops presentation work is preserved. Final actual-app visual/replay checks deferred.
- **Control paths:** manual, Phone Sensors, GCS direct and GCS mission unchanged. Tutorial controls never call runtime/engine methods; replay does not suspend canonical visible-app sensor eligibility or stop an existing flight.
- **Runtime states:** no service, background retention or authoritative state lifecycle change. Tutorial UI restoration is tested initially and must be rechecked for the final motion implementation. Armed/AUTO/background/process execution not newly phone-qualified.
- **Simulation:** physics, autopilot, failures, battery, missions and state publication unchanged. Example mission geometry is purely an explicitly labelled illustration, not a loaded/demo route in the runtime.
- **Protocol:** no new transport, identity/capability, command, peer or telemetry behavior. Tutorial GCS dots do not observe or fabricate live connection state.
- **Reverse states:** Back/Skip/complete and preview sliders have regression tests; final device execution pending. Flight, recording, mission and fault reverse commands unchanged and not exercised.
- **Evidence:** passing host gate, focused unit coverage, compiled device tests, honest initial device failures and explicit final deferral above.
- **Documentation:** new user onboarding guide and ADR-0012; user/decision indexes updated. Existing navigation, recording, control and mission contracts reviewed. No new preference, persisted format or glossary term.
