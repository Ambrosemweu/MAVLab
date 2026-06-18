package com.ascend.mavlab

import android.content.Intent
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
        ContextCompat.startForegroundService(
            this,
            Intent(this, SimulationService::class.java),
        )
        setContent {
            MAVLabTheme {
                MavLabAppShell()
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            stopService(Intent(this, SimulationService::class.java))
        }
        super.onDestroy()
    }
}
