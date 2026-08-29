package com.shawanga.stir_shaken

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StirShakenService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection == Call.Details.DIRECTION_INCOMING) {
            val status = callDetails.callerNumberVerificationStatus

            val statusString = when (status) {
                Connection.VERIFICATION_STATUS_PASSED -> "✅ PASSED (Attestation A)"
                Connection.VERIFICATION_STATUS_FAILED -> "❌ FAILED (Spoofed / C)"
                Connection.VERIFICATION_STATUS_NOT_VERIFIED -> "❓ NOT VERIFIED"
                else -> "UNKNOWN STATUS"
            }

            // 1. Get Phone Number & Preferences
            val phoneNumber = callDetails.handle?.schemeSpecificPart ?: "Unknown Number"
            val timestamp = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date())
            val logEntry = "[$timestamp] $phoneNumber\nResult: $statusString"

            val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val maxLogs = prefs.getInt("MAX_LOGS", 50)

            // 2. Enforce limit and save with newest at the top
            try {
                val file = File(filesDir, "call_log.txt")
                val existingLogs = if (file.exists()) {
                    file.readText().split("\n\n").filter { it.isNotBlank() }
                } else {
                    emptyList()
                }

                // Add new entry to the front, keep only up to maxLogs
                val updatedLogs = listOf(logEntry) + existingLogs
                val trimmedLogs = updatedLogs.take(maxLogs)

                // Rewrite file
                file.writeText(trimmedLogs.joinToString("\n\n") + "\n\n")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Show UI
            val usePopup = prefs.getBoolean("USE_POPUP", true)
            val useNotification = prefs.getBoolean("USE_NOTIFICATION", true)

            if (usePopup && Settings.canDrawOverlays(this)) showPopup(statusString)
            if (useNotification) showNotification(statusString)
        }

        // 4. Respond to the call
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)
    }

    private fun showNotification(message: String) {
        val channelId = "stir_shaken_channel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            channelId, "Call Verification", NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("STIR/SHAKEN Status")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    private fun showPopup(message: String) {
        Handler(Looper.getMainLooper()).post {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            val textView = TextView(this).apply {
                text = "\nSTIR/SHAKEN:\n$message\n(Tap to dismiss)\n"
                textSize = 20f
                setBackgroundColor(Color.parseColor("#EE000000"))
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(30, 30, 30, 30)
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP

            windowManager.addView(textView, params)

            textView.setOnClickListener {
                try { windowManager.removeView(textView) } catch (e: Exception) {}
            }

            Handler(Looper.getMainLooper()).postDelayed({
                try { windowManager.removeView(textView) } catch (e: Exception) {}
            }, 90000)
        }
    }
}