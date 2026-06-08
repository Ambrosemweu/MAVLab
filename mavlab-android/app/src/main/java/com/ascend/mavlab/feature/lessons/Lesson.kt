package com.ascend.mavlab.feature.lessons

import com.ascend.mavlab.simulation.engine.FlightMode

data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    val estimatedMinutes: Int,
    val steps: List<LessonStep>,
)

data class LessonStep(
    val instruction: String,
    val detail: String,
    val action: StepAction,
    val completionCheck: CompletionCheck,
    val hint: String? = null,
)

sealed interface StepAction {
    data object ReadOnly : StepAction
    data object ArmDrone : StepAction
    data class ChangeMode(val mode: FlightMode) : StepAction
    data class Takeoff(val altitude: Float) : StepAction
    data class InjectFailure(val failureId: String) : StepAction
    data object LoadMission : StepAction
    data object StartMission : StepAction
    data object LandDrone : StepAction
}

sealed interface CompletionCheck {
    data object Manual : CompletionCheck
    data object DroneArmed : CompletionCheck
    data object DroneDisarmed : CompletionCheck
    data class DroneInMode(val mode: FlightMode) : CompletionCheck
    data class AltitudeAbove(val meters: Float) : CompletionCheck
    data class AltitudeBelow(val meters: Float) : CompletionCheck
    data class ActiveFailure(val failureId: String) : CompletionCheck
    data object MissionLoaded : CompletionCheck
    data object DroneOnGround : CompletionCheck
}
