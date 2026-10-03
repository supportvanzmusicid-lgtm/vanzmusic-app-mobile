package com.vanz.musicplayer.ui.viewmodel

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.vanz.musicplayer.data.local.PlaylistEntity
import com.vanz.musicplayer.data.model.LyricLine
import com.vanz.musicplayer.data.model.Track
import com.vanz.musicplayer.data.repository.MusicRepository
import com.vanz.musicplayer.player.AppPlaybackState
import com.vanz.musicplayer.player.MusicPlayerManager
import com.vanz.musicplayer.ui.theme.AppleMusicRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val searchResults: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val recentSearches: List<String> = listOf(
        "Bernadya",
        "Nao",
        "HONNE",
        "Khalid",
        "Bruno Mars",
        "Juicy Luicy",
        "Billie Eilish",
        "Taylor Swift"
    )
)

data class RadioStation(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String,
    val query: String
)

data class HomeUiState(
    val topPicks: List<Track> = emptyList(),
    val recentlyPlayed: List<Track> = emptyList(),
    val radioStations: List<RadioStation> = emptyList(),
    val newReleases: List<Track> = emptyList(),
    val isLoading: Boolean = false
)

data class PlayerColors(
    val dominantColor: Color = AppleMusicRed,
    val vibrantColor: Color = AppleMusicRed,
    val darkMutedColor: Color = Color(0xFF1A1A1E),
    val lightVibrantColor: Color = Color(0xFFFF7597)
)

