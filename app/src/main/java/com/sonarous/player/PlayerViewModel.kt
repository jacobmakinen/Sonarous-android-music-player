package com.sonarous.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.media3.session.MediaController
import com.sonarous.player.ui.theme.LcdBlueWhite
import com.sonarous.player.ui.theme.LcdGrey

class PlayerViewModel : ViewModel() {
    //========================= Media info =========================
    var duration by mutableFloatStateOf(0f) // Length of song
        private set
    var currentSongPosition by mutableFloatStateOf((0f)) // Current position in song
    var songIndex by mutableIntStateOf(0)
    var selectedAlbum by mutableStateOf("")
    var shuffledAlbumSongInfo = mutableListOf<SongInfo>()
    var queuedSongs = mutableStateListOf<SongInfo>()
    var shuffleSongInfo = listOf<SongInfo>()
    var lastPlayedUnshuffledSong = 0
    var mediaInfoPair: Pair<List<SongInfo>, List<AlbumInfo>>? = null
    //========================= Playing modes =========================
    var isPlaying by mutableStateOf(false)
    var playingFromSongsScreen by mutableStateOf(true)
    var shuffleMode by mutableStateOf(false)
    var repeatMode by mutableStateOf("normal")
    var queueingSongs by mutableStateOf(false)
    //============================ Colours ===========================
    var backgroundColor = LcdGrey
    var textColor = Color.White
    var iconColor = Color.White
    var eqLevelColor = Color.White
    var eqTextColor = LcdBlueWhite
    var sliderThumbColor = Color.White
    var sliderTrackColor = Color.White
    val colorMap = mutableStateMapOf(
        "Dark blue" to LcdGrey,
        "Red" to Color.Red,
        "Green" to Color.Green,
        "Blue" to Color.Blue,
        "Light blue" to LcdBlueWhite,
        "Yellow" to Color.Yellow,
        "Orange" to Color(0xffFFA500),
        "Black" to Color.Black,
        "White" to Color.White,
        "Light grey" to Color(0xffcccccc),
        "Pink" to Color(0xffFFC0CB),
        "Purple" to Color(0xffA020F0),
    )
    val otherColorMap = mutableMapOf(
        "White" to Color.White,
        "Red" to Color.Red,
        "Green" to Color.Green,
        "Blue" to Color.Blue,
        "Light blue" to LcdBlueWhite,
        "Yellow" to Color.Yellow,
        "Orange" to Color(0xffFFA500),
        "Black" to Color.Black,
        "Pink" to Color(0xffFFC0CB),
        "Purple" to Color(0xffA020F0)
    )
    val customColorMap = mutableMapOf<String, Int>()
    //========================= Miscellaneous/Settings =========================//
    var loadingFinished by mutableStateOf(false)
    var showEqualiser by mutableStateOf(true)
    var audioEffectMenuExpanded by mutableStateOf(false)
    var audioEffectSpeed by mutableFloatStateOf(1f)
    var audioEffectPitch by mutableFloatStateOf(1f)
    val menuWidth by mutableStateOf(120.dp)
    //========================= More options screen =========================//
    var showMoreOptions by mutableStateOf(false)
    lateinit var moreOptionsSelectedSong: SongInfo
    //========================= Init from Json =========================//
    fun initViewModel(context: Context) {
        val settingsManager = SettingsManager(context)
        val settings = settingsManager.loadSettings()
        backgroundColor = Color(settings.backgroundColor)
        textColor = Color(settings.textColor)
        iconColor = Color(settings.iconColor)
        eqTextColor = Color(settings.eqTextColor)
        eqLevelColor = Color(settings.eqLevelColor)
        sliderThumbColor = Color(settings.sliderThumbColor)
        sliderTrackColor = Color(settings.sliderTrackColor)
        customColorMap.putAll(settings.customColors)
        val intToColorMap = mutableMapOf<String, Color>()
        for (i in settings.customColors.keys) {
            intToColorMap[i] = Color(settings.customColors[i]!!)
        }
        colorMap.putAll(intToColorMap)
        otherColorMap.putAll(intToColorMap)
        showEqualiser = settings.showEqualiser
    }
    //========================= Setters =========================
    fun updateLastPlayedUnshuffledSong() {
        lastPlayedUnshuffledSong = songIndex
    }
    fun updateCustomColors(color: Color, name: String) {
        colorMap[name] = color
        otherColorMap[name] = color
        customColorMap[name] = color.toArgb()
    }

    fun updateColor(choice: String, color: Color?) {
        if (color != null) {
            when (choice) {
                "background" -> {
                    backgroundColor = color
                }
                "text" -> {
                    textColor = color
                }
                "icon" -> {
                    iconColor = color
                }
                "eqLevel" -> {
                    eqLevelColor = color
                }
                "eqText" -> {
                    eqTextColor = color
                }
                "sliderThumb" -> {
                    sliderThumbColor = color
                }
                "sliderTrack" -> {
                    sliderTrackColor = color
                }
            }
        }
    }
    fun incrementSongIterator(increment: Int) {
        songIndex += increment
    }
    fun updateCurrentSongPosition(time: Long) {
        currentSongPosition = time.toFloat() / 1000f
    }
    fun updateSongDuration(time: Long) {
        duration = time / 1000f
    }
    fun updateSongPosition(mediaController: MediaController?, time: Long) {
        mediaController?.seekTo(time * 1000)
    }
}