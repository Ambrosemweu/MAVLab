# Continuous page transitions — working-tree verification

Date: 2026-10-11, Africa/Nairobi. Tester: Codex. Baseline commit `b1aa29a19722e54dcd41c4289e6d95de103dfc25`; debug 1.5.3/code 18 with uncommitted changes. QGC not used.

## Cause and change

The user rejected the [previous motion revision](../2026-10-10-fluid-swipe/README.md). Its offset compressed normal finger travel using a 96 dp resistance curve, then reset to a 48 dp entry offset on selection. This created a discontinuity independent of gesture recognition correctness. The previous tests checked a nonzero offset and successful selection, not continuous page geometry.

The guarded detector now drives an Android Compose HorizontalPager. Drag pixels map one-to-one to page scroll, incoming/outgoing shells remain adjacent, and release changes only the target. Settling retains position and release velocity and can be interrupted. Preview pages cannot invoke touch controls or Back handlers; their lifecycle-aware collectors are stopped. SIM renderer cold-start is deferred until arrival. See [ADR-0009](../../../internals/decisions/0009-display-and-navigation.md).

The user subsequently identified persistent middle-screen failures in Ops and intermittent ones in Cockpit. Their previous registration covered only selected status regions, leaving most Ops cards and gaps ineligible. Both surfaces now accept navigation across informational content and blank space. Explicit exclusions preserve buttons, action rows, charts, switches, filters and location fields. Vertical intent still scrolls locally. Two new Android cases cover a middle gap and an excluded button inside a passive parent; a unit case covers exclusion priority, scrolling and disposal.

The user also reported a bright white flash on opening. A phone launch recording and source inspection identified the light native starting window before the dark Compose UI. Native launch and Android 12 splash backgrounds now match `#0A0E1A`.

The first expanded-coverage Android run passed 16/17 cases but found that dragging horizontally inside an excluded wide button could trigger its tap. Tap-only exclusions now consume horizontal drag after local touch slop, cancelling that press. Sliders, switches and fields preserve their own gestures. The regression also verifies a normal subsequent tap still works.

## Evidence

Final working-tree APK installed on Samsung SM_G970U, serial `R58M83HFYYY`, Android 12, 1080 × 2280, 480 dpi, font scale 0.8, three-button system navigation. Version 1.5.3/code 18; installed 2026-10-11 at 14:03:44 Africa/Nairobi. APK SHA-256: `1eed6595dec6ace020f153cb171a7e47fcce463d5482a5feab559c4c273d0e8a`. Application data and the full-screen preference were preserved. Emulator remains stopped.

- [Final Gradle gate](evidence/final-gradle-gate.txt): `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --max-workers=2 --console=plain`, successful in 1m 34s. Unit XML totals: 240 tests, zero failures/errors/skips. Lint: zero errors, 34 warnings; this is not a warning-free claim. API 27 navigation-icon styling is resource-qualified for minimum SDK 26.
- [Final physical-phone instrumentation](evidence/final-phone-tests.txt): 17/17 cases passed in 14.221 seconds (15 gesture/pager cases and two native launch-theme cases). Coverage includes vertical scroll arbitration, interactive controls, repeated/reversed gestures, interrupted settling, one-to-one geometry, no release-time teleport, inactive preview lifecycle, middle-gap recognition and button drag cancellation followed by an ordinary tap.
- Actual app: the previous Ops Runtime-card swipe `(360,1230) → (750,1230)`, 250 ms, left the app on Ops ([baseline](evidence/mavlab-ops-baseline.xml), [failed swipe](evidence/mavlab-ops-middle-old.xml)). The exact same horizontal gesture on the final APK moved to SIM on the first attempt ([start](evidence/mavlab-final-ops-start.xml), [result](evidence/mavlab-final-ops-middle.xml)).
- Actual app: a vertical middle-content drag `(550,1640) → (550,880)`, 500 ms, scrolled Ops without navigating. Runtime components moved from y=1182 to y=258 and recordings became visible ([result](evidence/mavlab-final-ops-vertical.xml)). A subsequent horizontal swipe across the scrolled Recordings heading `(360,1120) → (750,1120)`, 250 ms, moved to SIM on the first attempt ([start](evidence/mavlab-final-ops-after-scroll-start.xml), [result](evidence/mavlab-final-ops-after-scroll-swipe.xml)).
- Actual app: a horizontal in-bounds drag on Show Runtime components `(250,484) → (520,484)`, 300 ms, neither navigated nor expanded the section ([result](evidence/mavlab-final-ops-button-drag.xml)). A normal subsequent tap at `(330,484)` expanded it ([result](evidence/mavlab-final-ops-button-tap.xml)); the section was collapsed again afterwards. No simulation or recording action was invoked.
- Actual app: a swipe through Cockpit's middle gap `(760,1240) → (400,1240)`, 300 ms, moved to Control on the first attempt ([start](evidence/mavlab-final-cockpit.xml), [result](evidence/mavlab-final-cockpit-gap.xml)). All navigation checks used a disarmed vehicle with recording stopped and no detected GCS peer.
- Cold launch: a seven-second screen recording showed a dark native starting window, then dark Cockpit, with no observed white flash. Cold activity launch reported 1987 ms. The [contact sheet](evidence/launch-dark-window-contact.png) samples at 5 fps from 0.8 seconds into the capture, excluding the preceding launcher. System bars remained briefly visible during startup before immersive presentation; this observation does not claim immediate bar hiding or frame-by-frame proof on every device. MAVLab was left open on Cockpit.

These are bounded working-tree checks, not publication, real-aircraft compatibility, or a zero-jank performance guarantee. The earlier failing button regression is retained alongside the passing final run rather than rewritten as a pass.

## Impact matrix and limits

- Product: Cockpit, Control, SIM and Ops share the pager; contextual mission request consumption is restricted to the active page. Onboarding unchanged.
- Control paths: no local/manual, phone-sensor, GCS direct or mission authority changes; no navigation-generated flight commands. Existing runtime regressions required.
- Runtime: inactive preview UI lifecycle is capped at CREATED; global AppRuntime/service lifecycle is unchanged. Background handoff, armed/AUTO operation, explicit stop and process recreation are not newly device-qualified.
- Simulation/protocol: no engine, physics, autopilot, battery, mission protocol, telemetry, identity or capability modifications.
- Reverse states: cancel, reverse swipe, endpoint, immediate next swipe and blocked control gestures are in scope. Arm/disarm, land/takeoff, recording and failure reset paths are unchanged.
- Evidence: unit tests, lint/build, Android gesture/geometry tests, bounded actual-app phone checks. Passing tests are not a guarantee of frame rate or subjective smoothness.
- Documentation: user navigation guide and ADR-0009 revised. This is not a release or hardware/QGC qualification.

Remaining acceptance: user feel across all screens; broader fonts, accessibility, navigation modes, orientations and Android versions.