class MusicViewModel(
    private val repository: MusicRepository,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    companion object {
        private const val TAG = "MusicViewModel"
    }

    private val _searchUiState = MutableStateFlow(SearchUiState())
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _syncedLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val syncedLyrics: StateFlow<List<LyricLine>> = _syncedLyrics.asStateFlow()

    private val _plainLyrics = MutableStateFlow<String?>(null)
    val plainLyrics: StateFlow<String?> = _plainLyrics.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    private val _isSeeking = MutableStateFlow(false)
    val isSeeking: StateFlow<Boolean> = _isSeeking.asStateFlow()

    private val _seekPosition = MutableStateFlow(0L)
    val seekPosition: StateFlow<Long> = _seekPosition.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    private val _isLyricsViewOpen = MutableStateFlow(false)
    val isLyricsViewOpen: StateFlow<Boolean> = _isLyricsViewOpen.asStateFlow()

    private val _playerColors = MutableStateFlow(PlayerColors())
    val playerColors: StateFlow<PlayerColors> = _playerColors.asStateFlow()

    val playbackState: StateFlow<AppPlaybackState> = playerManager.playbackState
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val trackDuration: StateFlow<Long> = playerManager.duration
    val volumeState: StateFlow<Float> = playerManager.volume
    val isShuffle: StateFlow<Boolean> = playerManager.isShuffle
    val repeatMode: StateFlow<Int> = playerManager.repeatMode
    val currentQueue: StateFlow<List<Track>> = playerManager.queue

    val currentLyricIndex: StateFlow<Int> = combine(
        currentPosition,
        _syncedLyrics
    ) { positionMs, lyrics ->
        if (lyrics.isEmpty()) return@combine -1
        val foundIndex = lyrics.indexOfLast { line -> line.timestampMs <= positionMs }
        if (foundIndex >= 0) foundIndex else 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    val favoriteTracks: StateFlow<List<Track>> = repository.getFavoriteTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayedTracks: StateFlow<List<Track>> = repository.getRecentlyPlayedTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customPlaylists: StateFlow<List<PlaylistEntity>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null
    private var lyricsJob: Job? = null

    init {
        playerManager.setOnTrackChangedListener { nextTrack ->
            if (nextTrack != null && nextTrack.id != _currentTrack.value?.id) {
                viewModelScope.launch {
                    loadAndPlayTrackInternal(nextTrack)
                }
            }
        }

        // Set initial default track matching user screenshot ("Outta My Head" by Khalid or "All Of Me" by Nao)
        val defaultTracks = repository.getCuratedTracks()
        _currentTrack.value = defaultTracks[1] // Khalid - Outta My Head

        loadInitialHomeContent()
    }

    private fun loadInitialHomeContent() {
        viewModelScope.launch {
            _homeUiState.value = _homeUiState.value.copy(isLoading = true)
            try {
                val curated = repository.getCuratedTracks()
                val radioList = listOf(
                    RadioStation(
                        id = "radio_discovery",
                        title = "Discovery Station",
                        subtitle = "Radio Station",
                        coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Features126/v4/44/14/c1/4414c1eb-1808-1f51-a20c-7b02c89f5bc5/mza_1067439327581781293.png/300x300bb.png",
                        query = "Apple Music Discovery Station"
                    ),
                    RadioStation(
                        id = "radio_nts",
                        title = "NTS Radio 1",
                        subtitle = "TuneIn",
                        coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/55/12/34/551234cb-e5eb-0498-5c46-d56df3d85d71/cover.jpg/300x300bb.jpg",
                        query = "NTS Radio Live"
                    ),
                    RadioStation(
                        id = "radio_vanities",
                        title = "Vanities",
                        subtitle = "Malibu",
                        coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/88/99/00/88990073-b3c1-0955-f126-1077759aa59d/cover.jpg/300x300bb.jpg",
                        query = "Vanities Malibu Chill"
                    ),
                    RadioStation(
                        id = "radio_apple1",
                        title = "Apple Music 1",
                        subtitle = "Live Radio",
                        coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Features115/v4/33/22/11/332211aa-b3c1-0955-f126-1077759aa59d/mza_384729103.png/300x300bb.jpg",
                        query = "Apple Music 1 Live Hits"
                    )
                )

                _homeUiState.value = HomeUiState(
                    topPicks = curated,
                    recentlyPlayed = curated.drop(1),
                    radioStations = radioList,
                    newReleases = curated,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Home load error", e)
                _homeUiState.value = _homeUiState.value.copy(isLoading = false)
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchUiState.value = _searchUiState.value.copy(query = "", searchResults = emptyList(), isLoading = false)
            return
        }

        _searchUiState.value = _searchUiState.value.copy(query = query, isLoading = true, error = null)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                val results = repository.searchTracks(query)
                val updatedRecent = (_searchUiState.value.recentSearches.toMutableList().apply {
                    remove(query)
                    add(0, query)
                }).take(10)

                _searchUiState.value = _searchUiState.value.copy(
                    searchResults = results,
                    isLoading = false,
                    recentSearches = updatedRecent
                )
            } catch (e: Exception) {
                _searchUiState.value = _searchUiState.value.copy(
                    isLoading = false,
                    error = "Gagal memuat hasil pencarian: ${e.message}"
                )
            }
        }
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            loadAndPlayTrackInternal(track)
        }
    }

    fun playQueue(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        val target = tracks[startIndex.coerceIn(0, tracks.size - 1)]
        viewModelScope.launch {
            _currentTrack.value = target
            repository.recordPlayedTrack(target)
            val streamUrl = repository.resolveStreamUrl(target)
            playerManager.playQueue(tracks, startIndex, streamUrl)
            loadLyricsForTrack(target)
        }
    }

    private suspend fun loadAndPlayTrackInternal(track: Track) {
        _currentTrack.value = track
        repository.recordPlayedTrack(track)

        val streamUrl = repository.resolveStreamUrl(track)
        playerManager.playTrack(track, streamUrl)
        loadLyricsForTrack(track)
    }

    private fun loadLyricsForTrack(track: Track) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _isLyricsLoading.value = true
            _syncedLyrics.value = emptyList()
            _plainLyrics.value = null
            try {
                val (synced, plain) = repository.fetchLyrics(track.title, track.artist)
                _syncedLyrics.value = synced
                _plainLyrics.value = plain
            } catch (e: Exception) {
                Log.w(TAG, "Lyrics load failed", e)
            } finally {
                _isLyricsLoading.value = false
            }
        }
    }

    fun togglePlayPause() {
        val cur = _currentTrack.value
        if (playerManager.playbackState.value == AppPlaybackState.IDLE && cur != null) {
            playTrack(cur)
        } else {
            playerManager.togglePlayPause()
        }
    }

    fun playNext() {
        playerManager.playNextTrack()
    }

    fun playPrevious() {
        playerManager.playPreviousTrack()
    }

    fun onSeekingProgress(posMs: Long) {
        _isSeeking.value = true
        _seekPosition.value = posMs
    }

    fun onSeekingFinished(posMs: Long) {
        _isSeeking.value = false
        playerManager.seekTo(posMs)
    }

    fun seekTo(posMs: Long) {
        playerManager.seekTo(posMs)
    }

    fun setVolume(vol: Float) {
        playerManager.setVolume(vol)
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun cycleRepeatMode() {
        playerManager.cycleRepeatMode()
    }

    fun expandPlayer() {
        _isPlayerExpanded.value = true
    }

    fun collapsePlayer() {
        _isPlayerExpanded.value = false
    }

    fun toggleLyricsView() {
        _isLyricsViewOpen.value = !_isLyricsViewOpen.value
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val newStatus = repository.toggleFavorite(track)
            if (_currentTrack.value?.id == track.id) {
                _currentTrack.value = _currentTrack.value?.copy(isFavorite = newStatus)
            }
        }
    }

    fun createPlaylist(name: String, desc: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(name, desc)
        }
    }

    fun addToQueue(track: Track) {
        playerManager.addToQueue(track)
    }

    fun playNextInQueue(track: Track) {
        playerManager.playNext(track)
    }

    fun extractPaletteFromUrl(context: Context, imageUrl: String) {
        if (imageUrl.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val req = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()

                val result = (loader.execute(req) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    val dominant = palette.getDominantColor(0xFFFF2D55.toInt())
                    val vibrant = palette.getVibrantColor(0xFFFF2D55.toInt())
                    val darkMuted = palette.getDarkMutedColor(0xFF1C1C1E.toInt())
                    val lightVibrant = palette.getLightVibrantColor(0xFFFF7597.toInt())

                    _playerColors.value = PlayerColors(
                        dominantColor = Color(dominant),
                        vibrantColor = Color(vibrant),
                        darkMutedColor = Color(darkMuted),
                        lightVibrantColor = Color(lightVibrant)
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Palette extraction error: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
