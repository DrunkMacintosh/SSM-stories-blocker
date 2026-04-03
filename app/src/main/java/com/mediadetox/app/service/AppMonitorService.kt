package com.mediadetox.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mediadetox.app.overlay.OverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppMonitorService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var monitoringJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val blockedApps = listOf("com.instagram.android")
    private var lastHeartbeatLog = 0L

    // --- Lifecycle ---

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // MUST be first — Android 12+ kills service if startForeground not called within 5s
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "Service started")

                // Prevent Samsung from killing service
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "MediaDetox::MonitorWakeLock"
                )
                @Suppress("WakelockTimeout")
                wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max

                startMonitoring()
                isRunning = true
            }
            ACTION_STOP -> {
                stopMonitor()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        serviceJob.cancel()
    }

    // --- Notification ---

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Media Detox Active",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitoring for Instagram"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Media Detox")
            .setContentText("Protecting you from the feed")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    // --- Monitor loop ---

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                if (now - lastHeartbeatLog >= 5_000L) {
                    Log.d(TAG, "MediaDetox - Still running, checking apps...")
                    lastHeartbeatLog = now
                }

                val foregroundApp = getForegroundApp(this@AppMonitorService)

                if (foregroundApp != null) {
                    Log.d(TAG, "MediaDetox - Current foreground: $foregroundApp")
                }

                if (foregroundApp in blockedApps) {
                    Log.d(TAG, "MediaDetox - INSTAGRAM DETECTED")
                    if (!OverlayActivity.isShowing) {
                        triggerOverlay(foregroundApp!!)
                        delay(3000L) // Wait 3 seconds before checking again
                    } else {
                        Log.d(TAG, "MediaDetox - Overlay already showing, skipping")
                        delay(500L)
                    }
                } else {
                    delay(500L) // Normal check interval
                }
            }
        }
    }

    private fun stopMonitor() {
        monitoringJob?.cancel()
        monitoringJob = null
        isRunning = false
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
    }

    // --- Usage stats ---

    private fun getForegroundApp(context: Context): String? {
        val usageStatsManager = context.getSystemService(
            Context.USAGE_STATS_SERVICE
        ) as UsageStatsManager

        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 10,
            time
        )

        return stats
            ?.filter { it.packageName != packageName }
            ?.filter { it.packageName != "com.mediadetox.app" }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }

    // --- Overlay trigger ---

    private fun triggerOverlay(packageName: String) {
        val intent = Intent(this, OverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_BLOCKED_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    // --- Companion ---

    companion object {
        const val ACTION_START = "com.mediadetox.app.START"
        const val ACTION_STOP = "com.mediadetox.app.STOP"
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"

        private const val TAG = "MediaDetox"
        private const val CHANNEL_ID = "media_detox_monitor"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
