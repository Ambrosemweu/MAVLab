package com.ascend.mavlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ascend.mavlab.core.ui.theme.MAVLabTheme
import com.ascend.mavlab.feature.navigation.MavLabAppShell

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MAVLabTheme {
                MavLabAppShell()
            }
        }
    }
}
