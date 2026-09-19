package com.sonarous.player.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Draggable2DState
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.session.MediaController
import com.sonarous.player.LargeText
import com.sonarous.player.SongInfo
import com.sonarous.player.Text
import com.sonarous.player.components.OverlayService
import com.sonarous.player.components.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class OverlayScreen(
    private val songs: List<SongInfo>,
    private val mediaController: MediaController,
    private val viewModel: PlayerViewModel,
    private val overlayService: OverlayService
) {
    private val maxYDrag = 5f
    private val initYOffset = -50f
    private val maxXOffset = 70f
//    private val hitboxDeltaMultiplier = 1f - mediaController.currentMediaItemIndex.toFloat() / songs.size.toFloat() Attempt to have window scroll approx match scrolled item position

    private val columnHeight = 170.dp
    private val songRowHeight = 80.dp

    private var yHitboxPointer by mutableFloatStateOf(initYOffset)
    private var yScroll by mutableFloatStateOf(0f)
    private var xPointer by mutableFloatStateOf(0f)  // Midpoint
    private var selectedIndex by mutableIntStateOf(mediaController.currentMediaItemIndex)

    private val highlightedColor = Color.White // Color(0xFF010CFC) // Color(0xFF0354F8) // Color(0xFFC1FE02) // Color(0xFF01058C)
    private val lazyColumnState = LazyListState()

    @Composable
    fun SongSelectOverlay() {
        val scope = rememberCoroutineScope()
        val mediaItemList by remember (songs) {
            derivedStateOf {
                getMediaItemList(songs)
            }
        }

        val playSongCallback = remember {
            {
                overlayService.updateActivityPlayingIndex(selectedIndex + 1)
                mediaController.clearMediaItems()
                mediaController.addMediaItems(mediaItemList)
                mediaController.prepare()
                mediaController.seekTo(selectedIndex, 0L)
                mediaController.play()
            }
        }

        val dragState = rememberDraggable2DState { delta ->
            if (yHitboxPointer < maxYDrag) {
                yHitboxPointer = (yHitboxPointer + delta.y).coerceIn(minimumValue = -50f, maximumValue = maxYDrag)
            // Lock yScrolling if element has x offset and primary delta is not y
            } else if (xPointer == 0f && delta.y.absoluteValue > delta.x.absoluteValue) {
                yScroll = (yScroll + delta.y).coerceIn(0f, lazyColumnState.layoutInfo.viewportSize.height.toFloat())
                val newIndex = (yScroll / lazyColumnState.layoutInfo.viewportSize.height.toFloat() * (songs.size - 1)).roundToInt()

                if (newIndex != selectedIndex) {
                    selectedIndex = newIndex
                    scope.launch { lazyColumnState.animateScrollToItem(selectedIndex) }
                }
            }
            // Only allow x movement when hitbox is at max offset and the delta is large enough [only consider delta strength at 0 offset]
            if (yHitboxPointer >= maxYDrag && xPointer == 0f && delta.x > 2f) {
                xPointer = (xPointer + delta.x).coerceIn(0f, maxXOffset)
            } else if (yHitboxPointer >= maxYDrag && xPointer != 0f) {
                xPointer = (xPointer + delta.x).coerceIn(0f, maxXOffset)
            }
        }

        SongListOverlay(dragState, mediaController, playSongCallback)
    }

    private fun resetPointers(scope: CoroutineScope) {
        scope.launch {
            yHitboxPointer = initYOffset
            xPointer = 0f
            yScroll = 0f
            selectedIndex = mediaController.currentMediaItemIndex
            lazyColumnState.scrollToItem(selectedIndex)
        }
    }

    @Composable
    private fun SongListOverlay(dragState: Draggable2DState, mediaController: MediaController, playSongCallback: () -> Unit) {
        val width = 400.dp
//        val width = remember (yHitboxPointer) { if (yHitboxPointer > initYOffset) 400.dp else 400.dp }
        val height = remember (yHitboxPointer) { if (yHitboxPointer > initYOffset) columnHeight else 15.dp }
        val scope = rememberCoroutineScope()

        Box(modifier = Modifier.size(width, height)) {
            HitboxMarker(dragState, scope, playSongCallback)
            // Prevent box from being composed off-screen
            if (yHitboxPointer == initYOffset) return
            Box(
                modifier = Modifier
                    .size(width, columnHeight)
                    .offset(y = yHitboxPointer.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    Modifier
                        .fillMaxSize(0.95f)
                        .background(Color(0x44000000)),
//                        .zIndex(100f),
                    lazyColumnState
                ) {
                    items(songs.size) { i ->
                        SongRow(songs[i], viewModel, i)
                    }
                }
                // Scroll to currently playing song and center it
                LaunchedEffect(Unit) {
                    lazyColumnState.scrollToItem(mediaController.currentMediaItemIndex)
                    lazyColumnState.scrollBy(-100f)
                }
                Canvas(modifier = Modifier.fillMaxSize(0.96f)) {
                    val path = Path()
                    getBorderPath(path, this.size)
                    drawPath(
                        path = path,
                        color = highlightedColor,
                        style = Stroke(1.4f)
                    )
                }
            }
        }
    }

    @Composable
    private fun BoxScope.HitboxMarker(dragState: Draggable2DState, scope: CoroutineScope, playSongCallback: () -> Unit) {
        Box(
            modifier = Modifier
                .size(25.dp, 3.dp)
                .draggable2D(
                    state = dragState,
                    onDragStopped = {
                        if (xPointer == maxXOffset) playSongCallback()
                        resetPointers(scope)
                    }
                )
                .align(Alignment.TopCenter)
                .drawBehind {
                    val path = Path().apply {
                        moveTo(0f, 5f)
                        relativeLineTo(size.width, 0f)
                    }
                    drawPath(
                        path = path,
                        color = highlightedColor,
                        style = Stroke(1.4f)
                    )
                }
        )
    }

    @Composable
    private fun SongRow(songInfo: SongInfo, viewModel: PlayerViewModel, index: Int, modifier: Modifier = Modifier) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(songRowHeight)
                .padding(5.dp)
                .border(0.dp, if (selectedIndex == index) Color.White else Color.Transparent)
                .offset(x = if (selectedIndex == index) xPointer.dp else 0.dp)
                .zIndex(100f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.fillMaxWidth(0.8f)) {
                AlbumCover(songInfo)
                Spacer(modifier = Modifier.width(10.dp))
                SongTextColumn(songInfo, viewModel)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                SongDurationText(songInfo, viewModel)
            }
        }
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
                maxLength = 15
            )
            Spacer(
                modifier = Modifier.height(3.dp)
            )
            Text( // Artist name
                text = songInfo.artist,
                viewModel = viewModel,
                maxTextLength = 17
            )
            Spacer(Modifier.height(1.5.dp))
            Text( // Album name
                text = songInfo.album,
                viewModel = viewModel,
                maxTextLength = 17
            )
        }
    }

    private fun getBorderPath(path: Path, size: Size) {
        val indent = 20f
        val borderXtra = 10f
        path.moveTo(0f, -borderXtra)
        path.relativeLineTo(indent, 0f)
        path.moveTo(0f, -borderXtra)
        path.relativeLineTo(0f, size.height + borderXtra)
        path.relativeLineTo(indent, 0f)
        path.moveTo(size.width - indent, -borderXtra)
        path.relativeLineTo(indent, 0f)
        path.relativeLineTo(0f, size.height + borderXtra)
        path.relativeLineTo(-indent, 0f)
    }
}

