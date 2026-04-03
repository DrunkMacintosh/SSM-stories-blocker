package com.mediadetox.app.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mediadetox.app.databinding.ActivityMainBinding
import com.mediadetox.app.service.AppMonitorService
import com.mediadetox.app.util.BatteryOptimizationHelper
import com.mediadetox.app.util.PrefsHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupPermissionButtons()
        setupMonitoringToggle()
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    // --- Permission setup ---

    private fun setupPermissionButtons() {
        binding.layoutPermissionSetup.btnGrantUsage.setOnClickListener {
            PermissionHelper.openUsageAccessSettings(this)
        }
        binding.layoutPermissionSetup.btnGrantOverlay.setOnClickListener {
            PermissionHelper.openOverlaySettings(this)
        }
        binding.layoutPermissionSetup.btnContinue.setOnClickListener {
            showDashboard()
        }
    }

    // --- Monitoring toggle ---

    private fun setupMonitoringToggle() {
        binding.switchMonitoring.setOnCheckedChangeListener { _, isChecked ->
            PrefsHelper.setMonitoringEnabled(this, isChecked)
            if (isChecked) {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, AppMonitorService::class.java).apply {
                        action = AppMonitorService.ACTION_START
                    }
                )
                verifyServiceStarted()
            } else {
                startService(
                    Intent(this, AppMonitorService::class.java).apply {
                        action = AppMonitorService.ACTION_STOP
                    }
                )
            }
            updateProtectionStatus(isChecked)
        }
    }

    // --- UI routing ---

    private fun refreshUI() {
        val hasUsage = PermissionHelper.hasUsageStatsPermission(this)
        val hasOverlay = PermissionHelper.hasOverlayPermission(this)

        if (hasUsage && hasOverlay) {
            showDashboard()
        } else {
            showPermissionSetup(hasUsage, hasOverlay)
        }
    }

    private fun showPermissionSetup(hasUsage: Boolean, hasOverlay: Boolean) {
        binding.layoutPermissionSetup.root.visibility = View.VISIBLE
        binding.layoutDashboard.visibility = View.GONE

        val setup = binding.layoutPermissionSetup

        if (hasUsage) {
            setup.textUsageStatus.text = "\u2713 Granted"
            setup.textUsageStatus.setTextColor(0xFF4CAF50.toInt())
        } else {
            setup.textUsageStatus.text = "\u2717 Not granted"
            setup.textUsageStatus.setTextColor(0xFFF44336.toInt())
        }

        if (hasOverlay) {
            setup.textOverlayStatus.text = "\u2713 Granted"
            setup.textOverlayStatus.setTextColor(0xFF4CAF50.toInt())
        } else {
            setup.textOverlayStatus.text = "\u2717 Not granted"
            setup.textOverlayStatus.setTextColor(0xFFF44336.toInt())
        }

        setup.btnContinue.isEnabled = hasUsage && hasOverlay
    }

    private fun showDashboard() {
        binding.layoutPermissionSetup.root.visibility = View.GONE
        binding.layoutDashboard.visibility = View.VISIBLE

        // Sync toggle state with persisted preference without firing the listener
        val enabled = PrefsHelper.isMonitoringEnabled(this)
        binding.switchMonitoring.setOnCheckedChangeListener(null)
        binding.switchMonitoring.isChecked = enabled
        setupMonitoringToggle()
        updateProtectionStatus(enabled)

        checkBatteryOptimization()
    }

    private fun checkBatteryOptimization() {
        if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            AlertDialog.Builder(this)
                .setTitle("One more thing")
                .setMessage(
                    "Samsung will kill Media Detox in the background unless you disable " +
                    "battery optimization for this app. This is required for it to work."
                )
                .setCancelable(false)
                .setPositiveButton("Fix it now") { _, _ ->
                    BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
                }
                .show()
        }
    }

    private fun verifyServiceStarted() {
        Handler(Looper.getMainLooper()).postDelayed({
            val manager = getSystemService(
                Context.ACTIVITY_SERVICE
            ) as ActivityManager

            val running = manager.getRunningServices(100)
                .any { it.service.className ==
                    AppMonitorService::class.java.name }

            Log.d("MediaDetox", "Service running: $running")

            if (!running) {
                Log.e("MediaDetox", "SERVICE FAILED TO START")
                Toast.makeText(
                    this,
                    "Service failed to start - check permissions",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Log.d("MediaDetox", "SERVICE IS ALIVE")
                Toast.makeText(
                    this,
                    "Protection active",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, 2000L)
    }

    private fun updateProtectionStatus(enabled: Boolean) {
        if (enabled) {
            binding.textProtectionStatus.text = "Protection: ACTIVE"
            binding.textProtectionStatus.setTextColor(0xFF4CAF50.toInt())
        } else {
            binding.textProtectionStatus.text = "Protection: OFF"
            binding.textProtectionStatus.setTextColor(0xFFF44336.toInt())
        }
    }
}
