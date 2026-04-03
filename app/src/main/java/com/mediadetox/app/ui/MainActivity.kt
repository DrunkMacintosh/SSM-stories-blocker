package com.mediadetox.app.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mediadetox.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupPermissionButtons()
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

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
    }
}
