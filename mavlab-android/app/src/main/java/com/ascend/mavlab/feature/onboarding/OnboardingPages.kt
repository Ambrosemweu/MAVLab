package com.ascend.mavlab.feature.onboarding

data class OnboardingPage(
    val title: String,
    val body: String,
    val primaryAction: String,
)

val OnboardingPages = listOf(
    OnboardingPage(
        title = "Welcome to MAVLab",
        body = "Run a phone-based drone digital twin. MAVLab simulates a quadcopter, broadcasts MAVLink telemetry, and connects to Ground Control Station software.",
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
        body = "Use Cockpit, Controller, Mission, SIM, and Ops to operate the simulator, test missions, inspect live vehicle state, and diagnose QGC connections.",
        primaryAction = "Start flying",
    ),
)
