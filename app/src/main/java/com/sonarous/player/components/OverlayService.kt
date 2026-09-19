@file:OptIn(UnstableApi::class)

package com.sonarous.player.components

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.sonarous.player.SongInfo
import com.sonarous.player.getSongInfo
import com.sonarous.player.screens.OverlayScreen

class OverlayService : LifecycleService(), SavedStateRegistryOwner {
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager

    private var overlayView: ComposeView? = null

    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private lateinit var mediaController: MediaController
    private lateinit var songs: List<SongInfo>
    private lateinit var albumArtworks: Map<String, ImageBitmap>
    private val viewModel = PlayerViewModel()
    private var connectedMediaController by mutableStateOf(false)

    override fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)

        super.onCreate()
        createNotificationChannel()

        setMediaController()

        // Fill albums
        getSongInfo(this).apply {
            val tmpAlbumMap = mutableMapOf<String, ImageBitmap>()
            second.forEach { (album, art) ->

                tmpAlbumMap[album] = art
            }
            albumArtworks = tmpAlbumMap
        }


        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        if (overlayView == null) {
            createOverlay()
        }

        return START_STICKY
    }

    fun updateActivityPlayingIndex(index: Int) {
        val intent = Intent(ACTION_UPDATE_INDEX).apply {
            setPackage(packageName)
            putExtra(EXTRA_INDEX, index)
        }
        sendBroadcast(intent)
    }

    private fun setMediaController() {
        try {
            controllerFuture = MediaController.Builder(
                this,
                SessionToken(
                    this,
                    ComponentName(this, PlayerService::class.java)
                )
            ).buildAsync()
            controllerFuture.addListener(
                {
                    mediaController = controllerFuture.get()
                    connectedMediaController = true
                },
                MoreExecutors.directExecutor()
            )
        } catch (_: java.util.concurrent.CancellationException) {
            Log.e("SonarousLogs", "Get media session coroutine was cancelled")
        }
    }

    private fun createOverlay() {
        val composeView = ComposeView(this)

        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)
        composeView.setContent {
            if (connectedMediaController) {
                // May not be an accurate key
                songs = remember(mediaController.currentTimeline) { getSongs() }

                val overlayUi = remember(songs) { OverlayScreen(songs, mediaController, viewModel, this) }
                overlayUi.SongSelectOverlay()
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
        windowManager.addView(composeView, params)

        overlayView = composeView
    }

    private fun getSongs(): List<SongInfo> {
        val tmpSongs = mutableListOf<SongInfo>()

        for (i in 0 until mediaController.mediaItemCount) {
            mediaController.getMediaItemAt(i).mediaMetadata.apply {
                tmpSongs.add(
                    SongInfo(
                        title.toString(),
                        "",
                        Uri.parse(extras?.getString("MEDIA_URI")),
                        durationMs?.toFloat() ?: 0f,
                        artist.toString(),
                        albumTitle.toString(),
                        albumArtworks[albumTitle.toString()] ?: createBitmap(500, 500).asImageBitmap()
                    )
                )
            }
        }
        return tmpSongs
    }

    override fun onDestroy() {
        MediaController.releaseFuture(controllerFuture)
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {  }
        }

        overlayView = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Overlay Service",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        ).build()
//            .setContentTitle("UtilBar")
//            .setContentText("Overlay running")
//            .setSmallIcon(android.R.drawable.ic_dialog_info)
//            .build()
    }

    companion object {
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1002

        // Update index intent values
        const val ACTION_UPDATE_INDEX = "com.sonarous.player.UPDATE_INDEX"
        const val EXTRA_INDEX = "extra_index_value"
    }
}