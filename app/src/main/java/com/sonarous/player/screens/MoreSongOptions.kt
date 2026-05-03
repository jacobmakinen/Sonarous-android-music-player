package com.sonarous.player.screens

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import org.jaudiotagger.audio.asf.data.ContentDescription
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

@Composable
fun MoreSongOptions(
    viewModel: PlayerViewModel,
    mediaController: MediaController?,
    context: Context,
) {
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
                DeleteSong(viewModel, context.contentResolver)
            }
        }
    }
}

@Composable
fun ReplicateAlbumArt(viewModel: PlayerViewModel) {
    MoreOptionRow(
        viewModel,
        true,
        "Copy album art",
        R.drawable.copy_album_art,
        "Copy album art",
    ) {
        viewModel.replicatedAlbumArt = viewModel.moreOptionsSelectedSong.albumArt.asAndroidBitmap()
        viewModel.showMoreSongOptions = false
    }
}

@Composable
fun DeleteSong(viewModel: PlayerViewModel, contentResolver: ContentResolver) {
    val songUri = viewModel.moreOptionsSelectedSong.songUri
    MoreOptionRow(
        viewModel,
        true,
        "Delete song",
        R.drawable.delete,
        "Delete song",
    ) {
        val request = MediaStore.createDeleteRequest(contentResolver, listOf(songUri))
        viewModel.editSongLauncher?.launch(IntentSenderRequest.Builder(request).build())
        viewModel.showMoreSongOptions = false
    }
}

@Composable
fun MoreOptionRow(
    viewModel: PlayerViewModel,
    enabled: Boolean = true,
    contentDescription: String,
    @DrawableRes iconId: Int,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable(
                onClick = onClick,
                enabled = enabled
            )
            .padding(start = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            painter = painterResource(iconId),
            contentDescription = contentDescription,
            tint = viewModel.iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(text, viewModel = viewModel)
    }
}

@Composable
fun ReplaceAlbumArt(viewModel: PlayerViewModel, context: Context) {
    val songUri = viewModel.moreOptionsSelectedSong.songUri
    MoreOptionRow(
        viewModel,
        viewModel.replicatedAlbumArt != null,
        "Replace album art",
        R.drawable.replace_image,
        "Replace album art"
    ) {
        val writeRequest = MediaStore.createWriteRequest(context.contentResolver, listOf(songUri))
        viewModel.editSongLauncher?.launch(
            IntentSenderRequest.Builder(writeRequest.intentSender).build()
        )
        viewModel.showMoreSongOptions = false
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

@Composable
fun AddSongToQueue(viewModel: PlayerViewModel, mediaController: MediaController?) {
    val song = viewModel.moreOptionsSelectedSong
    MoreOptionRow(
        viewModel,
        true,
        "Add song to queue",
        R.drawable.queue_music,
        "Add song to queue"
    ) {
        addSongToQueueLogic(mediaController, song, viewModel)
    }
}

fun addSongToQueueLogic(
    mediaController: MediaController?,
    song: SongInfo,
    viewModel: PlayerViewModel,
) {
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