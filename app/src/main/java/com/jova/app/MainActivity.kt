package com.jova.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
    }

    private fun checkPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            showPermissionDialog("Жоғардан көрсету рұқсатын беріңіз") {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
            return
        }

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            showPermissionDialog("Батареяны шектеусіз пайдалануға рұқсат беріңіз") {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            return
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            return
        }

        val prefs = getSharedPreferences("JovaAppPrefs", Context.MODE_PRIVATE)
        val hasCrashed = prefs.getBoolean("hasCrashed", false)

        if (!hasCrashed) {
            prefs.edit().putBoolean("hasCrashed", true).apply()
            throw RuntimeException("All permissions granted - TEST CRASH")
        } else {
            val overlayIntent = Intent(this, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(overlayIntent)
            } else {
                startService(overlayIntent)
            }
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            checkPermissions()
        }
    }

    private fun showPermissionDialog(text: String, action: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Рұқсат керек")
            .setMessage(text)
            .setCancelable(false)
            .setPositiveButton("Рұқсат беру") { _, _ -> action() }
            .setNegativeButton("Кейін") { dialog, _ -> dialog.dismiss(); finish() }
            .show()
    }
}
