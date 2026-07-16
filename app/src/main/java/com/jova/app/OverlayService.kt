package com.jova.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.util.zip.ZipFile

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: FrameLayout
    private val clockHandler = Handler(Looper.getMainLooper())
    private lateinit var clockRunnable: Runnable
    private var terminalHistory = "Welcome to Termux Emulator\nType 'help' for commands, 'unzip-test' to read gh.zip.\n\n$ "

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(101, createNotification())

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_home, null) as FrameLayout

        // 1. НАҚТЫ УАҚЫТ (Сағат)
        val clockView = overlayView.findViewById<TextView>(R.id.status_bar_clock)
        clockRunnable = object : Runnable {
            override fun run() {
                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                clockView.text = sdf.format(java.util.Date())
                clockHandler.postDelayed(this, 1000)
            }
        }
        clockHandler.post(clockRunnable)

        // 2. ИНТЕРФЕЙС ЭЛЕМЕНТТЕРІ
        val desktopView = overlayView.findViewById<LinearLayout>(R.id.desktop_view)
        val appWindow = overlayView.findViewById<FrameLayout>(R.id.app_window)
        
        val mockTermux = overlayView.findViewById<LinearLayout>(R.id.mock_termux_layout)
        val mockSettings = overlayView.findViewById<ScrollView>(R.id.mock_settings_layout)
        val mockRom = overlayView.findViewById<LinearLayout>(R.id.mock_rom_layout)
        val mockBrowser = overlayView.findViewById<LinearLayout>(R.id.mock_browser_layout)
        val mockJovaMod = overlayView.findViewById<LinearLayout>(R.id.mock_jovamod_layout)

        fun openApp(appId: String) {
            desktopView.visibility = View.GONE
            appWindow.visibility = View.VISIBLE

            mockTermux.visibility = View.GONE
            mockSettings.visibility = View.GONE
            mockRom.visibility = View.GONE
            mockBrowser.visibility = View.GONE
            mockJovaMod.visibility = View.GONE

            when (appId) {
                "termux" -> mockTermux.visibility = View.VISIBLE
                "settings" -> mockSettings.visibility = View.VISIBLE
                "rom" -> mockRom.visibility = View.VISIBLE
                "browser" -> mockBrowser.visibility = View.VISIBLE
                "jovamod" -> mockJovaMod.visibility = View.VISIBLE
            }
        }

        fun goHome() {
            appWindow.visibility = View.GONE
            desktopView.visibility = View.VISIBLE
        }

        // Батырмаларды байланыстыру
        overlayView.findViewById<LinearLayout>(R.id.app_termux).setOnClickListener { openApp("termux") }
        overlayView.findViewById<LinearLayout>(R.id.app_settings).setOnClickListener { openApp("settings") }
        overlayView.findViewById<LinearLayout>(R.id.app_rom).setOnClickListener { openApp("rom") }
        overlayView.findViewById<LinearLayout>(R.id.app_browser).setOnClickListener { openApp("browser") }
        overlayView.findViewById<LinearLayout>(R.id.app_jovamod).setOnClickListener { openApp("jovamod") }

        overlayView.findViewById<ImageButton>(R.id.nav_home).setOnClickListener { goHome() }
        overlayView.findViewById<ImageButton>(R.id.nav_back).setOnClickListener { goHome() }
        overlayView.findViewById<ImageButton>(R.id.nav_exit).setOnClickListener { stopSelf() }

        // 3. ROM INSTALLER - ФОНДЫҚ АҒЫНДА ОҚУ
        val romInfoView = overlayView.findViewById<TextView>(R.id.rom_zip_info)
        val btnScanZip = overlayView.findViewById<Button>(R.id.btn_scan_zip)

        btnScanZip.setOnClickListener {
            romInfoView.text = "gh.zip оқылуда (бұл процесті қатырмайды)..."
            Thread {
                val zipPath = "/storage/emulated/0/gh.zip"
                val file = File(zipPath)
                val resultText = if (!file.exists()) {
                    "ҚАТЕ: gh.zip файлы табылмады!\nЖол: /storage/emulated/0/gh.zip"
                } else {
                    try {
                        val zipFile = ZipFile(file)
                        val entries = zipFile.entries()
                        val sb = StringBuilder()
                        sb.append("✅ gh.zip сәтті табылды!\n")
                        sb.append("Көлемі: ${String.format("%.2f", file.length().toDouble() / (1024 * 1024))} MB\n")
                        sb.append("Файлдар саны: ${zipFile.size()}\n\n")
                        sb.append("Құрылымы (алғашқы 8):\n")
                        
                        var count = 0
                        while (entries.hasMoreElements() && count < 8) {
                            val entry = entries.nextElement()
                            sb.append(" 📄 ${entry.name}\n")
                            count++
                        }
                        zipFile.close()
                        sb.toString()
                    } catch (e: Exception) {
                        "ZIP қатесі: ${e.localizedMessage}"
                    }
                }
                Handler(Looper.getMainLooper()).post {
                    romInfoView.text = resultText
                }
            }.start()
        }

        // 4. ИНТЕРАКТИВТІ TERMUX - ФОНДЫҚ АҒЫНМЕН
        val termuxInput = overlayView.findViewById<EditText>(R.id.termux_input)
        val termuxOutput = overlayView.findViewById<TextView>(R.id.termux_output)
        val termuxSend = overlayView.findViewById<Button>(R.id.termux_send)
        val termuxScroll = overlayView.findViewById<ScrollView>(R.id.termux_scroll)

        termuxSend.setOnClickListener {
            val command = termuxInput.text.toString().trim()
            termuxInput.setText("")
            if (command.isEmpty()) return@setOnClickListener

            if (command.lowercase() == "unzip-test") {
                // Экран қатып қалмас үшін уақытша хабарлама шығарып, фондық ағынды қосамыз
                terminalHistory += "unzip-test\n[*] gh.zip оқылуда, сәл күте тұрыңыз...\n\n$ "
                termuxOutput.text = terminalHistory
                termuxScroll.post { termuxScroll.fullScroll(View.FOCUS_DOWN) }

                Thread {
                    val file = File("/storage/emulated/0/gh.zip")
                    val asyncResult = if (!file.exists()) {
                        "Қате: /storage/emulated/0/gh.zip файлы табылмады!"
                    } else {
                        try {
                            val zipFile = ZipFile(file)
                            val list = zipFile.entries().asSequence().take(8).map { " -> ${it.name}" }.joinToString("\n")
                            val res = "gh.zip табылды! Көлемі: ${file.length() / 1024 / 1024}MB\nІшіндегі файлдар:\n$list\n ... барлығы ${zipFile.size()} файл."
                            zipFile.close()
                            res
                        } catch (e: Exception) {
                            "Қате: ${e.localizedMessage}"
                        }
                    }

                    // Нәтижені негізгі ағынға қайтару
                    Handler(Looper.getMainLooper()).post {
                        // Күту хабарламасын нақты нәтижемен алмастыру
                        terminalHistory = terminalHistory.substringBefore("[*] gh.zip") + asyncResult + "\n\n$ "
                        termuxOutput.text = terminalHistory
                        termuxScroll.post { termuxScroll.fullScroll(View.FOCUS_DOWN) }
                    }
                }.start()
                return@setOnClickListener
            }

            // Қалған жылдам командалар негізгі ағында орындала береді
            var result = ""
            when (command.lowercase()) {
                "help" -> {
                    result = "Командалар:\n  help        - Көмек терезесі\n  ls          - Файлдар тізімі\n  unzip-test  - ZIP файлын оқу (қауіпсіз)\n  clear       - Экранды тазарту"
                }
                "ls" -> result = "app/\nbuild.gradle.kts\nsettings.gradle.kts"
                "clear" -> {
                    terminalHistory = "$ "
                    termuxOutput.text = terminalHistory
                    return@setOnClickListener
                }
                else -> result = "sh: command not found: $command"
            }

            terminalHistory += "$command\n$result\n\n$ "
            termuxOutput.text = terminalHistory
            termuxScroll.post { termuxScroll.fullScroll(View.FOCUS_DOWN) }
        }

        // Оверлей параметрлері
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        termuxInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            } else {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            windowManager.updateViewLayout(overlayView, params)
        }

        windowManager.addView(overlayView, params)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "jova_overlay_channel",
                "Jova66 Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "jova_overlay_channel")
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("JovaOS Белсенді")
            .setContentText("ZIP оқу жүйесі іске қосылды")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacks(clockRunnable)
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
