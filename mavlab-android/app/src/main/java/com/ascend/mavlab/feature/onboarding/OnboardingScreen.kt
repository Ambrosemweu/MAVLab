package com.ascend.mavlab.feature.onboarding

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ascend.mavlab.core.ui.navigation.*

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    swipeEnabled: Boolean = true,
) {
    // Restore the committed step, never a half-drag. The existing app motion
    // model drives both button and swipe transitions without competing sliders.
    var restoredStep by rememberSaveable { mutableIntStateOf(0) }
    val motion = rememberScreenSwipeMotion(restoredStep, OnboardingPages.size)
    val areas = remember { ScreenSwipeAreas() }
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val gestures = WindowInsets.systemGestures
    val edgeGuard = maxOf(gestures.getLeft(density, direction), gestures.getRight(density, direction),
        with(density) { 24.dp.toPx().toInt() }).toFloat()
    val context = LocalContext.current
    var linkError by remember { mutableStateOf<String?>(null) }
    val index = motion.selectedPage
    val page = OnboardingPages[index]
    val busy by remember(motion) { derivedStateOf {
        motion.pager.isScrollInProgress || kotlin.math.abs(motion.offset) > .5f
    } }
    fun moveTo(target: Int) {
        motion.select(target)
        restoredStep = motion.selectedPage
    }
    BackHandler(enabled = index > 0) { moveTo(index - 1) }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 12.dp)) {
                ProgressRail(index, OnboardingPages.size)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${index + 1} / ${OnboardingPages.size}", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onComplete, modifier = Modifier.testTag("onboarding-skip")) {
                        Text("Skip for now", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            CompositionLocalProvider(LocalScreenSwipeAreas provides areas) {
              Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().testTag("onboarding-pager")
                .screenSwipeArea().screenSwipeNavigation(areas, swipeEnabled, index, OnboardingPages.size,
                    edgeGuard, onDrag = motion::drag, onDragEnd = motion::finish, onPage = ::moveTo)) {
              ScreenSwipePager(motion) { pageIndex, _, _ ->
                val item = OnboardingPages[pageIndex]
                BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
                    // Natural-height visuals can scroll on short displays and at
                    // large font scales, while the action dock stays reachable.
                    Column(Modifier.verticalScroll(rememberScrollState()).fillMaxWidth()
                        .heightIn(min = maxHeight).padding(horizontal = 24.dp, vertical = 12.dp)) {
                        if (pageIndex == 0) {
                            Text("MAVLab · by Ascend Labs", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(10.dp))
                        }
                        Text(item.title, style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.height(10.dp))
                        Text(item.body, style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                            OnboardingVisualContent(item.visual, onOpenQGroundControl = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                                    "https://docs.qgroundcontrol.com/master/en/qgc-user-guide/" +
                                        "getting_started/download_and_install.html#android"))
                                runCatching { context.startActivity(intent) }.onFailure {
                                    linkError = "No browser available. Visit docs.qgroundcontrol.com for QGroundControl downloads."
                                }
                            })
                        }
                        Text(item.previewLabel, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                    }
                }
              }
              }
            }
            linkError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (index > 0) OutlinedButton(
                    onClick = { moveTo(index - 1) }, enabled = !busy,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("onboarding-back"),
                ) { Text("Back") }
                Button(
                    onClick = { if (index == OnboardingPages.lastIndex) onComplete() else moveTo(index + 1) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("onboarding-next"),
                ) { Text(page.primaryAction, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun ProgressRail(current: Int, total: Int) {
    Row(Modifier.fillMaxWidth().semantics { contentDescription = "Onboarding step ${current + 1} of $total" },
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { index ->
            Surface(Modifier.weight(1f).height(5.dp), shape = CircleShape,
                color = if (index <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {}
        }
    }
}
