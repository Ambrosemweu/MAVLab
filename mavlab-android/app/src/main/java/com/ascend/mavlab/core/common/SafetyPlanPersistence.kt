package com.ascend.mavlab.core.common

import android.content.Context
import com.ascend.mavlab.simulation.safety.*

object SafetyPlanPersistence {
    fun load(context: Context): SafetyPlans? = SafetyPlanCodec.decode(prefs(context).getString("snapshot_v1", null))
    fun save(context: Context, plans: SafetyPlans) {
        prefs(context).edit().putString("snapshot_v1", SafetyPlanCodec.encode(plans)).apply()
    }
    private fun prefs(context: Context) = context.getSharedPreferences("mavlab_safety", Context.MODE_PRIVATE)
}
