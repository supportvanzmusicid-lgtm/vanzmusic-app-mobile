package com.vanz.musicplayer.data.model

data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val endTimeMs: Long = Long.MAX_VALUE
)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val highResThumbnailUrl: String = thumbnailUrl,
    val durationMs: Long = 0L,
    val isFavorite: Boolean = false,
    val streamUrl: String? = null,
    val syncedLyrics: List<LyricLine> = emptyList(),
    val plainLyrics: String? = null,
    val album: String = "Vanz Music",
    val audioQuality: String = "Dolby Atmos"
)
