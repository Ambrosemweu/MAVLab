package com.ascend.mavlab.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ascend.mavlab.core.common.AppRuntime

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val status by AppRuntime.status.collectAsState()
    val systemId by AppRuntime.systemId.collectAsState()

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            InfoCard(
                title = "MAVLink",
                body = "Status: $status\nSystem ID: $systemId\nQGroundControl should connect over UDP 14550 on the same phone or Wi-Fi network.",
            )
            InfoCard(
                title = "Troubleshooting",
                body = "If QGC does not connect, restart QGC, keep MAVLab open, and verify both devices are on the same network. If phone tilt is unavailable, use manual fallback controls in Controller.",
            )
            InfoCard(
                title = "Release QA",
                body = "Run onboarding, Lesson 1, demo mission, GPS loss, and QGC split-screen before tagging a release.",
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
            )
        }
    }
}
