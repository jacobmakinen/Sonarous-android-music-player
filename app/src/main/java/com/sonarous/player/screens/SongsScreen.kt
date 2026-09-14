package com.sonarous.player.screens

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.sonarous.player.LargeText
import com.sonarous.player.R
import com.sonarous.player.ScrollBar
import com.sonarous.player.SongInfo
import com.sonarous.player.Text
import com.sonarous.player.components.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@OptIn(UnstableApi::class)
@Composable
fun SongsScreen(
    songs: List<SongInfo>,
    mediaController: MediaController?,
    viewModel: PlayerViewModel,
    pagerState: PagerState,
    context: Context
) {
    val mediaItemList by remember {
        derivedStateOf {
            getMediaItemList(songs)
        }
    }

    val playSongCallback = remember {
        { i: Int ->
            viewModel.queueingSongs = false
            viewModel.shuffleMode = false

            mediaController?.clearMediaItems()
            mediaController?.addMediaItems(mediaItemList)
            mediaController?.prepare()
            mediaController?.seekTo(i, 0L)
            mediaController?.play()

            pagerState.requestScrollToPage(1)
            viewModel.queuedSongs = songs.toMutableStateList()
            viewModel.updateSongDuration((songs[i].duration).toLong())
            viewModel.songIndex = i
            viewModel.playingFromSongsScreen = true
        }
    }

    val searchText = remember { mutableStateOf("") }
    var searchedSongs by remember { mutableStateOf<List<SongInfo>>(listOf()) }

    val searchedSongsMediaList by remember(searchedSongs, songs) {
        derivedStateOf {
            val tmpList = mutableListOf<MediaItem>()
            for (song in searchedSongs) {
                tmpList.add(MediaItem.fromUri(song.uri))
            }
            tmpList
        }
    }

    val searchedPlaySongCallback = remember(searchedSongsMediaList) {
        { i: Int ->
            viewModel.queueingSongs = true
            viewModel.shuffleMode = false

            mediaController?.clearMediaItems()
            // May want to change what songs are played after a searched one
            mediaController?.addMediaItem(MediaItem.fromUri(searchedSongs[i].uri))
            mediaController?.prepare()
//            mediaController?.seekTo(i, 0L)
            mediaController?.play()

            pagerState.requestScrollToPage(1)
            searchText.value = ""
            viewModel.showSearchBar = false
//            viewModel.queuedSongs = searchedSongs.toMutableStateList()
            viewModel.queuedSongs = mutableStateListOf(searchedSongs[i])
            viewModel.updateSongDuration((searchedSongs[i].duration).toLong())
            viewModel.songIndex = 0
            viewModel.playingFromSongsScreen = true
        }
    }

    LaunchedEffect(searchText.value) {
        this.launch(Dispatchers.Default) {
            searchedSongs = Search.searchSongs(songs, searchText.value)
        }
    }

    Box {
        Column {
            if (viewModel.showSearchBar) {
                SearchBar(
                    searchText,
                    Modifier
                        .padding(5.dp)
                        .border(0.dp, Color.White)
                        .padding(start = 10.dp),
                    Color(0x00000000)
                )
            } else {
                searchText.value = ""
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
                    state = viewModel.songsScreenLazyColumnState,
                ) {
                    items(
                        (if (searchText.value == "") songs.size else searchedSongs.size)
                    ) { i ->
                        SongRow(
                            ( if (searchText.value == "") songs[i] else searchedSongs[i] ),
                            viewModel,
                            i,
                            ( if (searchText.value == "") playSongCallback else searchedPlaySongCallback )
                        )
                    }
                }
                ScrollBar(
                    viewModel.songsScreenLazyColumnState,
                    viewModel,
                    (if (searchText.value == "") songs.size.toFloat() else searchedSongs.size.toFloat())
                )
            }
        }
        if (viewModel.showMoreSongOptions) {
            MoreSongOptions(viewModel, mediaController, context)
        }
    }
}

fun getMediaItemList(songs: List<SongInfo>): List<MediaItem> {
    val tmpList = mutableListOf<MediaItem>()
    for (song in songs) {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.name)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setDurationMs(song.duration.toLong())
            .build()

        tmpList.add(MediaItem.Builder().setUri(song.uri).setMediaMetadata(metadata).build())
    }
    return tmpList
}

@Composable
fun SongRow(
    songInfo: SongInfo,
    viewModel: PlayerViewModel,
    i: Int,
    playSongCallback: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(5.dp)
            .clickable(
                onClick = {
                    playSongCallback(i)
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(0.8f)) {
            AlbumCover(songInfo)
            Spacer(modifier = Modifier.width(10.dp))
            SongTextColumn(songInfo, viewModel)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            SongDurationText(songInfo, viewModel)
            MoreOptionsButton(songInfo, viewModel)
        }
    }
}

@Composable
fun SongDurationText(song: SongInfo, viewModel: PlayerViewModel) {
    Text(getTextSongDuration(song.duration), viewModel = viewModel)
}

@Composable
fun AlbumCover(songInfo: SongInfo, size: Dp = 65.dp) {
    Image( // Album art
        bitmap = remember(songInfo.albumArt) { songInfo.albumArt },
        modifier = Modifier
            .size(size),
        contentDescription = null,
        contentScale = ContentScale.Crop
    )
}

@Composable
fun SongTextColumn(songInfo: SongInfo, viewModel: PlayerViewModel) {
    Column(
        modifier = Modifier
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        LargeText( // Song name
            text = songInfo.name,
            viewModel = viewModel,
            maxLength = 20
        )
        Spacer(
            modifier = Modifier
                .height(3.dp)
        )
        Text( // Artist name
            text = songInfo.artist,
            viewModel = viewModel
        )
        Spacer(Modifier.height(1.5.dp))
        Text( // Album name
            text = songInfo.album,
            viewModel = viewModel
        )
    }
}

@Composable
fun MoreOptionsButton(song: SongInfo, viewModel: PlayerViewModel) {
    IconButton(
        modifier = Modifier
            .size(30.dp),
        onClick = {
            viewModel.showMoreSongOptions = !viewModel.showMoreSongOptions
            viewModel.moreOptionsSelectedSong = song
        },
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = viewModel.iconColor,
        ),
        enabled = !viewModel.showMoreSongOptions
    ) {
        Icon(
            painter = painterResource(R.drawable.more_menu),
            contentDescription = "More options",
        )
    }
}

