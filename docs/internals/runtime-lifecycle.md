# Runtime Lifecycle Contract

Audience: Maintainers and Android contributors
Status: Current updated v1.5.3 contract
Last verified: 2026-09-02

This document explains when MAVLab keeps the on-phone runtime alive and when it stops.

## Owners

- `MainActivity` reports visibility and explicit lifecycle actions.
- `SimulationService` owns the foreground service, notification, runtime startup/shutdown, and wake/Wi-Fi locks.
- `BackgroundRuntimePolicy` decides whether background execution is retained.
- `AppRuntime` owns simulation and supporting components.

`AppRuntime.runtimeLifecycle` publishes `STARTING`, `RUNNING`, `STOPPING`, `STOPPED`, or `ERROR` for UI state. `stateUpdatedAtElapsedRealtimeMs` records the latest engine-state emission using Android elapsed realtime. These are runtime/freshness signals; neither replaces MAVLink connection status or control authority.

All Controller input application belongs to AppRuntime. Phone samples retain the canonical foreground lifecycle; Custom Input setters send PilotInput directly, and Direct RPM follows armed/mission state through runtime observation. Composing or leaving Control does not apply, cancel or recreate these control paths. This ownership change does not change background retention or phone-sensor suspension. See [ADR-0006](decisions/0006-controller-operational-layout.md).

## Intended model

```text
App visible
  -> retain runtime

App enters background
  -> one-minute QGC handoff window
  -> retain if a validated GCS is connected
  -> retain if the vehicle is armed
  -> retain if an AUTO mission is active
  -> otherwise stop after the grace window

Explicit Stop / close-removal
  -> stop immediately
```

The handoff window lets a user open MAVLab and switch to QGroundControl on the same phone before the simulator disappears.

## Background behavior

While retained in the background:

- simulation and MAVLink may continue;
- the foreground-service notification remains visible;
- wake and Wi-Fi locks support active work;
- phone-sensor control is disabled to prevent hidden gyro input;
- drone sound remains active so arming and motor changes commanded from QGroundControl stay audible.

A backgrounded, idle, disconnected runtime outside the grace period should stop rather than consume battery indefinitely.

## Explicit shutdown

Deliberate shutdown wins over retention conditions. Notification Stop and explicit close/removal paths should release locks, stop protocol work, stop simulation, and terminate the service cleanly.

## Invariants

1. Do not start two independent shared runtimes.
2. Acquire and release wake/Wi-Fi locks with service ownership.
3. Never retain forever only because the app was opened once.
4. Do not use phone tilt as live input after backgrounding.
5. Keep motor audio synchronized with authoritative simulation state for the full retained runtime.
6. Timing or retention changes require policy tests and user-doc review.
7. Manifest foreground-service declarations must match actual use.
8. A stopped, unavailable, or stale runtime must not present default telemetry as live state.

## Evidence

Primary source:

- `mavlab-android/app/src/main/java/com/ascend/mavlab/MainActivity.kt`
- `.../service/SimulationService.kt`
- `.../service/BackgroundRuntimePolicy.kt`
- `.../core/common/AppRuntime.kt`

Focused test:

- `mavlab-android/app/src/test/java/com/ascend/mavlab/service/BackgroundRuntimePolicyTest.kt`
- `mavlab-android/app/src/test/java/com/ascend/mavlab/core/common/RuntimeComponentPolicyTest.kt`

Real-device validation records the device, Android version, battery restrictions, QGC build, exact MAVLab build, and observed notification/background behavior.

## Mission consolidation review

Four-tab navigation and contextual mission views create no additional runtime owner. Received source survives process recreation; the stored mission restores as loaded, with fresh execution history and no fabricated upload ACK. Unknown legacy source remains retained but blocked for local AUTO/route display until re-upload. Mission action freshness uses the existing RUNNING and elapsed-realtime state-age contract. Retention, background handoff, locks and phone-sensor suspension are unchanged. See [ADR-0007](decisions/0007-contextual-missions.md).
