# MAVLab Product Purpose

Source-of-truth vault note: `/home/ambrose/Documents/Obsidian Vault/Ascend Operating System/Ascend Labs/MAVLab Product Documentation.md`

Updated: 2026-06-20

## Core purpose

MAVLab exists to make drone simulation approachable.

A learner who wants to understand drones is often pushed straight into a hard professional stack:

- ROS / ROS 2
- Gazebo / Webots / JSBSim
- ArduPilot SITL / PX4 SITL
- Docker, Python bridges, MAVProxy, Linux networking, UDP routing, and multi-window setup
- QGroundControl or Mission Planner configuration

Those tools are powerful, but they create a steep learning curve before the learner has understood the drone itself.

MAVLab solves that problem by giving learners a friendly first layer: one Android-first simulation app where they can see and operate the core drone concepts before graduating into the full professional toolchain.

## Product statement

MAVLab is a user-friendly phone-based drone simulation and digital-twin platform that helps students, operators, and builders learn drone systems without first needing to master ROS, Gazebo, ArduPilot/PX4 SITL, Docker, MAVProxy, Linux networking, or simulator infrastructure.

It uses the phone as the simulation device, accepts phone/manual control and QGroundControl/MAVLink control, then visualizes the resulting drone state through cockpit telemetry, missions, failures, logs, and a functional 3D SIM.

## Learning-order thesis

MAVLab does not compete with professional drone tools. It changes the learning order.

```text
MAVLab first
  -> understand sensors, attitude, telemetry, flight modes, missions, failures, GCS workflows, and simulation state
  -> then graduate into ArduPilot SITL, PX4 SITL, Gazebo, ROS 2, Webots, JSBSim, hardware-in-the-loop, and real aircraft
```

## What MAVLab abstracts away at the beginning

MAVLab should hide or simplify the early complexity of:

- Installing ROS/Gazebo/SITL stacks.
- Building ArduPilot/PX4 locally.
- Running multiple terminals and bridges.
- Debugging UDP endpoints before understanding MAVLink.
- Needing a physical Pixhawk, drone frame, batteries, RC transmitter, GPS module, telemetry radio, or safe test field for the first learning loop.

## What MAVLab should still teach

MAVLab should still expose real drone concepts:

- Arm/disarm and flight modes.
- Roll, pitch, yaw, altitude, velocity, GPS, and heading.
- Sensor quality and failures.
- MAVLink telemetry and command flow.
- QGroundControl connection and mission workflows.
- Manual control vs GCS mission control authority.
- Autonomous mission upload/execution.
- Wind, battery, payload, motor, GPS, compass, barometer, and link-loss failures.
- 3D state visualization through the SIM tab.
- Logs, reports, replay, and debrief.

## Positioning rule

Use this framing in public docs:

> MAVLab is not a replacement for ROS, Gazebo, ArduPilot, PX4, QGroundControl, or Mission Planner. MAVLab is the friendly simulation and learning layer before them.

Avoid framing MAVLab as only:

- A drone game.
- A static course app.
- A generic FPV simulator.
- A thin QGroundControl clone.
- A replacement for real autopilot stacks.

## Implications for documentation

Every major MAVLab doc should make the beginner-to-professional bridge clear:

1. README: explain the problem and the friendly first-layer solution.
2. Setup guide: emphasize no ROS/Gazebo/SITL setup is required for the core experience.
3. Teacher guide: show how instructors can teach drone concepts before introducing professional stacks.
4. Product surface definition: each tab should reduce complexity and reveal one system concept at a time.
5. Roadmap: advanced bridges to ArduPilot SITL, PX4 SITL, ROS 2, Gazebo/Webots/JSBSim should remain future graduation paths, not prerequisites.
