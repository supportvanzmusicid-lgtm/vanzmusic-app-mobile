package com.vanz.musicplayer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vanz.musicplayer.ui.theme.AppleMusicRed
import com.vanz.musicplayer.ui.theme.DarkBackground
import com.vanz.musicplayer.ui.theme.DarkSurface
import com.vanz.musicplayer.ui.theme.DarkSurfaceVariant
import com.vanz.musicplayer.ui.theme.TextPrimary
import com.vanz.musicplayer.ui.theme.TextSecondary
import com.vanz.musicplayer.ui.viewmodel.MusicViewModel

enum class MainTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    LISTEN_NOW("Listen Now", Icons.Filled.PlayCircle, Icons.Filled.PlayCircle),
    BROWSE("Browse", Icons.Filled.GridView, Icons.Outlined.GridView),
    RADIO("Radio", Icons.Filled.Radio, Icons.Outlined.Radio),
    LIBRARY("Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search)
}

@Composable
fun MainScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val trackDuration by viewModel.trackDuration.collectAsStateWithLifecycle()
    val volume by viewModel.volumeState.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsStateWithLifecycle()
    val isLyricsViewOpen by viewModel.isLyricsViewOpen.collectAsStateWithLifecycle()
    val syncedLyrics by viewModel.syncedLyrics.collectAsStateWithLifecycle()
    val plainLyrics by viewModel.plainLyrics.collectAsStateWithLifecycle()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsStateWithLifecycle()
    val isLyricsLoading by viewModel.isLyricsLoading.collectAsStateWithLifecycle()
    val playerColors by viewModel.playerColors.collectAsStateWithLifecycle()
    val currentQueue by viewModel.currentQueue.collectAsStateWithLifecycle()

    val searchUiState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val homeUiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayedTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.customPlaylists.collectAsStateWithLifecycle()

    BackHandler(enabled = isPlayerExpanded) {
        viewModel.collapsePlayer()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Scaffold(
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                ) {
                    // Floating Mini Player (Matches Screenshot 1)
                    if (currentTrack != null) {
                        FloatingMiniPlayer(
                            track = currentTrack!!,
                            playbackState = playbackState,
                            onPlayerClick = { viewModel.expandPlayer() },
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onNextClick = { viewModel.playNext() }
                        )
                    }

                    // 5-Tab Apple Music Bottom Bar (Screenshot 1)
                    NavigationBar(
                        containerColor = DarkSurface.copy(alpha = 0.95f),
                        contentColor = TextPrimary,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .testTag("bottom_nav_bar")
                    ) {
                        MainTab.values().forEachIndexed { index, tab ->
                            val isSelected = selectedTab == index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = index },
                                icon = {
                                    if (tab == MainTab.LISTEN_NOW) {
                                        // Circular Red background with white play triangle (Screenshot 1)
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) AppleMusicRed else TextSecondary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = tab.title,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title,
                                            tint = if (isSelected) AppleMusicRed else TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        color = if (isSelected) AppleMusicRed else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            },
            containerColor = DarkBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(
                                animationSpec = tween(350, easing = FastOutSlowInEasing),
                                initialOffsetX = { width -> width }
                            ) + fadeIn()).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                                    targetOffsetX = { width -> -width }
                                ) + fadeOut()
                            )
                        } else {
                            (slideInHorizontally(
                                animationSpec = tween(350, easing = FastOutSlowInEasing),
                                initialOffsetX = { width -> -width }
                            ) + fadeIn()).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                                    targetOffsetX = { width -> width }
                                ) + fadeOut()
                            )
                        }
                    },
                    label = "tab_slide_animation"
                ) { targetIndex ->
                    when (targetIndex) {
                        0 -> HomeScreen(
                            homeUiState = homeUiState,
                            currentTrack = currentTrack,
                            playbackState = playbackState,
                            recentlyPlayed = recentlyPlayed,
                            onTrackClick = { viewModel.playTrack(it) },
                            onPlayQueue = { list, idx -> viewModel.playQueue(list, idx) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onPlayNext = { viewModel.playNextInQueue(it) },
                            onAddToQueue = { viewModel.addToQueue(it) }
                        )
                        1 -> BrowseScreen(
                            homeUiState = homeUiState,
                            currentTrack = currentTrack,
                            playbackState = playbackState,
                            onTrackClick = { viewModel.playTrack(it) },
                            onPlayQueue = { list, idx -> viewModel.playQueue(list, idx) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onPlayNext = { viewModel.playNextInQueue(it) },
                            onAddToQueue = { viewModel.addToQueue(it) }
                        )
                        2 -> RadioScreen(
                            homeUiState = homeUiState,
                            currentTrack = currentTrack,
                            playbackState = playbackState,
                            onTrackClick = { viewModel.playTrack(it) }
                        )
                        3 -> LibraryScreen(
                            favoriteTracks = favoriteTracks,
                            recentlyPlayed = recentlyPlayed,
                            playlists = playlists,
                            currentTrack = currentTrack,
                            playbackState = playbackState,
                            onTrackClick = { viewModel.playTrack(it) },
                            onPlayQueue = { list, idx -> viewModel.playQueue(list, idx) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
                            onPlayNext = { viewModel.playNextInQueue(it) },
                            onAddToQueue = { viewModel.addToQueue(it) }
                        )
                        4 -> SearchScreen(
                            searchUiState = searchUiState,
                            currentTrack = currentTrack,
                            playbackState = playbackState,
                            onSearch = { viewModel.search(it) },
                            onTrackClick = { viewModel.playTrack(it) },
                            onPlayQueue = { list, idx -> viewModel.playQueue(list, idx) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onPlayNext = { viewModel.playNextInQueue(it) },
                            onAddToQueue = { viewModel.addToQueue(it) }
                        )
                    }
                }
            }
        }

        // Full Screen Player Modal with Smooth Slide Up / Down Animation
        AnimatedVisibility(
            visible = isPlayerExpanded && currentTrack != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            currentTrack?.let { track ->
                PlayerFullScreen(
                    track = track,
                    playbackState = playbackState,
                    currentPosition = currentPosition,
                    duration = trackDuration,
                    volume = volume,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    isLyricsViewOpen = isLyricsViewOpen,
                    syncedLyrics = syncedLyrics,
                    plainLyrics = plainLyrics,
                    currentLyricIndex = currentLyricIndex,
                    isLyricsLoading = isLyricsLoading,
                    playerColors = playerColors,
                    queue = currentQueue,
                    onCollapse = { viewModel.collapsePlayer() },
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onNextClick = { viewModel.playNext() },
                    onPreviousClick = { viewModel.playPrevious() },
                    onSeekingProgress = { viewModel.onSeekingProgress(it) },
                    onSeekingFinished = { viewModel.onSeekingFinished(it) },
                    onSeekTo = { viewModel.seekTo(it) },
                    onVolumeChange = { viewModel.setVolume(it) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onToggleLyrics = { viewModel.toggleLyricsView() },
                    onPlayQueueItem = { idx -> viewModel.playQueue(currentQueue, idx) },
                    onExtractColors = { ctx, url -> viewModel.extractPaletteFromUrl(ctx, url) }
                )
            }
        }
    }
}
