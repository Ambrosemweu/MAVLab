package com.ascend.mavlab.core.ui.navigation

import android.os.Build
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.ascend.mavlab.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LaunchThemeTest {
    @Test fun nativeStartingWindowMatchesDarkComposeBackground() {
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_MAVLab)
        val attributes = context.obtainStyledAttributes(intArrayOf(
            android.R.attr.windowBackground,
            android.R.attr.windowLightStatusBar,
            android.R.attr.windowLightNavigationBar,
        ))
        try {
            assertEquals(0xFF0A0E1A.toInt(), attributes.getColor(0, 0))
            assertFalse(attributes.getBoolean(1, true))
            if (Build.VERSION.SDK_INT >= 27) assertFalse(attributes.getBoolean(2, true))
        } finally { attributes.recycle() }
    }

    @Test fun android12SplashBackgroundMatchesAppWindow() {
        if (Build.VERSION.SDK_INT < 31) return
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_MAVLab)
        val attributes = context.obtainStyledAttributes(intArrayOf(android.R.attr.windowSplashScreenBackground))
        try { assertEquals(0xFF0A0E1A.toInt(), attributes.getColor(0, 0)) }
        finally { attributes.recycle() }
    }
}
