package com.ascend.mavlab.feature.lessons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ascend.mavlab.core.common.AppRuntime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonScreen(modifier: Modifier = Modifier) {
    val droneState by AppRuntime.state.collectAsState()
    val failures by AppRuntime.failures.collectAsState()
    val mission by AppRuntime.missionProgress.collectAsState()
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
    var stepIndex by remember { mutableIntStateOf(0) }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Lessons", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Structured flight-systems curriculum with simulator actions and completion checks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LessonCatalog.forEach { lesson ->
                    FilterChip(
                        selected = selectedLesson?.id == lesson.id,
                        onClick = {
                            selectedLesson = lesson
                            stepIndex = 0
                        },
                        label = { Text(lesson.title.substringAfter(". ")) },
                    )
                }
            }

            val lesson = selectedLesson ?: LessonCatalog.first()
            val step = lesson.steps[stepIndex]
            val complete = LessonEngine.isComplete(step.completionCheck, droneState, failures, mission)

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(lesson.title, style = MaterialTheme.typography.titleLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${lesson.estimatedMinutes} min",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Step ${stepIndex + 1}/${lesson.steps.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (stepIndex + 1).toFloat() / lesson.steps.size },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(step.instruction, style = MaterialTheme.typography.titleMedium)
                    Text(
                        step.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                    )
                    step.hint?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    Text(
                        text = statusText(complete),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (complete) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                    )
                    Button(
                        onClick = { LessonEngine.perform(step.action) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(actionLabel(step.action))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { if (stepIndex > 0) stepIndex-- },
                            enabled = stepIndex > 0,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Back")
                        }
                        Button(
                            onClick = {
                                if (stepIndex < lesson.steps.lastIndex) {
                                    stepIndex++
                                }
                            },
                            enabled = complete || step.completionCheck == CompletionCheck.Manual,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (stepIndex == lesson.steps.lastIndex) "Done" else "Next")
                        }
                    }
                }
            }
        }
    }
}

private fun actionLabel(action: StepAction): String {
    return when (action) {
        StepAction.ReadOnly -> "Mark read"
        StepAction.ArmDrone -> "Arm"
        is StepAction.ChangeMode -> action.mode.displayName
        is StepAction.Takeoff -> "Takeoff"
        is StepAction.InjectFailure -> "Inject"
        StepAction.LoadMission -> "Load"
        StepAction.StartMission -> "Start"
        StepAction.LandDrone -> "Land"
    }
}

private fun statusText(complete: Boolean): String {
    return if (complete) "Completion detected" else "Action required"
}
