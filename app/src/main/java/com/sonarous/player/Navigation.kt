@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.sonarous.player

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.OptIn
import androidx.collection.IntList
import androidx.collection.intListOf
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@OptIn(UnstableApi::class)
@Composable
fun NavHost(
    mediaController: MediaController?,
    songInfo: List<SongInfo>,
    audioProcessor: PlayerService.SpectrumAnalyzer,
    viewModel: PlayerViewModel,
    albumInfo: List<AlbumInfo>,
    context: Context
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "pager",
        modifier = Modifier
            .background(viewModel.backgroundColor),
    ) {
        composable(route = "pager") {
            Pager(mediaController, audioProcessor, viewModel, songInfo, albumInfo, navController)
        }
        composable(route = "album_songs_screen") {
            AlbumSongsScreen(
                viewModel.selectedAlbum,
                songInfo,
                mediaController,
                viewModel,
                navController
            )
        }
        composable(route = "settings") {
            Settings(navController, viewModel)
        }
        composable(route = "theme_change") {
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                PortraitThemeChange(viewModel, navController, context)
            } else {
                HorizontalThemeChange(viewModel, navController, context)
            }
        }
        composable(route = "color_picker") {
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                PortraitColorPicker(viewModel, navController)
            } else {
                HorizontalColorPicker(viewModel, navController)
            }
        }

        composable(route = "info_screen") {
            InfoScreen(viewModel, navController)
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@OptIn(UnstableApi::class)
@Composable
fun Pager(
    mediaController: MediaController?,
    spectrumAnalyzer: PlayerService.SpectrumAnalyzer,
    viewModel: PlayerViewModel,
    songInfo: List<SongInfo>,
    albumInfo: List<AlbumInfo>,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 1
    ) {
        4
    }
    val selectedTab = remember { derivedStateOf { pagerState.currentPage } }
    val iconList = intListOf(
        R.drawable.play_arrow, R.drawable.outline_play_arrow, R.drawable.library_music,
        R.drawable.outline_library_music, R.drawable.album, R.drawable.outline_album,
    )
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
        PortraitTabRow(
            mediaController, spectrumAnalyzer,
            viewModel, songInfo,
            albumInfo, navController,
            selectedTab, pagerState,
            scope, iconList
        )
    } else {
        HorizontalTabRow(
            mediaController, spectrumAnalyzer,
            viewModel, songInfo,
            albumInfo, navController,
            selectedTab, pagerState,
            scope, iconList
        )
    }
}

@ExperimentalFoundationApi
@OptIn(UnstableApi::class)
@Composable
fun HorizontalTabRow(
    mediaController: MediaController?,
    spectrumAnalyzer: PlayerService.SpectrumAnalyzer,
    viewModel: PlayerViewModel,
    songInfo: List<SongInfo>,
    albumInfo: List<AlbumInfo>,
    navController: NavController,
    selectedTab: State<Int>,
    pagerState: PagerState,
    scope: CoroutineScope,
    iconList: IntList
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(viewModel.backgroundColor)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            TabRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                containerColor = viewModel.backgroundColor,
                selectedTabIndex = selectedTab.value,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier
                            .tabIndicatorOffset(
                                tabPositions[pagerState.currentPage]
                            ),
                        color = viewModel.iconColor,
                        height = 2.dp
                    )
                }
            ) {
                Tab(
                    modifier = Modifier
                        .fillMaxSize(),
                    selected = selectedTab.value == 0,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = "Queue music"
                        )
                    },
                    selectedContentColor = viewModel.iconColor,
                    unselectedContentColor = viewModel.iconColor,
                )
                for (i in 1..3) {
                    Tab(
                        modifier = Modifier
                            .fillMaxSize(),
                        selected = selectedTab.value == i,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(i)
                            }
                        },
                        icon = {
                            if (selectedTab.value == i) {
                                Icon(
                                    painter = painterResource(iconList[(i - 1) * 2]),
                                    contentDescription = null
                                )
                            } else {
                                Icon(
                                    painter = painterResource(iconList[(i - 1) * 2 + 1]),
                                    contentDescription = null
                                )
                            }
                        },
                        selectedContentColor = viewModel.iconColor,
                        unselectedContentColor = viewModel.iconColor,
                    )
                }
                var dropDownMenu by remember { mutableStateOf(false) }
                Tab( // More options menu
                    modifier = Modifier
                        .fillMaxSize(0.15f),
                    selected = false,
                    onClick = {
                        dropDownMenu = !dropDownMenu
                    },
                    // More options i.e. settings button
                    content = {
                        Icon(
                            painterResource(R.drawable.more_menu),
                            contentDescription = "More options"
                        )
                        DropdownMenu(
                            containerColor = viewModel.backgroundColor,
                            expanded = dropDownMenu,
                            onDismissRequest = { dropDownMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    LcdText(
                                        "Settings",
                                        viewModel = viewModel
                                    )
                                },
                                onClick = {
                                    dropDownMenu = false
                                    navController.navigate("settings")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    LcdText(
                                        "Info",
                                        viewModel = viewModel
                                    )
                                },
                                onClick = {
                                    dropDownMenu = false
                                    navController.navigate("info_screen")
                                }
                            )
                        }
                    },
                    selectedContentColor = viewModel.iconColor,
                    unselectedContentColor = viewModel.iconColor
                )
            }
        }
        HorizontalPager(
            state = pagerState
        ) { currentPage ->
            when (currentPage) {
                0 -> SongQueue(viewModel, mediaController)
                1 -> PlayerScreen(mediaController, spectrumAnalyzer, viewModel, songInfo)
                2 -> SongsScreen(songInfo, mediaController, viewModel, pagerState)
                3 -> AlbumScreen(albumInfo, viewModel, navController, 6)
            }
        }
    }
}

