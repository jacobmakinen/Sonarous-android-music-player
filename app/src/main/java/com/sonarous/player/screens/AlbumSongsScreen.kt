package com.sonarous.player.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import androidx.navigation.NavController
import com.sonarous.player.BackButtonRow
import com.sonarous.player.ScrollBar
import com.sonarous.player.SongInfo
import com.sonarous.player.components.PlayerViewModel

@ExperimentalFoundationApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumSongsScreen(
    album: String,
    songInfo: List<SongInfo>,
    mediaController: MediaController?,
    viewModel: PlayerViewModel,
    navController: NavController,
    context: Context
) {
    val albumSongs = mutableListOf<SongInfo>()
    for (i in 0 until songInfo.count()) {
        if (songInfo[i].album == album) {
            albumSongs.add(songInfo[i])
        }
    }

    val songMediaItems by remember(songInfo) {
        derivedStateOf {
            getMediaItemList(albumSongs)
        }
    }

    val playSongCallback = remember(songMediaItems) {
        { i: Int ->
            if (mediaController != null) {
                viewModel.queueingSongs = false
                viewModel.shuffleMode = false
                mediaController.clearMediaItems()
                mediaController.addMediaItems(songMediaItems)
                mediaController.prepare()
                mediaController.seekTo(i, 0L)
                mediaController.play()
                viewModel.queuedSongs = albumSongs.toMutableStateList()
                viewModel.songIndex = i
                viewModel.updateSongDuration((albumSongs[i].duration).toLong())
                viewModel.playingFromSongsScreen = false // Shows details from albums list
                navController.navigate("pager")
                viewModel.showSearchBar = false
            }
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
        BackButtonRow(viewModel, 60.dp, album) {
            navController.popBackStack()
        }
        Box {
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
                val lazyColumnSize = albumSongs.count()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.955f),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start,
                    state = lazyColumnState,
                ) {
                    items(lazyColumnSize) { i ->
                        SongRow(albumSongs[i], viewModel, i, playSongCallback)
                    }
                }
                ScrollBar(lazyColumnState, viewModel, lazyColumnSize.toFloat())
            }
            if (viewModel.showMoreSongOptions) {
                MoreSongOptions(viewModel, mediaController, context)
            }
        }
    }
}