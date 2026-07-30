package com.sonarous.player.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonarous.player.AlbumInfo
import com.sonarous.player.R
import com.sonarous.player.SongInfo

//@Composable
//fun SearchScreen(
//    viewModel: PlayerViewModel,
//    songInfo: List<SongInfo>,
//    mediaController: MediaController?,
//    pagerState: PagerState,
//    context: Context,
//    navController: NavController
//) {
//    // Ensure that any previous "viewModel.selectedAlbum" is cleared >keyed> to deal with recomp
//    key(Unit) { viewModel.selectedAlbum = "" }
//
//    val selectedArtist = remember { mutableStateOf<String?>(null) }
//
//    when {
//        selectedArtist.value == null && viewModel.selectedAlbum == "" -> SearchList(viewModel, songInfo, mediaController, pagerState, selectedArtist)
//        selectedArtist.value != null && viewModel.selectedAlbum == "" -> ArtistSongs(selectedArtist, songInfo, viewModel, mediaController, pagerState, context)
//        viewModel.selectedAlbum != "" && selectedArtist.value == null -> navController.navigate("album_songs_screen")
//    }
//}
//
//@Composable
//fun SearchList(
//    viewModel: PlayerViewModel,
//    songInfo: List<SongInfo>,
//    mediaController: MediaController?,
//    pagerState: PagerState,
//    selectedArtist: MutableState<String?>,
//) {
//    val lazyListState = rememberLazyListState(
//        initialFirstVisibleItemIndex = 0,
//        initialFirstVisibleItemScrollOffset = 0,
//    )
//
//    var searchedData by remember { mutableStateOf<Map<String, List<Any>>>(mapOf()) }
//    val searchText = remember { mutableStateOf("") }
//
//    val playSongCallback = remember {
//        { i: Int ->
//            viewModel.queueingSongs = false
//            viewModel.shuffleMode = false
//
//            val song = searchedData["songs"]!![i] as SongInfo
//
//            mediaController?.clearMediaItems()
//            mediaController?.addMediaItem(MediaItem.fromUri(song.songUri))
//            mediaController?.prepare()
//            mediaController?.seekTo(i, 0L)
//            mediaController?.play()
//
//            pagerState.requestScrollToPage(1)
//            viewModel.queuedSongs = songInfo.toMutableStateList()
//            viewModel.updateSongDuration((songInfo[i].time).toLong())
//            viewModel.songIndex = i
//            viewModel.playingFromSongsScreen = true
//        }
//    }
//
//    LaunchedEffect(searchText.value) {
//        this.launch(Dispatchers.Default) {
//            searchedData = Search(searchText.value, songInfo).searchData()
//            if (searchedData["songs"]!!.isNotEmpty()) Log.d("SonarousLogs", "Final: ${searchedData["songs"]?.get(0)}")
//            if (searchedData["songs"]!!.isEmpty()) Log.d("SonarousLogs", "Empty")
//        }
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//    ) {
//        SearchBar(searchText)
//        if (searchedData.isEmpty()) return
//        Row(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(viewModel.backgroundColor)
//                .windowInsetsPadding(WindowInsets.statusBars)
//                .windowInsetsPadding(WindowInsets.navigationBars),
//            verticalAlignment = Alignment.Top,
//            horizontalArrangement = Arrangement.Start,
//        ) {
//            LazyColumn(
//                Modifier
//                    .fillMaxHeight()
//                    .fillMaxWidth(0.955f),
//                verticalArrangement = Arrangement.Top,
//                horizontalAlignment = Alignment.Start,
//                state = lazyListState
//            ) {
//                // ------------- Songs ------------- //
//                if (searchedData["songs"] != null) {
//                    items(searchedData["songs"]!!.size) { i ->
//                        SongRow(searchedData["songs"]!![i] as SongInfo, viewModel, i, playSongCallback)
//                    }
//                }
//                // ------------- Albums ------------- //
//                if (searchedData["albums"] != null) {
//                    items(searchedData["albums"]!!.size) { i ->
//                        TitleRowButton(searchedData["albums"]!![i] as String, viewModel) {
//                            viewModel.selectedAlbum = searchedData["albums"]!![i] as String
//                        }
//                    }
//                }
//                // ------------- Artists ------------- //
//                if (searchedData["artists"] != null) {
//                    items(searchedData["artists"]!!.size) { i ->
//                        TitleRowButton(searchedData["artists"]!![i] as String, viewModel) {
//                            selectedArtist.value = searchedData["artists"]!![i] as String
//                        }
//                    }
//                }
//            }
//            ScrollBar(lazyListState, viewModel, getSearchedMapSize(searchedData).toFloat())
//        }
//    }
//}
//
//fun getSearchedMapSize(searchedData: Map<String, List<Any>>): Int {
//    return searchedData["songs"]?.size?.plus(
//        searchedData["albums"]?.size ?: 0
//    )?.plus(
//        searchedData["artists"]?.size ?: 0
//    ) ?: 0
//}

object Search {
    fun searchSongs(songs: List<SongInfo>, searchText: String): List<SongInfo> {
        val regex = buildSearchRegEx(searchText)
        val searchedSongs = mutableListOf<SongInfo>()
        for (song in songs) {
            if (song.name.lowercase().matches(regex)) searchedSongs.add(song)
        }
        return searchedSongs
    }

    fun searchAlbums(albums: List<AlbumInfo>, searchText: String): List<AlbumInfo> {
        val regex = buildSearchRegEx(searchText)
        val searchedAlbums = mutableListOf<AlbumInfo>()
        for (album in albums) {
            if (album.albumName.lowercase().matches(regex)) searchedAlbums.add(album)
        }
        return searchedAlbums
    }

    fun searchArtists(artists: List<String>, searchText: String): List<String> {
        val regex = buildSearchRegEx(searchText)
        val searchedArtists = mutableListOf<String>()
        for (artist in artists) {
            if (artist.lowercase().matches(regex)) searchedArtists.add(artist)
        }
        return searchedArtists
    }

    private fun buildSearchRegEx(searchText: String): Regex {
        var pattern = ""
        for (char in searchText) {
            pattern += """([a-zA-Z0-9]|\s|\p{P})*${char.lowercase()}([a-zA-Z0-9]|\s|\p{P})*"""
        }
        return pattern.toRegex()
    }
}

@Composable
fun SearchBar(searchText: MutableState<String>, modifier: Modifier = Modifier, bgColor: Color = Color.Black) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(bgColor, RoundedCornerShape(100f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        var inputText by remember { mutableStateOf("") }

//        Spacer(Modifier.width(5.dp))

        Icon(
            modifier = Modifier.size(25.dp),
            painter = painterResource(R.drawable.search),
            contentDescription = null,
            tint = Color.White
        )

        BasicTextField(
            value = inputText,
            onValueChange = {
                inputText = it
                searchText.value = it
            },
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.7f),
            textStyle = TextStyle(
                fontSize = 18.sp,
                color = Color.White
            ),
            cursorBrush = SolidColor(Color.White),
            decorationBox = { innerTextField ->
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
//                        .padding(2.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    innerTextField()
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxHeight(0.5f)
                            .fillMaxWidth(0.7f) // .5f
                    ) {
                        drawLine(
                            color = Color.White,
                            start = Offset(0f, 0f),
                            end = Offset(
                                x = size.width,
                                0f
                            )
                        )
                    }
                }
            },
        )
    }
}