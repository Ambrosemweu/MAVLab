# ADR-0011: SIM initial inspection camera

Status: Accepted for working-tree implementation
Date: 2026-10-11

## Context

The generic orbit home opened SIM almost straight on, obscuring the battery top and depth of the frame. The requested reference shows an elevated front three-quarter inspection view. The drone may already have a nonzero position or heading when SIM opens; rotating the asset independently would contradict the digital twin.

## Decision

At renderer creation, snapshot the rendered body position and simulation yaw. Center the orbit target on that position and place the eye 1.05 scene units away, at 35 degrees to the asset's local +Z front and 28 degrees elevation. Rotate the horizontal offset by the same positive Y-axis yaw as `DroneModelController.bodyRotation`. Initialize both the camera node and SceneView's orbit manipulator from these coordinates to avoid a first-frame camera jump.

Remember these initial coordinates for the renderer lifetime, not keyed to telemetry. Keep SceneView's existing orbit, pan and zoom gestures. A fully offscreen SIM releases the renderer through its existing visibility gate; reopening creates a new home from the then-current state. Do not restart/reset the camera during a partial page drag. Route-view behavior, lighting, asset scale, materials and authoritative model transforms remain unchanged.

Physical-device inspection exposed a related framing defect: SceneView's default `autoCenterContent` adds a parent transform that cancels the supplied model position and recenters the asset bounds. Disable both `autoCenterContent` and `autoFitContent` for this scene: MAVLab already supplies state-derived model placement and an explicit orbit camera. Preserve `DroneModelController` and the propeller-animation path unchanged. No per-frame pose workaround is needed.

## Boundaries and verification

This changes inspection perspective only. It issues no arm, mode, attitude, position, sensor, mission or runtime command and adds no persisted settings. Unit tests cover angle/distance, positive yaw rotation, a translated vehicle at the reference heading, wrapped headings and invalid-heading fallback. Build/lint and actual-phone captures cover initial framing, free orbit, telemetry stability and leave/re-entry. Appearance matching is a visual assessment, not an exact photogrammetric reconstruction of the reference.

An attempted standalone SceneView instrumentation harness encountered a native Filament thread-affinity failure during model loading; it is not retained as a passing regression. Actual-app opening/orbit/re-entry captures and source review verify the centering fix. Automated native translated/tilted render qualification remains deferred; synthetic camera geometry checks do not establish live-flight or GCS acceptance.

API reviewed against the pinned SceneView 4.16.10 camera helpers and Filament 1.71.4 orbit manipulator. See [SIM user guidance](../../user/sim.md).
