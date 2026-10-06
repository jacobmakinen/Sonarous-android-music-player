package com.sonarous.player.components

import android.content.Context
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController

@OptIn(UnstableApi::class)
class PlayerListener(
    private val applicationContext: Context,
    private val viewModel: PlayerViewModel,
    private val mediaController: MediaController?
) : Player.Listener {
    @OptIn(UnstableApi::class)
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        viewModel.isPlaying = isPlaying
    }

    @OptIn(UnstableApi::class)
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        if (mediaController == null) return

        val index = mediaController.currentMediaItemIndex
        if (index != C.INDEX_UNSET) {
            viewModel.songIndex = index
        }
    }
    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Toast.makeText(applicationContext, "${error.message} >> ${error.errorCodeName}", Toast.LENGTH_LONG).show()
    }
}