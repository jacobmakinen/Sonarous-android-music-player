package com.sonarous.player

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
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
import com.sonarous.player.ui.theme.lcdFont
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
class MainActivity : ComponentActivity() {
    val viewModel by viewModels<PlayerViewModel>(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlayerViewModel(
                    ) as T
                }
            }
        }
    )
    lateinit var controllerFuture: ListenableFuture<MediaController>

    @SuppressLint("InlinedApi")
    @ExperimentalFoundationApi
    @OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var songInfo: List<SongInfo>?
        var albumInfo: List<AlbumInfo>?
        //==================== Check & request permissions ====================//
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
        lifecycleScope.launch {
            val audioProcessor = PlayerService.SpectrumAnalyzer
            audioProcessor.visualiserIsOn = true
            viewModel.mediaInfoPair = requestInitPermissions(applicationContext, requestPermissionLauncher)

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

            songInfo = viewModel.mediaInfoPair!!.first
            albumInfo = viewModel.mediaInfoPair!!.second
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
        MediaController.releaseFuture(controllerFuture)
        super.onDestroy()
    }
}

@SuppressLint("UnsafeOptInUsageError")
fun requestInitPermissions(
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
fun LcdText(text: String, modifier: Modifier = Modifier, viewModel: PlayerViewModel) {
    Text(
        modifier = modifier,
        text = if (text.length > 29) {
            "${text.removeRange(30 until text.length)}...".uppercase()
        } else {
            text.uppercase()
        },
        color = viewModel.textColor,
        fontSize = 15.sp,
        fontFamily = lcdFont,
        fontWeight = FontWeight.Normal,
        lineHeight = 4.sp
    )
}

@Composable
fun LargeLcdText(
    text: String,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel,
    lineHeight: TextUnit = 17.sp
) {
    Text(
        modifier = modifier,
        text = text.uppercase(),
        color = viewModel.textColor,
        fontSize = 20.sp,
        fontFamily = lcdFont,
        fontWeight = FontWeight.Normal,
        lineHeight = lineHeight
    )
}

@Composable
fun PlayerLargeLcdText(text: String, modifier: Modifier = Modifier, viewModel: PlayerViewModel) {
    Text(
        modifier = modifier,
        text = if (text.length > 31) {
            "${text.removeRange(32 until text.length)}...".uppercase()
        } else {
            text.uppercase()
        },
        color = viewModel.textColor,
        fontSize = 25.sp,
        fontFamily = lcdFont,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun PlayerLcdText(text: String, modifier: Modifier = Modifier, viewModel: PlayerViewModel) {
    Text(
        modifier = modifier,
        text = if (text.length > 31) {
            "${text.removeRange(32 until text.length)}...".uppercase()
        } else {
            text.uppercase()
        },
        color = viewModel.textColor,
        fontSize = 20.sp,
        fontFamily = lcdFont,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun AlbumScreenLcdText(
    text: String,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel
) {
    Text(
        modifier = modifier,
        text = text.uppercase(),
        color = viewModel.textColor,
        fontSize = 15.sp,
        fontFamily = lcdFont,
        fontWeight = FontWeight.Normal,
        lineHeight = 15.sp
    )
}

fun Color.increaseBrightness(brightness: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[2] += brightness
    return Color(ColorUtils.HSLToColor(hsl))
}