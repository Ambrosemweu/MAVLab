package com.ascend.mavlab.feature.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = OnboardingPages[pageIndex]
    val isLast = pageIndex == OnboardingPages.lastIndex
    val context = LocalContext.current

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "MAVLab",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Drone education simulator",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(text = page.title, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = page.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        )
                        FeatureRow(pageIndex)
                        if (pageIndex == 2) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=org.mavlink.qgroundcontrol"),
                                    )
                                    runCatching { context.startActivity(intent) }
                                },
                            ) {
                                Text("Open QGroundControl")
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OnboardingPages.indices.forEach { index ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp),
                            color = if (index <= pageIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = MaterialTheme.shapes.small,
                        ) {}
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (pageIndex > 0) {
                        OutlinedButton(
                            onClick = { pageIndex-- },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Button(
                        onClick = {
                            if (isLast) {
                                onComplete()
                            } else {
                                pageIndex++
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(page.primaryAction)
                    }
                }
                OutlinedButton(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Skip for now")
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(pageIndex: Int) {
    val labels = when (pageIndex) {
        0 -> listOf("Offline", "MAVLink", "Lessons")
        1 -> listOf("Physics", "Tilt control", "3D")
        2 -> listOf("QGC", "UDP", "Telemetry")
        else -> listOf("Dashboard", "Labs", "Missions")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEach { label ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
