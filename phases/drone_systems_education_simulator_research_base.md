# Drone Systems Education Simulator & ArduPilot/MAVLink Teaching Kit

> Status: Early research base. The active implementation roadmap is `mavlab_standalone_architecture_research.md` plus the corrected phase files in `phases/`. Phase 0 is now project skeleton only; Phase 1 is the QGC/MAVLink protocol proof.

**Working research base for the final system**  
**Project concept:** A low-cost drone systems education simulator that uses **Mission Planner**, **ArduPilot SITL**, **MAVLink**, and a custom **phone app / education dashboard** to teach students how drones sense, communicate, stabilize, navigate, and fail.

---

## 1. Executive Summary

The idea began with a simple question:

> Modern phones have gyroscopes, accelerometers, GPS, magnetometers, cameras, and network connectivity. Can we use a phone to trick drone software into thinking it is connected to a drone?

The refined vision is stronger than simply “phone as drone.” The project should become a **low-cost drone systems simulator and ArduPilot/MAVLink teaching kit**. The goal is to help students understand the internal stack of a drone without requiring every learner to own a Pixhawk, drone frame, batteries, RC transmitter, telemetry radio, or GPS module.

The system should teach:

- Drone sensors and sensor fusion basics
- Roll, pitch, yaw, altitude, velocity, GPS, and heading
- MAVLink telemetry and command messages
- Mission Planner workflows
- ArduPilot SITL simulation
- Flight modes and autonomous missions
- PID control behavior
- Failure modes and failsafes
- 3D visualization of drone state and missions
- Eventually, deeper physics simulation and ArduPilot external simulator integration

The strategic decision is to **not rebuild everything from scratch**. Instead, we use the mature open-source ecosystem:

- **Mission Planner** as the ground control station
- **ArduPilot SITL** as the autopilot brain
- **MAVLink** as the communication protocol
- **pymavlink / MAVProxy** as the first bridge tooling
- **Three.js / Babylon.js** for classroom-friendly browser 3D visualization
- **Gazebo / Webots / JSBSim** later for advanced physics and robotics simulation
- A custom **phone app** as the interactive physical input/sensor device
- A custom **education dashboard** as the teaching layer

The key product insight:

> We are not competing with Mission Planner or ArduPilot. We are building the educational layer that helps students understand what Mission Planner and ArduPilot are doing.

---

## 2. Core Product Identity

### 2.1 Product Category

This should be positioned as a:

> **Drone systems education simulator**

Not merely a drone flying game.

Most drone simulators focus on flying skills, FPV racing, beautiful environments, or pilot stick control. This project focuses on how drones actually work internally.

### 2.2 Positioning Statement

> A low-cost drone systems simulator that teaches students how drones sense, think, communicate, stabilize, and navigate using a smartphone, ArduPilot, MAVLink, Mission Planner, and an interactive 3D environment.

### 2.3 Simple Description

> A virtual drone lab for learning sensors, MAVLink, PID control, flight modes, and autonomous missions without expensive hardware.

### 2.4 Possible Names

- MAVLab
- DroneLab
- PocketPilot Lab
- AeroSim Learn
- ArduLab
- FlightStack Academy
- PocketPilot

Recommended early name:

> **MAVLab**

Reason: It connects MAVLink, learning, and lab-based experimentation.

### 2.5 Tagline Options

- Learn drone systems through ArduPilot, MAVLink, and live simulation.
- A hands-on drone systems lab powered by ArduPilot and Mission Planner.
- Turn your phone into a virtual drone lab.
- Learn how drones sense, communicate, stabilize, and fly.

---

## 3. Target Users

The product is ideal for:

- Drone bootcamps
- Universities and colleges
- IEEE student branches
- Robotics clubs
- STEM clubs
- Drone startup training programs
- ArduPilot beginners
- MAVLink learners
- Drone operators in training
- Students in regions where drone hardware is expensive or hard to access

This is especially valuable in Kenya and across Africa because physical drone training hardware can be expensive, fragile, and legally constrained. The simulator lets students build intuition before touching real aircraft.

---

## 4. Final System Vision

The final system should simulate and teach the full drone stack:

```text
Student / Instructor
        ↓
Phone App + Web Education Dashboard
        ↓
MAVLink Bridge / Simulation Middleware
        ↓
ArduPilot SITL
        ↓
Mission Planner
        ↓
3D Visualizer + Telemetry + Lessons + Failure Labs
```

The phone app is not the whole drone. It is the physical learning interface. It can act as:

- A virtual drone body
- A sensor lab
- A controller
- A source of phone IMU/GPS data
- A failure/noise injection device
- A student interaction device

The simulator platform provides:

- Telemetry visualization
- MAVLink explanation
- 3D drone visualization
- Mission planning lessons
- PID teaching modules
- Failure simulations
- ArduPilot SITL integration
- Mission Planner workflow integration

