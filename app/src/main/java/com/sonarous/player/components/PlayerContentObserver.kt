package com.sonarous.player.components

import android.database.ContentObserver
import android.os.Handler

class PlayerContentObserver(
    handler: Handler,
    private val onChanged: () -> Unit
) : ContentObserver(handler) {
    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        onChanged()
    }
}