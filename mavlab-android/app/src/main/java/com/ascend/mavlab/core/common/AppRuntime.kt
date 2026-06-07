package com.ascend.mavlab.core.common

import android.content.Context
import android.provider.Settings
import com.ascend.mavlab.core.mavlink.MavlinkSocketConfig
import com.ascend.mavlab.core.mavlink.MavlinkUdpServer
import com.ascend.mavlab.simulation.engine.SimpleSimLoop
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppRuntime {
    private val simLoop = SimpleSimLoop()
    private val fallbackStatus = MutableStateFlow("Stopped")
    private val mutableSystemId = MutableStateFlow(1)
    private var mavlinkServer: MavlinkUdpServer? = null

    val state = simLoop.state
    val status: StateFlow<String>
        get() = mavlinkServer?.status ?: fallbackStatus.asStateFlow()
    val systemId: StateFlow<Int> = mutableSystemId.asStateFlow()

    fun start(context: Context) {
        if (mavlinkServer == null) {
            val id = stableSystemId(context)
            mutableSystemId.value = id
            mavlinkServer = MavlinkUdpServer(
                simLoop = simLoop,
                config = MavlinkSocketConfig(systemId = id),
            )
        }
        simLoop.start()
        mavlinkServer?.start(context.applicationContext)
    }

    fun stop() {
        mavlinkServer?.stopNow()
        simLoop.stop()
    }

    private fun stableSystemId(context: Context): Int {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ).orEmpty()
        return (androidId.hashCode().absoluteValue % 250) + 1
    }
}