---

## 5. High-Level Architecture

### 5.1 First Practical Architecture

```text
┌─────────────────────┐
│   Phone App          │
│ Sensors + controls   │
└─────────┬───────────┘
          │ WebSocket / UDP / Wi-Fi
          ↓
┌─────────────────────┐
│ Bridge / Backend     │
│ Converts phone input │
│ to MAVLink commands  │
└─────────┬───────────┘
          │ MAVLink
          ↓
┌─────────────────────┐
│ ArduPilot SITL       │
│ Autopilot brain      │
└─────────┬───────────┘
          │ MAVLink
          ↓
┌─────────────────────┐
│ Mission Planner      │
│ Ground station       │
└─────────────────────┘
```

### 5.2 Expanded Education Architecture

```text
                 ┌─────────────────────┐
                 │   Mission Planner    │
                 │ GCS + missions       │
                 └──────────┬──────────┘
                            │ MAVLink
                            ↓
                 ┌─────────────────────┐
                 │   ArduPilot SITL     │
                 │ real autopilot brain │
                 └──────────┬──────────┘
                            │ MAVLink / JSON later
                            ↓
┌────────────────────────────────────────────────────┐
│              Education Platform                    │
│                                                    │
│  ┌────────────────┐  ┌──────────────────────────┐  │
│  │ MAVLink Reader │  │ Lesson Engine             │  │
│  └────────────────┘  └──────────────────────────┘  │
│                                                    │
│  ┌────────────────┐  ┌──────────────────────────┐  │
│  │ 3D Visualizer  │  │ PID Explainer             │  │
│  └────────────────┘  └──────────────────────────┘  │
│                                                    │
│  ┌────────────────┐  ┌──────────────────────────┐  │
│  │ Phone Bridge   │  │ Failure Injection Lab     │  │
│  └────────────────┘  └──────────────────────────┘  │
└────────────────────────────────────────────────────┘
```

### 5.3 Final Advanced Architecture

```text
Phone App / Desktop App
        ↓
Drone Physics Engine / External Simulator
        ↓
Sensor Simulation Layer
        ↓
ArduPilot SITL / Flight Controller Layer
        ↓
MAVLink Communication Layer
        ↓
Mission Planner + 3D Education Dashboard
```

---

## 6. Key System Components

## 6.1 Mission Planner

Mission Planner should be the first ground control station because it supports ArduPilot natively and includes SITL simulation workflows. Mission Planner’s simulation tab provides SITL capability and allows users to test missions, frame types, joystick-style flying, and parameter changes without risking real vehicles.

Role in the project:

- Professional ground station interface
- Mission planning
- Parameter editing
- Flight mode interaction
- Telemetry display
- Logs and status messages
- Familiar real-world ArduPilot workflow

Student learning value:

- Students learn a real ground control station, not a fake classroom-only tool.
- Students understand how professional drone operators interact with an autopilot.
- Students can later transfer skills to real ArduPilot vehicles.

Reference: Mission Planner Simulation documentation  
https://ardupilot.org/planner/docs/mission-planner-simulation.html

---

## 6.2 ArduPilot SITL

ArduPilot SITL should be the autopilot brain of the first working system. SITL allows ArduPilot to run directly on a computer without special flight controller hardware.

Role in the project:

- Autopilot logic
- Flight modes
- Mission execution
- PID controllers
- Parameter system
- Arming logic
- MAVLink interface
- Failsafes
- Sensor estimation behavior

Why this matters:

- We avoid writing a full autopilot from scratch.
- Students learn with a real open-source autopilot stack.
- The simulator becomes aligned with real drone development workflows.

Reference: ArduPilot SITL documentation  
https://ardupilot.org/dev/docs/sitl-simulator-software-in-the-loop.html

---

## 6.3 MAVLink

MAVLink is the language between the simulated drone/autopilot and ground control station. It is a lightweight messaging protocol for drones and onboard drone components.

Role in the project:

- Communication between ArduPilot SITL and Mission Planner
- Communication between the custom bridge and ArduPilot
- Telemetry visualization
- Command transmission
- Teaching students how drone systems communicate

Important MAVLink messages for MVP:

- `HEARTBEAT`
- `ATTITUDE`
- `GLOBAL_POSITION_INT`
- `LOCAL_POSITION_NED`
- `GPS_RAW_INT`
- `VFR_HUD`
- `SYS_STATUS`
- `BATTERY_STATUS`
- `STATUSTEXT`

Important MAVLink commands/control paths:

- `MANUAL_CONTROL`
- `RC_CHANNELS_OVERRIDE`
- `COMMAND_LONG`
- `SET_MODE`
- Mission protocol messages later

Reference: MAVLink Developer Guide  
https://mavlink.io/en/

