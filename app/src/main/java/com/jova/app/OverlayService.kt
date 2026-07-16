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

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: FrameLayout

    // Сағатты нақты уақытта жаңарту механизмі
    private val clockHandler = Handler(Looper.getMainLooper())
    private lateinit var clockRunnable: Runnable

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        val notification = createNotification()
        startForeground(101, notification)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Layout-ты XML-ден жүктеу
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_home, null) as FrameLayout

        // 1. НАҚТЫ УАҚЫТТАҒЫ САҒАТ (Live Clock)
        val clockView = overlayView.findViewById<TextView>(R.id.status_bar_clock)
        clockRunnable = object : Runnable {
            override fun run() {
                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                clockView.text = sdf.format(java.util.Date())
                clockHandler.postDelayed(this, 1000)
            }
        }
        clockHandler.post(clockRunnable)

        // 2. ИНТЕРФЕЙС ЭЛЕМЕНТТЕРІН БАЙЛАНЫСТЫРУ
        val desktopView = overlayView.findViewById<LinearLayout>(R.id.desktop_view)
        val appWindow = overlayView.findViewById<FrameLayout>(R.id.app_window)
        
        val mockTermux = overlayView.findViewById<LinearLayout>(R.id.mock_termux_layout)
        val mockSettings = overlayView.findViewById<ScrollView>(R.id.mock_settings_layout)
        val mockBrowser = overlayView.findViewById<LinearLayout>(R.id.mock_browser_layout)
        val mockJovaMod = overlayView.findViewById<LinearLayout>(R.id.mock_jovamod_layout)

        // Қосымшаларды ашу логикасы
        fun openApp(appId: String) {
            desktopView.visibility = View.GONE
            appWindow.visibility = View.VISIBLE

            mockTermux.visibility = View.GONE
            mockSettings.visibility = View.GONE
            mockBrowser.visibility = View.GONE
            mockJovaMod.visibility = View.GONE

            when (appId) {
                "termux" -> mockTermux.visibility = View.VISIBLE
                "settings" -> mockSettings.visibility = View.VISIBLE
                "browser" -> mockBrowser.visibility = View.VISIBLE
                "jovamod" -> mockJovaMod.visibility = View.VISIBLE
            }
        }

        // Жұмыс үстеліне қайту логикасы
        fun goHome() {
            appWindow.visibility = View.GONE
            desktopView.visibility = View.VISIBLE
        }

        // Жұмыс үстеліндегі иконкаларға басу
        overlayView.findViewById<LinearLayout>(R.id.app_termux).setOnClickListener { openApp("termux") }
        overlayView.findViewById<LinearLayout>(R.id.app_settings).setOnClickListener { openApp("settings") }
        overlayView.findViewById<LinearLayout>(R.id.app_browser).setOnClickListener { openApp("browser") }
        overlayView.findViewById<LinearLayout>(R.id.app_jovamod).setOnClickListener { openApp("jovamod") }

        // Астыңғы Навигация батырмалары
        overlayView.findViewById<ImageButton>(R.id.nav_home).setOnClickListener { goHome() }
        overlayView.findViewById<ImageButton>(R.id.nav_back).setOnClickListener { goHome() }
        overlayView.findViewById<ImageButton>(R.id.nav_exit).setOnClickListener { stopSelf() }

        // 3. ИНТЕРАКТИВТІ TERMUX ТЕРМИНАЛЫНЫҢ ЖҰМЫСЫ
        val termuxInput = overlayView.findViewById<EditText>(R.id.termux_input)
        val termuxOutput = overlayView.findViewById<TextView>(R.id.termux_output)
        val termuxSend = overlayView.findViewById<Button>(R.id.termux_send)
        val termuxScroll = overlayView.findViewById<ScrollView>(R.id.termux_scroll)

        var terminalHistory = "Welcome to Termux Emulator\nType 'help' to see all commands.\n\n$ "

        termuxSend.setOnClickListener {
            val command = termuxInput.text.toString().trim()
            termuxInput.setText("")
            if (command.isEmpty()) return@setOnClickListener

            var result = ""
            when (command.lowercase()) {
                "help" -> {
                    result = "Қолжетімді командалар:\n  help      - Осы көмек терезесін шығару\n  ls        - Файлдар тізімін көрсету\n  neofetch  - Виртуалды жүйе мәліметтерін шығару\n  clear     - Экранды толық тазарту\n  hack      - Виртуалды хакерлік шабуылды қосу"
                }
                "ls" -> {
                    result = "app/\nbuild.gradle.kts\nsettings.gradle.kts\nlocal.properties\ngradle.properties"
                }
                "neofetch" -> {
                    result = """
       _                 _ 
      | | ___  _   _  __| |
   _  | |/ _ \| | | |/ _` |
  | |_| | (_) | |_| | (_| |
   \___/ \___/ \__,_|\__,_|
                           
  OS: JovaOS 1.0 (Android 16 Emulator)
  Kernel: 6.1.15-jova-arm64
  Shell: bash 5.2.15
  CPU: Snapdragon 8 Gen 5 (8 Cores)
  Memory: 12180MiB / 16384MiB (74%)
                    """.trimIndent()
                }
                "clear" -> {
                    terminalHistory = "$ "
                    termuxOutput.text = terminalHistory
                    return@setOnClickListener
                }
                "hack" -> {
                    result = "[*] Хакерлік шабуыл басталды...\n[+] Порттар сканерленуде...\n[+] com.jova.app бағдарламасына рұқсат алынды!\n[SUCCESS] Виртуалды деректер жүктелді."
                }
                else -> {
                    result = "sh: command not found: $command"
                }
            }

            terminalHistory += "$command\n$result\n\n$ "
            termuxOutput.text = terminalHistory

            // Терминал логын автоматты түрде төмен айналдыру
            termuxScroll.post {
                termuxScroll.fullScroll(View.FOCUS_DOWN)
            }
        }

        // 4. ОВЕРЛЕЙДІ БҮКІЛ ЭКРАНҒА ОРНАТУ (Status Bar мен Навигацияны жабу)
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

        windowManager.addView(overlayView, params)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "jova_overlay_channel",
                "Jova66 Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фондық оверлей қызметінің арнасы"
            }
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
            .setContentTitle("Jova66 Белсенді")
            .setContentText("Фондық оверлей жұмыс істеп тұр")
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
