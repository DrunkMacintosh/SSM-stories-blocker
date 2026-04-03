package com.mediadetox.app.overlay

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mediadetox.app.data.AppDatabase
import com.mediadetox.app.data.BlockRepository
import com.mediadetox.app.databinding.ActivityOverlayBinding
import kotlinx.coroutines.launch

class OverlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOverlayBinding
    private lateinit var repository: BlockRepository

    private val handler = Handler(Looper.getMainLooper())
    private val holdRunnables = mutableListOf<Runnable>()

    private var blockedPackage: String? = null

    companion object {
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val MINUTES_PER_OPEN = 3
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Full screen — must be set before setContentView
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        super.onCreate(savedInstanceState)
        binding = ActivityOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        blockedPackage = intent.getStringExtra("blocked_package")
        repository = BlockRepository(AppDatabase.getInstance(this))

        loadStats()
        setupDmBypassButton()
        setupHoldUnlockButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelHoldCountdown()
    }

    // --- Stats ---

    private fun loadStats() {
        lifecycleScope.launch {
            val openCount = repository.countBlockedToday()
            val minutesWasted = openCount * MINUTES_PER_OPEN
            val streak = repository.getCurrentStreak()

            binding.tvOpenCount.text =
                "You\u2019ve opened Instagram $openCount times today."
            binding.tvTimeWasted.text =
                "That\u2019s roughly $minutesWasted minutes of your life gone."
            binding.tvStreak.text = if (streak > 0) {
                "$streak day clean streak. Don\u2019t blow it."
            } else {
                "No streak. Start one today."
            }
        }
    }

    // --- DM bypass ---

    private fun setupDmBypassButton() {
        binding.btnDmBypass.setOnClickListener {
            lifecycleScope.launch {
                repository.logDmBypass(blockedPackage ?: INSTAGRAM_PACKAGE)
            }
            val dmIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.instagram.com/direct/inbox/")
            ).apply {
                setPackage(INSTAGRAM_PACKAGE)
            }
            startActivity(dmIntent)
            finish()
        }
    }

    // --- Hold-to-unlock ---

    @SuppressLint("ClickableViewAccessibility")
    private fun setupHoldUnlockButton() {
        binding.btnHoldUnlock.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startHoldCountdown()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cancelHoldCountdown()
                    binding.tvUnlockHint.text = "Hold for 3 seconds to unlock"
                    true
                }
                else -> false
            }
        }
    }

    private fun startHoldCountdown() {
        cancelHoldCountdown()
        binding.tvUnlockHint.text = "Hold... 3"

        val tick2 = Runnable { binding.tvUnlockHint.text = "Hold... 2" }
        val tick1 = Runnable { binding.tvUnlockHint.text = "Hold... 1" }
        val unlock = Runnable { onHoldComplete() }

        holdRunnables.addAll(listOf(tick2, tick1, unlock))
        handler.postDelayed(tick2, 1_000)
        handler.postDelayed(tick1, 2_000)
        handler.postDelayed(unlock, 3_000)
    }

    private fun cancelHoldCountdown() {
        holdRunnables.forEach { handler.removeCallbacks(it) }
        holdRunnables.clear()
    }

    private fun onHoldComplete() {
        lifecycleScope.launch {
            repository.logCaved(blockedPackage ?: INSTAGRAM_PACKAGE)
        }
        val launchIntent = packageManager.getLaunchIntentForPackage(INSTAGRAM_PACKAGE)
        if (launchIntent != null) {
            startActivity(launchIntent)
        }
        finish()
    }

    // --- Back button — go home, not back to Instagram ---

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        startActivity(homeIntent)
        finish()
    }
}
