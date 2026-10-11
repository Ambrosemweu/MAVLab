# Onboarding CTA and graph refinement — working-tree validation

Date: 2026-10-11, Africa/Nairobi. Tester: Codex. Branch `feature/mavlab-uiux-refinements`, baseline `7948d64403fcb0768b41d9b761e78885f930f3f6` plus uncommitted changes. App version 1.5.3/code 18. No commit, push, tag or release.

## Scope

The final button now reads **Start flying**, retaining the existing completion callback and Cockpit landing; it does not arm/take off. The recording illustration reuses Cockpit's `RollingChart`, with the same four trace colours/styles, metre/degree axes, endpoint markers and ten-second layout. Smooth static example signals replace the old sparse Pen polylines. The source label explicitly says **not live telemetry**, including the chart accessibility description. The live chart's default source label, actual data and rendering remain unchanged. The Pen document is unchanged.

## Evidence

- [Host gate](evidence/gradle-gate.txt): unit tests, debug APK, Android test APK and lint passed in 4m 57s. **253 unit tests, zero failures/errors/skips**. [Preview unit tests](evidence/preview-unit-tests.xml) check sample continuity, window length, units, palette and line styles. [Lint](evidence/lint.txt): 0 errors, 34 existing warnings. `git diff --check` clean.
- Installed with `adb install -r`, preserving app data, on Samsung SM_G970U (`R58M83HFYYY`), Android 12, font scale 0.8. Android reports update time 2026-10-11 18:49:24 +03:00. APK SHA-256: `f19b66f09809db09f0ad2e0b3068ad589a3edb645f1f185e5edbf2e5ed0013af`.
- [First phone run](evidence/phone-tests-initial.txt): 7 passed, 2 failed. The new chart and final action checks passed. The reverse-swipe test began at the protected screen edge, and the slider test began at the edge instead of the current thumb. Tests were corrected to exercise interior navigation gestures and thumb dragging; no production gesture policy was changed for this refinement.
- [Test rebuild](evidence/test-build.txt) passed in 13s. [Final phone run](evidence/phone-tests-final.txt): **all 9 onboarding tests passed** in 14.037 seconds, including finger-following/reverse swipe, slider isolation and value change, endpoints/completion, Skip, saved-page restoration, accessibility-disabled swipe, short-display/large-text scrolling and the new chart/CTA check. This supersedes the focused onboarding test deferral in the [earlier checkpoint](../2026-10-11-onboarding/README.md), not its preserved historical failures.
- Actual app replay opened from Ops → About MAVLab. Advanced with the fixed action dock to step 9; visually checked the [settled final page](evidence/final-onboarding-chart.png) and retained its [UI hierarchy](evidence/final-onboarding-chart.xml). The chart and both action buttons are visible; the longer content can scroll. Left the final page open for user review. Did not invoke arm, takeoff, fault injection, recording or connection changes.
- The complete cross-surface Android suite was not rerun. No real flight, live-QGC command, download-browser, TalkBack, background or release qualification is claimed.

## Impact review

- **Product surfaces:** onboarding CTA and recording preview; shared chart accepts a source label, with Cockpit's default unchanged.
- **Control paths / simulation / protocol:** unchanged; example samples have no runtime subscription or command path. Manual/Phone Sensors/GCS authority unchanged.
- **Runtime states:** tutorial UI only; service, background, armed/AUTO and process lifecycle policy unchanged. Tutorial page restoration passed its focused phone test.
- **Reverse states:** Back/Skip/completion tested; Start flying is navigation only. Flight, fault and recording reversal unchanged and not exercised.
- **Evidence:** host and focused real-device results above; initial test failures retained separately from final passes.
- **Documentation:** [user guide](../../../user/onboarding.md) and [ADR-0012](../../../internals/decisions/0012-onboarding-paging-and-preview-truth.md) updated. No new persisted format or glossary term.
