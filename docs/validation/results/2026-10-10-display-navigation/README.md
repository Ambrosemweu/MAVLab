# Full-screen display and compact navigation — working-tree validation

Date: 2026-10-10, Africa/Nairobi. Tester: Codex, with user phone connection/support. This record is not a release or real-aircraft qualification.

## Build identity and environment

- Baseline commit: `b1aa29a19722e54dcd41c4289e6d95de103dfc25`; changes are uncommitted working-tree behavior.
- App: debug `1.5.3`, version code `18`; no version bump, tag, push or publication.
- Device: Samsung `SM_G970U`, Android 12, USB serial `R58M83HFYYY`; 1080 × 2280 physical pixels, density 480 dpi, font scale 0.8, three-button navigation (`navigation_mode=0`).
- QGC version: not applicable; no GCS endpoint used for this display/navigation pass.
- Gradle gate: `testDebugUnitTest lintDebug assembleDebug`, using local `GRADLE_USER_HOME="$PWD/.gradle"`. [Earlier completed gate](evidence/gradle-gate.txt). The intermediate `gradle-gate-final.txt` lint run was interrupted to prioritize building/installing the user's swipe-area fix; it is not a completed final gate. Final verification uses `--max-workers=2` and [this separate log](evidence/gradle-gate-verified.txt).
- Unit suite: 235 tests, zero failures/errors/skips. Six focused navigation tests cover direction/endpoints, short/reversed drags, vertical/diagonal cancellation, multi-touch/child consumption, turning vertical and passive-area registration/removal. Existing engine/runtime sensor-control tests also passed.
- APK: `mavlab-android/app/build/outputs/apk/debug/MAVlab.apk`. Final artifact identity and installation are recorded below after the final gate.

## Observed device checks

Phone testing confirmed scroll containers consumed the initial down before swipe recognition. The detector now observes passive-area down during Initial, finishes that event, then gives descendants priority during Main movement. User feedback also showed the header-only eligibility was too narrow; the passive Cockpit instruments/non-mission status text and Control's phone-tilt preview are now eligible. Controls, mission rows/actions and charts remain excluded. IME observation was moved from DecorView to content so it does not replace platform decor handling.

- Standard display opens with a visible four-destination dock; labels and 22 dp icons are visible. [Initial screenshot](evidence/01-standard-cockpit.png).
- An intentional header swipe switched Cockpit → Control. [Control](evidence/03-control-after-header-swipe.png).
- A Control status/header swipe switched Control → SIM. [SIM](evidence/04-sim-after-header-swipe.png).
- SIM's passive top strip switched SIM → Ops.
- An additional left swipe at Ops did not wrap to Cockpit. Vertical Ops scrolling stayed in Ops.
- Ops → Display exposes the full-screen switch and updates its checked semantics.

The initial screenshots `02`, `05`, `06` and `07` are retained as diagnostic checkpoints, not final full-screen acceptance evidence. UIAutomator bounds near the dock are clipped on this OEM even after moving the listener, although visual labels and actual button taps work; this is not proof of TalkBack acceptance. Final checks are recorded separately below.

## Design evidence

[Pen handoff](../../../../designs/full-screen-navigation/README.md) includes one reusable dock, four selected variants and eight annotated shell cases. Exports are 2×. All 72 original root subtrees, including onboarding, are structurally unchanged; the file now has 74 roots. New roots have no unfinished placeholders or enabled clipping issues. Design examples are not device qualification.

## Impact matrix

| Row | Scope and evidence |
|---|---|
| Product surfaces | Shared shell, Cockpit, Control, SIM, Ops, contextual mission/route Back behavior and onboarding insets. Build, navigation unit tests, Pen review and bounded phone checks. |
| Control paths | Navigation emits no engine commands. Local/manual, phone sensors, GCS direct and mission paths retain their owners. Existing runtime/engine regression suite passed; no new QGC or physical tilt evidence. |
| Runtime states | Presentation responds to focus/IME/multi-window; only selected screen composed. Foreground display/navigation tested. Existing background/handoff policy tests passed. Process, AUTO and background device matrix not newly qualified. |
| Simulation | No physics/autopilot/battery/failure/progression changes; regression suite passed. No simulation state synthesized on restoration. |
| Protocol | Not changed; no new identity/capability/ACK or real-endpoint claims. Existing tests passed. |
| Reverse states | Full screen has an off switch; endpoints do not wrap; Back closes visible route/mission detail. Flight, runtime, failures and recording reverse actions unchanged; not all exercised on phone in this pass. |
| Evidence | Unit tests, lint, APK and bounded Android phone checks. Remaining matrix below is explicitly pending. |
| Documentation | User navigation/display guide, Ops/Control guides, runtime lifecycle review and ADR-0009 updated. Obsidian source specification links canonical root docs and this record. |

## Remaining acceptance matrix

Pending unless explicitly recorded in the final check section: actual TalkBack exploration; gesture-navigation mode; Android 15+ edge-to-edge behavior; large text/display scaling, multi-window and landscape; keyboard/IME coexistence and permission/system dialogs; received-route pan/zoom and contextual Back with an uploaded mission; armed physical phone tilt continuity across tabs, local takeoff, AUTO/GCS exclusion and background handoff. These are not converted into permanent PASS claims by the successful build/unit tests or illustrative Pen frames.

## Final artifact and phone checks

- APK SHA-256: `ceb4bb7f07485a2170de1935263c325a8da510b9122206694582fd0bb7e1fc10`. `adb install -r` succeeded; package inspection confirmed version `1.5.3` / code `18`, last update `2026-10-10 22:13:06` on the phone. No data-clearing install was used.
- Full-screen selection persisted across the replacement install/relaunch and hid Android bars while leaving the four-tab dock visible. [Cockpit](evidence/08-final-full-screen-cockpit.png).
- Android bottom-edge swipe temporarily revealed the system bars without exiting or hiding the dock. [Transient bars](evidence/09-transient-android-bars.png).
- This phone reports a 116 px top cutout and a 144 px mandatory bottom gesture inset even with three-button bars hidden. Those safe reserves are retained intentionally; full screen does not remove them. It therefore does not reclaim those vertical reserves on this configuration.
- Latest fix: a horizontal drag from `(850, 800)` to `(220, 800)` over the Cockpit instruments selected Control, with Disarmed still visible. [Evidence](evidence/10-control-after-instrument-swipe.png). This is a same-height horizontal gesture, not a vertical scroll. Prior header tests confirmed both directions and no-wrap endpoints. The later Control-preview reverse attempt overlapped user interaction and is not counted as an isolated PASS.
- Horizontal drags over the SIM camera remained in SIM. [Camera-owned state](evidence/11-sim-camera-owned.png). This does not qualify every camera gesture or mission view.
- Phone remains available to the user for the installed fix. No further automated phone interaction is needed for the recorded bounded scope.
- Final regression gate completed successfully in 4m 24s: `testDebugUnitTest lintDebug assembleDebug --max-workers=2 --console=plain`, 51 actionable tasks (10 executed, 41 up-to-date). [Final log](evidence/gradle-gate-verified.txt). The suite reports 235 tests and zero failures/errors/skips; final APK hash is unchanged from the installed artifact. `git diff --check` is clean.
- Pen follow-up source annotations were saved. The overview refresh produced fallback icons after external font-download timeouts; its [invalid diagnostic export](evidence/pen-font-fallback.png) is not offered as a finished design preview. Eight earlier individual 2× exports remain verified, and all 72 original Pen root subtrees remain unchanged with zero unfinished placeholders in the new roots.
