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
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import com.sonarous.player.components.PlayerViewModel
import com.sonarous.player.R

@ExperimentalFoundationApi
@Composable
fun SongQueue(viewModel: PlayerViewModel, mediaController: MediaController?) {
    val fetchStrategy = LazyListPrefetchStrategy(50)
    val lazyColumnState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0,
        initialFirstVisibleItemScrollOffset = 0,
        prefetchStrategy = fetchStrategy
    )
    val lazyListSize = viewModel.queuedSongs.count()
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
            state = lazyColumnState,
        ) {
            items(lazyListSize) { i ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(75.dp)
                        .border(
                            width = (
                                    if (i == viewModel.songIndex) {
                                        0.dp
                                    } else {
                                        (-1).dp
                                    }
                                    ),
                            color = viewModel.iconColor,
                            shape = RoundedCornerShape(corner = CornerSize(10.dp))
                        )
                        .padding(5.dp)
                        .clickable(
                            onClick = {
                                mediaController?.clearMediaItems()
                                for (j in 0 until viewModel.queuedSongs.count()) {
                                    mediaController?.addMediaItem(MediaItem.fromUri(viewModel.queuedSongs[j].songUri))
                                }
                                mediaController?.prepare()
                                mediaController?.seekTo(i, 0L)
                                mediaController?.play()
                                viewModel.updateSongDuration((viewModel.queuedSongs[i].time).toLong())
                                viewModel.songIndex = i
                                viewModel.playingFromSongsScreen = true
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    AlbumCover(viewModel.queuedSongs[i], 60.dp)
                    Spacer(
                        modifier = Modifier
                            .width(10.dp)
                    )
                    SongTextColumn(viewModel.queuedSongs[i], viewModel)
                    RemoveFromQueue(viewModel, mediaController, i)
                }
            }
        }
        ScrollBar(lazyColumnState, viewModel, lazyListSize.toFloat(), 10.toFloat())
    }
}

@Composable
fun RemoveFromQueue(viewModel: PlayerViewModel, mediaController: MediaController?, index: Int) {
    IconButton(
        onClick = {
            mediaController?.removeMediaItem(index)
            viewModel.queuedSongs.removeAt(index)
        },
        modifier = Modifier.size(36.dp),
        colors = IconButtonDefaults.iconButtonColors(contentColor = viewModel.iconColor)
    ) {
        Icon(
            painter = painterResource(R.drawable.remove_from_queue),
            "Remove song from queue"
        )
    }
}
