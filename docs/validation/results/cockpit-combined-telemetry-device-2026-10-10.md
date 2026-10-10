# Combined Cockpit telemetry installation — 2026-10-10

Follow-up device check for the [combined-chart build](cockpit-combined-telemetry-working-tree-2026-10-10.md). The earlier build-only record remains historical; this execution completes its pending installation and bounded portrait layout review.

## Identity and evidence

- MAVLab 1.5.3/code 18, debug APK SHA-256 `a323141616a6358be62757a8c642d294d4975d6e544bda40ad3fc1ef3adf71d7`.
- Branch `feature/mavlab-uiux-upgrade`; base commit `703b7f90d717ff386ee9a232bbc5a51a38af3ca0` plus uncommitted UI/UX changes.
- Samsung SM-G970U, Android 12, serial `R58M83HFYYY`; tester Codex; 2026-10-10.
- QGroundControl was not used; no QGC version or interoperability result applies.
- [Combined chart screenshot](evidence/cockpit-combined-telemetry-installed-2026-10-10.png) and [on-device UI hierarchy](evidence/cockpit-combined-telemetry-installed-2026-10-10.xml).

## Outcome and impact

| Row | Verification |
| --- | --- |
| Product surfaces | Cockpit launches and displays one chart. All four keys and values, metre/degree axes, matching line styles and the ten-second time labels fit in the portrait chart card. Other surfaces were not checked. |
| Control paths | No flight commands issued by the agent; this was installation and read-only telemetry/UI review with scrolling. Phone/manual/GCS control was not requalified. |
| Runtime states | Foreground launch and telemetry presentation checked. Background and process recreation were not exercised. |
| Simulation | No code changes. Observed altitude and attitude values occupy their respective axes; the screenshot shows zero altitude at the bottom, zero roll/pitch at the centre and nonzero yaw near the top of the degree scale. |
| Protocol | Not applicable: no protocol changes or live QGC exercise. |
| Reverse states | Expanded dropdown retains Hide telemetry & details; flight reverse actions were not exercised by the agent. |
| Evidence | `adb install -r` returned Success; package update time is 2026-10-10 08:39:23 (device clock). App launched, chart inspected, and Android crash buffer was empty during the bounded check. Existing build evidence records 180 passing unit tests, lint and assembleDebug; no rebuild was needed. |
| Documentation | This installation record resolves the prior pending device check without changing the immutable build-only record. |

This is bounded debug-build evidence, not release, landscape or large-font qualification.
