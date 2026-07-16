package com.jova.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: FrameLayout

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        val notification = createNotification()
        startForeground(101, notification)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Басты контейнер - Күңгірт қара түсті фон
        overlayView = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#FA121212")) // 98% мөлдір емес қара түс
        }

        // Элементтерді вертикалды орналастыруға арналған LinearLayout
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        // ТҮЗЕТУ: Жүйелік галерея суретін (бейне ретінде) қосу
        val imageView = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_gallery) // Жүйелік дайын сурет белгішесі
            layoutParams = LinearLayout.LayoutParams(250, 250).apply {
                bottomMargin = 50
            }
        }
        contentLayout.addView(imageView)

        // Мәтін жазу
        val textView = TextView(this).apply {
            text = "Jova66 Beta\nФондық режим толық белсенді!"
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 80
            }
        }
        contentLayout.addView(textView)

        // Жабу батырмасы
        val closeBtn = Button(this).apply {
            text = "Жабу (Exit)"
            setBackgroundColor(Color.parseColor("#FF5252"))
            setTextColor(Color.WHITE)
            setOnClickListener { stopSelf() }
        }
        contentLayout.addView(closeBtn)

        // Ортаға бағыттау
        val contentParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        overlayView.addView(contentLayout, contentParams)

        // ТОЛЫҚ ЭКРАН ТҮЗЕТУІ: Оверлей жоғарғы статус бар мен төменгі батырмаларды қоса толық жауып тұрады
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
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
