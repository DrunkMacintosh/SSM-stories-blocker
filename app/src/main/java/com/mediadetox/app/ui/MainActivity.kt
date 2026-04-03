package com.mediadetox.app.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mediadetox.app.databinding.ActivityMainBinding
import com.mediadetox.app.service.AppMonitorService
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
                AppMonitorService.start(this)
            } else {
                AppMonitorService.stop(this)
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
