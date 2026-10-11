# Fluid swipe motion — working-tree verification

Date: 2026-10-10, Africa/Nairobi. Tester: Codex. Baseline commit `b1aa29a19722e54dcd41c4289e6d95de103dfc25`, debug version 1.5.3/code 18, uncommitted changes.

The user requested smoother swiping across all screens after the [recognition follow-up](../2026-10-10-swipe-responsiveness/README.md). Passing a release-time swipe detector did not establish smooth visual interaction: the content previously stayed still during the drag and changed only when the finger lifted.

## Implementation

- Recognized horizontal drags move the active screen immediately through a graphics layer. Movement has continuous resistance, increasing at the first/last destination.
- A critically damped spring returns cancelled gestures or settles the new destination. A new drag interrupts settling and continues from the current visible offset.
- The input container stays stationary, and passive regions resolve their live translated bounds on touch-down. This avoids movement of the content distorting input coordinates or making animated regions temporarily miss gestures.
- Only the selected destination is composed. Drag frames update the graphics layer instead of recomposing the full screen; navigation emits no flight commands.
- Existing eligibility, vertical scrolling, local controls, multi-touch cancellation, TalkBack/IME exclusions and no-wrap endpoints are preserved. See [ADR-0009](../../../internals/decisions/0009-display-and-navigation.md) and the [user guide](../../../user/navigation-and-display.md).

## Verification status

Final verification of this intermediate build occurred on 2026-10-11. Debug APK SHA-256: `0b13efb3b84d81cc07a73b41a32ff8faf95b0b74ebdb1c2f62019141de2d8834`; installed at 08:16:55 Africa/Nairobi on Samsung SM-G970U, serial R58M83HFYYY, Android 12. The [Gradle gate](evidence/gradle-final.txt) passed in 3m17s (239 unit tests, lint, app and test APK builds). The [phone instrumentation](evidence/phone-instrumentation.txt) passed all ten tests in 11.139s.

**User acceptance failed:** the user reported that swiping was still not smooth/fluid. These passing tests only established dispatch, visible drag feedback and interruption; they did not detect the visual jump on commit or excessive drag resistance. This build is superseded by the [continuous-pager follow-up](../2026-10-11-continuous-pager/README.md). Do not treat this checkpoint as smoothness acceptance.

## Impact review

| Area | Scope |
|---|---|
| Product surfaces | Shared Cockpit, Control, SIM and Ops navigation motion. Onboarding and contextual mission controls unchanged. |
| Control paths | Local/manual, phone-sensor, GCS direct and GCS mission owners unchanged; runtime regression suite required. |
| Runtime states | Foreground presentation changed; background, service handoff, AUTO and process recreation unchanged. |
| Simulation / protocol | No physics, autopilot, failure, battery, mission or MAVLink changes. |
| Reverse states | Cancelled/interrupted swipes settle back; endpoints resist without wrapping. Flight/runtime/recording reverse actions unchanged. |
| Evidence | Unit regression, Android touch/motion tests, lint and APK build; device-specific results below. |
| Documentation | User navigation guide and ADR-0009 updated. This separate record preserves the prior recognition checkpoint. |

No new QGC endpoint is used. This focused change does not requalify armed physical tilt, background handoff, TalkBack, all Android versions/navigation modes, large fonts or landscape. Smoothness on the user's device requires the recorded phone checks and user feedback.
