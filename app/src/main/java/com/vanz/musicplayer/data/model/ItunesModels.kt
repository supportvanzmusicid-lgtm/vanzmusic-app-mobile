package com.vanz.musicplayer.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ItunesSearchResponse(
    val resultCount: Int? = 0,
    val results: List<ItunesTrackItem>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class ItunesTrackItem(
    val trackId: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val collectionName: String? = null,
    val previewUrl: String? = null,
    val artworkUrl100: String? = null,
    val artworkUrl60: String? = null,
    val trackTimeMillis: Long? = null
)
