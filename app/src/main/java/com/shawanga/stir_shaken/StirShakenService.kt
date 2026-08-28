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

class StirShakenService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        // 1. Only process incoming calls
        if (callDetails.callDirection == Call.Details.DIRECTION_INCOMING) {
            val status = callDetails.callerNumberVerificationStatus

            val message = when (status) {
                Connection.VERIFICATION_STATUS_PASSED -> "✅ PASSED (Attestation A)"
                Connection.VERIFICATION_STATUS_FAILED -> "❌ FAILED (Spoofed / C)"
                Connection.VERIFICATION_STATUS_NOT_VERIFIED -> "❓ NOT VERIFIED (No Token)"
                else -> "UNKNOWN STATUS ($status)"
            }

            // Read preferences
            val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val usePopup = prefs.getBoolean("USE_POPUP", true)
            val useNotification = prefs.getBoolean("USE_NOTIFICATION", true)

            // Trigger UI
            if (usePopup && Settings.canDrawOverlays(this)) showPopup(message)
            if (useNotification) showNotification(message)
        }

        // 2. Respond to the call LAST so the service doesn't shut down prematurely
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

            // ADDED FLAG_SHOW_WHEN_LOCKED so it appears on the lock screen
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

            // Let the user tap the pop-up to remove it instantly
            textView.setOnClickListener {
                try { windowManager.removeView(textView) } catch (e: Exception) {}
            }

            // Increased removal time to 90 seconds to outlast the ringing
            Handler(Looper.getMainLooper()).postDelayed({
                try { windowManager.removeView(textView) } catch (e: Exception) {}
            }, 90000)
        }
    }

//    private fun showPopup(message: String) {
//        Handler(Looper.getMainLooper()).post {
//            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
//
//            // Setup an overlay text view
//            val textView = TextView(this).apply {
//                text = "\nSTIR/SHAKEN:\n$message\n"
//                textSize = 20f
//                setBackgroundColor(Color.parseColor("#EE000000"))
//                setTextColor(Color.WHITE)
//                gravity = Gravity.CENTER
//                setPadding(30, 30, 30, 30)
//            }
//
//            val params = WindowManager.LayoutParams(
//                WindowManager.LayoutParams.MATCH_PARENT,
//                WindowManager.LayoutParams.WRAP_CONTENT,
//                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
//                PixelFormat.TRANSLUCENT
//            )
//            params.gravity = Gravity.TOP
//
//            windowManager.addView(textView, params)
//
//            // Remove popup after 10 seconds
//            Handler(Looper.getMainLooper()).postDelayed({
//                try { windowManager.removeView(textView) } catch (e: Exception) {}
//            }, 10000)
//        }
//    }
}