Reference: MAVLink Common Message Set  
https://mavlink.io/en/messages/common.html

---

## 6.4 Phone App

The phone app is the physical interaction layer. It should not initially replace the full drone physics simulator. Instead, it should act as a sensor/control interface that students can hold, tilt, shake, rotate, and move.

Role in MVP:

- Read phone IMU data
- Read GPS data
- Provide virtual RC/controller inputs
- Send data to backend over Wi-Fi
- Allow buttons for arm, disarm, takeoff, land, RTL
- Show live sensor values
- Later: simulate sensor failures/noise

Phone sensors to use:

- Accelerometer
- Gyroscope
- Magnetometer
- GPS / fused location
- Barometer if available
- Camera later

Phone controls:

- Tilt forward/backward → pitch input
- Tilt left/right → roll input
- Rotate phone → yaw input
- Touch slider → throttle
- Buttons → arm/disarm/takeoff/land/RTL

Recommended first role:

> Phone as controller/input device for ArduPilot SITL, not full sensor replacement.

Advanced later role:

> Phone as external sensor/failure source or virtual drone body through a simulation bridge.

---

## 6.5 Bridge / Backend

The bridge connects the phone app, ArduPilot SITL, Mission Planner, and the education dashboard.

Role in MVP:

- Receive phone data via WebSocket or UDP
- Convert phone tilt/touch data into MAVLink control commands
- Send commands to ArduPilot SITL
- Read telemetry from ArduPilot SITL
- Broadcast telemetry to dashboard
- Log telemetry for replay and lessons

Recommended first implementation:

- Python backend
- `pymavlink` for MAVLink communication
- FastAPI + WebSocket for dashboard/phone communication
- MAVProxy optionally for routing and inspection

Later implementation:

- Rust backend for performance and robust architecture
- `mavlink-rust` or similar Rust MAVLink crate

---

## 6.6 Education Dashboard

The education dashboard is the key product layer. Mission Planner shows telemetry, but the dashboard explains telemetry.

Role:

- Show MAVLink messages in human-friendly form
- Show roll, pitch, yaw, altitude, GPS, speed, battery
- Display the 3D drone model
- Display sensor graphs
- Guide students through lessons
- Show flight mode explanations
- Show mission execution steps
- Provide PID visualizers
- Provide failure-mode toggles
- Support instructor/classroom mode later

The dashboard is what turns the system from “a simulator” into “a teaching kit.”

---

## 6.7 3D Visualizer

The 3D visualizer should initially mirror ArduPilot SITL telemetry rather than simulate all physics itself.

MVP role:

- Receive roll, pitch, yaw, altitude, and position from MAVLink telemetry
- Move a 3D drone model in the browser
- Show path trail
- Show waypoints
- Show home point
- Show wind/failure indicators later

Recommended first tools:

- Three.js or Babylon.js
- Browser-based web app
- WebSocket telemetry feed from backend

Why browser first:

- Easy for bootcamps
- No heavy installation
- Works with Vercel/deployment workflows
- Students can open a link and start learning

Advanced tools later:

- Gazebo
- Webots
- Godot
- Unity
- Unreal Engine

---

## 6.8 Physics Engine / External Simulator

For the first version, do not build a full physics engine from scratch. Let ArduPilot SITL and its built-in models handle the initial simulation.

Later, the project can integrate external simulators:

- Gazebo
- Webots
- JSBSim
- Custom JSON physics backend

### Gazebo

Useful for advanced robotics simulation, indoor flight, multi-vehicle scenarios, rovers, boats, and detailed environments.

Reference: ArduPilot SITL with Gazebo  
https://ardupilot.org/dev/docs/sitl-with-gazebo.html

### Webots

Useful for robotics simulation and creating custom vehicles/worlds. ArduPilot supports Webots examples for multirotors and rovers.

Reference: ArduPilot SITL with Webots  
https://ardupilot.org/dev/docs/sitl-with-webots.html

### JSBSim

Most useful for fixed-wing flight dynamics and more aviation-style simulation.

Reference: ArduPilot SITL with JSBSim  
https://ardupilot.org/dev/docs/sitl-with-jsbsim.html

### JSON Interface

The ArduPilot JSON interface allows external physics simulators to exchange data with ArduPilot SITL over UDP. This is a strong path for building a custom educational physics backend later.

Reference: ArduPilot SITL with JSON  
https://ardupilot.org/dev/docs/sitl-with-JSON.html

---

## 7. Product Modes

The system should eventually support multiple learning modes.

---

## 7.1 Sensor Lab

Purpose:

> Teach students what drone sensors measure.

Features:

- Live accelerometer graph
- Live gyroscope graph
- Live magnetometer/heading display
- GPS position and accuracy
- Barometer altitude if available
- Roll, pitch, yaw visualization

