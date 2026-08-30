package com.shawanga.stir_shaken

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var logContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val btnSettings = Button(this).apply { text = "Open Settings" }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f
            )
        }

        logContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scrollView.addView(logContainer)
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
        logContainer.removeAllViews()

        val file = File(filesDir, "call_log.txt")
        if (!file.exists() || file.readText().isBlank()) {
            logContainer.addView(TextView(this).apply {
                text = "No previous calls logged."
                textSize = 16f
            })
            return
        }

        // Parse our formatted file
        val rawData = file.readText()
        val entries = if (rawData.contains("==ENTRY==")) {
            rawData.split("==ENTRY==")
        } else {
            rawData.split("\n\n")
        }

        for (entry in entries.filter { it.isNotBlank() }) {
            val parts = entry.split("==DEBUG==")
            val visibleText = parts[0].trim()
            val debugText = if (parts.size > 1) parts[1].trim() else "No advanced info available for this call."

            val itemView = TextView(this).apply {
                text = visibleText
                textSize = 16f
                setPadding(0, 30, 0, 30)

                // show the dialog when tapped
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Advanced Call Details")
                        .setMessage(debugText)
                        .setPositiveButton("Close", null)
                        .show()
                }
            }
            logContainer.addView(itemView)

            // Divider line
            val divider = android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
                setBackgroundColor(Color.LTGRAY)
            }
            logContainer.addView(divider)
        }
    }
}
