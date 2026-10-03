package com.vanz.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanz.musicplayer.data.model.Track
import com.vanz.musicplayer.player.AppPlaybackState
import com.vanz.musicplayer.ui.theme.AppleMusicRed
import com.vanz.musicplayer.ui.theme.DarkBackground
import com.vanz.musicplayer.ui.theme.TextPrimary
import com.vanz.musicplayer.ui.theme.TextSecondary
import com.vanz.musicplayer.ui.viewmodel.HomeUiState

@Composable
fun HomeScreen(
    homeUiState: HomeUiState,
    currentTrack: Track?,
    playbackState: AppPlaybackState,
    recentlyPlayed: List<Track>,
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
            .testTag("home_screen_lazy_column"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Top Header: "Home" title + User avatar icon (Screenshot 2)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Home",
                    style = MaterialTheme.typography.displayMedium,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profil",
                        tint = AppleMusicRed,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Section 1: "Top Picks for You" (Portrait Cards like HONNE in Screenshot 2)
        if (homeUiState.topPicks.isNotEmpty()) {
            item {
                SectionHeader(title = "Top Picks for You")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    itemsIndexed(homeUiState.topPicks) { index, track ->
                        val tag = when (index % 3) {
                            0 -> "New Release"
                            1 -> "Made for You"
                            else -> "Featured Station"
                        }
                        TopPicksCard(
                            track = track,
                            tagLabel = tag,
                            onClick = { onTrackClick(track) }
                        )
                    }
                }
            }
        }

        // Section 2: "Recently Played >" (Square Cards in Screenshot 2)
        item {
            SectionHeader(
                title = "Recently Played",
                hasChevron = true
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Discovery Station card (Apple Music signature)
                item {
                    SquareItemCard(
                        title = "Discovery Station",
                        subtitle = "Radio Station",
                        imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Features126/v4/44/14/c1/4414c1eb-1808-1f51-a20c-7b02c89f5bc5/mza_1067439327581781293.png/300x300bb.png",
                        onClick = {
                            if (homeUiState.topPicks.isNotEmpty()) {
                                onTrackClick(homeUiState.topPicks.first())
                            }
                        }
                    )
                }

                // NTS Radio 1 card
                item {
                    SquareItemCard(
                        title = "NTS Radio 1",
                        subtitle = "TuneIn",
                        imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/55/12/34/551234cb-e5eb-0498-5c46-d56df3d85d71/cover.jpg/300x300bb.jpg",
                        onClick = {
                            if (homeUiState.topPicks.size > 1) {
                                onTrackClick(homeUiState.topPicks[1])
                            }
                        }
                    )
                }

                // Vanities Malibu card
                item {
                    SquareItemCard(
                        title = "Vanities",
                        subtitle = "Malibu",
                        imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/88/99/00/88990073-b3c1-0955-f126-1077759aa59d/cover.jpg/300x300bb.jpg",
                        onClick = {
                            if (homeUiState.topPicks.size > 2) {
                                onTrackClick(homeUiState.topPicks[2])
                            }
                        }
                    )
                }

                items(recentlyPlayed) { track ->
                    SquareItemCard(
                        title = track.title,
                        subtitle = track.artist,
                        imageUrl = track.thumbnailUrl,
                        onClick = { onTrackClick(track) }
                    )
                }
            }
        }

        // Loading indicator if home feed is still loading
        if (homeUiState.isLoading && homeUiState.topPicks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppleMusicRed)
                }
            }
        }

        // Section 3: Popular Tracks list
        if (homeUiState.topPicks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Hits Populer Hari Ini",
                    subtitle = "Tangga Lagu Teratas"
                )
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