Learning outcomes:

- Understand IMU basics
- Understand phone sensors vs drone sensors
- Understand sensor noise
- Understand why calibration matters

---

## 7.2 MAVLink Lab

Purpose:

> Teach students how drones communicate with ground stations.

Features:

- Live MAVLink message viewer
- Message explanation cards
- Telemetry stream rates
- Command viewer
- Mission Planner connection view

Messages to explain first:

- `HEARTBEAT`: system alive/present
- `ATTITUDE`: roll, pitch, yaw
- `GLOBAL_POSITION_INT`: global position estimate
- `GPS_RAW_INT`: raw GPS information
- `VFR_HUD`: altitude, speed, climb
- `SYS_STATUS`: system health
- `BATTERY_STATUS`: battery telemetry
- `STATUSTEXT`: autopilot messages/warnings

Learning outcomes:

- Understand telemetry
- Understand command vs telemetry messages
- Understand how ground stations receive vehicle state
- Understand why MAVLink is central to ArduPilot workflows

---

## 7.3 Mission Planner Lab

Purpose:

> Teach real ArduPilot ground control workflows.

Features:

- Start/connect to SITL
- Arm/disarm
- Change flight modes
- Create waypoint mission
- Upload mission
- Start mission
- RTL
- Review logs/status

Learning outcomes:

- Understand Mission Planner interface
- Understand mission planning
- Understand flight modes
- Understand parameter changes and effects

---

## 7.4 PID Lab

Purpose:

> Teach how control loops stabilize drones.

Beginner version:

- Simplified PID visualizer
- Target angle vs actual angle
- Error graph
- P/I/D contribution bars
- Oscillation visualization
- Sliders for P, I, D

Advanced version:

- Students edit real ArduPilot PID-related parameters in SITL
- Observe vehicle response in Mission Planner/dashboard

Teaching points:

- P too low → slow/lazy response
- P too high → oscillation
- I too low → persistent drift
- I too high → slow overcorrection/windup
- D too low → overshoot
- D too high → twitchy/noisy behavior

Relevant ArduPilot parameter examples to research further:

- `ATC_RAT_RLL_P`
- `ATC_RAT_RLL_I`
- `ATC_RAT_RLL_D`
- `ATC_RAT_PIT_P`
- `ATC_RAT_PIT_I`
- `ATC_RAT_PIT_D`
- `ATC_RAT_YAW_P`
- `ATC_RAT_YAW_I`
- `ATC_RAT_YAW_D`

Learning outcomes:

- Understand feedback control
- Understand stabilization
- Understand tuning tradeoffs
- Understand why drones oscillate or drift

---

## 7.5 3D Drone Lab

Purpose:

> Make drone state visible and intuitive.

Features:

- 3D drone model
- Propeller animation
- Real-time roll, pitch, yaw
- Altitude display
- Position/path trail
- Waypoints
- Home location
- GPS uncertainty circle
- Flight mode label

Learning outcomes:

- Understand drone attitude
- Understand movement through thrust vectoring
- Understand relationship between telemetry and visible motion
- Understand mission path execution

---

## 7.6 Flight Mode Lab

Purpose:

> Teach what autopilot modes do.

Modes to teach:

- Stabilize
- Alt Hold
- Loiter
- Guided
- Auto
- RTL
- Land

Learning outcomes:

- Understand manual vs assisted vs autonomous modes
- Understand what ArduPilot controls in each mode
- Understand mode transitions
- Understand safe operating behavior

---

## 7.7 Mission Lab

Purpose:

> Teach autonomous drone mission planning.

Features:

- Waypoint creation
- Takeoff command
- Mission upload
- Mission start
- Current waypoint tracking
- Distance to waypoint
- Return-to-launch
- Landing
- Geofence visualization later

Learning outcomes:

- Understand autonomous mission structure
- Understand waypoint execution
- Understand home location and RTL
- Understand pre-flight mission checks

---

## 7.8 Failure Lab

Purpose:

> Teach safety, diagnosis, and real-world drone behavior.

Failure scenarios:

- GPS loss
- GPS noise
- Compass interference
- Barometer drift
- Battery sag
- Low battery failsafe
- Telemetry link loss
- Wind gust
- Motor failure
- Payload overweight
- Geofence breach
- Vibration/noisy IMU

Learning outcomes:

- Understand why failsafes matter
- Understand how sensor failure affects flight
- Understand why pre-flight checks matter
- Understand risk before flying real drones

---

## 7.9 ArduPilot Advanced Lab

Purpose:

> Bridge beginner education to real autopilot development.

Features:

- ArduPilot SITL setup
- MAVProxy usage
- Parameter tuning
- Log analysis
- Custom vehicle models
- JSON interface experiments
- Gazebo/Webots integration

Learning outcomes:

