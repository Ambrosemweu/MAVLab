# MAVLab v1.5 QA Test Matrix

This document defines the test matrix and checklist for verifying the functionality, stability, and UX quality of MAVLab v1.5 prior to release.

## 1. Onboarding Flow

| Test Case ID | Scenario | Steps | Expected Result | Status |
|---|---|---|---|---|
| ONB-001 | First Launch | Clean install or clear app data, then open app | App displays onboarding screens explaining Cockpit, Controller, SIM, Mission, Labs, and Ops. | Passed |
| ONB-002 | Onboarding Progress | Navigate through all onboarding steps using "Next" | Each slide transitions smoothly; final slide shows a "Get Started" button that loads the main dashboard. | Passed |
| ONB-003 | Onboarding Replay | In the Ops tab, click "Replay onboarding" | Onboarding overlay re-opens and can be navigated or dismissed. | Passed |

---

## 2. Dashboard Screen (Cockpit)

| Test Case ID | Scenario | Steps | Expected Result | Status |
|---|---|---|---|---|
| DASH-001 | Idle Telemetry | Open Cockpit tab before arming | Displays disconnected/disarmed state. Altitude is 0.0m AGL, battery shows default level (~85%). Rolling charts are flat. | Passed |
| DASH-002 | Active Telemetry | Arm the drone and take off | Telemetry panels update at 5Hz. Altitude AGL, pitch, roll, and battery percent values update dynamically. Rolling charts plot real-time curves. | Passed |

---

## 3. Controller Screen (Local Control)

| Test Case ID | Scenario | Steps | Expected Result | Status |
|---|---|---|---|---|
| CTRL-001 | Phone Sensor Toggle | Enable phone sensor control switch | Sensors activate. Tilting the device shifts the roll/pitch visual indicators. Yaw trim slider shifts yaw trim. | Passed |
| CTRL-002 | Manual Fallback | Disable phone sensor control, slide joysticks | Joystick sliders control drone roll, pitch, yaw, and throttle manually. | Passed |

---

## 4. SIM Screen (3D Visualization)

| Test Case ID | Scenario | Steps | Expected Result | Status |
|---|---|---|---|---|
| SIM-001 | Drone Animation | Arm and take off, then pitch/roll the drone | The 3D GLB drone model pitches, rolls, and rotates its propellers in sync with telemetry. | Passed |
| SIM-002 | Overlay Panel | View HUD overlay on SIM screen | Altitude, vertical speed, and battery remaining indicators match current simulation values. | Passed |

---

## 5. Mission Tab & GCS Integration

| Test Case ID | Scenario | Steps | Expected Result | Status |
|---|---|---|---|---|
| MIS-001 | Demo Mission Load | Tap "Load Demo Mission" in Mission tab | A 4-waypoint path is loaded. Map visualization shows waypoint pins and trajectory lines. | Passed |
| MIS-002 | QGC Discovery | Run QGroundControl on the same network | QGC automatically detects MAVLab vehicle at `127.0.0.1:14550` or local IP. Heartbeats are green. | Passed |
| MIS-003 | GCS Mission Upload | Upload mission from QGC | Mission is acknowledged; items immediately sync and show in the MAVLab Mission tab. | Passed |
| MIS-004 | AUTO Execution | Start AUTO mission | Drone arms, takes off, flies through waypoints. MAVLab transmits mission current/reached updates. QGC status updates in sync. | Passed |

---

## 6. Failure Lab & Scenarios (Ops Tab)

| Test Case ID | Scenario | Steps | Expected Result | Status |
|---|---|---|---|---|
| FAIL-001 | Collapsible Catalog | Open Ops tab and click "Show" on the failure scenarios card | The scenario list expands with a smooth vertical slide animation. | Passed |
| FAIL-002 | In-flight Failure | Inject "GPS Loss" scenario | Dashboard GPS indicator turns red/error; active count badge in Ops card increases and shows "1 scenario" in red. GPS Loss card shows red "ACTIVE" tag. | Passed |
| FAIL-003 | Failure Reset | Tap "Clear all failures" button | All failure conditions clear; badges hide and GPS returns to healthy state. | Passed |

---

## 7. Flight Logging & Sharing (Ops Tab)

| Test Case ID | Scenario | Steps | Expected Result | Status |
|---|---|---|---|---|
| LOG-001 | Log Generation | Arm, fly for 30 seconds, disarm | A new flight log session folder is created under `mavlab/flights/` containing manifest, telemetry, events, mission, and report files. | Passed |
| LOG-002 | View Report | Click "View Report" for the session | Opens a dialog displaying the Markdown report detailing dates, duration, max altitude, battery stats, event timelines, and safety observations. | Passed |
| LOG-003 | Share Session | Click the "Share" icon next to the session | Launches the Android Sharesheet containing all 5 flight log files for external export. | Passed |
