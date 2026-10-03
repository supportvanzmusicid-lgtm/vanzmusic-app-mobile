package com.vanz.musicplayer.data.remote

import com.vanz.musicplayer.data.model.ItunesSearchResponse
import com.vanz.musicplayer.data.model.LrcRecord
import com.vanz.musicplayer.data.model.YouTubeSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

const val YOUTUBE_API_KEY = "AIzaSyDfskbcdB6BckmsW9w0ZTG4nf2HPboDu1E"

interface YouTubeApiService {
    @GET("youtube/v3/search")
    suspend fun searchVideos(
        @Query("q") query: String,
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 25,
        @Query("key") apiKey: String = YOUTUBE_API_KEY
    ): YouTubeSearchResponse
}

interface ItunesApiService {
    @GET("search")
    suspend fun searchTracks(
        @Query("term") term: String,
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 25
    ): ItunesSearchResponse
}

interface LrcApiService {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String
    ): LrcRecord

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): List<LrcRecord>
}
