# Swipe responsiveness — working-tree follow-up

Date: 2026-10-10, Africa/Nairobi. Tester: Codex; user supplies natural-gesture feedback. This follows the [earlier display/navigation checkpoint](../2026-10-10-display-navigation/README.md), which passed long scripted gestures but did not establish natural swipe reliability.

## Identity and scope

- Baseline commit: `b1aa29a19722e54dcd41c4289e6d95de103dfc25`; uncommitted debug application `1.5.3`, code `18`. No release, tag, push or publication.
- Phone: Samsung SM_G970U, Android 12, USB `R58M83HFYYY`; 1080 × 2280 pixels, 480 dpi, font scale 0.8, three-button navigation (mode 0), all read back on the phone.
- QGC: not used; version not applicable.
- Changed behavior: passive-region horizontal gesture recognition/dispatch and animation eligibility. No simulation, MAVLink or runtime ownership changes.

## Root cause and implementation

The old recognizer required 18% of the screen width, permanently cancelled ambiguous initial diagonal movement, inspected movement after child vertical scrolling could consume it, and disabled navigation during the screen animation. Long straight injected gestures therefore did not adequately represent the user's shorter, slightly diagonal gestures.

The replacement locks a dominant axis after platform touch slop, waits for ambiguous starts, arbitrates horizontal motion in Initial only on registered passive regions, and checks Final consumption before taking an unclaimed gesture. It uses 48 dp travel or a 24 dp / 400 dp-per-second same-direction horizontal flick, tracked with Compose VelocityTracker. Updated callback/page state avoids restarting the detector; screen transition animation does not block new gestures. See [ADR-0009](../../../internals/decisions/0009-display-and-navigation.md) for the durable contract and official technical sources.

## Verification

- Final recognition APK SHA-256: `50240fca6c35947cb95ba15f34a9cbd744392c67454d107070082d079acffa06`. Replacement install succeeded without clearing data; package version 1.5.3/code 18, last update 2026-10-10 22:37:43.
- Gradle `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --max-workers=2` succeeded in 3m 20s. 239 unit tests, zero failures/errors/skips. [Build log](evidence/recognition-final-gate.txt).
- [Final Android instrumentation](evidence/instrumentation-final.txt): all eight tests passed on the phone in 7.895 seconds. Cases cover short movement, fast flicks, diagonal drift, vertical-scroll competition, local control ownership, multi-touch, disabled eligibility and consecutive navigation.
- The [intermediate 600 dp/second threshold](evidence/instrumentation-intermediate-600dp.txt) failed the short-flick case (seven of eight passed). The final 400 dp/second threshold passed that same test. The initial test-build log records a missing test import that was corrected before the completed gate.
- Actual-app ADB attempts are diagnostic only: `01-cockpit-before.xml` showed Cockpit, the planned 100 px / 70 ms flick still captured Cockpit in `02-control-short-flick.xml`, and the subsequent 180 px / 200 ms attempt captured Ops in `03-control-short-drag.xml`. This sequence was not isolated sufficiently to attribute the unexpected destination; those filenames describe intended cases, not PASS results.
- The user reported that ordinary swipes responded on the first attempt, then reported remaining issues and requested smoother motion across all screens. This record establishes recognition coverage, not final smoothness acceptance; see the fluid-motion follow-up.

## Impact review

| Row | Scope / qualification |
|---|---|
| Product surfaces | Cockpit, Control, SIM and Ops share the changed detector. Contextual mission/route controls remain excluded; onboarding is unchanged. |
| Control paths | No input/command path changes. Existing local/manual and phone-sensor regression tests remain required. No new GCS direct or mission qualification. |
| Runtime states | Foreground navigation affected; active-only screen composition preserved. Background, AUTO, handoff and process-recreation ownership unchanged; not newly device-qualified. |
| Simulation | No physics, autopilot, battery, failure or mission-state changes. |
| Protocol | No discovery, peer, telemetry, command, parameter, mission or capability changes. |
| Reverse states | Adjacent navigation remains reversible and non-wrapping. Flight/runtime/recording reverse actions unchanged; not all device-tested. |
| Evidence | Unit suite, lint/build, Compose Android tests and bounded actual-app checks are recorded separately below when executed. |
| Documentation | ADR-0009 and user navigation guide updated; prior evidence retained without rewriting its acceptance claims. No glossary or operating-procedure change needed. |

Natural-user smoothness remains a separate follow-up. This focused pass does not qualify TalkBack, other Android navigation modes, landscape, large fonts, received-route gestures, armed physical tilt, GCS authority or background handoff.
