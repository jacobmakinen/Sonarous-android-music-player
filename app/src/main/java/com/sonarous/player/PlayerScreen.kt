package com.sonarous.player

import android.content.res.Configuration
import androidx.annotation.OptIn
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import java.lang.Thread.sleep

@ExperimentalMaterial3Api
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    mediaController: MediaController?,
    audioProcessor: PlayerService.SpectrumAnalyzer,
    viewModel: PlayerViewModel,
    songInfo: List<SongInfo>
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
        PortraitOrientation(mediaController, audioProcessor, viewModel, songInfo)
    } else {
        HorizontalOrientation(mediaController, audioProcessor, viewModel, songInfo)
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun PortraitOrientation(
    mediaController: MediaController?,
    audioProcessor: PlayerService.SpectrumAnalyzer,
    viewModel: PlayerViewModel,
    songInfo: List<SongInfo>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(viewModel.backgroundColor),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(
            modifier = Modifier
                .height(10.dp)
        )
        PlayingMediaInfo(viewModel)
        PlaybackControls(mediaController, viewModel)
        if (viewModel.showEqualiser) {
            SpectrumAnalyzer(audioProcessor, viewModel)
        }
        OtherMediaControls(
            viewModel,
            mediaController,
            songInfo,
            audioProcessor
        )
        SeekBar(mediaController, viewModel)
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun HorizontalOrientation(
    mediaController: MediaController?,
    audioProcessor: PlayerService.SpectrumAnalyzer,
    viewModel: PlayerViewModel,
    songInfo: List<SongInfo>
) {
    Spacer(Modifier.width(10.dp))
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(viewModel.backgroundColor)
            .windowInsetsPadding(WindowInsets.displayCutout),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtHorizontalOrientation(viewModel)
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(5.dp))
            HorizontalPlayingMediaInfo(viewModel)
            PlaybackControls(mediaController, viewModel, 46.dp)
            SeekBarAndOtherControls(viewModel, mediaController, songInfo, audioProcessor)
            if (viewModel.showEqualiser) {
                SpectrumAnalyzer(audioProcessor, viewModel)
            }
        }
    }
}

@ExperimentalMaterial3Api
@UnstableApi
@Composable
fun SeekBarAndOtherControls(
    viewModel: PlayerViewModel,
    mediaController: MediaController?,
    songInfo: List<SongInfo>,
    audioProcessor: PlayerService.SpectrumAnalyzer
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SeekBar(mediaController, viewModel, 0.65f)
        //===================== Other controls =====================//
        // Audio effects
        AudioEffectMenu(viewModel, audioProcessor, mediaController)
        RepeatControls(mediaController, viewModel)
        ShuffleControls(mediaController, viewModel, songInfo)
        Spacer(modifier = Modifier.width(15.dp))
    }
}

@Composable
fun AlbumArtHorizontalOrientation(viewModel: PlayerViewModel) {
    Image( // Album art
        bitmap = (
                try {
                    viewModel.queuedSongs[viewModel.songIndex].albumArt
                } catch (_: IndexOutOfBoundsException) {
                    ImageBitmap(500, 500)
                }
                ),
        modifier = Modifier
            .size(250.dp),
        contentDescription = null,
        contentScale = ContentScale.Fit
    )
}

@Composable
fun HorizontalPlayingMediaInfo(viewModel: PlayerViewModel) {
    // Song name
    PlayerLargeLcdText(
        text = (
                try {
                    viewModel.queuedSongs[viewModel.songIndex].name
                } catch (_: IndexOutOfBoundsException) {
                    ""
                }
                ),
        viewModel = viewModel
    )
    Spacer(
        modifier = Modifier
            .height(5.dp)
    )
    // Artist name
    PlayerLcdText(
        try {
            viewModel.queuedSongs[viewModel.songIndex].artist
        } catch (_: IndexOutOfBoundsException) {
            ""
        },
        viewModel = viewModel
    )
    // Album name
    PlayerLcdText(
        try {
            viewModel.queuedSongs[viewModel.songIndex].album
        } catch (_: IndexOutOfBoundsException) {
            ""
        },
        viewModel = viewModel
    )
}

