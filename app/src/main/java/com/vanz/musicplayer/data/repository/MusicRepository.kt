package com.vanz.musicplayer.data.repository

import android.util.Log
import com.vanz.musicplayer.data.local.MusicDatabase
import com.vanz.musicplayer.data.local.PlaylistEntity
import com.vanz.musicplayer.data.local.TrackEntity
import com.vanz.musicplayer.data.model.LyricLine
import com.vanz.musicplayer.data.model.LrcParser
import com.vanz.musicplayer.data.model.Track
import com.vanz.musicplayer.data.remote.AudioStreamResolver
import com.vanz.musicplayer.data.remote.ItunesApiService
import com.vanz.musicplayer.data.remote.LrcApiService
import com.vanz.musicplayer.data.remote.NetworkClient
import com.vanz.musicplayer.data.remote.YouTubeApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

class MusicRepository(
    private val youTubeApiService: YouTubeApiService,
    private val itunesApiService: ItunesApiService,
    private val lrcApiService: LrcApiService,
    private val audioStreamResolver: AudioStreamResolver,
    private val database: MusicDatabase
) {
    companion object {
        private const val TAG = "MusicRepository"

        private val PIPED_SEARCH_INSTANCES = listOf(
            "https://api.piped.private.coffee",
            "https://pipedapi.kavin.rocks",
            "https://pa.il.ax"
        )
    }

    private val trackDao = database.trackDao()

    suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()

        // 1. YouTube Data API v3
        try {
            val response = youTubeApiService.searchVideos(query = query)
            val items = response.items ?: emptyList()
            for (item in items) {
                val videoId = item.id?.videoId ?: continue
                val snippet = item.snippet ?: continue
                val title = cleanTitle(snippet.title ?: "Unknown Title")
                val artist = snippet.channelTitle ?: "Unknown Artist"
                val thumbUrl = snippet.thumbnails?.high?.url
                    ?: snippet.thumbnails?.medium?.url
                    ?: snippet.thumbnails?.default?.url
                    ?: ""
                val highResUrl = snippet.thumbnails?.maxres?.url
                    ?: snippet.thumbnails?.standard?.url
                    ?: thumbUrl

                tracks.add(
                    Track(
                        id = videoId,
                        title = title,
                        artist = artist,
                        thumbnailUrl = thumbUrl,
                        highResThumbnailUrl = highResUrl
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "YouTube API search: ${e.message}")
        }

        // 2. Piped Instances Fallback
        if (tracks.isEmpty()) {
            for (instance in PIPED_SEARCH_INSTANCES) {
                try {
                    val searchUrl = "$instance/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&filter=all"
                    val request = Request.Builder()
                        .url(searchUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .build()

                    val response = NetworkClient.okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val items = json.optJSONArray("items")
                            if (items != null && items.length() > 0) {
                                for (i in 0 until items.length()) {
                                    val item = items.getJSONObject(i)
                                    val url = item.optString("url", "")
                                    val videoId = url.replace("/watch?v=", "").replace("/watch?v", "")
                                    if (videoId.isNotBlank() && !videoId.startsWith("/")) {
                                        val title = cleanTitle(item.optString("title", "Unknown Title"))
                                        val artist = item.optString("uploaderName", "Unknown Artist")
                                        val thumb = item.optString("thumbnail", "")
                                        tracks.add(
                                            Track(
                                                id = videoId,
                                                title = title,
                                                artist = artist,
                                                thumbnailUrl = thumb,
                                                highResThumbnailUrl = thumb,
                                                durationMs = item.optLong("duration", 0) * 1000L
                                            )
                                        )
                                    }
                                }
                                if (tracks.isNotEmpty()) break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Piped instance $instance failed: ${e.message}")
                }
            }
        }

        // 3. Apple Music / iTunes Search API
        if (tracks.isEmpty()) {
            try {
                val itunesRes = itunesApiService.searchTracks(term = query)
                val results = itunesRes.results ?: emptyList()
                for (item in results) {
                    val trackId = item.trackId?.toString() ?: continue
                    val title = item.trackName ?: continue
                    val artist = item.artistName ?: "Unknown Artist"
                    val artwork100 = item.artworkUrl100 ?: item.artworkUrl60 ?: ""
                    val highResArt = artwork100.replace("100x100bb.jpg", "600x600bb.jpg")
                    val duration = item.trackTimeMillis ?: 0L
                    val preview = item.previewUrl

                    tracks.add(
                        Track(
                            id = trackId,
                            title = title,
                            artist = artist,
                            thumbnailUrl = artwork100,
                            highResThumbnailUrl = highResArt,
                            durationMs = duration,
                            streamUrl = preview
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "iTunes search fallback error: ${e.message}")
            }
        }

        // 4. Curated Catalog Fallback
        if (tracks.isEmpty()) {
            tracks.addAll(getCuratedTracks().filter {
                it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
            })
            if (tracks.isEmpty()) {
                tracks.addAll(getCuratedTracks())
            }
        }

        return@withContext tracks.distinctBy { it.id }
    }

    suspend fun resolveStreamUrl(track: Track): String {
        if (!track.streamUrl.isNullOrBlank()) {
            return track.streamUrl
        }
        return audioStreamResolver.resolveAudioUrl(track.id)
    }

    suspend fun fetchLyrics(trackTitle: String, artistName: String): Pair<List<LyricLine>, String?> = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = cleanTrackNameForLyrics(trackTitle)
            val cleanArtist = cleanArtistNameForLyrics(artistName)

            try {
                val record = lrcApiService.getLyrics(trackName = cleanTitle, artistName = cleanArtist)
                if (!record.syncedLyrics.isNullOrBlank()) {
                    val lines = LrcParser.parse(record.syncedLyrics)
                    return@withContext Pair(lines, record.plainLyrics)
                } else if (!record.plainLyrics.isNullOrBlank()) {
                    return@withContext Pair(emptyList(), record.plainLyrics)
                }
            } catch (ignored: Exception) {}

            val searchQuery = "$cleanTitle $cleanArtist".trim()
            val searchResults = lrcApiService.searchLyrics(query = searchQuery)
            if (searchResults.isNotEmpty()) {
                val bestWithSynced = searchResults.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
                if (bestWithSynced != null) {
                    val lines = LrcParser.parse(bestWithSynced.syncedLyrics)
                    return@withContext Pair(lines, bestWithSynced.plainLyrics)
                }
                val firstPlain = searchResults.firstOrNull { !it.plainLyrics.isNullOrBlank() }
                if (firstPlain != null) {
                    return@withContext Pair(emptyList(), firstPlain.plainLyrics)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Lyrics search failed: ${e.message}")
        }
        return@withContext Pair(emptyList(), null)
    }

    // Room Database Operations
    fun getFavoriteTracks(): Flow<List<Track>> {
        return trackDao.getFavoriteTracks().map { list ->
            list.map { it.toTrack() }
        }
    }

    fun getRecentlyPlayedTracks(): Flow<List<Track>> {
        return trackDao.getRecentlyPlayedTracks().map { list ->
            list.map { it.toTrack() }
        }
    }

    suspend fun toggleFavorite(track: Track): Boolean = withContext(Dispatchers.IO) {
        val existing = trackDao.getTrackById(track.id)
        val newFav = if (existing != null) !existing.isFavorite else true
        val entity = TrackEntity.fromTrack(
            track = track,
            isFavorite = newFav,
            playedAt = existing?.playedAt ?: System.currentTimeMillis()
        )
        trackDao.insertOrUpdateTrack(entity)
        return@withContext newFav
    }

    suspend fun recordPlayedTrack(track: Track) = withContext(Dispatchers.IO) {
        val existing = trackDao.getTrackById(track.id)
        val entity = TrackEntity.fromTrack(
            track = track,
            isFavorite = existing?.isFavorite ?: false,
            playedAt = System.currentTimeMillis()
        )
        trackDao.insertOrUpdateTrack(entity)
    }

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = trackDao.getAllPlaylists()

    suspend fun createPlaylist(name: String, description: String = "") = withContext(Dispatchers.IO) {
        val id = "pl_" + System.currentTimeMillis()
        val playlist = PlaylistEntity(id = id, name = name, description = description)
        trackDao.insertPlaylist(playlist)
    }

    private fun cleanTitle(raw: String): String {
        return raw.replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun cleanTrackNameForLyrics(title: String): String {
        return title
            .replace(Regex("(?i)\\(official\\s*(music\\s*)?video\\)"), "")
            .replace(Regex("(?i)\\[official\\s*(music\\s*)?video\\]"), "")
            .replace(Regex("(?i)\\(audio\\)"), "")
            .replace(Regex("(?i)\\[audio\\]"), "")
            .replace(Regex("(?i)\\(lyric\\s*video\\)"), "")
            .replace(Regex("(?i)\\[lyric\\s*video\\]"), "")
            .replace(Regex("(?i)\\(lyrics\\)"), "")
            .replace(Regex("(?i)\\[lyrics\\]"), "")
            .replace(Regex("(?i)\\(visualizer\\)"), "")
            .replace(Regex("(?i)\\(ft\\..*?\\)"), "")
            .replace(Regex("(?i)\\(feat\\..*?\\)"), "")
            .trim()
    }

    private fun cleanArtistNameForLyrics(artist: String): String {
        return artist
            .replace(Regex("(?i) - topic"), "")
            .replace(Regex("(?i)vevo"), "")
            .replace(Regex("(?i)official"), "")
            .trim()
    }

    fun getCuratedTracks(): List<Track> {
        return listOf(
            Track(
                id = "nao_all_of_me",
                title = "All Of Me",
                artist = "Nao",
                album = "And Then Life Was Beautiful",
                thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/ed/fc/6e/edfc6e5e-a6a9-ce5a-353d-24fc9b3aa596/886449339304.jpg/300x300bb.jpg",
                highResThumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/ed/fc/6e/edfc6e5e-a6a9-ce5a-353d-24fc9b3aa596/886449339304.jpg/600x600bb.jpg",
                durationMs = 212000L,
                audioQuality = "Dolby Atmos"
            ),
            Track(
                id = "khalid_outta_my_head",
                title = "Outta My Head",
                artist = "Khalid & John Mayer",
                album = "Free Spirit",
                thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/bf/f4/bc/bff4bc7e-2cf0-a299-cbf1-a8e0e7a2df2f/886447614809.jpg/300x300bb.jpg",
                highResThumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/bf/f4/bc/bff4bc7e-2cf0-a299-cbf1-a8e0e7a2df2f/886447614809.jpg/600x600bb.jpg",
                durationMs = 177000L,
                audioQuality = "Lossless"
            ),
            Track(
                id = "honne_warm_on_a_cold_night",
                title = "Warm on a Cold Night (10 Years)",
                artist = "HONNE",
                album = "Warm on a Cold Night",
                thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/f2/14/c1/f214c1eb-1808-1f51-a20c-7b02c89f5bc5/0190295941916.jpg/300x300bb.jpg",
                highResThumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/f2/14/c1/f214c1eb-1808-1f51-a20c-7b02c89f5bc5/0190295941916.jpg/600x600bb.jpg",
                durationMs = 268000L,
                audioQuality = "Dolby Atmos"
            ),
            Track(
                id = "bernadya_untungnya",
                title = "Untungnya, Hidup Harus Tetap Berjalan",
                artist = "Bernadya",
                album = "Sialnya, Hidup Harus Tetap Berjalan",
                thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/61/fa/63/61fa635a-fc24-b63a-00eb-a88371c28eab/196874323458.jpg/300x300bb.jpg",
                highResThumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/61/fa/63/61fa635a-fc24-b63a-00eb-a88371c28eab/196874323458.jpg/600x600bb.jpg",
                durationMs = 210000L,
                audioQuality = "Lossless"
            ),
            Track(
                id = "die_with_a_smile",
                title = "Die With A Smile",
                artist = "Lady Gaga & Bruno Mars",
                album = "Die With A Smile - Single",
                thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/ed/46/bf/ed46bf4e-7cb9-965a-54f3-03059977fe6c/075679589293.jpg/300x300bb.jpg",
                highResThumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/ed/46/bf/ed46bf4e-7cb9-965a-54f3-03059977fe6c/075679589293.jpg/600x600bb.jpg",
                durationMs = 251000L,
                audioQuality = "Dolby Atmos"
            ),
            Track(
                id = "birds_of_a_feather",
                title = "BIRDS OF A FEATHER",
                artist = "Billie Eilish",
                album = "HIT ME HARD AND SOFT",
                thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/7b/0a/61/7b0a6147-920f-0796-039c-850fefaa2458/24UMGIM35305.rgb.jpg/300x300bb.jpg",
                highResThumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/7b/0a/61/7b0a6147-920f-0796-039c-850fefaa2458/24UMGIM35305.rgb.jpg/600x600bb.jpg",
                durationMs = 190000L,
                audioQuality = "Dolby Atmos"
            ),
            Track(
                id = "cruel_summer",
                title = "Cruel Summer",
                artist = "Taylor Swift",
                album = "Lover",
                thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/71/34/23/71342378-0056-b8db-47d3-05c069b2b527/19UMGIM69458.rgb.jpg/300x300bb.jpg",
                highResThumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/71/34/23/71342378-0056-b8db-47d3-05c069b2b527/19UMGIM69458.rgb.jpg/600x600bb.jpg",
                durationMs = 178000L,
                audioQuality = "Lossless"
            )
        )
    }
}
