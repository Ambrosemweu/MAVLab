# Vault Documentation Sync Map

Updated: 2026-06-20

Purpose: identify which Ascend Operating System / Obsidian vault docs should drive MAVLab's GitHub documentation.

## Primary vault sources

| Vault source | Why it matters | GitHub documentation target |
| --- | --- | --- |
| `/home/ambrose/Documents/Obsidian Vault/Ascend Operating System/Ascend Labs/MAVLab Product Documentation.md` | Main source-of-truth for product origin, purpose, architecture evolution, product definition, repo map, user journeys, risks, and roadmap links. | `README.md`, `docs/product_purpose.md`, `docs/MAVLAB_PRODUCT_DOCUMENTATION_POINTER.md`, `docs/mavlab_product_surface_definition.md`, `docs/mavlab_gcs_digital_twin_guidelines.md`, `docs/teacher_guide.md`, `docs/setup_guide.md` |
| `/home/ambrose/Documents/Obsidian Vault/Ascend Operating System/Ascend Labs/MAVLab_Future_Roadmap.md` | Long-term direction: bootcamp platform, operator training, medical logistics digital twin, AI debrief, ROS/Gazebo/SITL bridges as future graduation paths. | `MAVLab_Future_Roadmap.md`, `docs/product_purpose.md`, `docs/v1_5_release_notes.md`, `docs/teacher_guide.md` |
| `/home/ambrose/Documents/Obsidian Vault/Ascend Operating System/Ascend Labs/MAVLab_v1.5_Execution_Plan.md` | Execution-grade v1.5 scope, non-negotiable product decisions, tab names, acceptance tests, and implementation anchors. | `docs/plans/MAVLab_v1.5_Execution_Plan.md`, `docs/mavlab_product_surface_definition.md`, `docs/test_matrix.md`, `docs/v1_5_demo_script.md`, `mavlab-android/docs/v1_5_checklist.md` |

## Core purpose update to propagate

The new core purpose is:

> MAVLab makes drone simulation approachable. It lets students, operators, and builders learn drone systems through one friendly app instead of needing to first assemble and understand ROS, Gazebo, ArduPilot SITL, PX4 SITL, QGroundControl, MAVProxy, Python bridges, Linux networking, and simulator-specific tooling.

This should appear in, or influence, these repository docs:

1. `README.md`
   - First paragraph and documentation links.
   - Explain the steep-learning-curve problem clearly.

2. `docs/product_purpose.md`
   - Dedicated public-facing explanation of the MAVLab purpose.

3. `docs/MAVLAB_PRODUCT_DOCUMENTATION_POINTER.md`
   - Keep pointing to the vault source-of-truth, but summarize the new purpose.

4. `docs/setup_guide.md`
   - Clarify that the core learning loop does not require ROS, Gazebo, SITL, Docker, Python bridges, or cloud infrastructure.

5. `docs/teacher_guide.md`
   - Clarify the instructor value: teach concepts first, professional toolchains later.

6. `docs/mavlab_product_surface_definition.md`
   - Product frame should say every tab reduces a part of the toolchain complexity into an understandable surface.

7. `docs/mavlab_gcs_digital_twin_guidelines.md`
   - Keep the digital-twin pivot, but explicitly connect it to the larger purpose: replacing the first learning step of ROS/Gazebo/SITL complexity with one approachable app.

8. `MAVLab_Future_Roadmap.md`
   - Keep ROS 2, Gazebo, ArduPilot SITL, PX4 SITL, Webots, and JSBSim as future/progression bridges, not initial prerequisites.

9. `docs/v1_5_demo_script.md`
   - Demo should open by naming the problem: drone simulation is powerful but intimidating; MAVLab makes it learnable on a phone.

## Repo docs that are historical, not primary sync targets

These should not be aggressively rewritten unless they contain public-facing contradictions, because they preserve project history:

- `phases/drone_systems_education_simulator_research_base.md`
- `phases/mavlab_production_research.md`
- `phases/mavlab_standalone_architecture_research.md`
- `phases/stress_test_report.md`
- `phases/phase_*`

If changed, add notes that reflect evolution rather than pretending earlier assumptions never existed.

## Documentation rule going forward

When MAVLab docs mention ROS, Gazebo, ArduPilot SITL, PX4 SITL, Webots, JSBSim, or hardware-in-the-loop, frame them as:

- powerful professional tools,
- future graduation paths,
- advanced integrations,
- or optional bridges,

not as prerequisites for MAVLab's core beginner learning experience.
