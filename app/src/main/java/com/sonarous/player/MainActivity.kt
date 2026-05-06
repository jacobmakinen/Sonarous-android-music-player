package com.sonarous.player

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.sonarous.player.ui.theme.Audio_playerTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.sonarous.player.components.PlayerContentObserver
import com.sonarous.player.components.PlayerListener
import com.sonarous.player.components.PlayerService
import com.sonarous.player.components.PlayerViewModel
import com.sonarous.player.screens.BasicLoadingScreen
import com.sonarous.player.screens.editSongAlbumArt
import com.sonarous.player.screens.editSongTag
import com.sonarous.player.ui.theme.shareTechFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<PlayerViewModel>(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlayerViewModel(
                    ) as T
                }
            }
        }
    )
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    private lateinit var observer: PlayerContentObserver

    @SuppressLint("InlinedApi")
    @ExperimentalFoundationApi
    @OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        var songInfo: List<SongInfo>?
//        var albumInfo: List<AlbumInfo>?
        var songInfo = mutableStateListOf<SongInfo>()
        var albumInfo = mutableStateListOf<AlbumInfo>()

        //==================== Assign permission launchers ====================//
        val requestPermissionLauncher = registerForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { requests ->
            // Request file access
            if (Manifest.permission.READ_MEDIA_AUDIO in requests.keys || Manifest.permission.READ_EXTERNAL_STORAGE in requests.keys) {
                when {
                    Manifest.permission.READ_MEDIA_AUDIO in requests.keys -> {
                        if (requests[Manifest.permission.READ_MEDIA_AUDIO] == true) {
                            viewModel.mediaInfoPair = getSongInfo(applicationContext)
                        } else {
                            requestPermissions(
                                arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                                1
                            )
                        }
                    }

                    Manifest.permission.READ_EXTERNAL_STORAGE in requests.keys -> {
                        if (requests[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
                            viewModel.mediaInfoPair = getSongInfo(applicationContext)
                        } else {
                            requestPermissions(
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                                1
                            )
                        }
                    }
                }
            }
            if (Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK in requests.keys || Manifest.permission.POST_NOTIFICATIONS in requests.keys) {
                when {
                    Manifest.permission.POST_NOTIFICATIONS in requests.keys -> {
                        if (requests[Manifest.permission.POST_NOTIFICATIONS] == false) {
                            requestPermissions(
                                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                3
                            )
                        }
                    }

                    Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK in requests.keys -> {
                        if (requests[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
                            requestPermissions(
                                arrayOf(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK),
                                4
                            )
                        }
                    }
                }
            }
        }

        viewModel.editAlbumArtLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && viewModel.replicatedAlbumArt != null) {
                editSongAlbumArt(this,viewModel.moreOptionsSelectedSong.songUri, viewModel.replicatedAlbumArt!!, viewModel)
            }
        }

        viewModel.editSongTagLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && viewModel.editSongTags != null) {
                editSongTag(this,viewModel.moreOptionsSelectedSong.songUri, viewModel.editSongTags!!, viewModel)
            }
        }

        // --------------------- Loading --------------------- //

        // Sets the settings' variables from the json
        viewModel.initViewModel(applicationContext)

        // Init media dependencies
        var mediaController: MediaController? = null
        controllerFuture = MediaController.Builder(
            this,
            SessionToken(
                this,
                ComponentName(this, PlayerService::class.java)
            )
        ).buildAsync()
        controllerFuture.addListener(
            { mediaController = controllerFuture.get() },
            MoreExecutors.directExecutor()
        )

        // --------------------- Assign content observer --------------------- //
        observer = PlayerContentObserver(Handler(Looper.getMainLooper())) {
            getMediaInfo(this, requestPermissionLauncher).also {
                if (it == null) return@also
                songInfo.clear()
                albumInfo.clear()
                songInfo.addAll(it.first)
                albumInfo.addAll(it.second)
            }
        }
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        lifecycleScope.launch {
            val audioProcessor = PlayerService.SpectrumAnalyzer
            audioProcessor.visualiserIsOn = true
            viewModel.mediaInfoPair = getMediaInfo(applicationContext, requestPermissionLauncher)

            enableEdgeToEdge()
            setContent {
                BasicLoadingScreen(viewModel)
            }
            while (mediaController == null || viewModel.mediaInfoPair == null) {
                delay(50)
            }
            while (!viewModel.loadingFinished) {
                delay(10)
            }

            songInfo.addAll(viewModel.mediaInfoPair!!.first)
            albumInfo.addAll(viewModel.mediaInfoPair!!.second)
            val listener = PlayerListener(applicationContext, viewModel, mediaController)
            mediaController.addListener(listener)

            setContent {
                Audio_playerTheme {
                    NavHost(
                        mediaController,
                        songInfo,
                        audioProcessor,
                        viewModel,
                        albumInfo,
                        applicationContext
                    )

                    if (viewModel.isPlaying) {
                        LaunchedEffect(Unit) {
                            while (true) {
                                mediaController.let { viewModel.updateCurrentSongPosition(it.currentPosition) }
                                delay(1.seconds / 30)
                            }
                        }
                        LaunchedEffect(Unit) {
                            while (true) {
                                mediaController.let {
                                    if (it.duration != C.TIME_UNSET) {
                                        viewModel.updateSongDuration(mediaController.duration)
                                    }
                                    delay(1.seconds / 30)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PlayerService.SpectrumAnalyzer.visualiserIsOn = true
    }

    override fun onStop() {
        super.onStop()
        PlayerService.SpectrumAnalyzer.visualiserIsOn = false
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(observer)
        MediaController.releaseFuture(controllerFuture)
        super.onDestroy()
    }
}

@SuppressLint("UnsafeOptInUsageError")
fun getMediaInfo(
    context: Context,
    requestPermissionLauncher: ActivityResultLauncher<Array<String>>
): Pair<List<SongInfo>, List<AlbumInfo>>? {
    val permissionList = mutableListOf<String>()
    var mediaInfoPair: Pair<List<SongInfo>, List<AlbumInfo>>? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_DENIED -> {
                permissionList.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK
            ) == PackageManager.PERMISSION_DENIED -> {
                permissionList.add(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK)
            }
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        when (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        ) {
            PackageManager.PERMISSION_DENIED -> {
                permissionList.add(Manifest.permission.READ_MEDIA_AUDIO)
            }

            PackageManager.PERMISSION_GRANTED -> {
                mediaInfoPair = getSongInfo(context)
            }
        }
    } else {
        when (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            PackageManager.PERMISSION_DENIED -> {
                permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            PackageManager.PERMISSION_GRANTED -> {
                mediaInfoPair = getSongInfo(context)
            }
        }
    }
    requestPermissionLauncher.launch(permissionList.toTypedArray())
    return mediaInfoPair
}

// Uppercase as lcd font only supports capitals
@Composable
fun Text(text: String, modifier: Modifier = Modifier, viewModel: PlayerViewModel) {
    Text(
        modifier = modifier,
        text = if (text.length > 25) {
            "${text.removeRange(26 until text.length)}..."
        } else {
            text
        },
        color = viewModel.textColor,
        fontSize = 15.sp,
        fontFamily = shareTechFont,
        fontWeight = FontWeight.Normal,
        lineHeight = 4.sp,
    )
}

@Composable
fun LargeText(
    text: String,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel,
    lineHeight: TextUnit = 17.sp
) {
    Text(
        modifier = modifier,
        text = text,
        color = viewModel.textColor,
        fontSize = 17.sp,
        fontFamily = shareTechFont,
        fontWeight = FontWeight.Normal,
        lineHeight = lineHeight
    )
}

@Composable
fun PlayerLargeText(text: String, modifier: Modifier = Modifier, viewModel: PlayerViewModel) {
    Text(
        modifier = modifier,
        text = if (text.length > 31) {
            "${text.removeRange(32 until text.length)}..."
        } else {
            text
        },
        color = viewModel.textColor,
        fontSize = 25.sp,
        fontFamily = shareTechFont,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun PlayerText(text: String, modifier: Modifier = Modifier, viewModel: PlayerViewModel) {
    Text(
        modifier = modifier,
        text = if (text.length > 31) {
            "${text.removeRange(32 until text.length)}..."
        } else {
            text
        },
        color = viewModel.textColor,
        fontSize = 20.sp,
        fontFamily = shareTechFont,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun AlbumScreenText(
    text: String,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel
) {
    Text(
        modifier = modifier,
        text = text,
        color = viewModel.textColor,
        fontSize = 15.sp,
        fontFamily = shareTechFont,
        fontWeight = FontWeight.Normal,
        lineHeight = 15.sp
    )
}

@Composable
fun MyTextField(value: MutableState<String>) {
    BasicTextField(
        value = value.value,
        onValueChange = {
            value.value = it
        },
    )
}

fun Color.increaseBrightness(brightness: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[2] += brightness
    return Color(ColorUtils.HSLToColor(hsl))
}