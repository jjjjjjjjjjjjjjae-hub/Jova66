package com.jova.beta

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val videoView = findViewById<VideoView>(R.id.videoView)
        videoView.setVideoURI(Uri.parse("android.resource://" + packageName + "/" + R.raw.anim))
        videoView.setOnPreparedListener { it.isLooping = true; it.start() }

        // Рұқсат алу батырмасы (сенің талабың бойынша)
        findViewById<Button>(R.id.btn_permission).setOnClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, 100)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && Settings.canDrawOverlays(this)) {
            // Рұқсат алған соң crash/exit
            finishAffinity()
        }
    }
}
