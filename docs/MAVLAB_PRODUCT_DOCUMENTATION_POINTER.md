# MAVLab Product Documentation

This repository-level documentation is maintained from the Ascend Operating System source-of-truth note:

`/home/ambrose/Documents/Obsidian Vault/Ascend Operating System/Ascend Labs/MAVLab Product Documentation.md`

MAVLab is the first product coming out of Ascend Labs, Ascend’s R&D branch.

MAVLab makes drone simulation approachable. It is a user-friendly phone-based drone simulation and digital-twin application for learners who want to understand drones without first fighting the steep learning curve of ROS, Gazebo, ArduPilot/PX4 SITL, Docker, MAVProxy, Linux networking, and multi-window simulation setup.

MAVLab uses an Android phone as the primary simulation device, uses the phone’s sensors and built-in/manual controls as a drone-control interface, broadcasts MAVLink telemetry, connects with real ground control station software such as QGroundControl, and visualizes drone behaviour through cockpit telemetry, missions, failures, and a 3D drone twin.

The idea began with a simple question:

> Modern phones already have gyroscopes, accelerometers, GPS, magnetometers, cameras, compute, battery, screen, and network connectivity. Can we use a phone to make drone software believe it is connected to a drone?

That question evolved into a stronger product direction:

> Build a low-cost drone systems simulator that helps students, operators, and builders understand how drones sense, communicate, stabilize, navigate, fail, and execute missions — without needing every learner to own a Pixhawk, drone frame, batteries, RC transmitter, telemetry radio, GPS module, or safe flight field.

For the full detailed documentation, open the Obsidian source-of-truth note above.

Quick links in this repository:

- `README.md` — high-level product overview.
- `docs/product_purpose.md` — core purpose and positioning: MAVLab as the friendly layer before ROS/Gazebo/SITL.
- `docs/vault_documentation_sync_map.md` — vault-to-GitHub documentation sync targets.
- `docs/architecture.md` — runtime architecture.
- `docs/setup_guide.md` — setup and QGroundControl usage.
- `docs/teacher_guide.md` — classroom lesson flow.
- `docs/mavlab_product_surface_definition.md` — product surface decisions.
- `docs/mavlab_gcs_digital_twin_guidelines.md` — current digital twin direction.
- `phases/` — idea history, research, roadmap, and phase progression.
- `mavlab-android/` — active Android implementation.
