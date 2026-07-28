package com.sonarous.player.components

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.media3.common.util.UnstableApi

@UnstableApi
class PlayerApp: Application() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            "audio_channel",
            "Audio notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}