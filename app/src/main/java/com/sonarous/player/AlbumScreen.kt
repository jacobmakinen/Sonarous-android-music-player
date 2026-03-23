package com.sonarous.player

import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController

@ExperimentalFoundationApi
@OptIn(UnstableApi::class)
@Composable
fun AlbumScreen(
    albumInfo: List<AlbumInfo>,
    viewModel: PlayerViewModel,
    navController: NavController,
    elementsPerRow: Int = 3,
) {
    val lazyColumnState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0,
        initialFirstVisibleItemScrollOffset = 0,
    )
    val rowNumbers = (
            if (albumInfo.count() % elementsPerRow != 0) {
                albumInfo.count() / elementsPerRow + 1
            } else {
                albumInfo.count() / elementsPerRow
            }
            )
    val albumWidth = 110.dp
    Row(
        modifier = Modifier
            .fillMaxSize()
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
            state = lazyColumnState
        ) {
            items(rowNumbers) { rowIndex ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (albumInfo.count() % elementsPerRow == 0) {
                        for (index in 0 until elementsPerRow) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 5.dp)
                                    .fillMaxHeight()
                                    .width(albumWidth)
                                    .clickable(
                                        onClick = {
                                            viewModel.selectedAlbum = albumInfo[rowIndex * elementsPerRow + index].albumName
                                            navController.navigate("album_songs_screen")
                                        }
                                    ),
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Image(
                                    modifier = Modifier
                                        .aspectRatio(1f),
                                    bitmap = albumInfo[rowIndex * elementsPerRow + index].albumArt,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.height(5.dp))
                                AlbumScreenLcdText(
                                    albumInfo[rowIndex * elementsPerRow + index].albumName,
                                    viewModel = viewModel,
                                )
                            }
                        }
                    } else {
                        if (rowNumbers != rowIndex + 1) {
                            for (index in 0..elementsPerRow - 1) {
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 5.dp)
                                        .fillMaxHeight()
                                        .width(albumWidth)
                                        .clickable(
                                            onClick = {
                                                viewModel.selectedAlbum = albumInfo[rowIndex * elementsPerRow + index].albumName
                                                navController.navigate("album_songs_screen")
                                            }
                                        ),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Image(
                                        modifier = Modifier
                                            .aspectRatio(1f),
                                        bitmap = albumInfo[rowIndex * elementsPerRow + index].albumArt,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    AlbumScreenLcdText(
                                        albumInfo[rowIndex * elementsPerRow + index].albumName,
                                        viewModel = viewModel,
                                    )
                                }
                            }
                        } else {
                            for (index in 0 until (albumInfo.count() % elementsPerRow)) {
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 5.dp)
                                        .fillMaxHeight()
                                        .width(albumWidth)
                                        .clickable(
                                            onClick = {
                                                viewModel.selectedAlbum = albumInfo[rowIndex * elementsPerRow + index].albumName
                                                navController.navigate("album_songs_screen")
                                            }
                                        ),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Image(
                                        modifier = Modifier
                                            .aspectRatio(1f),
                                        bitmap = albumInfo[rowIndex * elementsPerRow + index].albumArt,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    AlbumScreenLcdText(
                                        albumInfo[rowIndex * elementsPerRow + index].albumName,
                                        viewModel = viewModel,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        ScrollBar(lazyColumnState, viewModel, rowNumbers.toFloat(), 4.toFloat())
    }
}
