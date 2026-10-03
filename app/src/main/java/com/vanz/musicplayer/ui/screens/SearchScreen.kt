package com.vanz.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanz.musicplayer.data.model.Track
import com.vanz.musicplayer.player.AppPlaybackState
import com.vanz.musicplayer.ui.theme.AppleMusicRed
import com.vanz.musicplayer.ui.theme.DarkBackground
import com.vanz.musicplayer.ui.theme.DarkSearchInput
import com.vanz.musicplayer.ui.theme.DarkSurfaceVariant
import com.vanz.musicplayer.ui.theme.TextPrimary
import com.vanz.musicplayer.ui.theme.TextSecondary
import com.vanz.musicplayer.ui.viewmodel.SearchUiState

data class SearchCategory(
    val title: String,
    val query: String,
    val colors: List<Color>
)

val EXPLORE_CATEGORIES = listOf(
    SearchCategory("Pop Indonesia", "Pop Indonesia Terpopuler 2026", listOf(Color(0xFFFF2D55), Color(0xFFFF375F))),
    SearchCategory("Top 50 Global", "Billboard Hot 100", listOf(Color(0xFF5E5CE6), Color(0xFF0A84FF))),
    SearchCategory("K-Pop Hitz", "K-Pop New Releases Hits", listOf(Color(0xFFFF375F), Color(0xFFAF52DE))),
    SearchCategory("Akustik Santai", "Acoustic Pop Chill Vibes", listOf(Color(0xFFFF9500), Color(0xFFFFCC00))),
    SearchCategory("R&B / Hip-Hop", "R&B Soul Vibes", listOf(Color(0xFF30D158), Color(0xFF66D4CF))),
    SearchCategory("Rock Klasik", "Classic Rock Hits", listOf(Color(0xFFFF453A), Color(0xFFFF9F0A))),
    SearchCategory("Lagu Viral TikTok", "Lagu Viral TikTok 2026", listOf(Color(0xFFBF5AF2), Color(0xFFFF2D55))),
    SearchCategory("Fokus & Belajar", "Lofi Beats for Studying Chill", listOf(Color(0xFF64D2FF), Color(0xFF5E5CE6)))
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    searchUiState: SearchUiState,
    currentTrack: Track?,
    playbackState: AppPlaybackState,
    onSearch: (String) -> Unit,
    onTrackClick: (Track) -> Unit,
    onPlayQueue: (List<Track>, Int) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchInput by remember { mutableStateOf(searchUiState.query) }
    val focusManager = LocalFocusManager.current
    val isPlaying = playbackState == AppPlaybackState.PLAYING

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("search_screen_lazy_column"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.displayMedium,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchInput,
                    onValueChange = {
                        searchInput = it
                        if (it.isBlank()) {
                            onSearch("")
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Artists, Songs, Lyrics, and More",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchInput = ""
                                    onSearch("")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Hapus",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            onSearch(searchInput)
                        }
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSearchInput,
                        unfocusedContainerColor = DarkSearchInput,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AppleMusicRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("search_text_input")
                )
            }
        }

        if (searchUiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AppleMusicRed)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Searching music...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        if (!searchUiState.isLoading && searchUiState.searchResults.isNotEmpty()) {
            item {
                Text(
                    text = "Top Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            itemsIndexed(searchUiState.searchResults) { index, track ->
                TrackRow(
                    track = track,
                    isPlaying = isPlaying,
                    isCurrent = currentTrack?.id == track.id,
                    onClick = { onPlayQueue(searchUiState.searchResults, index) },
                    onFavoriteClick = { onToggleFavorite(track) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToQueue = { onAddToQueue(track) }
                )
            }
        }

        if (!searchUiState.isLoading && searchUiState.searchResults.isEmpty() && searchUiState.query.isBlank()) {
            if (searchUiState.recentSearches.isNotEmpty()) {
                item {
                    SectionHeader(title = "Recent Searches")
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        searchUiState.recentSearches.take(6).forEach { recentQuery ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(DarkSurfaceVariant)
                                    .clickable {
                                        searchInput = recentQuery
                                        onSearch(recentQuery)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = recentQuery,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                SectionHeader(title = "Browse Categories")
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
                                searchInput = category.query
                                onSearch(category.query)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (!searchUiState.isLoading && searchUiState.searchResults.isEmpty() && searchUiState.query.isNotBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Results",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try searching for a different artist, song, or album title.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
