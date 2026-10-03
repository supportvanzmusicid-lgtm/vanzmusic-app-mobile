package com.vanz.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanz.musicplayer.data.model.Track
import com.vanz.musicplayer.player.AppPlaybackState
import com.vanz.musicplayer.ui.theme.DarkBackground
import com.vanz.musicplayer.ui.theme.TextPrimary
import com.vanz.musicplayer.ui.viewmodel.HomeUiState

@Composable
fun BrowseScreen(
    homeUiState: HomeUiState,
    currentTrack: Track?,
    playbackState: AppPlaybackState,
    onTrackClick: (Track) -> Unit,
    onPlayQueue: (List<Track>, Int) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying = playbackState == AppPlaybackState.PLAYING

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("browse_screen_lazy_column"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Browse",
                    style = MaterialTheme.typography.displayMedium,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        // New Releases
        if (homeUiState.newReleases.isNotEmpty()) {
            item {
                SectionHeader(title = "New Music", subtitle = "Featured")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(homeUiState.newReleases) { track ->
                        TopPicksCard(
                            track = track,
                            tagLabel = "Spatial Audio",
                            onClick = { onTrackClick(track) }
                        )
                    }
                }
            }
        }

        // Explore Categories
        item {
            SectionHeader(title = "Must-Hear Music")
        }

        itemsIndexed(EXPLORE_CATEGORIES.chunked(2)) { _, pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { category ->
                    CategoryCard(
                        title = category.title,
                        gradientColors = category.colors,
                        onClick = {
                            if (homeUiState.topPicks.isNotEmpty()) {
                                onTrackClick(homeUiState.topPicks.random())
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Top 100 Charts
        if (homeUiState.topPicks.isNotEmpty()) {
            item {
                SectionHeader(title = "Top 100: Global")
            }
            itemsIndexed(homeUiState.topPicks) { index, track ->
                TrackRow(
                    track = track,
                    isPlaying = isPlaying,
                    isCurrent = currentTrack?.id == track.id,
                    onClick = { onPlayQueue(homeUiState.topPicks, index) },
                    onFavoriteClick = { onToggleFavorite(track) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToQueue = { onAddToQueue(track) }
                )
            }
        }
    }
}
