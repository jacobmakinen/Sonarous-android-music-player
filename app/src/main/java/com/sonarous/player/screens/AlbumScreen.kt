package com.sonarous.player.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.sonarous.player.AlbumInfo
import com.sonarous.player.AlbumScreenText
import com.sonarous.player.ScrollBar
import com.sonarous.player.components.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@OptIn(UnstableApi::class)
@Composable
fun AlbumScreen(
    albumInfo: List<AlbumInfo>,
    viewModel: PlayerViewModel,
    navController: NavController,
    elementsPerRow: Int = 3,
) {
    val searchText = remember { mutableStateOf("") }
    var searchedAlbums by remember { mutableStateOf<List<AlbumInfo>>(listOf()) }

    val rowNumbers = if (searchText.value == "") {
        if (albumInfo.count() % elementsPerRow != 0) {
            albumInfo.count() / elementsPerRow + 1
        } else {
            albumInfo.count() / elementsPerRow
        }
    } else {
        if (searchedAlbums.count() % elementsPerRow != 0) {
            searchedAlbums.count() / elementsPerRow + 1
        } else {
            searchedAlbums.count() / elementsPerRow
        }
    }


    LaunchedEffect(searchText.value) {
        this.launch(Dispatchers.Default) {
            searchedAlbums = Search.searchAlbums(albumInfo, searchText.value)
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
                state = viewModel.albumScreenLazyColumnState
            ) {
                items(rowNumbers) { rowIndex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumRow(
                            ( if (searchText.value == "") albumInfo else searchedAlbums ),
                            elementsPerRow,
                            navController,
                            viewModel,
                            rowIndex,
                            rowNumbers
                        )
                    }
                }
            }
            ScrollBar(
                viewModel.albumScreenLazyColumnState,
                viewModel,
                rowNumbers.toFloat(),
                5.toFloat()
            )
        }
    }
}

@Composable
fun AlbumRow(
    albumInfo: List<AlbumInfo>,
    elementsPerRow: Int,
    navController: NavController,
    viewModel: PlayerViewModel,
    rowIndex: Int,
    rowNumbers: Int,
) {
    val albumWidth = 100.dp

    when {
        albumInfo.count() % elementsPerRow == 0 -> {
            for (index in 0 until elementsPerRow) {
                Album(
                    viewModel,
                    albumInfo[rowIndex * elementsPerRow + index].albumArt,
                    albumInfo[rowIndex * elementsPerRow + index].albumName,
                    albumWidth
                ) {
                    viewModel.selectedAlbum = albumInfo[rowIndex * elementsPerRow + index].albumName
                    navController.navigate("album_songs_screen")
                }
            }
        }

        rowNumbers != rowIndex + 1 -> {
            for (index in 0 until elementsPerRow) {
                Album(
                    viewModel,
                    albumInfo[rowIndex * elementsPerRow + index].albumArt,
                    albumInfo[rowIndex * elementsPerRow + index].albumName,
                    albumWidth
                ) {
                    viewModel.selectedAlbum = albumInfo[rowIndex * elementsPerRow + index].albumName
                    navController.navigate("album_songs_screen")
                }
            }
        }

        else -> {
            for (index in 0 until (albumInfo.count() % elementsPerRow)) {
                Album(
                    viewModel,
                    albumInfo[rowIndex * elementsPerRow + index].albumArt,
                    albumInfo[rowIndex * elementsPerRow + index].albumName,
                    albumWidth
                ) {
                    viewModel.selectedAlbum = albumInfo[rowIndex * elementsPerRow + index].albumName
                    navController.navigate("album_songs_screen")
                }
            }
        }
    }
}

@Composable
fun Album(viewModel: PlayerViewModel, bitmap: ImageBitmap, albumText: String, albumWidth: Dp, onClick: () -> Unit) {
    val yInset = 0.85f
    val xInset = 0.8f
    Column(
        modifier = Modifier
            .padding(horizontal = 5.dp)
            .drawBehind {
                val path = androidx.compose.ui.graphics.Path().apply {
                    relativeLineTo(size.width, 0f)
                    relativeLineTo(0f, size.height * yInset)
                    moveTo(0f,0f)
                    relativeLineTo(0f, size.height)
                    relativeLineTo(size.width * xInset, 0f)
                    lineTo(size.width, size.height * yInset)
                }
                drawPath(
                    path,
                    viewModel.iconColor,
                    style = Stroke()
                )
            }
            .padding(5.dp)
            .fillMaxHeight()
            .width(albumWidth)
            .clickable(onClick = { onClick() }),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Image(
            modifier = Modifier.aspectRatio(1f),
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        AlbumScreenText(
            albumText,
            viewModel,
            Modifier.fillMaxWidth(0.9f).fillMaxHeight()
        )
    }
}