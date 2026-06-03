# MAVLab — Production Research & Technical Blueprint

> Status: Superseded planning reference. The active roadmap is `mavlab_standalone_architecture_research.md` plus `phases/phase_0_project_skeleton.md` through `phases/phase_6_polish_release.md`. The current product direction is standalone/offline Android first, MIT licensed, with Phase 0 as scaffold-only and Phase 1 as the QGC/MAVLink protocol proof.

**Project:** Drone Systems Education Simulator  
**Target Platform:** Android (Primary), Backend Server, Cloud SITL  
**License Model:** Source-Available / Non-Commercial  
**Ground Control Station:** QGroundControl  
**Date:** June 2026  

---

## Table of Contents

1. [Licensing Strategy](#1-licensing-strategy)
2. [System Architecture Overview](#2-system-architecture-overview)
3. [QGroundControl Integration](#3-qgroundcontrol-integration)
4. [ArduPilot SITL — Setup, Deployment & Configuration](#4-ardupilot-sitl--setup-deployment--configuration)
5. [MAVLink Protocol Deep Dive](#5-mavlink-protocol-deep-dive)
6. [Android App Architecture](#6-android-app-architecture)
7. [Android Sensor Integration](#7-android-sensor-integration)
8. [MAVLink on Android — Library Selection](#8-mavlink-on-android--library-selection)
9. [3D Drone Visualization on Android](#9-3d-drone-visualization-on-android)
10. [Telemetry Charts & Data Visualization](#10-telemetry-charts--data-visualization)
11. [Backend Architecture — Python Bridge Server](#11-backend-architecture--python-bridge-server)
12. [Network & Communication Architecture](#12-network--communication-architecture)
13. [Deployment Strategy — SITL in the Cloud](#13-deployment-strategy--sitl-in-the-cloud)
14. [Competitor & Market Analysis](#14-competitor--market-analysis)
15. [Production Engineering — CI/CD, Testing, Quality](#15-production-engineering--cicd-testing-quality)
16. [3D Asset Pipeline](#16-3d-asset-pipeline)
17. [ArduPilot SITL Failure Injection](#17-ardupilot-sitl-failure-injection)
18. [Technology Stack Summary](#18-technology-stack-summary)
19. [Module Dependency Map](#19-module-dependency-map)
20. [Phased Build Plan](#20-phased-build-plan)
21. [Open Questions & Decisions](#21-open-questions--decisions)
22. [References](#22-references)

---

## 1. Licensing Strategy

### 1.1 The Goal

You want:
- Source code publicly visible on GitHub
- Others can study, fork, modify, and contribute
- Others **cannot** sell or commercialize the software
- The project should feel "open source" to the community

### 1.2 Critical Legal Distinction

The term "open source" has a strict legal definition from the Open Source Initiative (OSI). An OSI-approved license **cannot** restrict commercial use. If you want to prevent commercialization, the project is technically **"source-available"**, not "open source."

This is not a problem — many successful projects use this model (MongoDB, Redis, Sentry, Hashicorp Terraform pre-IBM). The key is choosing the right license and communicating clearly.

### 1.3 License Options Compared

| License | Source Visible | Modifications Shared | Commercial Use | OSI Approved | Best For |
|---------|---------------|---------------------|----------------|-------------|----------|
| **MIT** | ✅ | ❌ | ✅ | ✅ | Maximum adoption, no protection |
| **Apache 2.0** | ✅ | ❌ | ✅ | ✅ | Same as MIT + patent protection |
| **GPL-3.0** | ✅ | ✅ (copyleft) | ✅ | ✅ | Force sharing modifications |
| **AGPL-3.0** | ✅ | ✅ (network copyleft) | ✅ | ✅ | Force sharing even SaaS modifications |
| **CC BY-NC 4.0** | ✅ | ✅ | ❌ | ❌ | Creative works, not software |
| **BSL 1.1** | ✅ | ✅ | ❌ (time-limited) | ❌ | Source-available → auto-converts to OSS after N years |
| **Custom Non-Commercial** | ✅ | ✅ | ❌ | ❌ | Full control, but no legal precedent |

### 1.4 Recommended License: BSL 1.1 (Business Source License)

**Why BSL 1.1 is the best fit for MAVLab:**

1. **Source code is fully public** — anyone can read, study, modify, fork
2. **Non-production restriction** — others cannot deploy it commercially without permission
3. **"Additional Use Grant"** — you can explicitly allow: educational use, personal use, research use, non-profit use
4. **Springing license** — after a Change Date (e.g., 4 years), the code automatically converts to a true open source license (e.g., Apache 2.0 or GPL-3.0)
5. **Battle-tested** — used by MariaDB, CockroachDB, Sentry, HashiCorp, and many others
6. **Legally clear** — unlike AGPL + Commons Clause (which has legal conflicts), BSL 1.1 is a standalone license designed for exactly this purpose

**Recommended BSL 1.1 configuration for MAVLab:**

```
License: Business Source License 1.1
Licensor: Ascend / Ambrose [your entity name]
Change Date: 4 years from each version's release
Change License: Apache-2.0

Additional Use Grant:
  You may use the Licensed Work for:
  - Personal, non-commercial education
  - Academic research at accredited institutions
  - Non-profit educational programs
  - Contributing to the Licensed Work itself
```

### 1.5 Alternative: AGPL-3.0 (If You Want True Open Source)

If you decide you *can* tolerate commercial use as long as all modifications are shared back:

- AGPL-3.0 forces anyone who runs the software (even as a service) to share their source code
- This effectively deters most commercial exploitation because companies don't want to open-source their modifications
- ArduPilot itself uses GPL-3.0, so AGPL-3.0 is compatible
- QGroundControl uses a dual LGPL-3.0/Apache-2.0 license

**Verdict:** If "prevent commercial use" is a hard requirement → **BSL 1.1**. If "make commercialization unattractive" is enough → **AGPL-3.0**.

### 1.6 License Compatibility Matrix

| Component | License | Compatible with BSL 1.1? | Compatible with AGPL-3.0? |
|-----------|---------|--------------------------|---------------------------|
| ArduPilot | GPL-3.0 | ✅ (we don't redistribute ArduPilot, we connect via MAVLink) | ✅ |
| QGroundControl | LGPL-3.0 / Apache-2.0 | ✅ (we don't modify QGC) | ✅ |
| pymavlink | LGPL-3.0 | ✅ | ✅ |
| mavlink-kotlin | Apache-2.0 | ✅ | ✅ |
| Three.js / SceneView | MIT | ✅ | ✅ |
| FastAPI | MIT | ✅ | ✅ |
| OkHttp | Apache-2.0 | ✅ | ✅ |

All dependencies are license-compatible. MAVLab connects to ArduPilot SITL over MAVLink (network protocol), so we are not distributing ArduPilot code. No license conflicts.

---

## 2. System Architecture Overview

### 2.1 Production Architecture

```text
┌──────────────────────────────────────────────────────────────────┐
│                        CLOUD / SERVER                            │
│                                                                  │
│  ┌─────────────────────┐    ┌──────────────────────────────────┐ │
│  │  ArduPilot SITL      │    │  MAVLab Bridge Server            │ │
│  │  (Docker Container)  │    │  (Python / FastAPI)              │ │
│  │                      │◄──►│                                  │ │
│  │  • Autopilot brain   │    │  • MAVLink ↔ WebSocket bridge   │ │
│  │  • Flight modes      │MAV │  • Telemetry parser              │ │
│  │  • PID controllers   │Link│  • Phone input → MAVLink cmds   │ │
│  │  • Sensor simulation │UDP │  • Connection manager            │ │
│  │  • Failsafes         │    │  • Session/classroom management  │ │
│  └─────────────────────┘    └──────────┬───────────────────────┘ │
│                                         │ WebSocket (wss://)     │
└─────────────────────────────────────────┼────────────────────────┘
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    │                     │                     │
                    ▼                     ▼                     ▼
    ┌───────────────────┐  ┌───────────────────┐  ┌──────────────────┐
    │  Android App       │  │  QGroundControl    │  │  Web Dashboard   │
    │  (MAVLab)          │  │  (Optional GCS)    │  │  (Future)        │
    │                    │  │                    │  │                  │
    │  • 3D Drone Viz    │  │  • Map view        │  │  • Instructor    │
    │  • Sensor Lab      │  │  • Parameter edit  │  │    control panel │
    │  • MAVLink Explorer│  │  • Mission plan    │  │  • Classroom     │
    │  • Phone Controls  │  │  • Log analysis    │  │    management    │
    │  • PID Visualizer  │  │                    │  │                  │
    │  • Failure Lab     │  │                    │  │                  │
    │  • Lesson Engine   │  │                    │  │                  │
    └───────────────────┘  └───────────────────┘  └──────────────────┘
```

### 2.2 Deployment Topologies

**Topology A — Local Development (Single Machine)**

```text
Laptop running:
  ├── ArduPilot SITL (sim_vehicle.py)
  ├── MAVLab Bridge (python server, localhost)
  ├── QGroundControl (connects to localhost:14550)
  └── Android app (connects to laptop IP over Wi-Fi)
```

**Topology B — Cloud Classroom (Production)**

```text
Cloud Server (DigitalOcean/AWS):
  ├── ArduPilot SITL (Docker, headless)
  └── MAVLab Bridge (Docker, public WebSocket endpoint)

Student devices (anywhere):
  ├── Android app → connects to cloud WebSocket
  └── QGroundControl (optional) → connects to cloud UDP
```

**Topology C — Bootcamp (Local Network)**

```text
Instructor Laptop:
  ├── ArduPilot SITL
  ├── MAVLab Bridge (LAN accessible)
  └── QGroundControl

Student Phones (same Wi-Fi):
  └── Android app → connects to instructor's LAN IP
```

### 2.3 Communication Protocol Stack

```text
Layer           Protocol        Purpose
─────────────────────────────────────────────────
Application     MAVLink 2.0     Drone ↔ GCS communication
Transport       UDP             SITL ↔ Bridge, SITL ↔ QGC
Application     WebSocket       Bridge ↔ Android App
                (JSON/Binary)
Transport       TCP (wss://)    Android ↔ Bridge server
Application     HTTP/REST       Bridge API (health, config)
Transport       TCP (https://)  Management endpoints
```

---

## 3. QGroundControl Integration

### 3.1 Why QGroundControl Over Mission Planner

| Factor | Mission Planner | QGroundControl |
|--------|----------------|----------------|
| **Platforms** | Windows only | Windows, macOS, Linux, Android, iOS |
| **License** | GPL-3.0 | LGPL-3.0 / Apache-2.0 |
| **Mobile** | ❌ | ✅ Native Android/iOS apps |
| **Framework** | .NET / WinForms | Qt / QML |
| **ArduPilot Support** | Full (primary GCS) | Full (via FirmwarePlugin) |
| **PX4 Support** | Limited | Full |
| **Classroom Use** | Requires Windows | Any device |
| **API/Extensibility** | Limited | Plugin architecture |

**Decision: QGroundControl** is the correct choice for a cross-platform, Android-first project.

### 3.2 QGroundControl Connection to SITL

QGroundControl auto-detects vehicles broadcasting MAVLink on UDP port `14550`.

**Setup steps:**
1. Start ArduPilot SITL: `sim_vehicle.py -v ArduCopter --out=udp:<QGC_IP>:14550`
2. Open QGroundControl — it will auto-connect
3. If manual: Settings → Comm Links → Add → UDP → Port 14550

**Key architectural details of QGC:**
- `LinkManager` — manages all MAVLink connections (UDP/TCP/Serial)
- `MAVLinkProtocol` — serializes/deserializes MAVLink messages
- `MultiVehicleManager` — creates `Vehicle` objects on heartbeat detection
- `FirmwarePlugin` — handles ArduPilot vs PX4 differences
- `AutoPilotPlugin` — parameter management per vehicle type

### 3.3 MAVLab's Relationship to QGroundControl

MAVLab **does not replace** QGroundControl. They coexist:

```text
ArduPilot SITL
    │
    ├── UDP:14550 → QGroundControl (professional GCS view)
    │
    └── UDP:14551 → MAVLab Bridge → WebSocket → Android App (education layer)
```

Students learn with both:
- QGC teaches real-world GCS workflows
- MAVLab explains *what* QGC is showing and *why*

### 3.4 QGroundControl Android App

QGC is available on Android via Google Play. Students can install it on the same phone or a tablet alongside MAVLab. This means:

- No need to rebuild GCS features in MAVLab
- MAVLab focuses purely on education
- QGC handles mission planning, parameter editing, map view
- MAVLab handles explanation, visualization, sensor labs, PID teaching

---

## 4. ArduPilot SITL — Setup, Deployment & Configuration

### 4.1 What SITL Provides

ArduPilot SITL runs the full autopilot code on a PC instead of a flight controller. It simulates:

- Complete autopilot logic (same code that runs on Pixhawk)
- All flight modes (Stabilize, Alt Hold, Loiter, Guided, Auto, RTL, Land)
- PID controllers
- Arming checks
- Failsafes (battery, GPS, geofence)
- Sensor estimation (EKF)
- MAVLink communication
- Parameter system (1000+ parameters)
- Mission execution

### 4.2 SITL Launch Options

**Basic launch:**
```bash
cd /path/to/ardupilot
./Tools/autotest/sim_vehicle.py -v ArduCopter --console --map
```

**Headless (no GUI — for servers/Docker):**
```bash
sim_vehicle.py -v ArduCopter --no-mavproxy --out=udp:0.0.0.0:14550
```

**With multiple outputs (for both QGC and MAVLab Bridge):**
```bash
sim_vehicle.py -v ArduCopter \
    --out=udp:127.0.0.1:14550 \
    --out=udp:127.0.0.1:14551 \
    --no-mavproxy
```

**Key sim_vehicle.py flags:**
| Flag | Purpose |
|------|---------|
| `-v ArduCopter` | Vehicle type (ArduCopter, ArduPlane, etc.) |
| `-f quad` | Frame type (quad, hexa, octa, etc.) |
| `--out=udp:<IP>:<PORT>` | Add MAVLink output destination |
| `--no-mavproxy` | Headless mode (no console/map) |
| `-w` | Wipe EEPROM (reset all parameters) |
| `-I <N>` | Instance number (for multi-vehicle) |
| `--speedup=<N>` | Simulation speed multiplier |
| `--model=JSON:<IP>` | Use external physics via JSON interface |

### 4.3 SITL Default Ports

```text
Port     Direction    Purpose
──────────────────────────────────────────
5760     TCP          MAVLink (primary, MAVProxy uses this)
14550    UDP          GCS connection (QGroundControl)
14551    UDP          Secondary GCS / API connection
5501     UDP          RC input (RC override simulation)
9002     UDP          JSON external simulator interface
```

### 4.4 Docker Deployment

**Dockerfile for ArduPilot SITL:**

```dockerfile
FROM ubuntu:22.04

RUN apt-get update && apt-get install -y \
    git python3 python3-pip python3-dev \
    build-essential ccache g++ gawk \
    libxml2-dev libxslt1-dev python3-lxml \
    python3-matplotlib python3-serial \
    && rm -rf /var/lib/apt/lists/*

RUN git clone --recurse-submodules https://github.com/ArduPilot/ardupilot.git /ardupilot
WORKDIR /ardupilot

RUN Tools/environment_install/install-prereqs-ubuntu.sh -y
RUN ./waf configure --board sitl
RUN ./waf copter

EXPOSE 14550/udp 14551/udp 5760/tcp

CMD ["Tools/autotest/sim_vehicle.py", \
     "-v", "ArduCopter", \
     "--no-mavproxy", \
     "--out=udp:0.0.0.0:14550", \
     "--out=udp:0.0.0.0:14551"]
```

**Docker Compose (SITL + Bridge):**

```yaml
version: '3.8'
services:
  sitl:
    build: ./sitl
    ports:
      - "14550:14550/udp"
      - "14551:14551/udp"
      - "5760:5760/tcp"
    restart: unless-stopped

  bridge:
    build: ./bridge
    ports:
      - "8000:8000"
    depends_on:
      - sitl
    environment:
      - SITL_HOST=sitl
      - SITL_PORT=14551
      - WS_PORT=8000
    restart: unless-stopped
```

### 4.5 SITL JSON Interface (Advanced — For Custom Physics Later)

ArduPilot's JSON interface allows an external simulator to provide physics data to SITL over UDP.

**Protocol:**
- ArduPilot sends → binary packet with PWM servo outputs (16 or 32 channels)
- External sim sends → JSON string with vehicle state

**JSON state fields:**
```json
{
  "timestamp": 1234567890.123,
  "imu": {
    "gyro": [0.0, 0.0, 0.0],
    "accel_body": [0.0, 0.0, -9.81]
  },
  "position": [0.0, 0.0, 0.0],
  "velocity": [0.0, 0.0, 0.0],
  "attitude": [1.0, 0.0, 0.0, 0.0]
}
```

**Position:** NED (North, East, Down) in meters  
**Velocity:** NED in m/s  
**Attitude:** Quaternion [w, x, y, z]  
**Gyro:** Body frame rates in rad/s  
**Accel:** Body frame acceleration in m/s²  

**Launch with JSON backend:**
```bash
sim_vehicle.py -v ArduCopter -f JSON:127.0.0.1
```
SITL sends PWM to port 9002, expects JSON response from the physics backend.

**Relevance to MAVLab:** This is the path for Version 2.0+ when you want to build a custom educational physics engine or connect phone sensor data directly to ArduPilot.

---

## 5. MAVLink Protocol Deep Dive

### 5.1 Protocol Basics

MAVLink (Micro Air Vehicle Link) is a lightweight binary protocol for drone communication.

- **Version:** MAVLink 2.0 (current standard)
- **Encoding:** Binary (not JSON/XML)
- **Transport:** Typically UDP, also TCP or Serial
- **Message size:** 8-280 bytes (very compact)
- **System ID:** Identifies each vehicle (1-255)
- **Component ID:** Identifies components within a vehicle (1-255)

### 5.2 Messages Required for MAVLab MVP

**Telemetry Messages (SITL → MAVLab):**

| Message ID | Name | Key Fields | Education Value |
|-----------|------|------------|-----------------|
| 0 | `HEARTBEAT` | type, autopilot, base_mode, custom_mode, system_status | "Is the drone alive? What mode is it in?" |
| 30 | `ATTITUDE` | roll, pitch, yaw, rollspeed, pitchspeed, yawspeed | "How is the drone oriented in space?" |
| 33 | `GLOBAL_POSITION_INT` | lat, lon, alt, relative_alt, vx, vy, vz | "Where is the drone on the map?" |
| 24 | `GPS_RAW_INT` | fix_type, lat, lon, alt, eph, epv, satellites_visible | "How good is the GPS signal?" |
| 74 | `VFR_HUD` | airspeed, groundspeed, heading, throttle, alt, climb | "Dashboard instruments" |
| 1 | `SYS_STATUS` | onboard_control_sensors_health, voltage_battery, current_battery, battery_remaining | "System health overview" |
| 147 | `BATTERY_STATUS` | voltages, current_battery, energy_consumed, battery_remaining | "Battery state details" |
| 253 | `STATUSTEXT` | severity, text | "Autopilot text messages and warnings" |

**Command Messages (MAVLab → SITL):**

| Message ID | Name | Purpose |
|-----------|------|---------|
| 69 | `MANUAL_CONTROL` | Send joystick-like inputs (x, y, z, r, buttons) |
| 70 | `RC_CHANNELS_OVERRIDE` | Override RC channels directly |
| 76 | `COMMAND_LONG` | Send commands (arm, disarm, takeoff, etc.) |
| 11 | `SET_MODE` | Change flight mode |

### 5.3 Key MAVLink Commands via COMMAND_LONG

| Command | MAV_CMD ID | Parameters | Description |
|---------|-----------|------------|-------------|
| Arm | `MAV_CMD_COMPONENT_ARM_DISARM` (400) | param1=1 | Arm motors |
| Disarm | `MAV_CMD_COMPONENT_ARM_DISARM` (400) | param1=0 | Disarm motors |
| Takeoff | `MAV_CMD_NAV_TAKEOFF` (22) | param7=altitude | Takeoff to altitude |
| Land | `MAV_CMD_NAV_LAND` (21) | — | Land at current position |
| RTL | Set mode to RTL | — | Return to launch |
| Set Message Rate | `MAV_CMD_SET_MESSAGE_INTERVAL` (511) | param1=msg_id, param2=interval_us | Request telemetry at specific rate |

### 5.4 MANUAL_CONTROL Message (Primary Phone Control Path)

The `MANUAL_CONTROL` message is the cleanest way to send phone tilt/touch data to ArduPilot:

```text
Field    Range        Mapping
──────────────────────────────────────────
x        -1000..1000  Pitch (forward/backward)
y        -1000..1000  Roll (left/right)
z        0..1000      Throttle (up/down)
r        -1000..1000  Yaw (rotation)
buttons  uint16       Button bitmask
target   uint8        Target system ID
```

**Phone mapping:**
- Phone tilt forward → positive x (pitch forward)
- Phone tilt left → negative y (roll left)
- Touch slider → z (throttle)
- Phone rotation → r (yaw)

---

## 6. Android App Architecture

### 6.1 Architecture Pattern

The app will follow **Clean Architecture + MVVM** with Jetpack Compose, which is the production standard for Android in 2025/2026.

```text
┌─────────────────────────────────────────────────────┐
│                    Presentation Layer                │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────┐ │
│  │  Compose UI  │  │  ViewModels  │  │  Navigation│ │
│  └─────────────┘  └──────────────┘  └────────────┘ │
├─────────────────────────────────────────────────────┤
│                      Domain Layer                    │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │  Use Cases    │  │  Entities     │  │ Repository│ │
│  │              │  │              │  │ Interfaces│ │
│  └──────────────┘  └──────────────┘  └───────────┘ │
├─────────────────────────────────────────────────────┤
│                      Data Layer                      │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │  WebSocket    │  │  Sensor      │  │ MAVLink   │ │
│  │  Client       │  │  Repository  │  │ Parser    │ │
│  └──────────────┘  └──────────────┘  └───────────┘ │
└─────────────────────────────────────────────────────┘
```

### 6.2 Module Structure

```text
mavlab-android/
├── app/                          # Application module (entry point)
├── core/
│   ├── ui/                       # Design system, theme, shared components
│   ├── network/                  # WebSocket client, OkHttp config
│   ├── mavlink/                  # MAVLink message parsing & types
│   ├── sensors/                  # Phone sensor abstraction
│   └── common/                   # Shared utilities, extensions
├── domain/                       # Business logic, use cases, entities
│   ├── model/                    # Domain models (DroneState, Attitude, etc.)
│   ├── repository/               # Repository interfaces
│   └── usecase/                  # Use cases (GetTelemetry, SendControl, etc.)
├── data/                         # Data layer implementations
│   ├── websocket/                # WebSocket repository implementation
│   ├── sensor/                   # Sensor repository implementation
│   └── mavlink/                  # MAVLink data source
├── feature/
│   ├── dashboard/                # Main dashboard screen
│   ├── controller/               # Phone tilt controller
│   ├── sensor-lab/               # Phone sensor visualization
│   ├── mavlink-explorer/         # MAVLink message inspector
│   ├── drone-3d/                 # 3D drone visualization
│   ├── pid-lab/                  # PID control teaching module
│   ├── flight-modes/             # Flight mode explainer
│   ├── mission-lab/              # Mission planning education
│   ├── failure-lab/              # Failure injection & diagnosis
│   └── lessons/                  # Guided lesson engine
└── build-logic/                  # Gradle convention plugins
```

### 6.3 Key Dependencies

```kotlin
// build.gradle.kts (version catalog)
[versions]
kotlin = "2.1.0"
compose-bom = "2025.05.00"
hilt = "2.51"
okhttp = "4.12.0"
sceneview = "4.16.10"
vico = "2.1.0"
mavlink-kotlin = "2.12.0"
coroutines = "1.9.0"
navigation = "2.8.0"

[libraries]
// Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-animation = { group = "androidx.compose.animation", name = "animation" }

// Architecture
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }

// Networking
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }

// 3D Rendering
sceneview = { group = "io.github.sceneview", name = "sceneview", version.ref = "sceneview" }

// Charts
vico-compose = { group = "com.patrykandpatrick.vico", name = "compose-m3", version.ref = "vico" }

// MAVLink
mavlink-definitions = { group = "com.divpundir.mavlink", name = "definitions", version.ref = "mavlink-kotlin" }
mavlink-connection = { group = "com.divpundir.mavlink", name = "connection-core", version.ref = "mavlink-kotlin" }
mavlink-coroutines = { group = "com.divpundir.mavlink", name = "adapter-coroutines", version.ref = "mavlink-kotlin" }
```

### 6.4 App-Level Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| UI Framework | Jetpack Compose | Modern declarative UI, lifecycle-aware |
| DI Framework | Hilt | Compile-time safety, Android-first |
| Navigation | Compose Navigation | Single-activity, type-safe routes |
| State Management | StateFlow + UDF | Predictable, testable |
| Concurrency | Kotlin Coroutines + Flow | Native Kotlin, structured concurrency |
| Networking | OkHttp WebSocket | Mature, reliable, ping/pong support |
| MAVLink | mavlink-kotlin | Kotlin-native, type-safe, multiplatform |
| 3D Rendering | SceneView (Filament) | Compose-native, PBR, GLB support |
| Charts | Vico | Compose-native, real-time capable |
| Min SDK | API 26 (Android 8.0) | Covers 95%+ of active devices |
| Target SDK | API 35 (Android 15) | Latest platform features |

---

## 7. Android Sensor Integration

### 7.1 Sensors Available

| Sensor | Android API | Drone Equivalent | Use in MAVLab |
|--------|------------|------------------|---------------|
| Accelerometer | `Sensor.TYPE_ACCELEROMETER` | IMU accelerometer | Tilt detection, sensor lab |
| Gyroscope | `Sensor.TYPE_GYROSCOPE` | IMU gyroscope | Rotation rate, sensor lab |
| Magnetometer | `Sensor.TYPE_MAGNETIC_FIELD` | Compass | Heading, sensor lab |
| GPS | `FusedLocationProvider` | GPS module | Position, sensor lab |
| Barometer | `Sensor.TYPE_PRESSURE` | Barometric altimeter | Altitude estimate, sensor lab |
| Rotation Vector | `Sensor.TYPE_ROTATION_VECTOR` | Fused orientation | Clean orientation for controller |
| Game Rotation Vector | `Sensor.TYPE_GAME_ROTATION_VECTOR` | Relative orientation | Controller (no magnetic interference) |

### 7.2 Production Architecture for Sensors

```kotlin
// SensorRepository interface (domain layer)
interface SensorRepository {
    fun getAccelerometerFlow(): Flow<AccelerometerData>
    fun getGyroscopeFlow(): Flow<GyroscopeData>
    fun getOrientationFlow(): Flow<OrientationData>
    fun getLocationFlow(): Flow<LocationData>
    fun getBarometerFlow(): Flow<BarometerData>
}

// Implementation using callbackFlow (data layer)
class AndroidSensorRepository @Inject constructor(
    private val sensorManager: SensorManager
) : SensorRepository {

    override fun getOrientationFlow(): Flow<OrientationData> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: throw SensorNotAvailableException("Game Rotation Vector not available")

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)

                trySend(OrientationData(
                    azimuth = orientation[0],  // yaw
                    pitch = orientation[1],     // pitch
                    roll = orientation[2],      // roll
                    timestamp = event.timestamp
                ))
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
```

### 7.3 Sensor Sampling Rates

| Rate Constant | Approximate Rate | Use Case |
|--------------|-----------------|----------|
| `SENSOR_DELAY_FASTEST` | ~200-500 Hz | Never use (battery killer) |
| `SENSOR_DELAY_GAME` | ~50 Hz | Controller mode ✅ |
| `SENSOR_DELAY_UI` | ~15 Hz | Sensor lab display ✅ |
| `SENSOR_DELAY_NORMAL` | ~5 Hz | Background monitoring |

**Recommendation:** Use `SENSOR_DELAY_GAME` for the controller, `SENSOR_DELAY_UI` for sensor lab visualization.

### 7.4 Phone-to-Drone Control Mapping

```text
Phone Orientation              → MAVLink MANUAL_CONTROL
────────────────────────────────────────────────────────
Pitch (tilt forward/back)      → x  (-1000 to 1000)
Roll  (tilt left/right)        → y  (-1000 to 1000)
Yaw   (rotate clockwise/ccw)   → r  (-1000 to 1000)
Throttle slider (touch)        → z  (0 to 1000)

Calibration:
- On "Calibrate" button press, record current orientation as zero reference
- All subsequent readings are relative to calibrated zero
- Apply deadzone (±50) to prevent drift from noise
- Apply exponential curve for finer control near center
```

### 7.5 Google Fused Orientation Provider (FOP)

For production quality, consider using Google's Fused Orientation Provider API from Play Services:

- Automatically fuses accelerometer, gyroscope, and magnetometer
- Handles manufacturer-specific sensor quirks
- Compensates for magnetic declination
- More stable than manual sensor fusion

---

## 8. MAVLink on Android — Library Selection

### 8.1 Options Compared

| Library | Language | Level | KMP | Maintained | Best For |
|---------|----------|-------|-----|------------|----------|
| **mavlink-kotlin** | Kotlin | Low (raw messages) | ✅ | ✅ Active | Custom protocols, type-safe |
| **MAVSDK-Java** | Java/Kotlin | High (abstracted) | ❌ | ✅ Active | Quick integration, standard actions |
| **Custom UDP parser** | Kotlin | Lowest | N/A | N/A | Maximum control, learning |

### 8.2 Recommended: mavlink-kotlin

**Why mavlink-kotlin is the right choice for MAVLab:**

1. **Kotlin-native** — idiomatic Kotlin, works with Coroutines and Flow
2. **Type-safe** — every MAVLink message is a generated Kotlin class
3. **Kotlin Multiplatform** — can share MAVLink code with a future iOS app or backend
4. **No gRPC overhead** — MAVSDK requires a gRPC server; mavlink-kotlin works directly
5. **Education value** — students can see actual MAVLink messages, not abstracted API calls
6. **Active maintenance** — regularly updated by divyanshupundir

### 8.3 mavlink-kotlin Setup

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.divpundir.mavlink:definitions:2.12.0")
    implementation("com.divpundir.mavlink:connection-core:2.12.0")
    implementation("com.divpundir.mavlink:adapter-coroutines:2.12.0")
}
```

### 8.4 mavlink-kotlin Usage Pattern

```kotlin
// MAVLink service wrapping mavlink-kotlin
class MavlinkService @Inject constructor() {

    private var adapter: CoroutinesMavConnection? = null

    suspend fun connect(host: String, port: Int) {
        val connection = UdpMavConnection(host, port)
        adapter = connection.asCoroutine()
        adapter?.connect()
    }

    fun getTelemetryFlow(): Flow<TelemetryState> = adapter?.message
        ?.filterIsInstance<MavMessage<*>>()
        ?.map { message ->
            when (message.payload) {
                is Attitude -> TelemetryUpdate.AttitudeUpdate(message.payload as Attitude)
                is GlobalPositionInt -> TelemetryUpdate.PositionUpdate(...)
                is VfrHud -> TelemetryUpdate.HudUpdate(...)
                is Heartbeat -> TelemetryUpdate.HeartbeatUpdate(...)
                else -> null
            }
        }
        ?.filterNotNull() ?: emptyFlow()

    suspend fun sendManualControl(x: Int, y: Int, z: Int, r: Int) {
        adapter?.send(
            systemId = 255u,
            componentId = 0u,
            message = ManualControl(
                target = 1u,
                x = x.toShort(),
                y = y.toShort(),
                z = z.toUShort(),
                r = r.toShort(),
                buttons = 0u
            )
        )
    }

    suspend fun arm() {
        adapter?.send(
            systemId = 255u,
            componentId = 0u,
            message = CommandLong(
                targetSystem = 1u,
                targetComponent = 1u,
                command = MavCmd.COMPONENT_ARM_DISARM,
                confirmation = 0u,
                param1 = 1f, // arm
                param2 = 0f, param3 = 0f, param4 = 0f,
                param5 = 0f, param6 = 0f, param7 = 0f
            )
        )
    }
}
```

### 8.5 Architectural Decision: Direct MAVLink vs Bridge WebSocket

There are two approaches for Android ↔ SITL communication:

**Option A — Direct MAVLink UDP (Android ↔ SITL)**
```text
Android App ←→ (MAVLink UDP) ←→ ArduPilot SITL
```
- Pros: Lower latency, simpler deployment
- Cons: No backend logic, no multi-client, harder NAT traversal

**Option B — Bridge WebSocket (Android ↔ Bridge ↔ SITL)**
```text
Android App ←→ (WebSocket JSON) ←→ Python Bridge ←→ (MAVLink UDP) ←→ SITL
```
- Pros: Multi-client, classroom management, easier NAT/firewall, backend processing
- Cons: Additional hop, requires bridge server

**Recommendation:** Use **Option B (Bridge)** for production. Use **Option A** as an advanced/developer mode.

The bridge approach allows:
- Multiple students connecting to the same SITL
- Instructor controls (pause, inject failures)
- Telemetry logging and replay
- Easier cloud deployment (WebSocket through firewalls)
- Backend processing (lesson state, progress tracking)

---

## 9. 3D Drone Visualization on Android

### 9.1 Technology: SceneView + Google Filament

SceneView is the recommended solution for 3D in Jetpack Compose:

- Built on **Google Filament** (same engine as Google Maps, Android Auto)
- **Compose-native** — works as a `@Composable`
- **PBR rendering** — physically-based, professional-quality
- **GLB/glTF support** — industry-standard 3D format
- **Async loading** — handles large models without UI jank

### 9.2 Integration Code

```kotlin
@Composable
fun DroneVisualization(
    roll: Float,    // radians
    pitch: Float,   // radians
    yaw: Float,     // radians
    altitude: Float // meters
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator()
    ) {
        rememberModelInstance(modelLoader, "models/drone.glb")?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.0f,
                autoAnimate = true // spinning rotors
            ).apply {
                // Apply telemetry orientation
                rotation = Rotation(
                    x = Math.toDegrees(pitch.toDouble()).toFloat(),
                    y = Math.toDegrees(yaw.toDouble()).toFloat(),
                    z = Math.toDegrees(roll.toDouble()).toFloat()
                )
                position = Position(
                    x = 0f,
                    y = altitude,
                    z = 0f
                )
            }
        }
    }
}
```

### 9.3 3D Scene Elements

For the MVP visualization:

| Element | Implementation | Priority |
|---------|---------------|----------|
| Drone model (quad) | GLB model with rotor animation | P0 |
| Attitude rotation | Map MAVLink roll/pitch/yaw to model transform | P0 |
| Altitude display | Y-axis position + text overlay | P0 |
| Ground plane | Simple grid plane | P1 |
| Flight path trail | Line renderer from position history | P1 |
| Waypoint markers | Sphere/pin models at GPS positions | P1 |
| Home marker | Special pin at launch location | P1 |
| GPS uncertainty | Translucent circle around drone | P2 |
| Wind indicator | Arrow showing wind direction/strength | P2 |
| Compass rose | Cardinal direction overlay | P2 |
| Propeller speed | Animation speed proportional to throttle | P2 |

### 9.4 Performance Budget

- Target: 60 FPS on mid-range Android devices
- Drone model: ≤ 10K triangles, ≤ 2 materials
- Texture size: ≤ 1024x1024 per material
- Frame rendering: ≤ 16ms per frame
- Use `Profile GPU Rendering` in developer options to monitor

---

## 10. Telemetry Charts & Data Visualization

### 10.1 Library: Vico

Vico is the recommended charting library for Jetpack Compose:

- Compose-native (no `AndroidView` wrapper needed)
- Supports line charts, bar charts, and combined charts
- Handles real-time data efficiently
- Active development and community

### 10.2 Telemetry Charts Required

| Chart | Data Source | Update Rate | Type |
|-------|-----------|-------------|------|
| Roll/Pitch/Yaw | `ATTITUDE` | 10 Hz | Rolling line chart |
| Altitude | `VFR_HUD.alt` | 5 Hz | Rolling line chart |
| Ground speed | `VFR_HUD.groundspeed` | 5 Hz | Rolling line chart |
| Climb rate | `VFR_HUD.climb` | 5 Hz | Rolling line chart |
| Battery voltage | `SYS_STATUS.voltage_battery` | 1 Hz | Gauge + line chart |
| GPS satellite count | `GPS_RAW_INT.satellites_visible` | 1 Hz | Bar/number display |
| Throttle | `VFR_HUD.throttle` | 10 Hz | Vertical gauge |
| PID response | Computed from attitude setpoint vs actual | 10 Hz | Dual line chart |

### 10.3 Real-Time Chart Performance

```kotlin
// Real-time telemetry chart with Vico
@Composable
fun AttitudeChart(telemetryFlow: Flow<AttitudeData>) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(Unit) {
        telemetryFlow
            .sample(100.milliseconds) // Cap at 10 Hz rendering
            .collect { attitude ->
                modelProducer.runTransaction {
                    lineSeries {
                        series(attitude.rollHistory)  // last 100 values
                        series(attitude.pitchHistory)
                        series(attitude.yawHistory)
                    }
                }
            }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer()
        ),
        modelProducer = modelProducer,
        diffAnimationSpec = null, // Disable animations for real-time
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}
```

**Key performance practices:**
- Use `Flow.sample()` to cap update rate at 10 Hz for charts
- Disable `diffAnimationSpec` for real-time data
- Keep history buffer to ~100 data points per series
- Use `Flow.conflate()` to drop intermediate values if the UI is lagging

---

## 11. Backend Architecture — Python Bridge Server

### 11.1 Architecture Overview

```text
┌──────────────────────────────────────────────────────────────┐
│                   MAVLab Bridge Server                        │
│                                                              │
│  ┌─────────────────┐     ┌─────────────────────────────────┐ │
│  │ MAVLink Reader   │     │ Connection Manager               │ │
│  │                  │     │                                   │ │
│  │ pymavlink UDP    │────►│ Active WebSocket connections     │ │
│  │ → parse messages │     │ Session tracking                 │ │
│  │ → filter/route   │     │ Per-client queues (backpressure) │ │
│  └─────────────────┘     └──────────┬──────────────────────┘ │
│                                      │                        │
│  ┌─────────────────┐     ┌──────────▼──────────────────────┐ │
│  │ Command Handler  │     │ Broadcast Hub                    │ │
│  │                  │     │                                   │ │
│  │ WebSocket input  │────►│ Telemetry → all connected clients│ │
│  │ → validate       │     │ Commands → SITL via pymavlink    │ │
│  │ → convert to MAV │     │ Events → specific clients        │ │
│  └─────────────────┘     └──────────────────────────────────┘ │
│                                                              │
│  ┌─────────────────┐     ┌──────────────────────────────────┐ │
│  │ REST API         │     │ Telemetry Logger                  │ │
│  │                  │     │                                   │ │
│  │ /health          │     │ SQLite/JSON file logging          │ │
│  │ /config          │     │ Replay capability                │ │
│  │ /sessions        │     │ Lesson progress tracking          │ │
│  └─────────────────┘     └──────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

### 11.2 Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | FastAPI | 0.115+ |
| ASGI Server | Uvicorn | 0.32+ |
| MAVLink | pymavlink | 2.4.41+ |
| WebSocket | FastAPI WebSocket | built-in |
| Async | asyncio | Python 3.12+ |
| Data Validation | Pydantic v2 | 2.9+ |
| Logging | structlog | 24.0+ |
| Database (later) | SQLite / PostgreSQL | — |

### 11.3 Core Server Code Structure

```text
mavlab-bridge/
├── main.py                    # FastAPI app entry point
├── config.py                  # Settings & environment variables
├── mavlink/
│   ├── connection.py          # pymavlink connection management
│   ├── reader.py              # MAVLink telemetry reader (async)
│   ├── writer.py              # MAVLink command sender
│   └── parser.py              # Message → JSON conversion
├── websocket/
│   ├── manager.py             # Connection manager
│   ├── handler.py             # WebSocket message handler
│   └── models.py              # WebSocket message schemas
├── api/
│   ├── health.py              # Health check endpoints
│   ├── config.py              # Configuration endpoints
│   └── sessions.py            # Session management
├── education/
│   ├── lessons.py             # Lesson state machine
│   ├── failures.py            # Failure injection controller
│   └── progress.py            # Student progress tracking
├── Dockerfile
├── requirements.txt
└── docker-compose.yml
```

### 11.4 Bridge Server — Key Implementation

```python
# Simplified bridge server core
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from pymavlink import mavutil
import asyncio
import json

app = FastAPI(title="MAVLab Bridge")

class ConnectionManager:
    def __init__(self):
        self.active_connections: list[WebSocket] = []
        self.client_queues: dict[WebSocket, asyncio.Queue] = {}

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)
        self.client_queues[websocket] = asyncio.Queue(maxsize=100)

    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)
        del self.client_queues[websocket]

    async def broadcast(self, data: dict):
        """Non-blocking broadcast using per-client queues"""
        for ws, queue in self.client_queues.items():
            try:
                queue.put_nowait(data)
            except asyncio.QueueFull:
                # Drop oldest to prevent backlog
                try:
                    queue.get_nowait()
                except asyncio.QueueEmpty:
                    pass
                queue.put_nowait(data)

manager = ConnectionManager()

class MavlinkBridge:
    def __init__(self, connection_string: str):
        self.conn_string = connection_string
        self.master = None

    async def connect(self):
        self.master = mavutil.mavlink_connection(self.conn_string)
        self.master.wait_heartbeat()

    async def read_telemetry_loop(self):
        """Continuously read MAVLink and broadcast to WebSocket clients"""
        while True:
            msg = self.master.recv_match(blocking=False)
            if msg:
                msg_type = msg.get_type()
                if msg_type in ['ATTITUDE', 'GLOBAL_POSITION_INT',
                               'VFR_HUD', 'SYS_STATUS', 'HEARTBEAT',
                               'GPS_RAW_INT', 'BATTERY_STATUS', 'STATUSTEXT']:
                    data = {
                        "type": "telemetry",
                        "message_type": msg_type,
                        "data": msg.to_dict(),
                        "timestamp": msg._timestamp
                    }
                    await manager.broadcast(data)
            else:
                await asyncio.sleep(0.01)  # 100 Hz polling

    def send_manual_control(self, x, y, z, r, buttons=0):
        self.master.mav.manual_control_send(
            self.master.target_system, x, y, z, r, buttons
        )

    def send_arm(self):
        self.master.mav.command_long_send(
            self.master.target_system,
            self.master.target_component,
            mavutil.mavlink.MAV_CMD_COMPONENT_ARM_DISARM,
            0, 1, 0, 0, 0, 0, 0, 0
        )

    def set_mode(self, mode_name: str):
        mode_id = self.master.mode_mapping()[mode_name]
        self.master.mav.set_mode_send(
            self.master.target_system,
            mavutil.mavlink.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
            mode_id
        )

bridge = MavlinkBridge("udp:127.0.0.1:14551")

@app.on_event("startup")
async def startup():
    await bridge.connect()
    asyncio.create_task(bridge.read_telemetry_loop())

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await manager.connect(websocket)
    try:
        # Start sending telemetry from queue
        send_task = asyncio.create_task(
            _send_telemetry(websocket, manager.client_queues[websocket])
        )
        # Receive commands from client
        while True:
            data = await websocket.receive_json()
            await _handle_command(data)
    except WebSocketDisconnect:
        manager.disconnect(websocket)
        send_task.cancel()

async def _send_telemetry(ws: WebSocket, queue: asyncio.Queue):
    while True:
        data = await queue.get()
        await ws.send_json(data)

async def _handle_command(data: dict):
    cmd = data.get("command")
    if cmd == "manual_control":
        bridge.send_manual_control(
            data["x"], data["y"], data["z"], data["r"]
        )
    elif cmd == "arm":
        bridge.send_arm()
    elif cmd == "set_mode":
        bridge.set_mode(data["mode"])
```

### 11.5 WebSocket Message Protocol

**Client → Server (Commands):**
```json
{
  "command": "manual_control",
  "x": 150,
  "y": -200,
  "z": 500,
  "r": 0
}

{
  "command": "arm"
}

{
  "command": "set_mode",
  "mode": "GUIDED"
}

{
  "command": "inject_failure",
  "failure_type": "gps_loss"
}
```

**Server → Client (Telemetry):**
```json
{
  "type": "telemetry",
  "message_type": "ATTITUDE",
  "data": {
    "roll": 0.05,
    "pitch": -0.02,
    "yaw": 1.57,
    "rollspeed": 0.001,
    "pitchspeed": -0.003,
    "yawspeed": 0.0
  },
  "timestamp": 1717430400.123
}

{
  "type": "telemetry",
  "message_type": "VFR_HUD",
  "data": {
    "airspeed": 0.0,
    "groundspeed": 0.5,
    "heading": 90,
    "throttle": 50,
    "alt": 10.5,
    "climb": 0.1
  },
  "timestamp": 1717430400.456
}

{
  "type": "event",
  "event_type": "mode_change",
  "data": {
    "old_mode": "STABILIZE",
    "new_mode": "GUIDED"
  }
}
```

---

## 12. Network & Communication Architecture

### 12.1 MAVLink Routing

When multiple clients need MAVLink data, you need a routing solution:

**Option A — MAVProxy as Router**
```text
ArduPilot SITL → MAVProxy → udp:14550 (QGC)
                          → udp:14551 (Bridge)
                          → udp:14552 (extra)
```
MAVProxy is a CLI MAVLink proxy that can split and route MAVLink streams.

**Option B — sim_vehicle.py Multiple Outputs**
```bash
sim_vehicle.py -v ArduCopter \
    --out=udp:127.0.0.1:14550 \
    --out=udp:127.0.0.1:14551
```
SITL can natively output to multiple endpoints.

**Option C — mavlink-router**
```text
ArduPilot SITL → mavlink-router → multiple endpoints
```
Lightweight C++ MAVLink router, good for embedded/server use.

**Recommendation:** Use `sim_vehicle.py --out` for development, **mavlink-router** for production Docker deployment.

### 12.2 Network Topology for Cloud Deployment

```text
┌─────────────────────────────────────────────────┐
│              Cloud Server (VPS)                  │
│                                                  │
│   SITL ──(MAVLink UDP 14551)──► Bridge Server   │
│                                    │              │
│                                    │ wss://       │
│                                    │ :443/ws      │
│                                    │              │
│   SITL ──(MAVLink UDP 14550)──────────────────► │
│                                    │        │     │
└────────────────────────────────────┼────────┼─────┘
                                     │        │
                    ┌────────────────┘        └───────────┐
                    │                                      │
            ┌───────▼───────┐                    ┌────────▼───────┐
            │  Android App   │                    │  QGroundControl │
            │  (WebSocket)   │                    │  (UDP)          │
            └───────────────┘                    └────────────────┘
```

### 12.3 WebSocket Client on Android (OkHttp)

```kotlin
class WebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private var webSocket: WebSocket? = null
    private val _messages = MutableSharedFlow<ServerMessage>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<ServerMessage> = _messages

    fun connect(url: String) {
        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val message = Json.decodeFromString<ServerMessage>(text)
                _messages.tryEmit(message)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Reconnect with exponential backoff
                scheduleReconnect()
            }
        })
    }

    fun sendCommand(command: ClientCommand) {
        val json = Json.encodeToString(command)
        webSocket?.send(json)
    }
}
```

**OkHttpClient configuration:**
```kotlin
val okHttpClient = OkHttpClient.Builder()
    .pingInterval(30, TimeUnit.SECONDS)  // Keep-alive
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.SECONDS)     // No read timeout for WebSocket
    .build()
```

---

## 13. Deployment Strategy — SITL in the Cloud

### 13.1 Why Cloud SITL?

- Students don't need to install ArduPilot (complex setup)
- Works from any Android device with internet
- Instructor can manage the simulation centrally
- Multi-student sessions with one SITL instance
- Always-on classroom environment

### 13.2 Recommended Cloud Provider

For Africa-focused deployment, consider:

| Provider | Closest Region | Estimated Cost | SITL Feasible? |
|----------|---------------|---------------|----------------|
| **DigitalOcean** | London, Amsterdam | $12/mo (2 vCPU, 2GB) | ✅ |
| **Hetzner** | Falkenstein, Helsinki | €4.5/mo (2 vCPU, 4GB) | ✅ Best value |
| **AWS** | Cape Town (af-south-1) | ~$20/mo (t3.small) | ✅ Lowest latency |
| **Google Cloud** | Johannesburg | ~$25/mo | ✅ |
| **Linode** | London | $12/mo | ✅ |

**Recommendation:** **Hetzner** for cost, **AWS Cape Town** for latency.

### 13.3 Minimum Server Requirements

```text
CPU:    2 vCPUs (SITL is single-threaded but needs headroom)
RAM:    2 GB (SITL ~200MB + Bridge ~100MB + OS)
Disk:   20 GB SSD
OS:     Ubuntu 22.04 LTS
Network: 1 Gbps (standard for all cloud providers)
```

### 13.4 Docker Compose Production Stack

```yaml
version: '3.8'

services:
  sitl:
    build:
      context: ./sitl
    container_name: mavlab-sitl
    network_mode: host  # Simplifies MAVLink UDP
    restart: unless-stopped
    command: >
      Tools/autotest/sim_vehicle.py
      -v ArduCopter
      --no-mavproxy
      --out=udp:127.0.0.1:14550
      --out=udp:127.0.0.1:14551

  bridge:
    build:
      context: ./bridge
    container_name: mavlab-bridge
    ports:
      - "8000:8000"
    environment:
      SITL_CONNECTION: "udp:127.0.0.1:14551"
      WS_HOST: "0.0.0.0"
      WS_PORT: "8000"
      CORS_ORIGINS: "*"
    depends_on:
      - sitl
    network_mode: host
    restart: unless-stopped

  caddy:
    image: caddy:2
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile
    depends_on:
      - bridge
    restart: unless-stopped
```

**Caddyfile (TLS termination + WebSocket proxy):**
```
mavlab.yourdomain.com {
    reverse_proxy /ws localhost:8000
    reverse_proxy /api/* localhost:8000
}
```

---

## 14. Competitor & Market Analysis

### 14.1 Market Landscape

The drone education simulator market has three tiers:

**Tier 1 — Hobbyist/Racing Simulators**
| Product | Focus | Price | Education Features |
|---------|-------|-------|--------------------|
| Liftoff | FPV racing | $20 | ❌ None |
| VelociDrone | FPV racing | $20 | ❌ None |
| RealFlight | RC flying | $130-200 | ❌ None |
| DRL Simulator | FPV racing | Free | ❌ None |

**Tier 2 — Educational Platforms**
| Product | Focus | Price | Drone Internals |
|---------|-------|-------|-----------------|
| DroneBlocks | Block coding + STEM | Subscription | ❌ No MAVLink/ArduPilot |
| Zephyr | Part 107 training | $200-600/yr | ❌ No internals |
| Tello Edu | Coding with DJI Tello | $130 (drone) | ❌ Proprietary |

**Tier 3 — Professional/Military**
| Product | Focus | Price |
|---------|-------|-------|
| CAE | Military training | $100K+ |
| Lockheed Martin | Defense simulation | $1M+ |
| L3Harris | Defense simulation | $500K+ |

### 14.2 The Gap MAVLab Fills

```text
                    Hobbyist        MAVLab         Professional
                    Sims            TARGET         Sims
                    ▼               ▼              ▼
Pilot Skills     ████████████     ████             ████████████████
Drone Internals                   ████████████
MAVLink/ArduPilot                 ████████████████
PID Control                       ████████████
Failure Analysis                  ████████████     ████████████████
Cost             $20-200          FREE             $100K-1M+
```

**MAVLab's unique position:** It's the only product that teaches drone systems internals (MAVLink, PID, sensor fusion, ArduPilot workflow) using actual production tools (ArduPilot SITL, QGroundControl) at zero hardware cost.

### 14.3 Competitive Advantages

1. **Uses real tools** — students learn ArduPilot and MAVLink, not toy abstractions
2. **Free** — source-available, no subscription, no hardware required
3. **Mobile-first** — Android app, works in resource-constrained environments
4. **Africa/developing-market focus** — designed for regions where drone hardware is expensive
5. **Open ecosystem** — BSL 1.1, community can contribute and adapt
6. **Phone-as-controller** — novel physical interaction without joystick hardware

---

## 15. Production Engineering — CI/CD, Testing, Quality

### 15.1 Repository Structure

```text
mavlab/                          (GitHub monorepo)
├── .github/
│   └── workflows/
│       ├── android-ci.yml       # Build + test on PR
│       ├── android-release.yml  # Build + deploy to Play Store
│       ├── bridge-ci.yml        # Python lint + test
│       └── bridge-deploy.yml    # Deploy bridge to cloud
├── android/                     # Android app (Gradle project)
├── bridge/                      # Python bridge server
├── sitl/                        # SITL Docker configuration
├── docs/                        # Documentation
├── curriculum/                  # Lesson content (Markdown)
├── assets/                      # 3D models, images
├── LICENSE                      # BSL 1.1
└── README.md
```

### 15.2 Android CI Pipeline (GitHub Actions)

```yaml
name: Android CI
on:
  pull_request:
    paths: ['android/**']
  push:
    branches: [main]
    paths: ['android/**']

concurrency:
  group: android-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Run Lint
        working-directory: android
        run: ./gradlew lintDebug

      - name: Run Unit Tests
        working-directory: android
        run: ./gradlew testDebugUnitTest

      - name: Build Debug APK
        working-directory: android
        run: ./gradlew assembleDebug

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: android/app/build/outputs/apk/debug/app-debug.apk
```

### 15.3 Android Release Pipeline

```yaml
name: Android Release
on:
  push:
    tags: ['v*']

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Decode Keystore
        run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > android/keystore.jks

      - name: Build Release Bundle
        working-directory: android
        run: ./gradlew bundleRelease
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}

      - name: Upload to Google Play
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.PLAY_SERVICE_ACCOUNT }}
          packageName: com.ascend.mavlab
          releaseFiles: android/app/build/outputs/bundle/release/app-release.aab
          track: internal  # Start with internal testing track
```

### 15.4 Bridge Server CI

```yaml
name: Bridge CI
on:
  pull_request:
    paths: ['bridge/**']

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.12'
          cache: 'pip'

      - name: Install Dependencies
        run: pip install -r bridge/requirements.txt -r bridge/requirements-dev.txt

      - name: Lint
        run: ruff check bridge/

      - name: Type Check
        run: mypy bridge/

      - name: Unit Tests
        run: pytest bridge/tests/ -v --cov=bridge
```

### 15.5 Testing Strategy

| Layer | Tool | What to Test |
|-------|------|-------------|
| Android Unit Tests | JUnit 5 + MockK | ViewModels, Use Cases, MAVLink parsing |
| Android UI Tests | Compose Test | Screen rendering, user interactions |
| Bridge Unit Tests | pytest | MAVLink parsing, command handling |
| Bridge Integration | pytest + asyncio | WebSocket communication, SITL connection |
| E2E Tests | Maestro (Android) | Full user flows on device/emulator |
| MAVLink Protocol | Custom test harness | Message encoding/decoding correctness |

---

## 16. 3D Asset Pipeline

### 16.1 Drone Model Requirements

| Requirement | Specification |
|-------------|--------------|
| Format | GLB (binary glTF 2.0) |
| Polygon count | ≤ 10,000 triangles |
| Materials | PBR (metallic-roughness workflow) |
| Textures | ≤ 1024×1024, PNG or KTX2 |
| Animations | Propeller rotation (4 rotors) |
| Coordinate system | Y-up (Android/Filament standard) |
| Scale | 1 unit = 1 meter |

### 16.2 Asset Sources

| Source | License | Quality | Notes |
|--------|---------|---------|-------|
| Sketchfab | CC0 / CC-BY | High | Search "quadcopter" + filter "Downloadable" |
| Google Poly (archive) | CC-BY | Medium | Archived but models still available |
| TurboSquid (free section) | Various | Medium | Check license carefully |
| Create in Blender | Own | Full control | Best for custom educational model |

### 16.3 Custom Model Creation (Recommended)

Create a purpose-built educational drone model in Blender:

1. **Simplified quadcopter frame** — X or + configuration
2. **Color-coded arms** — front arms different color for orientation
3. **Visible propellers** — animated rotation
4. **Transparent flight controller** — to show "brain"
5. **Labeled sensors** — GPS antenna, compass, barometer
6. **Coordinate axes** — small RGB arrows showing body frame

Export from Blender: File → Export → glTF 2.0 (.glb) with:
- Compression: Draco
- Include animations
- Apply modifiers

---

## 17. ArduPilot SITL Failure Injection

### 17.1 Failure Parameters

ArduPilot SITL provides parameters prefixed with `SIM_` to simulate failures:

| Failure Type | Parameter | Values | Effect |
|-------------|-----------|--------|--------|
| **GPS Loss** | `SIM_GPS_DISABLE` | 0=normal, 1=disabled | Total GPS failure |
| **GPS Noise** | `SIM_GPS_NOISE` | 0-5 (meters) | Adds noise to GPS position |
| **GPS Glitch** | `SIM_GPS_GLITCH_X/Y/Z` | meters | Sudden GPS position offset |
| **Compass Disable** | `COMPASS_ENABLE` | 0=disabled | Compass failure |
| **Battery Drain** | `SIM_BATT_VOLTAGE` | volts | Force specific battery voltage |
| **Motor Failure** | `SIM_ENGINE_FAIL` | bitmask | Disable specific motors |
| **Wind** | `SIM_WIND_SPD` | m/s | Constant wind speed |
| **Wind Direction** | `SIM_WIND_DIR` | degrees | Wind direction |
| **Wind Turbulence** | `SIM_WIND_TURB` | m/s | Wind turbulence intensity |
| **Vibration** | `SIM_VIB_MOT_MAX` | m/s² | Motor vibration level |

### 17.2 Failure Injection via MAVLink

The bridge can inject failures by setting parameters via MAVLink:

```python
def inject_gps_failure(self):
    """Simulate GPS loss"""
    self.master.mav.param_set_send(
        self.master.target_system,
        self.master.target_component,
        b'SIM_GPS_DISABLE',
        1.0,
        mavutil.mavlink.MAV_PARAM_TYPE_REAL32
    )

def inject_wind(self, speed_ms: float, direction_deg: float):
    """Simulate wind"""
    self.master.mav.param_set_send(
        self.master.target_system,
        self.master.target_component,
        b'SIM_WIND_SPD',
        speed_ms,
        mavutil.mavlink.MAV_PARAM_TYPE_REAL32
    )
    self.master.mav.param_set_send(
        self.master.target_system,
        self.master.target_component,
        b'SIM_WIND_DIR',
        direction_deg,
        mavutil.mavlink.MAV_PARAM_TYPE_REAL32
    )
```

### 17.3 Failure Lab Scenarios for Education

| Scenario | Parameters Changed | Student Task | Learning Goal |
|----------|-------------------|-------------|---------------|
| "GPS Lost" | `SIM_GPS_DISABLE=1` | Observe mode change, diagnose via telemetry | Understand GPS failsafe |
| "GPS Drift" | `SIM_GPS_NOISE=3` | Notice position uncertainty, check satellite count | Understand GPS accuracy |
| "Battery Low" | `SIM_BATT_VOLTAGE=10.5` | Observe RTL trigger, monitor voltage | Understand battery failsafe |
| "Windy Day" | `SIM_WIND_SPD=8, SIM_WIND_DIR=90` | Observe position hold effort, PID response | Understand wind compensation |
| "Motor Out" | `SIM_ENGINE_FAIL=1` | Observe instability, emergency response | Understand motor redundancy |
| "Compass Interference" | `COMPASS_ENABLE=0` | Observe heading drift, EKF warnings | Understand compass importance |
| "Vibrations" | `SIM_VIB_MOT_MAX=30` | Observe noisy sensor data, degraded performance | Understand vibration effects |

---

## 18. Technology Stack Summary

### 18.1 Complete Stack

```text
┌─────────────────────────────────────────────────────────────┐
│                        ANDROID APP                           │
│                                                              │
│  Language:      Kotlin 2.1                                   │
│  UI:            Jetpack Compose + Material 3                 │
│  Architecture:  Clean Architecture + MVVM                    │
│  DI:            Hilt                                         │
│  Navigation:    Compose Navigation                           │
│  Networking:    OkHttp (WebSocket)                           │
│  MAVLink:       mavlink-kotlin (divyanshupundir)             │
│  3D Engine:     SceneView / Google Filament                  │
│  Charts:        Vico                                         │
│  Sensors:       Android SensorManager / Fused Location       │
│  Async:         Kotlin Coroutines + Flow                     │
│  Build:         Gradle (Kotlin DSL) + Version Catalog        │
│  Min SDK:       26 (Android 8.0)                             │
│  Target SDK:    35 (Android 15)                              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      BRIDGE SERVER                           │
│                                                              │
│  Language:      Python 3.12+                                 │
│  Framework:     FastAPI                                      │
│  ASGI:          Uvicorn                                      │
│  MAVLink:       pymavlink                                    │
│  WebSocket:     FastAPI built-in                             │
│  Validation:    Pydantic v2                                  │
│  Logging:       structlog                                    │
│  Testing:       pytest + pytest-asyncio                      │
│  Linting:       ruff + mypy                                  │
│  Container:     Docker                                       │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    SIMULATION ENGINE                         │
│                                                              │
│  Autopilot:     ArduPilot SITL                               │
│  GCS:           QGroundControl                               │
│  Protocol:      MAVLink 2.0                                  │
│  Transport:     UDP (MAVLink), WebSocket (app)               │
│  Container:     Docker                                       │
│  Orchestration: Docker Compose                               │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE                            │
│                                                              │
│  Cloud:         Hetzner / AWS / DigitalOcean                 │
│  TLS:           Caddy (auto HTTPS)                           │
│  CI/CD:         GitHub Actions                               │
│  Distribution:  Google Play Store                            │
│  Source:        GitHub (BSL 1.1 license)                     │
│  3D Assets:     Blender → GLB                                │
│  Docs:          MkDocs or Docusaurus                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 19. Module Dependency Map

```text
                    ┌────────────────┐
                    │      :app      │
                    └───────┬────────┘
                            │
            ┌───────────────┼───────────────┐
            │               │               │
     ┌──────▼──────┐ ┌─────▼──────┐ ┌──────▼──────┐
     │ :feature:*  │ │   :data    │ │  :domain    │
     │ (all feats) │ │            │ │             │
     └──────┬──────┘ └─────┬──────┘ └──────┬──────┘
            │               │               │
            │         ┌─────┼─────┐         │
            │         │     │     │         │
            │    ┌────▼─┐ ┌▼────┐│    ┌────▼────┐
            │    │:core:│ │:core:││    │ :domain │
            │    │ net  │ │ mav  ││    │ models  │
            │    └──────┘ └─────┘│    └─────────┘
            │                    │
     ┌──────▼──────┐      ┌─────▼─────┐
     │  :core:ui   │      │:core:sens │
     │  (theme,    │      │ (sensors) │
     │   widgets)  │      └───────────┘
     └─────────────┘

     ┌─────────────┐
     │:core:common  │  ← Used by everything
     └─────────────┘

Dependency Rules:
  • :feature:* → :domain, :core:ui, :core:common
  • :data → :domain, :core:network, :core:mavlink, :core:sensors
  • :domain → :core:common (ONLY — no Android dependencies)
  • :core:* → :core:common (only)
```

---

## 20. Phased Build Plan

### Phase 0 — Foundation (Week 1-2)

**Goal:** Prove the SITL → Bridge → Android pipeline works end-to-end.

| Task | Output |
|------|--------|
| Set up ArduPilot SITL on Linux | Running SITL, QGC connected |
| Build minimal Python bridge | pymavlink reads HEARTBEAT + ATTITUDE |
| Add FastAPI WebSocket server | Bridge streams telemetry as JSON |
| Build minimal Android app | Kotlin + Compose, connects to WebSocket |
| Display roll/pitch/yaw | Numbers on screen updating live |
| **Milestone:** Phone shows live telemetry from SITL | ✅ Architecture validated |

### Phase 1 — Core Dashboard (Week 3-5)

**Goal:** Build a useful education dashboard on Android.

| Task | Output |
|------|--------|
| Parse all MVP telemetry messages | ATTITUDE, VFR_HUD, GPS, BATTERY, etc. |
| Build telemetry dashboard UI | Cards showing all key values |
| Add Vico charts | Roll/pitch/yaw/altitude rolling graphs |
| Add MAVLink message inspector | Live message feed with explanations |
| Add flight mode display + explanation | "You are in LOITER mode: ArduPilot holds position using GPS" |
| Implement design system | Material 3, dark theme, typography |

### Phase 2 — Phone Controller (Week 6-8)

**Goal:** Control the simulated drone with phone tilt.

| Task | Output |
|------|--------|
| Implement sensor repository | Orientation flow from SensorManager |
| Build controller UI | Visual tilt indicator + throttle slider |
| Add calibration | "Hold phone flat and tap calibrate" |
| Map tilt to MANUAL_CONTROL | Phone orientation → x/y/z/r values |
| Send controls via WebSocket → Bridge → MAVLink | Phone controls SITL drone |
| Add arm/disarm/takeoff/land buttons | Command buttons on controller screen |
| **Milestone:** Student tilts phone, drone responds in QGC | ✅ Flagship feature working |

### Phase 3 — 3D Visualization (Week 9-11)

**Goal:** Show the drone in 3D on the phone.

| Task | Output |
|------|--------|
| Create/acquire drone GLB model | Optimized 3D asset with propeller animation |
| Integrate SceneView | 3D scene rendering in Compose |
| Map telemetry to 3D transform | Model rotates with roll/pitch/yaw |
| Add ground plane and grid | Spatial reference |
| Add flight path trail | Line showing drone's traveled path |
| Add altitude indicator | Visual altitude reference |

### Phase 4 — Education Modules (Week 12-16)

**Goal:** Add structured learning content.

| Task | Output |
|------|--------|
| Sensor Lab module | Visualize phone IMU/GPS with educational annotations |
| PID Lab module | Interactive P/I/D sliders with response graphs |
| Flight Mode Lab | Mode selector with explanations and effects |
| Lesson Engine | Guided step-by-step lessons with checkpoints |
| Failure Lab | Toggle GPS loss, wind, battery drain; observe effects |
| MAVLink Lab | Deep-dive message inspector with field explanations |

### Phase 5 — Production Polish (Week 17-20)

**Goal:** Ship-quality product.

| Task | Output |
|------|--------|
| Onboarding flow | First-time setup, connection wizard |
| Error handling | Graceful disconnects, reconnection |
| Performance optimization | 60 FPS 3D, smooth charts |
| Accessibility | Content descriptions, touch targets |
| CI/CD pipelines | Automated build, test, deploy |
| Docker deployment | SITL + Bridge packaged |
| Documentation | Setup guides, teacher manual |
| Google Play release | Internal testing → Beta → Production |

---

## 21. Open Questions & Decisions

### 21.1 Architecture Decisions Needed

| Question | Options | Recommendation | Rationale |
|----------|---------|----------------|-----------|
| Android ↔ SITL via Bridge or Direct? | Bridge (WebSocket) vs Direct (MAVLink UDP) | Bridge first, direct as advanced mode | Multi-client, classroom, NAT traversal |
| Monorepo or multi-repo? | Single repo vs separate repos | Monorepo | Simpler CI, atomic changes, easier contribution |
| App name / package name? | com.ascend.mavlab | Yes | Clean, professional, memorable |
| Minimum Android version? | API 24, 26, or 28 | API 26 (Android 8.0) | Covers 95%+ devices, avoids ancient APIs |
| Compose Navigation or Voyager? | Compose Nav vs Voyager | Compose Navigation | Official, Hilt integration, stable |

### 21.2 Product Questions

| Question | Impact | Notes |
|----------|--------|-------|
| Should students be able to connect without an instructor? | UX, cloud cost | If yes, need a "solo mode" with self-hosted SITL |
| Should lessons be offline-capable? | Architecture | Content can be bundled in APK, telemetry needs connection |
| Should there be an instructor/admin panel? | Scope | Defer to Phase 5+ |
| Should we support multiple simultaneous students on one SITL? | Backend complexity | Defer — first version is 1 student per SITL instance |
| Should the Android app also support direct USB connection to Pixhawk? | Hardware integration | Defer — future feature for students with real hardware |

### 21.3 Technical Research Still Needed

| Topic | Priority | Notes |
|-------|----------|-------|
| Best MAVLink message rates for education vs performance | High | Test what rates feel "real-time" without overloading |
| SceneView performance on low-end Android devices | High | Test on $100-150 phones common in Africa |
| WebSocket reliability over African mobile networks | High | Reconnection, offline handling |
| ArduPilot SITL Docker image size optimization | Medium | Full build is large; explore pre-compiled binaries |
| SITL parameter presets for education scenarios | Medium | Curated parameter files for each lesson |
| QGroundControl Android + MAVLab coexistence | Medium | Both can connect to same SITL? Port conflicts? |

---

## 22. References

### ArduPilot & SITL
1. ArduPilot SITL Documentation — https://ardupilot.org/dev/docs/sitl-simulator-software-in-the-loop.html
2. ArduPilot SITL with JSON Interface — https://ardupilot.org/dev/docs/sitl-with-JSON.html
3. ArduPilot SITL with Gazebo — https://ardupilot.org/dev/docs/sitl-with-gazebo.html
4. ArduPilot Parameters List — https://ardupilot.org/copter/docs/parameters.html
5. ArduPilot SIM_ Parameters — https://ardupilot.org/dev/docs/sitl-simulator-software-in-the-loop.html#simulation-parameters

### MAVLink
6. MAVLink Developer Guide — https://mavlink.io/en/
7. MAVLink Common Messages — https://mavlink.io/en/messages/common.html
8. MAVLink MANUAL_CONTROL — https://mavlink.io/en/messages/common.html#MANUAL_CONTROL
9. MAVLink COMMAND_LONG — https://mavlink.io/en/messages/common.html#COMMAND_LONG

### QGroundControl
10. QGroundControl Official — https://qgroundcontrol.com/
11. QGroundControl Developer Guide — https://dev.qgroundcontrol.com/
12. QGroundControl GitHub — https://github.com/mavlink/qgroundcontrol
13. QGroundControl Android — https://play.google.com/store/apps/details?id=org.mavlink.qgroundcontrol

### Android Development
14. Jetpack Compose — https://developer.android.com/compose
15. Hilt Dependency Injection — https://developer.android.com/training/dependency-injection/hilt-android
16. Android Sensor Overview — https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview

### Libraries
17. mavlink-kotlin — https://github.com/divyanshupundir/mavlink-kotlin
18. SceneView Android — https://github.com/SceneView/sceneview-android
19. Vico Charts — https://github.com/patrykandpatrick/vico
20. OkHttp — https://square.github.io/okhttp/
21. FastAPI — https://fastapi.tiangolo.com/
22. pymavlink — https://github.com/ArduPilot/pymavlink

### Licensing
23. BSL 1.1 Official — https://mariadb.com/bsl11/
24. BSL FAQ — https://mariadb.com/bsl-faq-adopting/
25. AGPL-3.0 Text — https://www.gnu.org/licenses/agpl-3.0.en.html

### Deployment
26. Docker — https://docs.docker.com/
27. Caddy Server — https://caddyserver.com/docs/
28. GitHub Actions — https://docs.github.com/en/actions

---

*This document is a living research base. Update as decisions are made and implementation progresses.*
