package com.sonarous.player.components

import android.content.Context
import android.widget.Toast
import androidx.annotation.OptIn
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
        viewModel.isPlaying = !viewModel.isPlaying
    }

    @OptIn(UnstableApi::class)
    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int
    ) {
        super.onMediaItemTransition(mediaItem, reason)
        if (mediaController == null) { return }

        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            viewModel.incrementSongIterator(1)
        } else if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
            if (mediaController.currentMediaItemIndex > viewModel.songIndex) {
                viewModel.incrementSongIterator(1)
            } else {
                viewModel.incrementSongIterator(-1)
            }
        }
    }
    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Toast.makeText(applicationContext, "Unknown error occurred", Toast.LENGTH_LONG).show()
    }
}