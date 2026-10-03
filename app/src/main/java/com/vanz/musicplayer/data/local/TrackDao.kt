package com.vanz.musicplayer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM saved_tracks WHERE isFavorite = 1 ORDER BY playedAt DESC, id DESC")
    fun getFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM saved_tracks WHERE playedAt > 0 ORDER BY playedAt DESC LIMIT 50")
    fun getRecentlyPlayedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM saved_tracks WHERE playlistId = :playlistId ORDER BY playedAt ASC")
    fun getTracksForPlaylist(playlistId: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM saved_tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTrack(track: TrackEntity)

    @Query("UPDATE saved_tracks SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    @Query("UPDATE saved_tracks SET playedAt = :playedAt WHERE id = :id")
    suspend fun updatePlayedAt(id: String, playedAt: Long)

    @Query("DELETE FROM saved_tracks WHERE id = :id")
    suspend fun deleteTrack(id: String)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)
}
