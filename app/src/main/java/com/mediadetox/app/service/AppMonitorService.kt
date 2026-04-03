package com.mediadetox.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AppMonitorService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: Background monitor goes here
        return START_STICKY
    }
}
