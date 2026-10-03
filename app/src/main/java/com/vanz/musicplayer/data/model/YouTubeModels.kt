package com.vanz.musicplayer.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
    val items: List<YouTubeSearchResultItem>? = emptyList(),
    val nextPageToken: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResultItem(
    val id: YouTubeResourceId?,
    val snippet: YouTubeSnippet?
)

@JsonClass(generateAdapter = true)
data class YouTubeResourceId(
    val kind: String?,
    val videoId: String?
)

@JsonClass(generateAdapter = true)
data class YouTubeSnippet(
    val title: String?,
    val channelTitle: String?,
    val description: String?,
    val publishedAt: String?,
    val thumbnails: YouTubeThumbnails?
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnails(
    val default: YouTubeThumbnailInfo?,
    val medium: YouTubeThumbnailInfo?,
    val high: YouTubeThumbnailInfo?,
    val standard: YouTubeThumbnailInfo?,
    val maxres: YouTubeThumbnailInfo?
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnailInfo(
    val url: String?,
    val width: Int?,
    val height: Int?
)