- Understand real drone development workflow
- Understand SITL/HITL concepts
- Understand how external simulators integrate with autopilots

---

## 8. MVP Definition

The first working version should be simple, impressive, and educational.

### 8.1 MVP Name

**Phone-to-ArduPilot SITL Controller + MAVLink Teaching Dashboard**

### 8.2 MVP Goal

> Use a phone app to control or interact with an ArduPilot SITL drone, show the drone in Mission Planner, and display an educational dashboard explaining telemetry and drone behavior.

### 8.3 MVP Components

1. Mission Planner
2. ArduPilot SITL
3. Python bridge/backend
4. Phone app or web-mobile control interface
5. Web education dashboard
6. Simple 3D drone visualizer

### 8.4 MVP Features

Phone app:

- Connect over Wi-Fi
- Tilt controls for roll/pitch
- Yaw control
- Throttle slider
- Arm/disarm button
- Takeoff button
- Land button
- RTL button
- Show phone sensor values

Bridge/backend:

- Receive phone input
- Convert to MAVLink commands
- Connect to ArduPilot SITL
- Read MAVLink telemetry
- Broadcast telemetry to dashboard

Mission Planner:

- Connect to SITL
- Show vehicle movement
- Show map and telemetry
- Allow mission creation

Dashboard:

- Show attitude
- Show GPS/altitude/speed/battery
- Show MAVLink messages
- Show 3D drone model
- Show current flight mode
- Show beginner explanations

### 8.5 MVP Success Criteria

The MVP is successful when:

- Mission Planner connects to ArduPilot SITL
- Phone app connects to backend
- Phone controls affect SITL vehicle behavior
- Mission Planner shows vehicle response
- Dashboard displays live telemetry
- 3D drone mirrors SITL attitude
- Students can identify basic MAVLink messages
- Students can perform arm → takeoff → control → land/RTL workflow

---

## 9. Development Roadmap

## Version 0.1 — SITL + Mission Planner Baseline

Goal:

- Run ArduPilot SITL
- Connect Mission Planner
- Confirm stable simulation

Tasks:

- Install Mission Planner
- Set up ArduPilot SITL
- Run basic quadcopter simulation
- Test arming/takeoff/RTL
- Save known-good parameter set

Output:

- A repeatable classroom setup guide

---

## Version 0.2 — MAVLink Listener Dashboard

Goal:

- Read telemetry from ArduPilot SITL
- Display it in a custom dashboard

Tasks:

- Build Python backend using `pymavlink`
- Connect to SITL MAVLink port
- Parse `HEARTBEAT`, `ATTITUDE`, `GLOBAL_POSITION_INT`, `VFR_HUD`, `SYS_STATUS`
- Send telemetry to browser over WebSocket
- Display live telemetry table

Output:

- First MAVLink teaching dashboard

---

## Version 0.3 — 3D Drone Visualizer

Goal:

- Show a browser-based 3D drone model mirroring SITL attitude

Tasks:

- Create 3D drone model in Three.js/Babylon.js
- Map MAVLink roll/pitch/yaw to model rotation
- Display altitude and path trail
- Add simple camera controls

Output:

- Visual simulation companion to Mission Planner

---

## Version 0.4 — Phone Control Interface

Goal:

- Use phone as control input for SITL

Tasks:

- Build mobile web app or Android app
- Read device orientation
- Add throttle slider and buttons
- Send data to backend
- Convert phone input to `MANUAL_CONTROL` or `RC_CHANNELS_OVERRIDE`
- Send commands to ArduPilot SITL

Output:

- Phone-controlled virtual drone in SITL

---

## Version 0.5 — Lesson Engine

Goal:

- Add guided education content

Lessons:

- What is a drone autopilot?
- What is MAVLink?
- Roll, pitch, yaw
- What is telemetry?
- How Mission Planner connects to ArduPilot
- Arm, takeoff, land, RTL
- Basic mission planning

Output:

- First bootcamp-ready teaching kit

---

## Version 0.6 — PID Visualizer

Goal:

- Teach PID control through interactive visualization

Tasks:

- Build simple PID demo
- Add sliders for P/I/D
- Show target vs actual response
- Show oscillation and overshoot
- Connect concept to ArduPilot parameters

Output:

- Beginner-friendly control systems lab

---

## Version 0.7 — Failure Lab

Goal:

- Teach safety and fault behavior

Tasks:

- Add simulated telemetry delay
- Add battery drain simulation
- Add fake GPS noise in dashboard
- Add explanations of GPS/compass/battery failures
- Later integrate with SITL failure tools where possible

Output:

- Safety/failsafe education module

---

## Version 1.0 — Bootcamp Release

Goal:

- Stable release for Ascend × IEEE drone bootcamp

Included:

