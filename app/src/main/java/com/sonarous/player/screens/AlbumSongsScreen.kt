package com.sonarous.player.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavController
import com.sonarous.player.LargeLcdText
import com.sonarous.player.LcdText
import com.sonarous.player.components.PlayerViewModel
import com.sonarous.player.R
import com.sonarous.player.SongInfo

@ExperimentalFoundationApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumSongsScreen(album: String, songInfo: List<SongInfo>, mediaController: MediaController?, viewModel: PlayerViewModel, navController: NavController) {
    val albumSongsList = mutableListOf<SongInfo>()
    for (i in 0 until songInfo.count()) {
        if (songInfo[i].album == album) {
            albumSongsList.add(songInfo[i])
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(viewModel.backgroundColor)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 5.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                content = {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = "Back arrow"
                    )
                },
                onClick = {
                    navController.popBackStack()
                },
                colors = IconButtonColors(
                    contentColor = viewModel.iconColor,
                    containerColor = Color.Transparent,
                    disabledContentColor = viewModel.iconColor,
                    disabledContainerColor = Color.Transparent
                )
            )
            Spacer(
                modifier = Modifier
                    .width(5.dp)
            )
            LargeLcdText(album, viewModel = viewModel)
        }
        Row(
            modifier = Modifier
                .fillMaxSize(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start
        ) {
            val fetchStrategy = LazyListPrefetchStrategy(50)
            val lazyColumnState = rememberLazyListState(
                initialFirstVisibleItemIndex = 0,
                initialFirstVisibleItemScrollOffset = 0,
                prefetchStrategy = fetchStrategy
            )
            val lazyColumnSize = albumSongsList.count()
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.955f),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
                state = lazyColumnState,
            ) {
                items(lazyColumnSize) { i ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(75.dp)
                            .padding(5.dp)
                            .clickable(
                                onClick = {
                                    if (mediaController != null) {
                                        viewModel.queueingSongs = false
                                        viewModel.shuffleMode = false
                                        mediaController.clearMediaItems()
                                        for (j in 0 until albumSongsList.count()) {
                                            mediaController.addMediaItem(
                                                MediaItem.fromUri(
                                                    albumSongsList[j].songUri
                                                )
                                            )
                                        }
                                        mediaController.prepare()
                                        mediaController.seekTo(i, 0L)
                                        mediaController.play()
                                        viewModel.queuedSongs = albumSongsList.toMutableStateList()
                                        viewModel.songIndex = i
                                        viewModel.updateSongDuration((albumSongsList[i].time).toLong())
                                        viewModel.playingFromSongsScreen = false // Shows details from albums list
                                        navController.navigate("pager")
                                    }
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Image( // Album art
                            bitmap = albumSongsList[i].albumArt,
                            modifier = Modifier
                                .size(60.dp),
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                        Spacer(
                            modifier = Modifier
                                .width(10.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.Start
                        ) {
                            LargeLcdText( //Song name
                                text = albumSongsList[i].name,
                                viewModel = viewModel
                            )
                            Spacer(
                                modifier = Modifier
                                    .height(5.dp)
                            )
                            LcdText( // Artist name
                                text = albumSongsList[i].artist,
                                viewModel = viewModel
                            )
                            LcdText( // Album name
                                text = albumSongsList[i].album,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
            ScrollBar(lazyColumnState, viewModel, lazyColumnSize.toFloat())
        }
    }
}