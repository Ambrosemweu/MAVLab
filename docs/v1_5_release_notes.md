# MAVLab v1.5 Release Notes

MAVLab v1.5 is a major feature release introducing dynamic flight logging, report generation, sharing mechanisms, and extensive user interface improvements for classroom and operator training.

---

## What's New in v1.5

### 1. Flight Logging & Reporting System
- **Rich Telemetry CSV:** Telemetry logs now capture essential context columns including `session_id`, `gpsFixType`, `headingDegrees`, `activeWaypoint`, and `failureFlags` for detailed post-flight analysis.
- **Auto-Generated Markdown Reports:** Every flight session automatically generates a formatted `report.md` summarizing:
  - Dates, durations, and control authorities used.
  - Mission parameters and waypoint success ratios.
  - Calculated flight envelopes (such as maximum altitude reached).
  - Chronological event logs and timelines (mode shifts, arm state transitions, battery warnings, and failures).
  - Safety observations (e.g. reminders to configure fail-safe behaviors after simulated link loss).
- **In-App Report Viewer:** Review markdown flight summaries directly within the app using the new viewer dialog.
- **Flight Sharesheet Export:** Share the complete flight folder (CSV, manifests, events, missions, and markdown reports) directly to email, drive, or chat apps using the standard Android Sharesheet.

### 2. Failure Lab Upgrades
- **Collapsible Scenarios:** The failure scenario catalog is now collapsible to preserve screen space on smaller devices.
- **Active State Badges:** Displays real-time indicators showing which failures are active, complete with red counting badges next to the card header.
- **ACTIVE Badges on Chips:** Each failure scenario chip highlights in red with an `● ACTIVE` label when active.

### 3. Onboarding & Diagnostics UI
- **First-Launch Onboarding:** Interactive multi-step walkthrough explaining the system architecture and navigation surface. Available to replay at any time from the Ops tab.
- **GCS Status Indicators:** Highly visible color-coded indicators for MAVLink telemetry routing and QGroundControl link states.

---

## Upgrade Instructions
1. Build and install the new package on your device:
   ```bash
   ./gradlew installDebug
   ```
2. Open the app and follow the onboarding steps.
3. Access flight logs, sharing options, and failure catalog cards from the **Ops** tab.
