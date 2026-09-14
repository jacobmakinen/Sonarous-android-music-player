package com.sonarous.player

object MergeSort {
    @JvmName ("albumSort")
    fun sort(albums: List<AlbumInfo>): List<AlbumInfo> {
        // Base case
        if (albums.size <= 1) {
            return albums
        } else {
            val midpoint = albums.size / 2
            var left = albums.subList(0, midpoint)
            var right = albums.subList(midpoint, albums.size)
            left = sort(left)
            right = sort(right)
            val sorted = merge(left, right)
            return sorted
        }
    }

    fun sort(songs: List<SongInfo>): List<SongInfo> {
        // Base case
        if (songs.size <= 1) {
            return songs
        } else {
            val midpoint = songs.size / 2
            var left = songs.subList(0, midpoint)
            var right = songs.subList(midpoint, songs.size)
            left = sort(left)
            right = sort(right)
            val sorted = merge(left, right)
            return sorted
        }
    }

    @JvmName ("artistSort")
    fun sort(artists: List<String>): List<String> {
        // Base case
        if (artists.size <= 1) {
            return artists
        } else {
            val midpoint = artists.size / 2
            var left = artists.subList(0, midpoint)
            var right = artists.subList(midpoint, artists.size)
            left = sort(left)
            right = sort(right)
            val sorted = merge(left, right)
            return sorted
        }
    }

    @JvmName ("artistMerge")
    private fun merge(left: List<String>, right: List<String>): List<String> {
        val mergedList = mutableListOf<String>()
        var leftIndex = 0
        var rightIndex = 0

        while (leftIndex < left.size || rightIndex < right.size) {
            try {
                if (left[leftIndex].lowercase() < right[rightIndex].lowercase()) {
                    mergedList.add(left[leftIndex])
                    leftIndex ++
                } else {
                    mergedList.add(right[rightIndex])
                    rightIndex ++
                }
            } catch (_: IndexOutOfBoundsException) {
                if (leftIndex >= left.size) {
                    for (i in rightIndex until right.size) {
                        mergedList.add(right[i])
                        rightIndex ++
                    }
                } else {
                    for (i in leftIndex until left.size) {
                        mergedList.add(left[i])
                        leftIndex ++
                    }
                }
            }
        }

        return mergedList
    }
    @JvmName ("albumMerge")
    private fun merge(left: List<AlbumInfo>, right: List<AlbumInfo>): List<AlbumInfo> {
        val mergedList = mutableListOf<AlbumInfo>()
        var leftIndex = 0
        var rightIndex = 0

        while (leftIndex < left.size || rightIndex < right.size) {
            try {
                if (left[leftIndex].albumName.lowercase() < right[rightIndex].albumName.lowercase()) {
                    mergedList.add(left[leftIndex])
                    leftIndex++
                } else {
                    mergedList.add(right[rightIndex])
                    rightIndex++
                }
            } catch (_: IndexOutOfBoundsException) {
                if (leftIndex >= left.size) {
                    for (i in rightIndex until right.size) {
                        mergedList.add(right[i])
                        rightIndex ++
                    }
                } else {
                    for (i in leftIndex until left.size) {
                        mergedList.add(left[i])
                        leftIndex ++
                    }
                }
            }
        }

        return mergedList
    }

    private fun merge(left: List<SongInfo>, right: List<SongInfo>): List<SongInfo> {
        val mergedList = mutableListOf<SongInfo>()
        var leftIndex = 0
        var rightIndex = 0

        while (leftIndex < left.size || rightIndex < right.size) {
            try {
                if (left[leftIndex].name.lowercase() < right[rightIndex].name.lowercase()) {
                    mergedList.add(left[leftIndex])
                    leftIndex++
                } else {
                    mergedList.add(right[rightIndex])
                    rightIndex++
                }
            } catch (_: IndexOutOfBoundsException) {
                if (leftIndex >= left.size) {
                    for (i in rightIndex until right.size) {
                        mergedList.add(right[i])
                        rightIndex ++
                    }
                } else {
                    for (i in leftIndex until left.size) {
                        mergedList.add(left[i])
                        leftIndex ++
                    }
                }
            }
        }

        return mergedList
    }
}