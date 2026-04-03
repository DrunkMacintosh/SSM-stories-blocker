package com.mediadetox.app.util

import android.content.Context

object PrefsHelper {

    private const val PREFS_NAME = "media_detox_prefs"
    private const val KEY_MONITORING = "monitoring_enabled"

    fun isMonitoringEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MONITORING, false)
    }

    fun setMonitoringEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MONITORING, enabled)
            .apply()
    }
}
