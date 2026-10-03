package com.vanz.musicplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vanz.musicplayer.data.model.Track

@Entity(tableName = "saved_tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val highResThumbnailUrl: String,
    val durationMs: Long,
    val isFavorite: Boolean,
    val playedAt: Long = 0L,
    val playlistId: String? = null
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        thumbnailUrl = thumbnailUrl,
        highResThumbnailUrl = highResThumbnailUrl,
        durationMs = durationMs,
        isFavorite = isFavorite
    )

    companion object {
        fun fromTrack(track: Track, isFavorite: Boolean = track.isFavorite, playedAt: Long = 0L, playlistId: String? = null): TrackEntity {
            return TrackEntity(
                id = track.id,
                title = track.title,
                artist = track.artist,
                thumbnailUrl = track.thumbnailUrl,
                highResThumbnailUrl = track.highResThumbnailUrl,
                durationMs = track.durationMs,
                isFavorite = isFavorite,
                playedAt = playedAt,
                playlistId = playlistId
            )
        }
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val coverUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
