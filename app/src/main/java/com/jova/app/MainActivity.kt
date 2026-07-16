package com.jova.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var statusOverlay: TextView
    private lateinit var statusBattery: TextView
    private lateinit var statusNotification: TextView
    private lateinit var btnAction: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ТЕКСЕРУ: Егер бұрын crash жасалып, рұқсаттар берілсе — бірден іске қосып, экраннан жоғаламыз
        val prefs = getSharedPreferences("JovaAppPrefs", Context.MODE_PRIVATE)
        val hasCrashed = prefs.getBoolean("hasCrashed", false)

        if (hasCrashed && hasAllPermissions()) {
            startOverlayService()
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        statusOverlay = findViewById(R.id.status_overlay)
        statusBattery = findViewById(R.id.status_battery)
        statusNotification = findViewById(R.id.status_notification)
        btnAction = findViewById(R.id.btn_action)

        btnAction.setOnClickListener {
            handlePermissionsFlow()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun hasAllPermissions(): Boolean {
        val hasOverlay = Settings.canDrawOverlays(this)
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val hasBattery = pm.isIgnoringBatteryOptimizations(packageName)
        val hasNotification = if (Build.VERSION.SDK_INT >= 33) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return hasOverlay && hasBattery && hasNotification
    }

    private fun updateUI() {
        // 1. Overlay status
        if (Settings.canDrawOverlays(this)) {
            statusOverlay.text = "✅ Белсенді (Рұқсат берілген)"
            statusOverlay.setTextColor(Color.parseColor("#00E676"))
        } else {
            statusOverlay.text = "❌ Рұқсат берілмеген"
            statusOverlay.setTextColor(Color.parseColor("#FF5252"))
        }

        // 2. Battery status
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            statusBattery.text = "✅ Белсенді (Оңтайландыру өшірілген)"
            statusBattery.setTextColor(Color.parseColor("#00E676"))
        } else {
            statusBattery.text = "❌ Рұқсат берілмеген"
            statusBattery.setTextColor(Color.parseColor("#FF5252"))
        }

        // 3. Notification status
        val hasNotification = if (Build.VERSION.SDK_INT >= 33) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasNotification) {
            statusNotification.text = "✅ Белсенді (Рұқсат берілген)"
            statusNotification.setTextColor(Color.parseColor("#00E676"))
        } else {
            statusNotification.text = "❌ Рұқсат берілмеген"
            statusNotification.setTextColor(Color.parseColor("#FF5252"))
        }

        // Басты батырма мәтіні
        if (hasAllPermissions()) {
            btnAction.text = "Іске қосу (Test Crash)"
            btnAction.setBackgroundColor(Color.parseColor("#2979FF"))
            btnAction.setTextColor(Color.WHITE)
        } else {
            btnAction.text = "Рұқсаттарды белсендіру"
            btnAction.setBackgroundColor(Color.parseColor("#00E676"))
            btnAction.setTextColor(Color.BLACK)
        }
    }

    private fun handlePermissionsFlow() {
        // Кезекпен-кезек рұқсат сұрау терезелеріне бағыттау
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
            return
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            return
        }

        // Егер бәрі дайын болса - алғашқы сынақтық crash жібереміз
        val prefs = getSharedPreferences("JovaAppPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("hasCrashed", true).apply()

        startOverlayService()
        throw RuntimeException("All permissions granted - INITIAL CRASH TEST SUCCESSFUL")
    }

    private fun startOverlayService() {
        val overlayIntent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(overlayIntent)
        } else {
            startService(overlayIntent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            updateUI()
        }
    }
}
