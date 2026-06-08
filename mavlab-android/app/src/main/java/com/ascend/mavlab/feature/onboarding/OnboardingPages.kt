package com.ascend.mavlab.feature.onboarding

data class OnboardingPage(
    val title: String,
    val body: String,
    val primaryAction: String,
)

val OnboardingPages = listOf(
    OnboardingPage(
        title = "Welcome to MAVLab",
        body = "Learn drone systems with your phone. MAVLab runs an offline quadcopter simulator, controller, telemetry dashboard, and MAVLink endpoint.",
        primaryAction = "Get started",
    ),
    OnboardingPage(
        title = "Your phone is the drone",
        body = "Tilt controls, simulated physics, MAVLink telemetry, missions, failures, and 3D visualization run inside this app.",
        primaryAction = "Next",
    ),
    OnboardingPage(
        title = "Connect QGroundControl",
        body = "Open QGroundControl on the same phone or Wi-Fi network. MAVLab broadcasts MAVLink telemetry so QGC can discover it automatically.",
        primaryAction = "Next",
    ),
    OnboardingPage(
        title = "Ready to fly",
        body = "Start with Lessons, then use Dashboard, Controller, 3D View, and Labs to explore the full simulator.",
        primaryAction = "Start flying",
    ),
)