@ExperimentalFoundationApi
@OptIn(UnstableApi::class)
@Composable
fun PortraitTabRow(
    mediaController: MediaController?,
    spectrumAnalyzer: PlayerService.SpectrumAnalyzer,
    viewModel: PlayerViewModel,
    songInfo: List<SongInfo>,
    albumInfo: List<AlbumInfo>,
    navController: NavController,
    selectedTab: State<Int>,
    pagerState: PagerState,
    scope: CoroutineScope,
    iconList: IntList
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(viewModel.backgroundColor)
            .windowInsetsPadding(WindowInsets.statusBars),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            TabRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                containerColor = viewModel.backgroundColor,
                selectedTabIndex = selectedTab.value,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier
                            .tabIndicatorOffset(
                                tabPositions[pagerState.currentPage]
                            ),
                        color = viewModel.iconColor,
                        height = 2.dp
                    )
                }
            ) {
                Tab(
                    modifier = Modifier
                        .fillMaxSize(),
                    selected = selectedTab.value == 0,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = "Queue music"
                        )
                    },
                    selectedContentColor = viewModel.iconColor,
                    unselectedContentColor = viewModel.iconColor,
                )
                for (i in 1..3) {
                    Tab(
                        modifier = Modifier
                            .fillMaxSize(),
                        selected = selectedTab.value == i,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(i)
                            }
                        },
                        icon = {
                            if (selectedTab.value == i) {
                                Icon(
                                    painter = painterResource(iconList[(i - 1) * 2]),
                                    contentDescription = null
                                )
                            } else {
                                Icon(
                                    painter = painterResource(iconList[(i - 1) * 2 + 1]),
                                    contentDescription = null
                                )
                            }
                        },
                        selectedContentColor = viewModel.iconColor,
                        unselectedContentColor = viewModel.iconColor,
                    )
                }
                var dropDownMenu by remember { mutableStateOf(false) }
                Tab( // More options menu
                    modifier = Modifier
                        .fillMaxSize(0.15f),
                    selected = false,
                    onClick = {
                        dropDownMenu = !dropDownMenu
                    },
                    // More options i.e. settings button
                    content = {
                        Icon(
                            painterResource(R.drawable.more_menu),
                            contentDescription = "More options"
                        )
                        DropdownMenu(
                            containerColor = viewModel.backgroundColor,
                            expanded = dropDownMenu,
                            onDismissRequest = { dropDownMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    LcdText(
                                        "Settings",
                                        viewModel = viewModel
                                    )
                                },
                                onClick = {
                                    dropDownMenu = false
                                    navController.navigate("settings")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    LcdText(
                                        "Info",
                                        viewModel = viewModel
                                    )
                                },
                                onClick = {
                                    dropDownMenu = false
                                    navController.navigate("info_screen")
                                }
                            )
                        }
                    },
                    selectedContentColor = viewModel.iconColor,
                    unselectedContentColor = viewModel.iconColor
                )
            }
        }
        HorizontalPager(
            state = pagerState
        ) { currentPage ->
            when (currentPage) {
                0 -> SongQueue(viewModel, mediaController)
                1 -> PlayerScreen(mediaController, spectrumAnalyzer, viewModel, songInfo)
                2 -> SongsScreen(songInfo, mediaController, viewModel, pagerState)
                3 -> AlbumScreen(albumInfo, viewModel, navController)
            }
        }
    }
}

@Composable
fun BackButtonRow(viewModel: PlayerViewModel, navController: NavController, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(43.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(viewModel.backgroundColor),
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
        Spacer(modifier = Modifier.width(5.dp))
        LargeLcdText(title, viewModel = viewModel)
    }
}