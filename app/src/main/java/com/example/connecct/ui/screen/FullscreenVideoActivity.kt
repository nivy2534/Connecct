package com.example.connecct.ui.screen

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.connecct.R

class FullscreenVideoActivity : ComponentActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force landscape fullscreen
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val uriString = intent.getStringExtra("VIDEO_URI")!!
        val uri = Uri.parse(uriString)

        // Root layout for overlay button
        val root = FrameLayout(this)

        // Player view
        playerView = PlayerView(this).apply {
            useController = true
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        root.addView(playerView)

        // --- BACK BUTTON OVERLAY ---
        val backButton = ImageButton(this).apply {
            setImageDrawable(ContextCompat.getDrawable(this@FullscreenVideoActivity, R.drawable.ic_back))
            background = null
            setOnClickListener {
                player.release()
                finish()
            }

            val size = (60 * resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                marginStart = 40
                topMargin = 40
            }
        }

        root.addView(backButton)

        setContentView(root)

        // Setup player
        player = ExoPlayer.Builder(this).build()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        playerView.player = player
        player.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}