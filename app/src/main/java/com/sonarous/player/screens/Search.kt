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