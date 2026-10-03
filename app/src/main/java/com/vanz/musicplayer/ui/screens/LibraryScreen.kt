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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanz.musicplayer.data.local.PlaylistEntity
import com.vanz.musicplayer.data.model.Track
import com.vanz.musicplayer.player.AppPlaybackState
import com.vanz.musicplayer.ui.theme.AppleMusicPink
import com.vanz.musicplayer.ui.theme.AppleMusicRed
import com.vanz.musicplayer.ui.theme.DarkBackground
import com.vanz.musicplayer.ui.theme.DarkDivider
import com.vanz.musicplayer.ui.theme.DarkSearchInput
import com.vanz.musicplayer.ui.theme.DarkSurface
import com.vanz.musicplayer.ui.theme.DarkSurfaceVariant
import com.vanz.musicplayer.ui.theme.TextPrimary
import com.vanz.musicplayer.ui.theme.TextSecondary

@Composable
fun LibraryScreen(
    favoriteTracks: List<Track>,
    recentlyPlayed: List<Track>,
    playlists: List<PlaylistEntity>,
    currentTrack: Track?,
    playbackState: AppPlaybackState,
    onTrackClick: (Track) -> Unit,
    onPlayQueue: (List<Track>, Int) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onCreatePlaylist: (String, String) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying = playbackState == AppPlaybackState.PLAYING
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("library_screen_lazy_column"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.displayMedium,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Playlist",
                        tint = AppleMusicRed
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New Playlist",
                        color = AppleMusicRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                LibraryNavRow(
                    icon = Icons.Default.QueueMusic,
                    title = "Playlists",
                    count = playlists.size,
                    onClick = { selectedFilter = "Playlist" }
                )
                Divider(color = DarkDivider, thickness = 0.6.dp, modifier = Modifier.padding(start = 44.dp))

                LibraryNavRow(
                    icon = Icons.Default.Star,
                    iconTint = AppleMusicRed,
                    title = "Favorites",
                    count = favoriteTracks.size,
                    onClick = { selectedFilter = "Favorit" }
                )
                Divider(color = DarkDivider, thickness = 0.6.dp, modifier = Modifier.padding(start = 44.dp))

                LibraryNavRow(
                    icon = Icons.Default.History,
                    iconTint = AppleMusicPink,
                    title = "Recently Added",
                    count = recentlyPlayed.size,
                    onClick = { selectedFilter = "Riwayat" }
                )
                Divider(color = DarkDivider, thickness = 0.6.dp, modifier = Modifier.padding(start = 44.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (favoriteTracks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onPlayQueue(favoriteTracks, 0) },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = AppleMusicRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val shuffled = favoriteTracks.shuffled()
                            onPlayQueue(shuffled, 0)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = AppleMusicRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        val listToDisplay = when (selectedFilter) {
            "Riwayat" -> recentlyPlayed
            else -> favoriteTracks
        }

        val sectionTitle = when (selectedFilter) {
            "Riwayat" -> "Recently Played"
            else -> "Favorite Songs"
        }

        item {
            SectionHeader(
                title = sectionTitle,
                subtitle = "${listToDisplay.size} SONGS"
            )
        }

        if (listToDisplay.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Songs Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Star favorite tracks to save them in your Library.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            itemsIndexed(listToDisplay) { index, track ->
                TrackRow(
                    track = track,
                    isPlaying = isPlaying,
                    isCurrent = currentTrack?.id == track.id,
                    onClick = { onPlayQueue(listToDisplay, index) },
                    onFavoriteClick = { onToggleFavorite(track) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToQueue = { onAddToQueue(track) }
                )
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "New Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Name your new playlist:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Playlist Name", color = TextSecondary) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkSearchInput,
                            unfocusedContainerColor = DarkSearchInput,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AppleMusicRed
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim(), "")
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleMusicRed)
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun LibraryNavRow(
    icon: ImageVector,
    title: String,
    count: Int,
    onClick: () -> Unit,
    iconTint: Color = AppleMusicRed,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (count > 0) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
