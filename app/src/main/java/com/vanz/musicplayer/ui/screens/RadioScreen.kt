package com.vanz.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
fun RadioScreen(
    homeUiState: HomeUiState,
    currentTrack: Track?,
    playbackState: AppPlaybackState,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("radio_screen_lazy_column"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Radio",
                    style = MaterialTheme.typography.displayMedium,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        // Live Stations
        item {
            SectionHeader(title = "Featured Stations", subtitle = "Broadcasting Live")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(homeUiState.radioStations) { station ->
                    SquareItemCard(
                        title = station.title,
                        subtitle = station.subtitle,
                        imageUrl = station.coverUrl,
                        onClick = {
                            if (homeUiState.topPicks.isNotEmpty()) {
                                onTrackClick(homeUiState.topPicks.random())
                            }
                        }
                    )
                }
            }
        }

        // International Radio
        item {
            SectionHeader(title = "International Stations")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(homeUiState.topPicks) { track ->
                    SquareItemCard(
                        title = "${track.artist} Radio",
                        subtitle = "Continuous Station",
                        imageUrl = track.thumbnailUrl,
                        onClick = { onTrackClick(track) }
                    )
                }
            }
        }
    }
}
