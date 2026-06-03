package com.ascend.mavlab.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Phase 0 placeholder only.
 *
 * Phase 1 will convert this into the foreground service that owns the MAVLink
 * protocol loop. No MAVLink, UDP, physics, or sensors run in Phase 0.
 */
class SimulationService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
