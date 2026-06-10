package com.ascend.mavlab.simulation.engine

enum class ControlAuthority(val displayName: String) {
    IDLE("Idle"),
    CONTROLLER("Controller"),
    GCS_DIRECT("GCS Direct"),
    GCS_MISSION("GCS Mission"),
}
