package com.ascend.mavlab.feature.lessons

import com.ascend.mavlab.simulation.engine.FlightMode

val LessonCatalog = listOf(
    Lesson(
        id = "first_flight",
        title = "1. Your First Flight",
        description = "Arm, take off, hover, and land.",
        estimatedMinutes = 10,
        steps = listOf(
            read("Every flight starts with a safe sequence.", "You will arm, take off, observe telemetry, and land."),
            LessonStep("Arm the drone.", "Arming enables the simulated motors.", StepAction.ArmDrone, CompletionCheck.DroneArmed),
            LessonStep("Take off to 10 meters.", "The guided takeoff controller climbs and holds altitude.", StepAction.Takeoff(10f), CompletionCheck.AltitudeAbove(8f)),
            read("Observe the dashboard.", "Altitude, attitude, throttle, battery, and GPS update from the simulator."),
            LessonStep("Land the drone.", "LAND mode descends and disarms on touchdown.", StepAction.LandDrone, CompletionCheck.DroneOnGround),
        ),
    ),
    Lesson(
        id = "mavlink",
        title = "2. Understanding MAVLink",
        description = "Learn what QGroundControl receives from MAVLab.",
        estimatedMinutes = 8,
        steps = listOf(
            read("MAVLink is the drone communication protocol.", "MAVLab broadcasts heartbeat, attitude, position, GPS, battery, and mission progress messages."),
            read("Open QGroundControl in split-screen.", "QGC should discover MAVLab through UDP broadcast or same-device routing."),
            LessonStep("Switch to Guided mode.", "Mode changes are sent in HEARTBEAT custom mode.", StepAction.ChangeMode(FlightMode.GUIDED), CompletionCheck.DroneInMode(FlightMode.GUIDED)),
            read("Watch telemetry update.", "QGC is reading the same state shown in MAVLab's Dashboard."),
        ),
    ),
    Lesson(
        id = "phone_controller",
        title = "3. Phone as Controller",
        description = "Use tilt or fallback controls to fly.",
        estimatedMinutes = 12,
        steps = listOf(
            read("Your phone can act like a controller.", "The Controller tab maps phone roll and pitch into pilot input."),
            LessonStep("Arm and take off.", "Use a stable hover before testing tilt controls.", StepAction.Takeoff(8f), CompletionCheck.AltitudeAbove(6f)),
            read("Open the Controller tab.", "Calibrate neutral attitude, then tilt gently."),
            LessonStep("Return to Loiter.", "Loiter captures the current position and tries to hold it.", StepAction.ChangeMode(FlightMode.LOITER), CompletionCheck.DroneInMode(FlightMode.LOITER)),
        ),
    ),
    Lesson(
        id = "flight_modes",
        title = "4. Flight Modes Explained",
        description = "Compare Stabilize, Alt Hold, Guided, Loiter, RTL, and Auto.",
        estimatedMinutes = 10,
        steps = listOf(
            read("Flight modes change what the autopilot controls.", "Stabilize is manual attitude, Alt Hold controls height, Guided/Auto control navigation."),
            LessonStep("Try Alt Hold.", "The autopilot maintains target altitude while allowing manual attitude input.", StepAction.ChangeMode(FlightMode.ALT_HOLD), CompletionCheck.DroneInMode(FlightMode.ALT_HOLD)),
            LessonStep("Try Loiter.", "Loiter holds the current local position when GPS is available.", StepAction.ChangeMode(FlightMode.LOITER), CompletionCheck.DroneInMode(FlightMode.LOITER)),
            LessonStep("Try RTL.", "RTL returns toward the arming home position.", StepAction.ChangeMode(FlightMode.RTL), CompletionCheck.DroneInMode(FlightMode.RTL)),
        ),
    ),
    Lesson(
        id = "pid_control",
        title = "5. PID Control",
        description = "See how control loops keep the drone stable.",
        estimatedMinutes = 15,
        steps = listOf(
            read("PID controllers close the loop.", "MAVLab uses attitude, rate, altitude, and position controllers."),
            LessonStep("Take off to a hover.", "A hover gives the controllers a steady target.", StepAction.Takeoff(10f), CompletionCheck.AltitudeAbove(8f)),
            read("Watch roll and pitch settle near zero.", "The attitude loop converts desired angles into rate commands, then motor mixing distributes thrust."),
            LessonStep("Add wind in Failure Lab.", "Wind disturbance makes controller response easier to see.", StepAction.InjectFailure("windy_day"), CompletionCheck.ActiveFailure("windy_day")),
        ),
    ),
    Lesson(
        id = "sensors",
        title = "6. Sensors",
        description = "Learn how GPS, compass, and attitude affect navigation.",
        estimatedMinutes = 12,
        steps = listOf(
            read("Drones estimate state from sensors.", "GPS supports position, compass supports heading, and IMU attitude supports stabilization."),
            LessonStep("Inject GPS drift.", "Noisy GPS makes assisted navigation wander.", StepAction.InjectFailure("gps_drift"), CompletionCheck.ActiveFailure("gps_drift")),
            LessonStep("Inject compass interference.", "Heading offset makes navigation less reliable.", StepAction.InjectFailure("compass_interference"), CompletionCheck.ActiveFailure("compass_interference")),
            read("Reset failures after the test.", "Always restore normal sensors before mission work."),
        ),
    ),
    Lesson(
        id = "failsafes",
        title = "7. Failsafes",
        description = "Inject failures and observe safe degradation.",
        estimatedMinutes = 15,
        steps = listOf(
            LessonStep("Load a demo mission.", "The Auto mission gives the aircraft an autonomous target.", StepAction.LoadMission, CompletionCheck.MissionLoaded),
            LessonStep("Start Auto.", "The drone follows the waypoint sequence.", StepAction.StartMission, CompletionCheck.DroneInMode(FlightMode.AUTO)),
            LessonStep("Inject GPS loss.", "Auto/Guided/Loiter degrade to Alt Hold when GPS is unavailable.", StepAction.InjectFailure("gps_loss"), CompletionCheck.ActiveFailure("gps_loss")),
            LessonStep("Inject fast battery drain.", "Low battery triggers RTL.", StepAction.InjectFailure("battery_low"), CompletionCheck.ActiveFailure("battery_low")),
        ),
    ),
)

private fun read(instruction: String, detail: String): LessonStep {
    return LessonStep(
        instruction = instruction,
        detail = detail,
        action = StepAction.ReadOnly,
        completionCheck = CompletionCheck.Manual,
    )
}
