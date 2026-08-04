package com.sonarous.player

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap

data class VisualiserData(
    val visualiserList: DoubleArray,
    val volume: Double
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VisualiserData

        if (volume != other.volume) return false
        if (!visualiserList.contentEquals(other.visualiserList)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = volume.hashCode()
        result = 31 * result + visualiserList.contentHashCode()
        return result
    }
}

data class SongInfo(
    val name: String,
    val fileName: String,
    val uri: Uri,
    val time: Float,
    val artist: String,
    val album: String,
    val albumArt: ImageBitmap
)

data class AlbumInfo(
    val albumName: String,
    val albumArt: ImageBitmap
)