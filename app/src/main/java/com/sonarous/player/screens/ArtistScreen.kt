package com.sonarous.player.screens

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import androidx.navigation.NavController
import com.sonarous.player.BackButtonRow
import com.sonarous.player.MergeSort
import com.sonarous.player.Screens
import com.sonarous.player.ScrollBar
import com.sonarous.player.SongInfo
import com.sonarous.player.Text
import com.sonarous.player.components.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ArtistSongs(
    artist: String,
    songs: List<SongInfo>,
    viewModel: PlayerViewModel,
    mediaController: MediaController?,
    context: Context,
    navController: NavController
) {
    val artistSongs = getArtistSongs(artist, songs)

    val mediaItemList by remember {
        derivedStateOf {
            getMediaItemList(artistSongs)
        }
    }

    val playSongCallback = remember {
        { i: Int ->
            viewModel.queueingSongs = true
            viewModel.shuffleMode = false
            viewModel.playingFromSongsScreen = false

            mediaController?.clearMediaItems()
            mediaController?.addMediaItems(mediaItemList)
            mediaController?.prepare()
            mediaController?.seekTo(i, 0L)
            mediaController?.play()

            navController.navigate(Screens.PAGER.name) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
            viewModel.showSearchBar = false
            viewModel.queuedSongs = artistSongs.toMutableStateList()
            viewModel.updateSongDuration((artistSongs[i].duration).toLong())
            viewModel.songIndex = i
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.displayCutout)
    ) {
        BackButtonRow(viewModel, text = artist) {
            navController.popBackStack()
        }
        Box {
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
                    items(artistSongs.size) { i ->
                        SongRow(artistSongs[i], viewModel, i, playSongCallback)
                    }
                }
                ScrollBar(
                    viewModel.songsScreenLazyColumnState,
                    viewModel,
                    artistSongs.size.toFloat(),
                    (
                            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                                8.5f
                            } else {
                                3.25f
                            }
                            )
                )
            }
            if (viewModel.showMoreSongOptions) {
                MoreSongOptions(viewModel, mediaController, context)
            }
        }
    }
}

@Composable
fun Artists(
    viewModel: PlayerViewModel,
    songInfo: List<SongInfo>,
    navController: NavController
) {
    val artists = remember(songInfo) { MergeSort.sort(getArtists(songInfo)) }
    val lazyColumnState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0,
        initialFirstVisibleItemScrollOffset = 0,
    )

    val searchText = remember { mutableStateOf("") }
    var searchedArtists by remember { mutableStateOf<List<String>>(listOf()) }

    LaunchedEffect(searchText.value) {
        this.launch(Dispatchers.Default) {
            searchedArtists = Search.searchArtists(artists, searchText.value)
        }
    }

    Column {
        if (viewModel.showSearchBar) {
            SearchBar(
                searchText,
                viewModel,
                Modifier
                    .padding(5.dp)
                    .border(0.dp, Color.White)
                    .padding(start = 10.dp),
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
                    .fillMaxWidth(0.955f)
                    .padding(5.dp),
                state = lazyColumnState
            ) {
                items(if (searchText.value == "") artists else searchedArtists) { name ->
                    TitleRowButton(name, viewModel) {
                        viewModel.selectedArtist = name
                        navController.navigate(Screens.ARTIST_SONGS.name)
                    }
                }
            }
            ScrollBar(
                lazyColumnState,
                viewModel,
                ( if (searchText.value == "") artists.size else searchedArtists.size).toFloat(),
                ( if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) 14f else 6f )
            )
        }
    }
}

@Composable
fun TitleRowButton(name: String, viewModel: PlayerViewModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(start = 5.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(name, viewModel = viewModel)
    }
}

fun getArtists(songInfo: List<SongInfo>): List<String> {
    val tmp = mutableListOf<String>()
    for (song in songInfo) {
        if (song.artist !in tmp) tmp.add(song.artist)
    }
    return tmp
}

fun getArtistSongs(artist: String, songInfo: List<SongInfo>): List<SongInfo> = songInfo.filter { it.artist == artist }
