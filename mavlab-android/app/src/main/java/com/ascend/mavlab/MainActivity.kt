package com.ascend.mavlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import com.ascend.mavlab.core.ui.theme.MAVLabTheme
import com.ascend.mavlab.feature.navigation.MavLabAppShell
import com.ascend.mavlab.service.SimulationService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MAVLabTheme {
                MavLabAppShell()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.startForegroundService(
            this,
            SimulationService.appVisibleIntent(this),
        )
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            if (isFinishing) {
                stopService(SimulationService.stopIntent(this))
            } else {
                startService(SimulationService.appBackgroundedIntent(this))
            }
        }
        super.onStop()
    }
}