- Setup guide
- Mission Planner + SITL workflow
- Phone control
- MAVLink dashboard
- 3D visualizer
- Beginner lessons
- PID visualizer
- Basic mission lab
- Basic failure lab

Output:

- Complete low-cost drone systems teaching kit

---

## Version 2.0 — External Simulator Integration

Goal:

- Add deeper simulation realism

Options:

- Gazebo integration
- Webots integration
- ArduPilot JSON interface
- Custom educational physics backend

Output:

- Advanced drone systems simulator

---

## 10. Open-Source Components to Reuse

### 10.1 Core Autopilot

- ArduPilot
- ArduPilot SITL

### 10.2 Ground Control

- Mission Planner
- MAVProxy
- QGroundControl later if needed

### 10.3 MAVLink Tools

- pymavlink
- MAVProxy
- MAVLink common message definitions
- mavlink-router if needed later
- Rust MAVLink crates later

### 10.4 Simulation / Physics

- ArduPilot built-in SITL simulation
- Gazebo
- Webots
- JSBSim
- ArduPilot JSON interface

### 10.5 3D / Visualization

- Three.js
- Babylon.js
- Godot later
- Blender for drone models/assets

### 10.6 Backend / Dashboard

- Python
- FastAPI
- WebSockets
- SQLite/PostgreSQL for logs later
- Rust later for robust backend

### 10.7 Mobile App

- Kotlin Android
- Android SensorManager
- Fused Location Provider
- WebSocket/UDP networking
- Mobile web app as temporary prototype

---

## 11. Recommended Technology Stack

### Fastest Prototype Stack

```text
Mission Planner
ArduPilot SITL
Python + pymavlink
FastAPI + WebSockets
Three.js
Mobile browser interface first
Android Kotlin app later
```

### More Serious Later Stack

```text
Mission Planner
ArduPilot SITL
Rust backend
MAVLink Rust crate
Web dashboard
Three.js/Babylon.js
Android Kotlin app
Gazebo/Webots integration
```

### Why Python First

- Faster prototyping
- Better examples for MAVLink/ArduPilot
- Easier debugging
- Easier bootcamp experimentation

### Why Rust Later

- Performance
- Reliability
- Strong typing
- Good for long-term product architecture
- Aligns with existing high-performance engineering interests

---

## 12. Data Flow for MVP

### 12.1 Phone to ArduPilot Control Flow

```text
Phone orientation / touch input
        ↓
Phone app sends JSON over WebSocket/UDP
        ↓
Backend receives input
        ↓
Backend maps input to MAVLink command
        ↓
ArduPilot SITL receives command
        ↓
Vehicle state changes
        ↓
Mission Planner displays response
        ↓
Dashboard explains telemetry
```

### 12.2 Telemetry Flow

```text
ArduPilot SITL
        ↓ MAVLink telemetry
Backend MAVLink reader
        ↓
Telemetry parser
        ↓
Dashboard WebSocket
        ↓
Telemetry cards + graphs + 3D drone
```

### 12.3 Learning Flow

```text
Student action
        ↓
Drone response
        ↓
Telemetry change
        ↓
MAVLink message explanation
        ↓
Lesson interpretation
        ↓
Student understanding
```

---

## 13. Key Design Decision: Control First, Sensor Injection Later

There are two ways to use the phone:

### Option A — Phone as Controller/Input Device

This is the recommended MVP.

```text
Phone tilt/touch controls → MAVLink control commands → ArduPilot SITL
```

Benefits:

- Easier to implement
- Works with existing SITL simulation
- Less risk
- Useful for teaching
- Gets to demo quickly

### Option B — Phone as Sensor/External Simulator Source

This is advanced.

```text
Phone sensors / custom physics → ArduPilot JSON interface → ArduPilot SITL
```

Benefits:

- More realistic sensor-level experimentation
- Enables sensor failure/noise experiments
- More powerful research direction

Risks:

- Harder to implement
- Requires careful timing/reference frames
- Requires understanding ArduPilot SITL backends
- More debugging complexity

Recommended approach:

> Start with Option A. Add Option B after the MVP works.

---

## 14. Educational Curriculum Structure

This simulator can power a full drone systems bootcamp.

### Day 1 — Drone Systems Overview

Topics:

- What is a drone system?
- Autopilot vs ground station
- Sensors, controls, telemetry, power, mission planning
- ArduPilot and Mission Planner overview

Lab:

- Start Mission Planner SITL
- Observe simulated vehicle

---

### Day 2 — Sensors and Orientation

Topics:

- IMU
- Accelerometer
- Gyroscope
- Magnetometer
- GPS
- Roll, pitch, yaw

Lab:

- Phone sensor lab
- 3D drone attitude visualization

---

### Day 3 — MAVLink and Telemetry

Topics:

