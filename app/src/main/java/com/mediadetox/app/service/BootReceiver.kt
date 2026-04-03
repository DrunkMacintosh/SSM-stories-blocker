package com.mediadetox.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mediadetox.app.util.PrefsHelper

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        // Restart service on boot goes here
        if (PrefsHelper.isMonitoringEnabled(context)) {
            AppMonitorService.start(context)
        }
    }
}
