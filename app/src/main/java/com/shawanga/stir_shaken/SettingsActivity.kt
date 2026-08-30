package com.shawanga.stir_shaken

import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val btnRequestRole = Button(this).apply { text = "1. Enable Caller ID Role" }
        val btnRequestOverlay = Button(this).apply { text = "2. Enable Pop-up Permission" }
        val btnRequestNotif = Button(this).apply { text = "3. Enable Notifications (Android 13+)" }

        val cbPopup = CheckBox(this).apply { text = "Show Pop-up on Call" }
        val cbNotif = CheckBox(this).apply { text = "Show Notification on Call" }

        val maxLogsLabel = TextView(this).apply {
            text = "\nMax logs to keep:"
            setPadding(0, 30, 0, 0)
        }
        val maxLogsInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val btnClearLog = Button(this).apply { text = "Clear Call Log" }

        layout.addView(btnRequestRole)
        layout.addView(btnRequestOverlay)
        layout.addView(btnRequestNotif)
        layout.addView(cbPopup)
        layout.addView(cbNotif)
        layout.addView(maxLogsLabel)
        layout.addView(maxLogsInput)
        layout.addView(btnClearLog)

        setContentView(layout)

        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        cbPopup.isChecked = prefs.getBoolean("USE_POPUP", true)
        cbNotif.isChecked = prefs.getBoolean("USE_NOTIFICATION", true)
        maxLogsInput.setText(prefs.getInt("MAX_LOGS", 50).toString())

        cbPopup.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("USE_POPUP", isChecked).apply() }
        cbNotif.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("USE_NOTIFICATION", isChecked).apply() }

        maxLogsInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString()?.toIntOrNull() ?: 50
                prefs.edit().putInt("MAX_LOGS", value).apply()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnClearLog.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Call Log")
                .setMessage("Are you sure you want to delete all call logs? This cannot be undone.")
                .setPositiveButton("Yes") { _, _ ->
                    File(filesDir, "call_log.txt").delete()
                    Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null) // Does nothing, just dismisses the dialog
                .show()
        }

        // Permissions logic
        val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) Toast.makeText(this, "Role Granted", Toast.LENGTH_SHORT).show()
        }
        btnRequestRole.setOnClickListener {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
            }
        }
        btnRequestOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else { Toast.makeText(this, "Overlay granted", Toast.LENGTH_SHORT).show() }
        }
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
        btnRequestNotif.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
