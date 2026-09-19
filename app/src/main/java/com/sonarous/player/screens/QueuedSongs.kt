package com.sonarous.player.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import com.sonarous.player.R
import com.sonarous.player.ScrollBar
import com.sonarous.player.components.PlayerViewModel

@ExperimentalFoundationApi
@Composable
fun SongQueue(viewModel: PlayerViewModel, mediaController: MediaController?) {
    val lazyListSize = viewModel.queuedSongs.count()
    val mediaItemList by remember (viewModel.queuedSongs) {
        derivedStateOf {
            getMediaItemList(viewModel.queuedSongs)
        }
    }

    val playSongCallback = remember (viewModel.queuedSongs) {
        { i: Int ->
            mediaController?.clearMediaItems()
            mediaController?.addMediaItems(mediaItemList)

            mediaController?.prepare()
            mediaController?.seekTo(i, 0L)
            mediaController?.play()
            viewModel.updateSongDuration((viewModel.queuedSongs[i].duration).toLong())
            viewModel.songIndex = i
            viewModel.playingFromSongsScreen = true
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(viewModel.backgroundColor)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.955f),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            state = viewModel.queuedSongsLazyColumnState,
        ) {
            items(lazyListSize) { i ->
                QueuedSongRow(viewModel, i, mediaController, playSongCallback)
            }
        }
        ScrollBar(viewModel.queuedSongsLazyColumnState, viewModel, lazyListSize.toFloat(), 10.toFloat())
    }
}

@Composable
fun QueuedSongRow(viewModel: PlayerViewModel, i: Int, mediaController: MediaController?, playedSongCallback: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp)
            .border(
                width = 0.dp,
                color = if (i == viewModel.songIndex) viewModel.iconColor else Color.Transparent,
                shape = RoundedCornerShape(corner = CornerSize(10.dp))
            )
            .padding(5.dp)
            .clickable(onClick = { playedSongCallback(i) }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(0.77f)) {
            AlbumCover(viewModel.queuedSongs[i], 60.dp)
            Spacer(modifier = Modifier.width(10.dp))
            SongTextColumn(viewModel.queuedSongs[i], viewModel)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SongDurationText(viewModel.queuedSongs[i], viewModel)
            Spacer(modifier = Modifier.width(5.dp))
            RemoveFromQueue(viewModel, mediaController, i)
        }
    }
}

@Composable
fun RemoveFromQueue(viewModel: PlayerViewModel, mediaController: MediaController?, index: Int) {
    IconButton(
        onClick = {
            mediaController?.removeMediaItem(index)
            viewModel.queuedSongs.removeAt(index)
        },
        modifier = Modifier.size(24.dp),
        colors = IconButtonDefaults.iconButtonColors(contentColor = viewModel.iconColor)
    ) {
        Icon(
            painter = painterResource(R.drawable.remove_from_queue),
            "Remove song from queue"
        )
    }
}
