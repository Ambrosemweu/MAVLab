# Runtime Lifecycle Contract

Audience: Maintainers and Android contributors
Status: Current v1.5.3 contract
Last verified: 2026-09-01

This document explains when MAVLab keeps the on-phone runtime alive and when it stops.

## Owners

- `MainActivity` reports visibility and explicit lifecycle actions.
- `SimulationService` owns the foreground service, notification, runtime startup/shutdown, and wake/Wi-Fi locks.
- `BackgroundRuntimePolicy` decides whether background execution is retained.
- `AppRuntime` owns simulation and supporting components.

## Intended model

```text
App visible
  -> retain runtime

App enters background
  -> 45-second QGC handoff window
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
- phone-sensor control and drone sound are disabled to avoid misleading input and unnecessary work.

A backgrounded, idle, disconnected runtime outside the grace period should stop rather than consume battery indefinitely.

## Explicit shutdown

Deliberate shutdown wins over retention conditions. Notification Stop and explicit close/removal paths should release locks, stop protocol work, stop simulation, and terminate the service cleanly.

## Invariants

1. Do not start two independent shared runtimes.
2. Acquire and release wake/Wi-Fi locks with service ownership.
3. Never retain forever only because the app was opened once.
4. Do not use phone tilt as live input after backgrounding.
5. Timing or retention changes require policy tests and user-doc review.
6. Manifest foreground-service declarations must match actual use.

## Evidence

Primary source:

- `mavlab-android/app/src/main/java/com/ascend/mavlab/MainActivity.kt`
- `.../service/SimulationService.kt`
- `.../service/BackgroundRuntimePolicy.kt`
- `.../core/common/AppRuntime.kt`

Focused test:

- `mavlab-android/app/src/test/java/com/ascend/mavlab/service/BackgroundRuntimePolicyTest.kt`

Real-device validation records the device, Android version, battery restrictions, QGC build, exact MAVLab build, and observed notification/background behavior.
