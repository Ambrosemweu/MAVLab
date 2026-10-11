# ADR-0012: Onboarding paging and truthful learning previews

Status: Accepted for the UI/UX feature branch; not a release claim.

## Decision

Implement directly in Android using the existing Pen onboarding's dark palette, typography, progress rail, pill buttons and vector illustrations. Preserve the nine topics and canonical Pen document without editing it in this implementation task. Move calibration/phone input before first flight; describe optional GCS use, contextual missions and opt-in recording accurately.

Reuse `ScreenSwipeMotion`, `ScreenSwipePager` and the guarded gesture detector over a tutorial-specific passive page area. A first native user-scrollable pager attempt snapped a short swipe back and intercepted a demo slider drag in phone tests. Reusing the app's established arbitration gives short swipes, finger-following motion, interruption and explicit control exclusions the same behavior as the main surfaces, without a second gesture policy. The committed step is saved for recreation; a half-drag is not restored. Buttons select the same motion model; progress and button context follow its committed destination. Disable button-driven page changes during motion, contain each page's drawing, do not wrap, and do not implicitly finish by swiping off the final page. Android Back pages backwards except at the initial page. The shell supplies existing touch-exploration/keyboard eligibility to disable swiping, retaining explicit buttons. Compose duration scaling remains in effect. Button-busy state is derived, so raw scroll offsets do not trigger full tutorial recomposition every frame.

Per-page vertical scrolling with natural-height illustrations handles short displays and larger text, while progress/Skip and the navigation dock remain outside the scrolling content. A two-by-two surface gallery replaces its competing horizontal carousel; slider and QGC-link action bounds are excluded from page navigation. Inactive previews inherit the shared pager's blocked semantics/touch and capped lifecycle. Stable page keys preserve tutorial view state.

Label fictional telemetry, GCS link examples, route and recording visuals as previews. Demo sliders own only local Compose values; they never call `AppRuntime`, the engine, mission storage, recording or fault injection. Remove a no-op reset button and misleading always-live GCS wording. Reuse the pure labelled GCS dot component without subscribing to runtime from the tutorial. The only external action opens official QGC download documentation, with visible fallback text if no browser can handle the intent.

Completion and Skip retain the existing `mavlab_app/onboarding_complete` preference and route to Cockpit. Do not version-bump/reset completion, automatically start a flight, change authority or record a session. Existing users can deliberately replay from Ops. Persisting the tutorial's page is UI restoration only, not restoration of a flight.

The final CTA is **Start flying**, an invitation to enter Cockpit, not an arm/takeoff command. The recording preview reuses `RollingChart` with its actual axis layout, time window and Cockpit trace palette/styles. Static 41-sample eased/sinusoidal signals replace the sparse Pen polylines. A configurable source label preserves the existing live-chart default while explicitly marking the onboarding chart as non-live, including its accessibility description. Real telemetry remains unfiltered and the live chart's geometry/values are unchanged.

## Boundaries and evidence

Navigation and documentation review is explicit: [ADR-0009](0009-display-and-navigation.md) owns the reused gesture/motion mechanism; background/runtime retention and canonical armed phone-sensor eligibility are unchanged. Onboarding composition never controls sensor eligibility. No MAVLink capability, coordinate frame, simulation or persisted recording/mission format changes.

Unit tests cover topics/order, calibration and Land-before-Disarm, optional GCS, preview labels and recording opt-in. Compose device tests cover continuous finger tracking, button/Android Back, endpoints/completion, Skip, page restoration, demo slider gesture priority, disabled swipe and short-display/large-text scrolling. Actual app/replay/install evidence belongs in `docs/validation/results/`; do not infer live-QGC, armed-flight or background qualification from tutorial tests.
