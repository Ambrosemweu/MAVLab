# MAVLab Teacher Guide

MAVLab is designed for classroom drone systems lessons without physical aircraft and without making students begin with the hardest simulation stack. The teaching purpose is to let learners understand drone behaviour first, then later graduate into ROS, Gazebo, ArduPilot/PX4 SITL, Webots, JSBSim, hardware-in-the-loop, and real aircraft.

## Teaching Philosophy

Professional tools are powerful, but they can bury beginners under installation, networking, simulator, and autopilot details before the drone concepts are clear. MAVLab should give instructors a friendlier first layer:

1. Show the concept in MAVLab.
2. Let students operate it from a phone.
3. Connect the concept to QGroundControl/MAVLink.
4. Only then explain how the same concept appears in ArduPilot SITL, PX4 SITL, ROS, Gazebo, or real aircraft.

## Recommended Lesson Order

1. Your First Flight
2. Understanding MAVLink
3. Phone as Controller
4. Flight Modes Explained
5. PID Control
6. Sensors
7. Failsafes

## Classroom Format

- Students run MAVLab on their phones.
- QGroundControl can be used in split-screen or on an instructor desktop.
- Start each session with the problem MAVLab solves: drone simulation usually requires a steep toolchain, but the concepts can be learned first in one friendly app.
- Use Failure Lab only after students understand normal flight.

## Assessment Ideas

- Explain the difference between Stabilize, Alt Hold, Guided, Loiter, Auto, and RTL.
- Identify which telemetry messages QGC uses to show attitude, position, GPS, and battery.
- Demonstrate GPS loss and explain why Auto/Loiter degrade.
- Load a demo mission and describe waypoint progression.
- Compare behavior with and without wind.

## Safety Framing

MAVLab is a simulator, but its lessons should reinforce real-world safety: never arm near people, validate GPS before autonomous flight, and understand failsafes before relying on them.