@Composable
fun PlayingMediaInfo(viewModel: PlayerViewModel) {
    // Album art
    Image(
        bitmap = (
                try {
                    viewModel.queuedSongs[viewModel.songIndex].albumArt
                } catch (_: IndexOutOfBoundsException) {
                    ImageBitmap(500, 500)
                }
                ),
        modifier = Modifier
            .size(300.dp),
        contentDescription = null,
        contentScale = ContentScale.Fit
    )
    Spacer(
        modifier = Modifier.height(10.dp)
    )
    // Song name
    PlayerLargeLcdText(
        text = (
                try {
                    viewModel.queuedSongs[viewModel.songIndex].name
                } catch (_: IndexOutOfBoundsException) {
                    ""
                }
                ),
        viewModel = viewModel
    )
    Spacer(
        modifier = Modifier
            .height(5.dp)
    )
    // Artist name
    PlayerLcdText(
        text = (
                try {
                    viewModel.queuedSongs[viewModel.songIndex].artist
                } catch (_: IndexOutOfBoundsException) {
                    ""
                }
                ),
        viewModel = viewModel
    )
    // Album name
    PlayerLcdText(
        text = (
                try {
                    viewModel.queuedSongs[viewModel.songIndex].album
                } catch (_: IndexOutOfBoundsException) {
                    ""
                }
                ),
        viewModel = viewModel
    )
}

@Composable
fun PlaybackControls(
    mediaController: MediaController?,
    viewModel: PlayerViewModel,
    height: Dp = 60.dp
) {
    Row( // Playback controls
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PreviousButton(mediaController, viewModel)
        PlayPauseButton(mediaController, viewModel)
        SkipButton(mediaController, viewModel)
    }
}

