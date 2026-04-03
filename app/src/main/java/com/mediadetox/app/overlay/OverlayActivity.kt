package com.mediadetox.app.overlay

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OverlayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: Overlay UI goes here (Stage 3)
        // val blockedPackage = intent.getStringExtra("blocked_package")

        val label = TextView(this).apply {
            text = "OVERLAY COMING IN STAGE 3"
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
        }
        setContentView(label)
        window.decorView.setBackgroundColor(Color.BLACK)
    }
}
