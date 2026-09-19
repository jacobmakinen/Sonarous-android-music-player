package com.sonarous.player.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.sonarous.player.AlbumInfo
import com.sonarous.player.NavHost
import com.sonarous.player.SongInfo
import com.sonarous.player.Text
import com.sonarous.player.getSongInfo
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
                    return PlayerViewModel() as T
                }
            }
        }
    )
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    private lateinit var observer: PlayerContentObserver

    // Index update broadcast receiver
    private val songIndexReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, receiverIntent: Intent?) {
            if (receiverIntent?.action == OverlayService.ACTION_UPDATE_INDEX) {
                viewModel.songIndex = receiverIntent.getIntExtra(OverlayService.EXTRA_INDEX, 0)
            }
        }
    }

    private var isUpdateReceiverRegistered = false

    @ExperimentalFoundationApi
    @OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val songInfo = mutableStateListOf<SongInfo>()
        val albumInfo = mutableStateListOf<AlbumInfo>()

        //==================== Assign activity launchers ====================//
        val requestPermissionLauncher = getFileActivityLauncher()

        setSongEditingActivityLaunchers()

        // --------------------- Loading --------------------- //

        // Sets the settings' variables from the JSON
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

        // --------------------- Assign thermal monitoring --------------------- //
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val thermalListener = PowerManager.OnThermalStatusChangedListener { status -> viewModel.thermalStatus = status }
        powerManager.addThermalStatusListener(thermalListener)
        viewModel.thermalStatus = powerManager.currentThermalStatus

        // --------------------- UI --------------------- //
        lifecycleScope.launch {
            val audioProcessor = PlayerService.AudioVisualizerProcessor
            audioProcessor.visualiserIsOn = true
            viewModel.mediaInfoPair = getMediaInfo(applicationContext, requestPermissionLauncher)

            val determinedOverlayRequest = mutableStateOf(false)
            enableEdgeToEdge()
            setContent {
                DrawOverlayPermission(determinedOverlayRequest, viewModel)
                viewModel.showOverlay = Settings.canDrawOverlays(this@MainActivity)

                if (determinedOverlayRequest.value) {
                    BasicLoadingScreen(viewModel)
                }
            }
            while (mediaController == null || viewModel.mediaInfoPair == null) {
                delay(50)
            }
            while (!viewModel.loadingFinished || !determinedOverlayRequest.value) {
                delay(10)
            }

            songInfo.addAll(viewModel.mediaInfoPair!!.first)
            albumInfo.addAll(viewModel.mediaInfoPair!!.second)
            val listener = PlayerListener(applicationContext, viewModel, mediaController)
            mediaController.addListener(listener)

            setContent {
                NavHost(
                    mediaController,
                    songInfo,
                    audioProcessor,
                    viewModel,
                    albumInfo,
                    applicationContext
                )

                // --------------------- Updating --------------------- //
                if (viewModel.isPlaying) {
                    LaunchedEffect(Unit) {
                        while (true) {
                            viewModel.updateCurrentSongPosition(mediaController.currentPosition)
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
                LaunchedEffect(viewModel.thermalStatus) {
                    if (viewModel.thermalStatus >= PowerManager.THERMAL_STATUS_EMERGENCY) {
                        audioProcessor.visualiserIsOn = false
                    } else if (viewModel.thermalStatus <= PowerManager.THERMAL_STATUS_SEVERE) {
                        // TODO - Should check to see if user wants visualizer hidden
                        audioProcessor.visualiserIsOn = true
                    }
                }
            }
        }
    }

    @Composable
    private fun DrawOverlayPermission(determinedOverlayRequest: MutableState<Boolean>, viewModel: PlayerViewModel) {
        if (Settings.canDrawOverlays(this@MainActivity)) {
            determinedOverlayRequest.value = true
            return
        }
        var launchRequestActivity by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(viewModel.backgroundColor),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Text(
                modifier = Modifier.fillMaxWidth(0.7f),
                text = "Allow overlaying over other apps for enhanced song selection controls",
                color = viewModel.textColor,
                fontSize = 14.sp,
                fontFamily = shareTechFont,
                fontWeight = FontWeight.Normal
            )

            TextButton(
                onClick = { launchRequestActivity = true }
            ) {
                Text("Yes", viewModel = viewModel)
            }
            TextButton(
                onClick = { determinedOverlayRequest.value = true }
            ) {
                Text("No", viewModel = viewModel)
            }
        }

        if (launchRequestActivity) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
            )
            determinedOverlayRequest.value = true
        }
    }

    private fun setSongEditingActivityLaunchers() {
        viewModel.editAlbumArtLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && viewModel.replicatedAlbumArt != null) {
                editSongAlbumArt(this,viewModel.moreOptionsSelectedSong.uri, viewModel.replicatedAlbumArt!!, viewModel)
            }
        }

        viewModel.editSongTagLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && viewModel.editSongTags != null) {
                editSongTag(this,viewModel.moreOptionsSelectedSong.uri, viewModel.editSongTags!!, viewModel)
            }
        }
    }

    private fun getFileActivityLauncher(): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(
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
    }

    override fun onStart() {
        super.onStart()
        if (isUpdateReceiverRegistered) {
            unregisterReceiver(songIndexReceiver)
            isUpdateReceiverRegistered = false
        }

        PlayerService.AudioVisualizerProcessor.visualiserIsOn = true
        stopService(Intent(this, OverlayService::class.java))
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStop() {
        super.onStop()
        if (viewModel.showOverlay) {
            val filter = IntentFilter(OverlayService.ACTION_UPDATE_INDEX)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(songIndexReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(songIndexReceiver, filter)
            }
            isUpdateReceiverRegistered = true

            startForegroundService(Intent(this, OverlayService::class.java))
        }

        PlayerService.AudioVisualizerProcessor.visualiserIsOn = false
    }

    override fun onDestroy() {
        if (isUpdateReceiverRegistered) {
            unregisterReceiver(songIndexReceiver)
            isUpdateReceiverRegistered = false
        }
        contentResolver.unregisterContentObserver(observer)
        MediaController.releaseFuture(controllerFuture)
        // Tie the services to the main activity to prevent memory leaks
        stopService(Intent(this, PlayerService::class.java))
        if (viewModel.showOverlay) stopService(Intent(this, OverlayService::class.java))
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