@Composable
fun PreviousButton(mediaController: MediaController?, viewModel: PlayerViewModel) {
    IconButton(
        modifier = Modifier
            .size(60.dp),
        onClick = {
            if (mediaController != null) {
                try {
                    if (mediaController.currentPosition < 10000L) {
                        if (mediaController.hasPreviousMediaItem()) {
                            mediaController.seekToPreviousMediaItem()
                        }
                    } else {
                        mediaController.seekTo(0L)
                    }
                } catch (_: IndexOutOfBoundsException) {
                }
            }
        },
        colors = IconButtonColors(
            containerColor = Color.Transparent,
            contentColor = viewModel.iconColor,
            disabledContentColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        content = {
            Icon(
                painter = painterResource(R.drawable.skip_previous),
                contentDescription = "Previous"
            )
        },
    )
}

@Composable
fun SkipButton(mediaController: MediaController?, viewModel: PlayerViewModel) {
    IconButton(
        // Skip button
        modifier = Modifier
            .size(60.dp),
        onClick = {
            if (mediaController != null) {
                if (mediaController.hasNextMediaItem()) {
                    mediaController.seekToNextMediaItem()
                }
            }
        },
        colors = IconButtonColors(
            containerColor = Color.Transparent,
            contentColor = viewModel.iconColor,
            disabledContentColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        content = {
            Icon(
                painter = painterResource(R.drawable.skip_next),
                contentDescription = "Next"
            )
        },
    )
}

@Composable
fun PlayPauseButton(mediaController: MediaController?, viewModel: PlayerViewModel) {
    IconButton( // Play & pause button
        onClick = {
            if (mediaController != null) {
                if (mediaController.isPlaying) {
                    mediaController.pause()
                } else {
                    mediaController.play()
                }
            }
        },
        modifier = Modifier
            .size(60.dp),
        colors = IconButtonColors(
            containerColor = Color.Transparent,
            contentColor = viewModel.iconColor,
            disabledContentColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        content = {
            Icon(
                painter = (
                        if (!viewModel.isPlaying) {
                            painterResource(R.drawable.large_play_arrow)
                        } else {
                            painterResource(R.drawable.pause)
                        }
                        ),
                contentDescription = "Play & pause"
            )
        }
    )
}

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun OtherMediaControls(
    viewModel: PlayerViewModel,
    mediaController: MediaController?,
    songInfo: List<SongInfo>,
    audioProcessor: PlayerService.SpectrumAnalyzer
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AudioEffectMenu(viewModel, audioProcessor, mediaController)
        RepeatControls(mediaController, viewModel)
        ShuffleControls(mediaController, viewModel, songInfo)
        Spacer(modifier = Modifier.width(15.dp))
    }
}

@Composable
fun RepeatControls(mediaController: MediaController?, viewModel: PlayerViewModel) {
    IconButton( // Repeat controls
        onClick = {
            when (viewModel.repeatMode) {
                "normal" -> {
                    mediaController?.repeatMode = ExoPlayer.REPEAT_MODE_ALL
                    viewModel.repeatMode = "repeatQueue"
                }

                "repeatQueue" -> {
                    mediaController?.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                    viewModel.repeatMode = "repeatSong"
                }

                "repeatSong" -> {
                    mediaController?.repeatMode = ExoPlayer.REPEAT_MODE_OFF
                    viewModel.repeatMode = "normal"
                }
            }
        },
        modifier = Modifier
            .size(40.dp),
        colors = IconButtonColors(
            containerColor = Color.Transparent,
            contentColor = viewModel.iconColor,
            disabledContentColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        content = {
            Icon(
                painter = (
                        when (viewModel.repeatMode) {
                            "normal" -> painterResource(R.drawable.repeat)
                            "repeatQueue" -> painterResource(R.drawable.repeat_on)
                            "repeatSong" -> painterResource(R.drawable.repeat_one_on)
                            else -> painterResource(R.drawable.repeat)
                        }
                        ),
                contentDescription = "Repeat controls"
            )
        }
    )
}

@Composable
fun ShuffleControls(mediaController: MediaController?, viewModel: PlayerViewModel, songInfo: List<SongInfo>) {
    val tmpSongInfo = mutableListOf<SongInfo>()
    IconButton( // Shuffle controls
        onClick = {
            if (!viewModel.shuffleMode) { // Switching to shuffle
                if (!viewModel.playingFromSongsScreen) { // Playing from albums
                    val tmpShuffledAlbumSongInfo = viewModel.shuffledAlbumSongInfo.shuffled()
                    tmpSongInfo.clear()
                    mediaController?.clearMediaItems()
                    for (i in tmpShuffledAlbumSongInfo) {
                        tmpSongInfo.add(i)
                    }
                    viewModel.shuffleSongInfo = tmpSongInfo
                    for (i in viewModel.shuffleSongInfo) {
                        mediaController?.addMediaItem(MediaItem.fromUri(i.songUri))
                    }
                } else { // Playing from songs screen
                    val tmpShuffledSongInfo = songInfo.shuffled()
                    tmpSongInfo.clear()
                    mediaController?.clearMediaItems()
                    for (i in tmpShuffledSongInfo) {
                        tmpSongInfo.add(i)
                    }
                    viewModel.shuffleSongInfo = tmpSongInfo
                    for (i in viewModel.shuffleSongInfo) {
                        mediaController?.addMediaItem(MediaItem.fromUri(i.songUri))
                    }
                }
                viewModel.queuedSongs = viewModel.shuffleSongInfo.toMutableStateList()
                viewModel.updateLastPlayedUnshuffledSong()
                viewModel.songIndex = 0
                mediaController?.prepare()
                mediaController?.play()
            } else { // Switching to normal playback
                viewModel.songIndex = viewModel.lastPlayedUnshuffledSong
                mediaController?.clearMediaItems()
                if (viewModel.playingFromSongsScreen) { // Playing from songs screen
                    viewModel.queuedSongs = songInfo.toMutableStateList()
                    for (i in songInfo) {
                        mediaController?.addMediaItem(MediaItem.fromUri(i.songUri))
                    }
                } else { // Playing from albums screen
                    viewModel.queuedSongs = viewModel.shuffledAlbumSongInfo.toMutableStateList()
                    for (i in viewModel.shuffledAlbumSongInfo) {
                        mediaController?.addMediaItem(MediaItem.fromUri(i.songUri))
                    }
                }
                mediaController?.prepare()
                mediaController?.seekTo(viewModel.songIndex, 0L)
                viewModel.incrementSongIterator(1)
            }
            viewModel.shuffleMode = !viewModel.shuffleMode
        },
        modifier = Modifier
            .size(40.dp),
        colors = IconButtonColors(
            containerColor = Color.Transparent,
            contentColor = viewModel.iconColor,
            disabledContentColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        content = {
            Icon(
                painter = (
                        if (!viewModel.shuffleMode) {
                            painterResource(R.drawable.arrow_right)
                        } else {
                            painterResource(R.drawable.shuffle)
                        }
                        ),
                contentDescription = "Shuffle controls"
            )
        }
    )
}

//============================== Seek bar ==============================//
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekBar(
    mediaController: MediaController?,
    viewModel: PlayerViewModel,
    widthFactor: Float = 1f
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(widthFactor)
            .height(20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        var currentSongPosition by remember { mutableFloatStateOf(viewModel.currentSongPosition) }
        var isSeeking by remember { mutableStateOf(false) }
        LcdText(
            getSongPositionString(viewModel, isSeeking, currentSongPosition),
            viewModel = viewModel
        )
        Slider(
            value = currentSongPosition,
            valueRange = 0f..viewModel.duration,
            modifier = Modifier
                .size(244.dp, 20.dp),
            onValueChange = {
                isSeeking = true
                currentSongPosition = it
            },
            onValueChangeFinished = {
                viewModel.updateSongPosition(mediaController, currentSongPosition.toLong())
                sleep(40)
                isSeeking = false
            },
            thumb = {
                SliderThumb(viewModel)
            },
            track = {
                SliderTrack(viewModel)
            },
        )
        LcdText(
            getSongDurationString(viewModel),
            viewModel = viewModel
        )
        LaunchedEffect(viewModel.currentSongPosition) {
            if (!isSeeking) {
                currentSongPosition = viewModel.currentSongPosition
            }
        }
    }
}

fun getSongPositionString(
    viewModel: PlayerViewModel,
    isSeeking: Boolean,
    currentSongPosition: Float
): String {
    var seconds: String
    var minutes: String
    if (!isSeeking) {
        minutes = "${(viewModel.currentSongPosition / 60).toInt()}:"
        seconds = "${(viewModel.currentSongPosition % 60).toInt()}"
    } else {
        minutes = "${(currentSongPosition / 60).toInt()}:"
        seconds = "${(currentSongPosition % 60).toInt()}"
    }
    while (seconds.length < 2) {
        seconds = "0$seconds"
    }
    return "$minutes$seconds"
}

fun getSongDurationString(viewModel: PlayerViewModel): String {
    val minutes = "${(viewModel.duration / 60).toInt()}:"
    var seconds = "${(viewModel.duration % 60).toInt()}"
    while (seconds.length < 2) {
        seconds = "0$seconds"
    }
    return "$minutes$seconds"
}

@Composable
fun SliderThumb(viewModel: PlayerViewModel) {
    Column(
        modifier = Modifier
            .size(30.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            modifier = Modifier,
            onDraw = {
                drawCircle(
                    color = viewModel.sliderThumbColor,
                    radius = 25f,
                    center = this.center,
                    style = Fill
                )
            }
        )
    }
}

@Composable
fun SliderTrack(viewModel: PlayerViewModel) {
    Column(
        modifier = Modifier
            .height(15.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Canvas(
            modifier = Modifier,
            onDraw = {
                drawRoundRect(
                    size = Size(600f, 15f),
                    style = Fill,
                    color = viewModel.sliderTrackColor,
                    cornerRadius = CornerRadius(10f, 10f),
                    topLeft = Offset(0f, -6.5f)
                )
            }
        )
    }
}

@ExperimentalMaterial3Api
@OptIn(UnstableApi::class)
@Composable
fun AudioEffectMenu(
    viewModel: PlayerViewModel,
    audioProcessor: PlayerService.SpectrumAnalyzer,
    mediaController: MediaController?,
) {
    val popupOffset =
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            IntOffset(175, -345)
        } else {
            IntOffset(60, 330)
        }
    IconButton( // Speed & pitch change
        onClick = {
            if (!viewModel.audioEffectMenuExpanded) {
                viewModel.audioEffectMenuExpanded = true
            }
        },
        modifier = Modifier
            .size(40.dp),
        colors = IconButtonColors(
            containerColor = Color.Transparent,
            contentColor = viewModel.iconColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = viewModel.iconColor,
        ),
        enabled = !viewModel.audioEffectMenuExpanded,
    ) {
        Icon(
            painter = (
                    painterResource(R.drawable.speed_pitch)
                    ),
            contentDescription = "Audio effects"
        )
    }
    // Popup audio effects menu
    if (viewModel.audioEffectMenuExpanded) {
        Popup(
            alignment = Alignment.Center,
            offset = popupOffset,
            onDismissRequest = {
                viewModel.audioEffectMenuExpanded = false
                viewModel.audioEffectSpeed = audioProcessor.speed
                viewModel.audioEffectPitch = audioProcessor.pitch
            },
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Column(
                modifier = Modifier
                    .animateContentSize()
                    .size(viewModel.menuWidth, 215.dp)
                    .background(viewModel.backgroundColor)
                    .border(0.dp, viewModel.iconColor),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val sliderColumnWidth = viewModel.menuWidth.value / 2f
                Spacer(Modifier.height(5.dp))
                Row(
                    modifier = Modifier
                        .animateContentSize()
                        .size(viewModel.menuWidth, 180.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    val sliderHeight = 120.dp // Is read as width due to rotation
                    // Speed slider
                    SpeedPitchSlider(viewModel, sliderColumnWidth, sliderHeight, "Speed")
                    JointSpeedPitchSlider(viewModel)
                    SpeedPitchSlider(viewModel, sliderColumnWidth, sliderHeight, "Pitch")
                }
                ApplyChangesButton(mediaController, audioProcessor, viewModel)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ApplyChangesButton(
    mediaController: MediaController?,
    audioProcessor: PlayerService.SpectrumAnalyzer,
    viewModel: PlayerViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            modifier = Modifier
                .size(50.dp),
            onClick = {
                val tmpIsPlaying = viewModel.isPlaying
                if (tmpIsPlaying) { mediaController?.pause() }
                audioProcessor.speed = viewModel.audioEffectSpeed
                audioProcessor.pitch = viewModel.audioEffectPitch
                audioProcessor.usingSonicProcessor = audioProcessor.pitch != 1f || audioProcessor.speed != 1f
                audioProcessor.configure(
                    AudioProcessor.AudioFormat(
                        44100,
                        2,
                        C.ENCODING_PCM_16BIT
                    )
                )
                audioProcessor.flush()
                if (tmpIsPlaying) { mediaController?.play() }
                viewModel.updateSongDuration(
                    audioProcessor.getDurationAfterProcessorApplied(viewModel.duration.toLong())
                )
                viewModel.audioEffectMenuExpanded = false
            },
            colors = IconButtonDefaults.iconButtonColors(contentColor = viewModel.iconColor)
        ) {
            Icon(
                painterResource(R.drawable.done_tick),
                contentDescription = "Apply changes"
            )
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedPitchSlider(viewModel: PlayerViewModel, sliderColumnWidth: Float, sliderHeight: Dp, type: String) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .animateContentSize()
            .width(sliderColumnWidth.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LcdText(
            "$type:",
            viewModel = viewModel
        )
        Slider(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints.fixed(
                            width = sliderHeight.roundToPx(),
                            height = 15.dp.roundToPx()
                        )
                    )
                    layout(15.dp.roundToPx(), sliderHeight.roundToPx()) {
                        placeable.place(
                            x = -(sliderHeight.roundToPx() - 15.dp.roundToPx()) / 2,
                            y = (sliderHeight.roundToPx() - 15.dp.roundToPx()) / 2,
                        )
                    }
                }
                .graphicsLayer(
                    rotationZ = -90f
                )
                .size(sliderHeight, 15.dp),
            value = (
                    if (type == "Speed") {
                        viewModel.audioEffectSpeed
                    } else {
                        viewModel.audioEffectPitch
                    }
                    ),
            valueRange = 0f..2f,
            steps = 39, // For 0.05 increments
            onValueChange = {
                (
                        if (type == "Speed") {
                            viewModel.audioEffectSpeed = it
                        } else {
                            viewModel.audioEffectPitch = it
                        }
                )
            },
            thumb = {
                SliderThumb(viewModel)
            },
            track = {
                SettingsSliderTrack(viewModel)
            },
        )
        LargeLcdText(
            "%.2f".format(
                    if (type == "Speed") {
                        viewModel.audioEffectSpeed
                    } else {
                        viewModel.audioEffectPitch
                    }
                    ),
            viewModel = viewModel,
            lineHeight = 1.sp
        )
    }
}

@ExperimentalMaterial3Api
@Composable
fun JointSpeedPitchSlider(viewModel: PlayerViewModel) {
    var combinedValue by remember {
        mutableFloatStateOf(
            if (viewModel.audioEffectSpeed == viewModel.audioEffectPitch) {
                viewModel.audioEffectSpeed
            } else {
                (viewModel.audioEffectPitch + viewModel.audioEffectSpeed) / 2f
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(10.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Slider(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints.fixed(
                            width = 120.dp.roundToPx(),
                            height = 15.dp.roundToPx()
                        )
                    )
                    layout(15.dp.roundToPx(), 120.dp.roundToPx()) {
                        placeable.place(
                            x = -(120.dp.roundToPx() - 15.dp.roundToPx()) / 2,
                            y = (120.dp.roundToPx() - 15.dp.roundToPx()) / 2,
                        )
                    }
                }
                .graphicsLayer(
                    rotationZ = -90f
                )
                .size(120.dp, 15.dp),
            value = combinedValue,
            valueRange = 0f..2f,
            steps = 39, // For 0.05 increments
            onValueChange = {
                combinedValue = it
                viewModel.audioEffectSpeed = it
                viewModel.audioEffectPitch = it
            },
            thumb = { CombinedSliderThumb() },
            track = { Spacer(Modifier.width(120.dp)) },
        )
    }
}

@Composable
fun CombinedSliderThumb() {
    Canvas(
        modifier = Modifier
            .size(30.dp)
    ) {
        val path = Path()
        path.moveTo(45f,-5f)
        path.relativeLineTo(0f, 40f)
        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(
                width = 1f
            )
        )
    }
}

@Composable
fun SettingsSliderTrack(viewModel: PlayerViewModel) {
    Column(
        modifier = Modifier
            .height(15.dp)
            .width(280.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Canvas(
            modifier = Modifier,
            onDraw = {
                drawRoundRect(
                    size = Size(250f, 15f),
                    style = Fill,
                    color = viewModel.sliderTrackColor,
                    cornerRadius = CornerRadius(10f, 10f),
                    topLeft = Offset(0f, -6.5f)
                )
            }
        )
    }
}