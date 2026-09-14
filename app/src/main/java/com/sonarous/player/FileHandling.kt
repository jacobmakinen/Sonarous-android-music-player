package com.sonarous.player

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.asImageBitmap
import androidx.media3.common.util.UnstableApi
import java.io.FileNotFoundException

@OptIn(UnstableApi::class)
fun getSongInfo(context: Context): Pair<List<SongInfo>, List<AlbumInfo>> {
    val songInfo = mutableListOf<SongInfo>()
    val externalUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media._ID
    )
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(
        externalUri,
        projection,
        MediaStore.Audio.Media.IS_MUSIC,
        null,
        null
    )
    val albumCoverNotFoundBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.album_art_not_found).asImageBitmap()

    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val fileNameColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val nameColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        while (it.moveToNext()) {
            val getName = it.getString(nameColumn)
            val getFileName = it.getString(fileNameColumn)
            val getAlbum = it.getString(albumColumn)
            val getArtist = it.getString(artistColumn)
            var getDuration = it.getDouble(durationColumn)
            val getId = it.getLong(idColumn)
            getDuration /= 1000
            val duration = getDuration.toFloat()
            val songUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, getId
            )
            val albumCover = try {
                contentResolver.loadThumbnail(
                    songUri,
                    android.util.Size(500,500),
                    null
                ).asImageBitmap()
            } catch (_: FileNotFoundException) {
                albumCoverNotFoundBitmap
            }
            songInfo.add(
                SongInfo(
                    getName,
                    getFileName,
                    songUri,
                    duration,
                    getArtist,
                    getAlbum,
                    albumCover
                )
            )
        }
    }
    val albumInfo = mutableListOf<AlbumInfo>()
    val addedAlbumNames = mutableListOf<String>()
    for (i in 0 until songInfo.size) {
        if (songInfo[i].album in addedAlbumNames) {
            continue
        } else {
            addedAlbumNames.add(songInfo[i].album)
            albumInfo.add(
                AlbumInfo(
                    songInfo[i].album,
                    songInfo[i].albumArt
                )
            )
        }
    }
    return Pair(MergeSort.sort(songInfo), MergeSort.sort(albumInfo))
}