- What is MAVLink?
- Telemetry vs commands
- HEARTBEAT, ATTITUDE, GPS messages

Lab:

- MAVLink dashboard
- Inspect live messages from SITL

---

### Day 4 — Mission Planner and Flight Modes

Topics:

- Ground control station workflow
- Arming
- Takeoff
- Land
- RTL
- Loiter
- Auto missions

Lab:

- Mission Planner flight mode exercises

---

### Day 5 — PID and Stabilization

Topics:

- Feedback control
- Error
- P/I/D behavior
- Oscillation and damping

Lab:

- PID visualizer
- Optional ArduPilot parameter experiments

---

### Day 6 — Autonomous Missions

Topics:

- Waypoints
- Home point
- Mission upload
- Mission execution
- Geofencing overview

Lab:

- Create and run SITL mission

---

### Day 7 — Failure Modes and Safety

Topics:

- GPS failure
- Compass interference
- Battery failsafe
- Telemetry loss
- Wind and payload
- Why real drones require safety discipline

Lab:

- Failure Lab scenarios
- Real drone demo after simulator understanding

---

## 15. Final Feature List

Eventually, the platform can simulate/teach:

### Drone Frame Types

- Quad X
- Quad +
- Hexacopter
- Fixed-wing later
- VTOL later

### Sensors

- IMU
- GPS
- Magnetometer
- Barometer
- Rangefinder
- Optical flow
- Camera feed later

### Communication

- MAVLink
- UDP
- Serial simulation
- Telemetry link
- Packet loss
- Latency

### Control

- Rate control
- Attitude control
- Altitude control
- Position control
- PID loops

### Autonomy

- Waypoint missions
- Loiter
- RTL
- Land
- Takeoff
- Guided mode
- Geofence

### Environment

- Wind
- Terrain
- Obstacles
- GPS noise
- Magnetic interference
- Weather later

### Power

- Battery voltage
- Current draw
- Battery sag
- Payload effect
- Flight time estimate

### Safety

- Arming checks
- Failsafes
- Low battery RTL
- GPS failsafe
- Telemetry failsafe
- Geofence breach

### Visualization

- 3D drone model
- Sensor graphs
- MAVLink packet inspector
- Mission map
- Flight logs
- Replay mode

---

## 16. Research Questions

These are the key research questions to answer during development.

### ArduPilot / SITL

1. What is the easiest reliable way to launch ArduPilot SITL for classroom use?
2. Can Mission Planner launch SITL directly in the target teaching environment?
3. What ports should the backend use to read MAVLink without disrupting Mission Planner?
4. Should MAVProxy be used as the router between SITL, Mission Planner, and dashboard?
5. Which frame type should be the default: quad X, quad +, or another?

### MAVLink

1. Should phone input be sent as `MANUAL_CONTROL` or `RC_CHANNELS_OVERRIDE` first?
2. Which messages are necessary for Mission Planner compatibility?
3. Which messages are best for teaching beginners?
4. How should the dashboard explain MAVLink messages clearly?
5. How should command acknowledgments be displayed to students?

### Phone App

1. Should the first version be Android native or mobile web?
2. How stable is browser DeviceOrientation access across Android devices?
3. What sensor sampling rate is enough for education?
4. How should phone tilt map to drone control?
5. How should calibration/reset orientation work?

### 3D Visualization

1. Three.js or Babylon.js for the first version?
2. Should the 3D model mirror telemetry only or include local physics?
3. How should missions and waypoints appear in 3D?
4. How can the visualization remain simple for beginner students?

### PID

1. Should the first PID module be independent from ArduPilot?
2. Which ArduPilot PID parameters should be exposed for advanced learners?
3. How can we safely show bad tuning without confusing students?
4. How can we visualize overshoot, oscillation, damping, and steady-state error?

### Failure Modes

1. Which failures can be simulated directly through ArduPilot SITL?
2. Which failures should be simulated only in the dashboard first?
3. How can we teach failures without making setup unstable?
4. How should students diagnose failures using telemetry?

---

## 17. Risks and Mitigation

### Risk 1: Scope Explosion

The idea can become too large quickly.

Mitigation:

- Build in strict phases
- Start with Mission Planner + SITL + MAVLink dashboard
- Add phone input after baseline works
- Add physics/Gazebo later

### Risk 2: Trying to Replace ArduPilot

Writing a full autopilot from scratch would slow the project.

Mitigation:

- Use ArduPilot SITL as the autopilot brain
- Only build educational layers at first

### Risk 3: Phone Sensor Complexity

Phone sensors have noise, delays, calibration issues, and inconsistent browser/device support.

Mitigation:

- Start with phone as controller, not flight sensor source
- Add sensor lab separately
- Use Android native if browser sensors are unreliable

### Risk 4: MAVLink Routing Confusion

Multiple clients may need MAVLink telemetry.

