package com.sonarous.player.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import com.sonarous.player.Text
import com.sonarous.player.components.PlayerViewModel
import com.sonarous.player.R
import com.sonarous.player.SongInfo
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

@Composable
fun MoreSongOptions(viewModel: PlayerViewModel, mediaController: MediaController?, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x44000000)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Popup(
            onDismissRequest = {
                viewModel.showMoreSongOptions = !viewModel.showMoreSongOptions
            },
            properties = PopupProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                focusable = true
            ),
            alignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.55f)
                    .fillMaxWidth(0.65f)
                    .padding(horizontal = 5.dp)
                    .background(viewModel.backgroundColor)
                    .border(0.dp, viewModel.iconColor)
                    .padding(5.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                AddSongToQueue(viewModel, mediaController)
                ReplicateAlbumArt(viewModel)
                ReplaceAlbumArt(viewModel, context)
            }
        }
    }
}

@Composable
fun ReplicateAlbumArt(viewModel: PlayerViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clickable(
                onClick = {
                    viewModel.replicatedAlbumArt = viewModel.moreOptionsSelectedSong.albumArt.asAndroidBitmap()
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            painter = painterResource(R.drawable.copy_album_art),
            contentDescription = "Copy album art",
            tint = viewModel.iconColor,
        )
        Text("Copy album art", viewModel = viewModel)
    }
}

@Composable
fun ReplaceAlbumArt(viewModel: PlayerViewModel, context: Context) {
    val songUri = viewModel.moreOptionsSelectedSong.songUri
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clickable(
                onClick = {
                    val writeRequest = MediaStore.createWriteRequest(context.contentResolver, listOf(songUri))

                    viewModel.editSongLauncher?.launch(IntentSenderRequest.Builder(writeRequest.intentSender).build())
                },
                enabled = viewModel.replicatedAlbumArt != null
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            painter = painterResource(R.drawable.copy_album_art),
            contentDescription = "Copy album art",
            tint = viewModel.iconColor,
        )
        Text("Replace album art", viewModel = viewModel)
    }
}

fun editSongAlbumArt(context: Context, songUri: Uri, bitmap: Bitmap, viewModel: PlayerViewModel) {
    val tempFile = File(context.cacheDir, "temp_${viewModel.moreOptionsSelectedSong.fileName}")

    try {
        // Copy original to temp file
        context.contentResolver.openInputStream(songUri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Failed to open input stream for $songUri")

        // Edit metadata on temp file
        val audioFile = AudioFileIO.read(tempFile)
        val tag = audioFile.tagOrCreateAndSetDefault

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)

        val artwork = ArtworkFactory.getNew().apply {
            binaryData = stream.toByteArray()
            mimeType = "image/jpeg"
            pictureType = 3
        }

        tag.deleteArtworkField()
        tag.setField(artwork)
        audioFile.commit()

        // "rwt" mode: opens for read/write and truncates only after opening,
        // preventing corruption if the stream fails to open
        context.contentResolver.openOutputStream(songUri, "rwt")?.use { output ->
            tempFile.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: throw IOException("Failed to open output stream for $songUri")

    } finally {
        tempFile.delete()
    }
}

fun setAlbumArtFromBitmap(tag: Tag, bitmap: Bitmap) {
    // Convert Bitmap to ByteArray
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
    val imageData = stream.toByteArray()

    // Build Artwork manually
    val artwork = ArtworkFactory.getNew().apply {
        binaryData = imageData
        mimeType = "image/jpeg"
        pictureType = 3  // 3 = Front Cover (standard ID3 picture type)
    }

    // Apply to tag
    tag.deleteArtworkField()
    tag.setField(artwork)
}

@Composable
fun AddSongToQueue(viewModel: PlayerViewModel, mediaController: MediaController?) {
    val song = viewModel.moreOptionsSelectedSong
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clickable(
                onClick = { addSongToQueueLogic(mediaController, song, viewModel) }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            painter = painterResource(R.drawable.queue_music),
            contentDescription = "Add song to queue",
            tint = viewModel.iconColor,
        )
        Text("Add song to queue", viewModel = viewModel)
    }
}

fun addSongToQueueLogic(mediaController: MediaController?, song: SongInfo, viewModel: PlayerViewModel) {
    if (viewModel.queueingSongs) {
        mediaController?.addMediaItem(MediaItem.fromUri(song.songUri))
        viewModel.queuedSongs.add(song)
    } else {
        viewModel.queueingSongs = true
        // Remove all songs except the currently playing one
        mediaController?.removeMediaItems(
            viewModel.songIndex + 1,
            viewModel.queuedSongs.size
        )
        mediaController?.removeMediaItems(0, viewModel.songIndex)
        viewModel.queuedSongs.removeAll { song ->
            song != viewModel.queuedSongs[viewModel.songIndex]
        }
        viewModel.queuedSongs.add(song)
        mediaController?.addMediaItem(MediaItem.fromUri(song.songUri))
        viewModel.songIndex = 0
    }
    viewModel.showMoreSongOptions = false
}