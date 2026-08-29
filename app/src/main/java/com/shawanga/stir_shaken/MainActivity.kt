package com.shawanga.stir_shaken

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val btnSettings = Button(this).apply { text = "Open Settings" }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }

        logTextView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 30, 0, 0)
        }

        scrollView.addView(logTextView)

        layout.addView(btnSettings)
        layout.addView(scrollView)
        setContentView(layout)

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadCallLog()
    }

    private fun loadCallLog() {
        val file = File(filesDir, "call_log.txt")
        if (file.exists()) {
            val content = file.readText().trim()
            logTextView.text = if (content.isEmpty()) "No previous calls logged." else content
        } else {
            logTextView.text = "No previous calls logged."
        }
    }
}
