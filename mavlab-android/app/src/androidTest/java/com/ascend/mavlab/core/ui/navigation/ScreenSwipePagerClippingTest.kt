package com.ascend.mavlab.core.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScreenSwipePagerClippingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun neighbouringCanvasCannotDrawAcrossTheSettledPageBoundary() {
        compose.setContent {
            val motion = rememberScreenSwipeMotion()
            Box(Modifier.size(300.dp, 200.dp).testTag("pager")) {
                ScreenSwipePager(motion) { page, _, _ ->
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        if (page == 1) Canvas(Modifier.fillMaxSize().padding(18.dp)) {
                            // Like SIM's first grid line: negative drawing coordinates
                            // can escape the padded canvas into the preceding page.
                            val x = -30.dp.toPx()
                            drawLine(Color.Red, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
                        }
                    }
                }
            }
        }
        val pixels = compose.onNodeWithTag("pager").captureToImage().toPixelMap()
        for (x in pixels.width - 50 until pixels.width) {
            assertEquals("Offscreen SIM-style grid must not bleed into Control at x=$x",
                Color.Black.toArgb(), pixels[x, pixels.height / 2].toArgb())
        }
    }
}
