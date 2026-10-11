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
    val previewLabel: String = "Learning preview · not live state",
)

val OnboardingPages = listOf(
    OnboardingPage(
        title = "What is MAVLab?",
        body = "A phone-based drone digital twin. Explore physics, telemetry and a 3D simulated vehicle — offline.",
        primaryAction = "Get started",
        visual = OnboardingVisual.DroneHero,
        previewLabel = "Simulation only · no real aircraft",
    ),
    OnboardingPage(
        title = "One shared runtime",
        body = "One simulated drone powers every view. Your inputs, telemetry and 3D motion stay connected.",
        primaryAction = "Next",
        visual = OnboardingVisual.PfdTelemetry,
    ),
    OnboardingPage(
        title = "Four surfaces, one drone",
        body = "Cockpit to understand. Control to fly. SIM to see. Ops to diagnose. Swipe or tap the tabs.",
        primaryAction = "Next",
        visual = OnboardingVisual.SurfaceGallery,
        previewLabel = "Four views · one simulated vehicle",
    ),
    OnboardingPage(
        title = "Connect a ground station",
        body = "QGroundControl is optional. Connect on this phone or the same Wi-Fi; check the GCS link in Ops.",
        primaryAction = "Next",
        visual = OnboardingVisual.QgcLink,
        showQGroundControlAction = true,
    ),
    OnboardingPage(
        title = "Fly with phone tilt",
        body = "Calibrate in Control before arming. Tilt to steer, rotate for yaw, and use the throttle slider to adjust height after takeoff.",
        primaryAction = "Next",
        visual = OnboardingVisual.TiltPad,
        previewLabel = "Demo sliders · no flight commands",
    ),
    OnboardingPage(
        title = "Your first takeoff",
        body = "Arm, then Takeoff. Land before Disarm. Watch the same flight in Cockpit and SIM.",
        primaryAction = "Next",
        visual = OnboardingVisual.ArmTakeoffLand,
    ),
    OnboardingPage(
        title = "Run a mission",
        body = "Upload from QGC. Start AUTO in Control, follow the received route in SIM, and inspect upload evidence in Ops.",
        primaryAction = "Next",
        visual = OnboardingVisual.MissionPlot,
        previewLabel = "Example route · not uploaded",
    ),
    OnboardingPage(
        title = "Test failures safely",
        body = "Choose a failure exercise in Ops. Observe its effects, recover in Control, then reset the injected fault in Ops.",
        primaryAction = "Next",
        visual = OnboardingVisual.FailurePanel,
    ),
    OnboardingPage(
        title = "Review and export",
        body = "Recording is off by default. Begin in Ops, or opt in to recording on arm. Stop and save, then review or export.",
        primaryAction = "Start flying",
        visual = OnboardingVisual.TelemetryChart,
        previewLabel = "Example recording · enable capture in Ops",
    ),
)
