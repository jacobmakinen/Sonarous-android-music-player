package com.sonarous.player.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import com.sonarous.player.LcdText
import com.sonarous.player.components.PlayerViewModel
import com.sonarous.player.R
import com.sonarous.player.SongInfo

@Composable
fun MoreSongOptions(viewModel: PlayerViewModel, mediaController: MediaController?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x44000000)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Popup(
            onDismissRequest = {
                viewModel.showMoreSongOptions = !viewModel.showMoreSongOptions
            },
            properties = PopupProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                focusable = true
            ),
            alignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.55f)
                    .fillMaxWidth(0.65f)
                    .padding(horizontal = 5.dp)
                    .background(viewModel.backgroundColor)
                    .border(0.dp, viewModel.iconColor)
                    .padding(5.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                AddSongToQueue(viewModel, mediaController)
            }
        }
    }
}

@Composable
fun ReplicateAlbumArt(viewModel: PlayerViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clickable(
                onClick = {
                    viewModel
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            painter = painterResource(R.drawable.queue_music),
            contentDescription = "Add song to queue",
            tint = viewModel.iconColor,
        )
        LcdText("Add song to queue", viewModel = viewModel)
    }
}

@Composable
fun AddSongToQueue(viewModel: PlayerViewModel, mediaController: MediaController?) {
    val song = viewModel.moreOptionsSelectedSong
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clickable(
                onClick = { addSongToQueueLogic(mediaController, song, viewModel) }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            painter = painterResource(R.drawable.queue_music),
            contentDescription = "Add song to queue",
            tint = viewModel.iconColor,
        )
        LcdText("Add song to queue", viewModel = viewModel)
    }
}

fun addSongToQueueLogic(mediaController: MediaController?, song: SongInfo, viewModel: PlayerViewModel) {
    if (viewModel.queueingSongs) {
        mediaController?.addMediaItem(MediaItem.fromUri(song.songUri))
        viewModel.queuedSongs.add(song)
    } else {
        viewModel.queueingSongs = true
        // Remove all songs except the currently playing one
        mediaController?.removeMediaItems(
            viewModel.songIndex + 1,
            viewModel.queuedSongs.size
        )
        mediaController?.removeMediaItems(0, viewModel.songIndex)
        viewModel.queuedSongs.removeAll { song ->
            song != viewModel.queuedSongs[viewModel.songIndex]
        }
        viewModel.queuedSongs.add(song)
        mediaController?.addMediaItem(MediaItem.fromUri(song.songUri))
        viewModel.songIndex = 0
    }
    viewModel.showMoreSongOptions = false
}