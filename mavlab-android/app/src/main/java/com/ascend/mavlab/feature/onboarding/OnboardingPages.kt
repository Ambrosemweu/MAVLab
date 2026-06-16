package com.ascend.mavlab.feature.onboarding

data class OnboardingPage(
    val title: String,
    val body: String,
    val primaryAction: String,
    val chips: List<String>,
    val showQGroundControlAction: Boolean = false,
)

val OnboardingPages = listOf(
    OnboardingPage(
        title = "What is MAVLab?",
        body = "MAVLab by Ascend Labs is a phone-based drone digital twin and training platform. It simulates a quadcopter, broadcasts MAVLink telemetry, and connects to Ground Control Station software.",
        primaryAction = "Get started",
        chips = listOf("Ascend Labs", "Digital twin", "Training"),
    ),
    OnboardingPage(
        title = "What is a drone digital twin?",
        body = "The app simulates drone state, sensors, telemetry, mission behavior, failures, and flight logs from one shared runtime.",
        primaryAction = "Next",
        chips = listOf("Physics", "Sensors", "Telemetry"),
    ),
    OnboardingPage(
        title = "Understand the app surfaces",
        body = "Cockpit is live operations, Controller is local/manual control, Mission is autonomous route execution, SIM is physical behavior visualization, and Ops is diagnostics and logs.",
        primaryAction = "Next",
        chips = listOf("Cockpit", "Controller", "Mission", "SIM", "Ops"),
    ),
    OnboardingPage(
        title = "Connect QGroundControl",
        body = "Use split-screen on the same phone, or run QGroundControl from a desktop on the same Wi-Fi network. MAVLab broadcasts MAVLink telemetry for QGC discovery.",
        primaryAction = "Next",
        chips = listOf("QGC", "MAVLink", "UDP"),
        showQGroundControlAction = true,
    ),
    OnboardingPage(
        title = "First simulated takeoff",
        body = "Use Cockpit or Controller to arm, take off, land, and watch the same DroneState update every surface.",
        primaryAction = "Next",
        chips = listOf("Arm", "Takeoff", "Land"),
    ),
    OnboardingPage(
        title = "Try phone tilt control",
        body = "Controller can map calibrated phone tilt into roll, pitch, throttle, and yaw. Custom sliders remain available as a manual fallback.",
        primaryAction = "Next",
        chips = listOf("Tilt", "Calibrate", "Fallback"),
    ),
    OnboardingPage(
        title = "Run a basic mission",
        body = "Load a route in Mission or upload one from QGroundControl, start AUTO, then track waypoint progress and control authority.",
        primaryAction = "Next",
        chips = listOf("Route", "AUTO", "Waypoints"),
    ),
    OnboardingPage(
        title = "Inject a simple failure",
        body = "Use advanced test inputs for GPS loss, wind drift, motor failure, or low-battery style scenarios while Cockpit and SIM show the impact.",
        primaryAction = "Next",
        chips = listOf("GPS loss", "Wind", "Recovery"),
    ),
    OnboardingPage(
        title = "Review and export flight",
        body = "Ops shows local flight sessions, telemetry CSV paths, event logs, mission snapshots, and the export/review area for v1.5.",
        primaryAction = "Start training",
        chips = listOf("Logs", "CSV", "Report"),
    ),
)
