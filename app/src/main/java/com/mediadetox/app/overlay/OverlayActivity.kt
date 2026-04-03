package com.mediadetox.app.overlay

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.graphics.Color
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
    private var pulseAnimator: ObjectAnimator? = null

    private var blockedPackage: String? = null

    companion object {
        private const val TAG = "MediaDetox"
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val MINUTES_PER_OPEN = 3

        @Volatile
        var isShowing: Boolean = false
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        super.onCreate(savedInstanceState)
        isShowing = true
        Log.d(TAG, "MediaDetox - Overlay opened")
        binding = ActivityOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        blockedPackage = intent.getStringExtra("blocked_package")
        repository = BlockRepository(AppDatabase.getInstance(this))

        loadStats()
        setupDmBypassButton()
        setupHoldUnlockButton()
        playEntryAnimation()

        // Prevent activity from being finished by back stack animation
        overridePendingTransition(0, 0)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MediaDetox - Overlay resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MediaDetox - Overlay paused")
    }

    override fun onDestroy() {
        super.onDestroy()
        isShowing = false
        Log.d(TAG, "MediaDetox - Overlay destroyed")
        cancelHoldCountdown()
        pulseAnimator?.cancel()
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

    // --- Entry animation ---

    private fun playEntryAnimation() {
        // Title: invisible + scaled up slightly → fade in + scale to 1.0
        binding.tvShameHeader.apply {
            alpha = 0f
            scaleX = 1.1f
            scaleY = 1.1f
        }

        // Stats: invisible, will fade in after title
        binding.tvOpenCount.alpha = 0f
        binding.tvTimeWasted.alpha = 0f

        // Buttons group: start 80dp below final position, invisible
        val offsetPx = (80 * resources.displayMetrics.density).toInt().toFloat()
        binding.btnDmBypass.apply { alpha = 0f; translationY = offsetPx }
        binding.btnHoldUnlock.apply { alpha = 0f; translationY = offsetPx }
        binding.tvUnlockHint.apply { alpha = 0f; translationY = offsetPx }

        // Step 1: title fades in and scales down to 1.0 over 400ms
        binding.tvShameHeader.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .withEndAction {
                // Step 2: stats fade in over 300ms, 200ms after title finishes
                binding.tvOpenCount.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
                binding.tvTimeWasted.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(80)
                    .withEndAction {
                        // Step 3: buttons slide up and fade in over 300ms
                        binding.btnDmBypass.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(300)
                            .start()
                        binding.btnHoldUnlock.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(300)
                            .setStartDelay(60)
                            .withEndAction { startUnlockButtonPulse() }
                            .start()
                        binding.tvUnlockHint.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(300)
                            .setStartDelay(120)
                            .start()
                    }
                    .start()
            }
            .start()
    }

    // --- Pulse animation on unlock button ---

    private fun startUnlockButtonPulse() {
        pulseAnimator = ObjectAnimator.ofArgb(
            binding.btnHoldUnlock,
            "textColor",
            Color.parseColor("#FF3B30"),
            Color.parseColor("#880000")
        ).apply {
            duration = 2_000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            setEvaluator(ArgbEvaluator())
            start()
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
                    shakeUnlockButton()
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

    private fun shakeUnlockButton() {
        binding.btnHoldUnlock.animate()
            .translationX(16f).setDuration(50)
            .withEndAction {
                binding.btnHoldUnlock.animate()
                    .translationX(-16f).setDuration(50)
                    .withEndAction {
                        binding.btnHoldUnlock.animate()
                            .translationX(0f).setDuration(50)
                            .start()
                    }.start()
            }.start()
    }

    private fun onHoldComplete() {
        lifecycleScope.launch {
            repository.logCaved(blockedPackage ?: INSTAGRAM_PACKAGE)
        }
        shameFlashThenLaunch()
    }

    // --- Shame flash ---

    private fun shameFlashThenLaunch() {
        binding.root.setBackgroundColor(Color.parseColor("#FF3B30"))
        handler.postDelayed({
            val launchIntent = packageManager.getLaunchIntentForPackage(INSTAGRAM_PACKAGE)
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
            finish()
        }, 200)
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
