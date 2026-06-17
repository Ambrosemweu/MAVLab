# MAVLab v1.5 Demonstration Script

This script provides a 7–10 minute step-by-step guide for presenters, instructors, or QA testers to demonstrate the primary features of MAVLab v1.5.

---

## 1. Setup & Onboarding (1 Minute)

1. **Launch MAVLab** on the Android device.
2. **Onboarding Walkthrough:**
   - Explain the concept of MAVLab: a digital twin simulator for drone operations education.
   - Point out the tabs: **Cockpit** (telemetry dashboard), **Controller** (joysticks/sensors), **SIM** (3D rendering), **Mission** (autonomous paths), and **Ops** (settings & flight logs).
3. Tap **Get Started** to load the main dashboard.

---

## 2. Pre-flight & Local Manual Control (2 Minutes)

1. Navigate to the **Controller** tab.
2. Toggle **Arm Vehicle** to arm the simulator.
3. Slide the throttle up or tap **Takeoff** (alt hold starts at 10m).
4. Go to the **SIM** tab to see the 3D drone takeoff and hover.
5. Go back to the **Controller** tab:
   - Toggle **Enable Phone Sensor Control**.
   - Tilt your phone to show the pitch/roll overlay tracking device angles.
   - Show how the drone in the **SIM** tab mimics device tilting.
6. Toggle **Sensor Control** off to return to standard joysticks.

---

## 3. QGroundControl Connection & AUTO Mission (3 Minutes)

1. **Connect QGC:**
   - On the same device, open QGC in split-screen, or open QGC on a desktop PC on the same Wi-Fi.
   - Confirm connection: QGC speaks "Armed" or shows telemetry heartbeat.
2. **Upload Waypoints:**
   - In QGC, plan a short mission with 3 waypoints and click **Upload**.
   - Navigate to MAVLab's **Mission** tab. Verify the mission loaded matching QGC's configuration.
3. **Execute AUTO Mission:**
   - In QGC, slide to start the mission or trigger **AUTO** mode.
   - Watch the drone fly from the **SIM** tab, noting the green rings representing active waypoints.
   - Observe the **Mission** tab index updating as each waypoint is reached.

---

## 4. In-flight Failures & Recovery (2 Minutes)

1. While the drone is executing the mission, switch to the **Ops** tab.
2. **Inject a Failure:**
   - Tap **Show** on the **Failure scenario catalog** card.
   - Select **GPS Loss (Jamming)**.
3. **Handle the Emergency:**
   - Point out the red **● ACTIVE** badge and count indicators.
   - Switch to the **Cockpit** tab: note the GPS lock type indicator changes from 3D Fix to No Fix (red).
   - In QGC, observe the GPS warning.
   - Execute a safe landing command (tap **Land** in MAVLab or QGC) to recover the drone.

---

## 5. Post-flight Review & Logs Export (2 Minutes)

1. Tap **Disarm** in MAVLab or land completely.
2. Go to the **Ops** tab:
   - Note the **Flight logs** section now displays the completed session.
   - Click **View Report** to display the Markdown summary.
   - Point out the timeline showing when the vehicle was armed, when the GPS failure occurred, and when the land command was received.
   - Point out the safety warning: *"GPS was lost during flight — practice altitude hold recovery..."*
3. Tap the **Share** button: show the Android Sharesheet popup, ready to email or copy the CSV and Markdown files.
