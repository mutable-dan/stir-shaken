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
                else -> "UNKNOWN STATUS ($status)"
            }

            // Extract normal data
            val phoneNumber = callDetails.handle?.schemeSpecificPart ?: "Unknown Number"
            val timestamp = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date())

            // Extract raw detail data
            val debugDump = """
                Stir/Shaken Status: $status
                Caller Name: ${callDetails.callerDisplayName ?: "N/A"}
                Handle URI: ${callDetails.handle}
                Call Properties: ${callDetails.callProperties}
                Call Capabilities: ${callDetails.callCapabilities}
                Timestamp (ms): ${callDetails.creationTimeMillis}
            """.trimIndent()

            // Format with a delimiter to separate visible log from debug data
            val logEntry = "[$timestamp] $phoneNumber\nResult: $statusString\n==DEBUG==\n$debugDump"

            val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val maxLogs = prefs.getInt("MAX_LOGS", 50)

            // Save to file using ==ENTRY== as the separator between different calls
            try {
                val file = File(filesDir, "call_log.txt")
                val existingLogs = if (file.exists()) {
                    file.readText().split("==ENTRY==").filter { it.isNotBlank() }
                } else {
                    emptyList()
                }

                val updatedLogs = listOf(logEntry) + existingLogs
                val trimmedLogs = updatedLogs.take(maxLogs)

                file.writeText(trimmedLogs.joinToString("==ENTRY=="))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // UI Triggers
            if (prefs.getBoolean("USE_POPUP", true) && Settings.canDrawOverlays(this)) showPopup(statusString)
            if (prefs.getBoolean("USE_NOTIFICATION", true)) showNotification(statusString)
        }

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