# ADR-0001: Phone-first offline core

Status: Accepted
Date recorded: 2026-08-31

## Context

Traditional drone-simulation learning often requires SITL, ROS/Gazebo, bridges, networking, several processes, and a capable computer before the learner can see core drone concepts.

## Decision

MAVLab’s core simulation, control, visualization, recording, and MAVLink endpoint run on the Android phone. QGroundControl may connect locally or over a LAN, but cloud services, Docker, Python bridges, ROS, Gazebo, and SITL are not prerequisites for the core learning loop.

## Consequences

- Classroom setup and first learning are simpler.
- Android lifecycle, power, memory, and performance are first-class constraints.
- Fidelity must be described honestly; MAVLab is not ArduPilot/PX4 SITL.
- Future professional-tool bridges are graduation paths, not dependencies.

## Replace when

Only reconsider if evidence shows target learning outcomes cannot be met on supported phones and the replacement preserves an equally accessible first-run path.