# SIM inspection lighting — working-tree verification

Date: 2026-10-11, Africa/Nairobi. Tester: Codex. Baseline commit `b1aa29a19722e54dcd41c4289e6d95de103dfc25` plus uncommitted UI/UX changes; not a release. QGC not used.

## Cause and implementation

SceneView 4.16.10's engine-only environment factory created no indirect light. Its environment-loader factory loads the bundled neutral prefiltered IBL. The old custom sun also changed direction with yaw and intensity with roll, while its fixed front/top point light did not illuminate inspected rear/bottom surfaces adequately.

SIM now loads the bundled IBL, with explicit stable key and camera-following fill replacing the default lights. Camera-to-body distance compensates point-light power across zoom. See [ADR-0010](../../../internals/decisions/0010-sim-inspection-lighting.md) and [user guidance](../../../user/sim.md). Asset, materials, simulation transforms, flight controls and pager startup guards are preserved.

## Identity and checks

Device: Samsung SM_G970U (`R58M83HFYYY`), Android 12, 1080 × 2280, 480 dpi, font scale 0.8, three-button system navigation; saved full-screen enabled. Version 1.5.3/code 18 installed 2026-10-11 at 14:39:35 Africa/Nairobi. APK SHA-256: `d27ba1e6f8106252a246fd13d7057d53982b72672c726069876faa07b483adeb`.

[Final build gate](evidence/gradle-gate.txt): `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --max-workers=2 --console=plain`, successful in 2m 6s. Unit XML totals: 243 tests, zero failures/errors/skips, including [three lighting regressions](evidence/lighting-unit-tests.xml) for zoom compensation, orbit-side independence and finite guarded distances. Lint reported zero errors and 34 warnings; not a warning-free claim.

[Physical-phone navigation regression run](evidence/navigation-phone-tests.txt): 17/17 passed in 13.974 seconds, with no concurrent capture/interaction commands during that final run. These tests cover the shared navigation/launch behavior, not subjective lighting quality.

Bounded actual-app visual inspection, with simulated vehicle disarmed and motors stopped:

- Reset the renderer through Cockpit → SIM to start with the same default orbit. Captured [before](evidence/mavlab-lighting-before-default.png) and [after](evidence/mavlab-lighting-after-default.png). The frame and forward-facing housing become distinguishable without emissive material changes.
- Repeated identical horizontal camera drags `(750,1130) → (390,1130)`, 700 ms: [first orbit before](evidence/mavlab-lighting-before-side.png), [after](evidence/mavlab-lighting-after-side.png); [second orbit before](evidence/mavlab-lighting-before-rear.png), [after](evidence/mavlab-lighting-after-rear.png). Camera angles are observed views, not instrument-measured headings. Previously dark battery/frame surfaces remain readable.
- Continued with `(540,1250) → (540,850)`, 700 ms: [underside before](evidence/mavlab-lighting-before-below.png), [after](evidence/mavlab-lighting-after-below.png). The underside plate, arms and motor bases no longer disappear into the background.
- After the final instrumentation run, reopened the real app and reset/re-entered SIM. [Re-entry](evidence/mavlab-lighting-after-reentry.png) retained the rig without an observed crash. Opposite-direction horizontal orbit `(350,1130) → (750,1130)`, 700 ms, produced a readable [opposite-side view](evidence/mavlab-lighting-after-opposite.png). A downward camera drag `(540,750) → (540,1200)`, 700 ms, produced a readable [top view](evidence/mavlab-lighting-after-top.png).
- Left the app open on SIM at an oblique inspection angle ([screen](evidence/mavlab-lighting-ready.png), [UI state](evidence/mavlab-lighting-final.xml)). No arm, recording, mission, failure or runtime-stop action was invoked. The existing close framing/HUD overlay is unchanged and is not part of this lighting qualification.

All captures show existing materials and model transforms, not a remodelled asset. Battery restarted at its runtime default after installing/relaunching; this is not a continuous-flight comparison. Visual readability is an assessment from these captures, not photometric calibration. The emulator remained stopped. No code was committed, pushed or published.

## Impact matrix

- Product: SIM 3D vehicle rendering only; Cockpit, Control, Ops, route panel and onboarding behavior unchanged.
- Control paths: no local/manual, phone-sensor, GCS direct or GCS mission eligibility changes. Camera gestures invoke no engine commands.
- Runtime: existing renderer-arrival/disposal gates retained; environment, key and fill have composition-scoped ownership. Global runtime/service retention unchanged. Background armed/AUTO flight not newly device-qualified.
- Simulation/protocol: no physics, autopilot, failures, battery, mission progression, state publication or MAVLink capability/identity changes.
- Reverse states: camera orbit and SIM leave/re-entry checked; arm/disarm, takeoff/land, start/stop, connect/disconnect, failure/reset and recording unchanged.
- Evidence: host unit/build/lint, bounded phone captures, navigation regression suite. No measured all-device frame-rate guarantee or QGC/real-aircraft qualification.
- Documentation: user SIM guide and ADR-0010 reviewed; no persisted format or new glossary term.

Remaining acceptance: user's preferred brightness and contrast; broader displays/material assets. Zoom mathematics is unit-verified; physical pinch-zoom acceptance is not yet recorded.
