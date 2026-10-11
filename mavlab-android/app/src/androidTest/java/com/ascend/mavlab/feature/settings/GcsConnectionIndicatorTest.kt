package com.ascend.mavlab.feature.settings

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.ascend.mavlab.core.ui.theme.MAVLabTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GcsConnectionIndicatorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun labelAndDotFollowConnectionIncludingDisconnect() {
        val connected = mutableStateOf(false)
        compose.setContent { MAVLabTheme { GcsConnectionIndicator(connected.value) } }
        assertStatus("GCS disconnected", Color(0xFFEF4444))
        compose.runOnIdle { connected.value = true }
        assertStatus("GCS connected", Color(0xFF34D399))
        compose.runOnIdle { connected.value = false }
        assertStatus("GCS disconnected", Color(0xFFEF4444))
    }

    private fun assertStatus(label: String, color: Color) {
        compose.onNodeWithText(label).assertIsDisplayed()
        val pixels = compose.onNodeWithTag("gcs-connection-dot", useUnmergedTree = true)
            .captureToImage().toPixelMap()
        assertEquals(color.toArgb(), pixels[pixels.width / 2, pixels.height / 2].toArgb())
    }
}