Mitigation:

- Research MAVProxy or mavlink-router
- Clearly define SITL → Mission Planner → dashboard routing
- Use stable ports and setup scripts

### Risk 5: Student Setup Friction

If installation is too hard, the bootcamp experience suffers.

Mitigation:

- Provide prebuilt setup scripts
- Provide a known-good Windows Mission Planner guide
- Provide a Linux backend setup option
- Use browser dashboard where possible

---

## 18. Recommended First Build Sprint

### Sprint Goal

Build the smallest end-to-end proof that connects ArduPilot SITL, Mission Planner, and a custom dashboard.

### Sprint Tasks

1. Install/run Mission Planner SITL.
2. Confirm quadcopter simulation works.
3. Build Python MAVLink listener.
4. Parse `HEARTBEAT` and `ATTITUDE`.
5. Display telemetry in terminal.
6. Add FastAPI WebSocket server.
7. Create basic browser dashboard.
8. Show roll, pitch, yaw live.
9. Add simple 3D cube/drone that rotates with telemetry.
10. Document setup.

### Sprint Output

A working dashboard that shows live telemetry from ArduPilot SITL while Mission Planner is connected.

This is the foundation. Once this works, everything else becomes layered development.

---

## 19. MVP Build Order

Recommended order:

```text
1. Mission Planner + ArduPilot SITL baseline
2. MAVLink telemetry listener
3. Web dashboard
4. 3D drone visualizer
5. Phone web controller
6. Android native app
7. MAVLink teaching cards
8. Lesson mode
9. PID visualizer
10. Failure lab
11. Mission lab
12. Gazebo/Webots/JSON advanced integration
```

Do not start with full physics. Start with telemetry and education.

---

## 20. Example MVP User Flow

1. Instructor opens Mission Planner.
2. Instructor starts ArduPilot SITL simulation.
3. Student opens the MAVLab dashboard.
4. Dashboard connects to the backend.
5. Backend receives MAVLink telemetry.
6. Student sees `HEARTBEAT`, attitude, GPS, altitude, and flight mode.
7. Student opens phone controller.
8. Student tilts phone or uses controls.
9. Backend sends control commands to SITL.
10. Mission Planner shows vehicle response.
11. Dashboard explains what changed and why.
12. Student runs a simple mission.
13. Student observes telemetry, path, and 3D motion.

---

## 21. What Not to Build First

Avoid building these too early:

- Full custom autopilot
- Full custom physics engine
- Advanced 3D world
- Realistic sensor fusion engine
- Full phone-as-IMU replacement
- Multi-vehicle swarm simulation
- Full mobile app with polished UI before protocol works
- Complex login/classroom management

These are valuable later, but dangerous at the start.

---

## 22. Final System Description

The final working system should be:

> A low-cost drone education platform where students use Mission Planner and ArduPilot SITL as the real autopilot environment, while a custom phone app and web dashboard help them understand sensors, MAVLink telemetry, PID control, flight modes, mission planning, failures, and 3D drone behavior.

The system should make invisible drone concepts visible.

It should help students understand:

- What the drone senses
- What the autopilot estimates
- What the ground station displays
- What MAVLink messages carry
- How PID stabilizes motion
- How missions are executed
- How failures affect safety
- How real drone systems are built

The first product is not a replacement for Mission Planner.

It is the instructor layer beside Mission Planner.

---

## 23. Source References

1. ArduPilot Mission Planner Simulation  
   https://ardupilot.org/planner/docs/mission-planner-simulation.html

2. ArduPilot SITL Simulator Documentation  
   https://ardupilot.org/dev/docs/sitl-simulator-software-in-the-loop.html

3. ArduPilot SITL with JSON Interface  
   https://ardupilot.org/dev/docs/sitl-with-JSON.html

4. ArduPilot SITL with Gazebo  
   https://ardupilot.org/dev/docs/sitl-with-gazebo.html

5. ArduPilot SITL with Webots  
   https://ardupilot.org/dev/docs/sitl-with-webots.html

6. ArduPilot SITL with JSBSim  
   https://ardupilot.org/dev/docs/sitl-with-jsbsim.html

7. MAVLink Developer Guide  
   https://mavlink.io/en/

8. MAVLink Common Message Set  
   https://mavlink.io/en/messages/common.html

---

## 24. Immediate Next Step

The next practical step is to build a small proof-of-concept:

> **ArduPilot SITL → MAVLink listener → Web dashboard → 3D attitude visualizer**

Once this works, add:

> **Phone controller → backend → MAVLink control commands → ArduPilot SITL → Mission Planner response**

That gives the project its first complete learning loop.

---

## 25. One-Line North Star

> Build a virtual drone systems lab that lets any student understand how drones sense, communicate, stabilize, and fly before touching expensive hardware.
