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
    val songs = mutableListOf<SongInfo>()
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

        it.apply {
            while (moveToNext()) {
                val getName = getString(nameColumn)
                val getFileName = getString(fileNameColumn)
                val getAlbum = getString(albumColumn)
                val getArtist = getString(artistColumn)
                val getDuration = getDouble(durationColumn) / 1000
                val getId = getLong(idColumn)
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
                songs.add(
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
    }

    val albums = mutableListOf<AlbumInfo>()
    val addedAlbumNames = mutableListOf<String>()
    // Fill albums
    for (i in 0 until songs.size) {
        if (songs[i].album in addedAlbumNames) {
            continue
        } else {
            addedAlbumNames.add(songs[i].album)
            albums.add(
                AlbumInfo(
                    songs[i].album,
                    songs[i].albumArt
                )
            )
        }
    }
    return Pair(MergeSort.sort(songs), MergeSort.sort(albums))
}