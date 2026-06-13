package com.ascend.mavlab.core.common

import android.content.Context
import com.ascend.mavlab.simulation.mission.MissionItem
import com.ascend.mavlab.simulation.mission.MissionSnapshotCodec

object MissionPersistence {
    fun load(context: Context): List<MissionItem> {
        val snapshot = prefs(context).getString(KeyMissionItems, null)
        return MissionSnapshotCodec.decode(snapshot)
    }

    fun save(context: Context, items: List<MissionItem>) {
        if (items.isEmpty()) {
            clear(context)
            return
        }
        prefs(context)
            .edit()
            .putString(KeyMissionItems, MissionSnapshotCodec.encode(items))
            .apply()
    }

    fun clear(context: Context) {
        prefs(context)
            .edit()
            .remove(KeyMissionItems)
            .apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)

    private const val PrefsName = "mavlab_mission"
    private const val KeyMissionItems = "mission_items"
}
