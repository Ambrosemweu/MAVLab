package com.ascend.mavlab.feature.onboarding

/** Selects which app-accurate visual an onboarding page renders below its copy. */
enum class OnboardingVisual {
    DroneHero,
    PfdTelemetry,
    SurfaceGallery,
    QgcLink,
    ArmTakeoffLand,
    TiltPad,
    MissionPlot,
    FailurePanel,
    TelemetryChart,
}

data class OnboardingPage(
    val title: String,
    val body: String,
    val primaryAction: String,
    val visual: OnboardingVisual,
    val showQGroundControlAction: Boolean = false,
)

val OnboardingPages = listOf(
    OnboardingPage(
        title = "What is MAVLab?",
        body = "A phone-based drone digital twin — physics, telemetry, and a 3D model, fully offline.",
        primaryAction = "Get started",
        visual = OnboardingVisual.DroneHero,
    ),
    OnboardingPage(
        title = "One shared runtime",
        body = "State, sensors, telemetry, missions and failures — all from one simulated drone.",
        primaryAction = "Next",
        visual = OnboardingVisual.PfdTelemetry,
    ),
    OnboardingPage(
        title = "Five surfaces, one drone",
        body = "Cockpit, Controller, SIM, Mission and Ops — swipe to explore.",
        primaryAction = "Next",
        visual = OnboardingVisual.SurfaceGallery,
    ),
    OnboardingPage(
        title = "Connect a ground station",
        body = "Same device or same Wi-Fi — MAVLab broadcasts MAVLink for QGroundControl or any GCS to discover.",
        primaryAction = "Next",
        visual = OnboardingVisual.QgcLink,
        showQGroundControlAction = true,
    ),
    OnboardingPage(
        title = "Your first takeoff",
        body = "Arm, take off, land — and watch every surface update together.",
        primaryAction = "Next",
        visual = OnboardingVisual.ArmTakeoffLand,
    ),
    OnboardingPage(
        title = "Fly with phone tilt",
        body = "Calibrated phone tilt maps to roll, pitch, throttle and yaw — sliders as fallback.",
        primaryAction = "Next",
        visual = OnboardingVisual.TiltPad,
    ),
    OnboardingPage(
        title = "Run a mission",
        body = "Upload a route, fly AUTO, and track waypoint progress.",
        primaryAction = "Next",
        visual = OnboardingVisual.MissionPlot,
    ),
    OnboardingPage(
        title = "Test failures safely",
        body = "Inject GPS loss, wind, or motor faults — practice recovery, nothing to crash.",
        primaryAction = "Next",
        visual = OnboardingVisual.FailurePanel,
    ),
    OnboardingPage(
        title = "Review and export",
        body = "Every flight logs to CSV and a shareable report you can export.",
        primaryAction = "Start flying",
        visual = OnboardingVisual.TelemetryChart,
    ),
)
