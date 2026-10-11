# ADR-0010: SIM inspection lighting

Status: Accepted for working-tree implementation
Date: 2026-10-11

## Context

The drone was readable from above/front but other sides disappeared into the black background. The explicit `rememberEnvironment(engine)` call created a skybox-only environment in the installed SceneView 4.16.10; unlike its loader overload, it did not load the bundled neutral IBL. MAVLab also added a yaw-following, roll-brightened sun and a fixed point fill alongside SceneView's default lights.

## Decision

Load SceneView's bundled, prefiltered neutral IBL using the environment-loader overload, retaining its black skybox. Set indirect intensity explicitly to 25,000. Use one stable directional key at 45,000 lux, with shadows, instead of driving presentation lighting from simulated yaw/roll. Replace the default fill with one camera-following point light without shadows. Its nominal facing-surface illuminance is 8,000 lux, below the key. Filament uses lumens for point lights: power is `4 pi r² E`, where r is camera-to-rendered-body distance in scene units. Its fade radius is ten times that distance. Guard invalid/extreme distances so power and radius remain finite.

SceneView invokes its frame callback after updating the orbit camera. Update only the fill position, power and range there; do not publish per-frame Compose state or allocate new lights. Reuse the existing asset, material definitions and camera gestures. The bundled probe adds no online requirement, HDR runtime preprocessing or new user toggle.

The environment and key follow SceneView's remembered-resource lifecycle. The point fill is remembered with the engine and destroyed on composition disposal; explicit ownership avoids SceneView's JVM-21 inline helper in this JVM-17 application. SIM's existing deferred renderer startup and pager lifecycle protections remain unchanged.

## Boundaries and verification

This is an inspection rig, not a simulation of outdoor sun/weather. Camera orbit changes presentation only. Physics, motors, mission progression, telemetry, recording, input eligibility and control authority remain unchanged. No emissive/unlit material replacement, model transform override, or new rendering engine is introduced.

Unit tests verify distance compensation, orbit-side independence and finite guarded values. Build/lint and bounded physical-phone captures must cover default, rear/side, top and underside views, plus zoom and SIM re-entry. Visual checks do not establish every material's physical accuracy or all-device frame-rate performance.

References: [Filament lighting and units](https://google.github.io/filament/main/filament.html), [SceneView 4.16.10 factories](https://github.com/sceneview/sceneview/blob/v4.16.10/sceneview/src/main/java/io/github/sceneview/SceneFactories.kt), [frame callback ordering](https://github.com/sceneview/sceneview/blob/v4.16.10/sceneview/src/main/java/io/github/sceneview/Scene.kt).
