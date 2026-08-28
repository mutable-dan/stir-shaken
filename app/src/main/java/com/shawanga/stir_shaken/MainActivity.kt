package com.shawanga.stir_shaken


import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val btnRequestRole = Button(this).apply { text = "1. Enable Caller ID Role" }
        val btnRequestOverlay = Button(this).apply { text = "2. Enable Pop-up Permission" }
        val btnRequestNotif = Button(this).apply { text = "3. Enable Notifications (Android 13+)" } // NEW

        val cbPopup = CheckBox(this).apply { text = "Show Pop-up on Call" }
        val cbNotif = CheckBox(this).apply { text = "Show Notification on Call" }

        layout.addView(btnRequestRole)
        layout.addView(btnRequestOverlay)
        layout.addView(btnRequestNotif) // NEW
        layout.addView(cbPopup)
        layout.addView(cbNotif)
        setContentView(layout)

        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        cbPopup.isChecked = prefs.getBoolean("USE_POPUP", true)
        cbNotif.isChecked = prefs.getBoolean("USE_NOTIFICATION", true)

        cbPopup.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("USE_POPUP", isChecked).apply()
        }
        cbNotif.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("USE_NOTIFICATION", isChecked).apply()
        }

        // 1. Request Call Screening Role
        val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) Toast.makeText(this, "Role Granted", Toast.LENGTH_SHORT).show()
        }
        btnRequestRole.setOnClickListener {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
            }
        }

        // 2. Request Overlay Permission
        btnRequestOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else {
                Toast.makeText(this, "Overlay already granted", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Request Notification Permission (NEW)
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            val msg = if (isGranted) "Notifications Granted" else "Notifications Denied"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        btnRequestNotif.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Toast.makeText(this, "Not required on this Android version", Toast.LENGTH_SHORT).show()
            }
        }
    }

}


