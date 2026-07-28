package com.ascend.mavlab.feature.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = OnboardingPages[pageIndex]
    val isFirst = pageIndex == 0
    val isLast = pageIndex == OnboardingPages.lastIndex
    val context = LocalContext.current

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 12.dp),
        ) {
            ProgressRail(current = pageIndex, total = OnboardingPages.size)

            Spacer(modifier = Modifier.height(28.dp))

            if (isFirst) {
                Text(
                    text = "MAVLab · by Ascend Labs",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = page.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Visual occupies the flexible middle band and stays vertically centered,
            // so it adapts to any device height instead of squashing like a raster.
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                OnboardingVisualContent(
                    visual = page.visual,
                    onOpenQGroundControl = {
                        // QGroundControl for Android isn't on the Play Store — send users to the
                        // official download-and-install docs to grab the APK.
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "https://docs.qgroundcontrol.com/master/en/qgc-user-guide/" +
                                    "getting_started/download_and_install.html#android",
                            ),
                        )
                        runCatching { context.startActivity(intent) }
                    },
                )
            }

            NavButtons(
                isFirst = isFirst,
                isLast = isLast,
                primaryLabel = page.primaryAction,
                onBack = { pageIndex-- },
                onPrimary = { if (isLast) onComplete() else pageIndex++ },
            )

            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (!isLast) {
                    TextButton(onClick = onComplete) {
                        Text(
                            text = "Skip for now",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun ProgressRail(current: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = if (index <= current) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {}
            }
        }
    }
}

@Composable
private fun NavButtons(
    isFirst: Boolean,
    isLast: Boolean,
    primaryLabel: String,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
) {
    if (isFirst) {
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(primaryLabel, fontWeight = FontWeight.SemiBold)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Text("Back")
            }
            Button(
                onClick = onPrimary,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Text(primaryLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
