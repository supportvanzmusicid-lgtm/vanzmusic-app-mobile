package com.vanz.musicplayer.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.SpeakerGroup
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.vanz.musicplayer.data.model.LyricLine
import com.vanz.musicplayer.data.model.Track
import com.vanz.musicplayer.player.AppPlaybackState
import com.vanz.musicplayer.ui.theme.AppleMusicPink
import com.vanz.musicplayer.ui.theme.AppleMusicRed
import com.vanz.musicplayer.ui.theme.DarkSurface
import com.vanz.musicplayer.ui.theme.DarkSurfaceVariant
import com.vanz.musicplayer.ui.theme.TextPrimary
import com.vanz.musicplayer.ui.theme.TextSecondary
import com.vanz.musicplayer.ui.viewmodel.PlayerColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerFullScreen(
    track: Track,
    playbackState: AppPlaybackState,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    isShuffle: Boolean,
    repeatMode: Int,
    isLyricsViewOpen: Boolean,
    syncedLyrics: List<LyricLine>,
    plainLyrics: String?,
    currentLyricIndex: Int,
    isLyricsLoading: Boolean,
    playerColors: PlayerColors,
    queue: List<Track>,
    onCollapse: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeekingProgress: (Long) -> Unit,
    onSeekingFinished: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onToggleLyrics: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onExtractColors: (Context, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPlaying = playbackState == AppPlaybackState.PLAYING

    LaunchedEffect(track.id) {
        val imgUrl = track.highResThumbnailUrl.ifBlank { track.thumbnailUrl }
        onExtractColors(context, imgUrl)
    }

    var sliderDragging by remember { mutableStateOf(false) }
    var localSliderValue by remember { mutableFloatStateOf(0f) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val activeSliderPos = if (sliderDragging) localSliderValue else {
        if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    }

    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.88f,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "cover_scale"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 35) {
                        onCollapse()
                    }
                }
            }
            .testTag("full_screen_player")
    ) {
        val screenHeight = maxHeight

        // Dynamic Blurred Album Art Background (Screenshot 3 style)
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = track.highResThumbnailUrl.ifBlank { track.thumbnailUrl },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(70.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                playerColors.dominantColor.copy(alpha = 0.50f),
                                playerColors.darkMutedColor.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
        }

        // Main Player UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Drag Handle Pill (Screenshot 3)
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(42.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.35f))
                    .clickable { onCollapse() }
            )

            // Middle Section: Large Album Art or Synchronized Lyrics View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isLyricsViewOpen,
                    transitionSpec = {
                        fadeIn(tween(350)) togetherWith fadeOut(tween(350))
                    },
                    label = "player_center_view"
                ) { lyricsOpen ->
                    if (lyricsOpen) {
                        LyricsView(
                            lyrics = syncedLyrics,
                            plainLyrics = plainLyrics,
                            currentLyricIndex = currentLyricIndex,
                            currentPositionMs = currentPosition,
                            isLoading = isLyricsLoading,
                            onLineClick = onSeekTo
                        )
                    } else {
                        val artSize = if (screenHeight < 680.dp) 240.dp else 310.dp
                        Box(
                            modifier = Modifier
                                .size(artSize)
                                .scale(coverScale)
                                .shadow(
                                    elevation = 32.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = playerColors.dominantColor,
                                    spotColor = playerColors.vibrantColor
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = track.highResThumbnailUrl.ifBlank { track.thumbnailUrl },
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Bottom Controls Layout (Matches Screenshot 3)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                // Title, Artist, Star Favorite & More Options Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Action buttons in subtle circles (Screenshot 3)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Star / Favorite button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .clickable { onToggleFavorite(track) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (track.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Suka",
                                tint = if (track.isFavorite) AppleMusicRed else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // More options 3 dots button
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .clickable { showMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "Opsi",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(DarkSurfaceVariant)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Lihat Antrean Lagu", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.QueueMusic, null, tint = AppleMusicRed) },
                                    onClick = {
                                        showMenu = false
                                        showQueueSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (track.isFavorite) "Hapus dari Favorit" else "Tambah ke Favorit", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            if (track.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                            null,
                                            tint = AppleMusicRed
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onToggleFavorite(track)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Seekable Progress Bar (Screenshot 3)
                Slider(
                    value = activeSliderPos,
                    onValueChange = { newValue ->
                        sliderDragging = true
                        localSliderValue = newValue
                        val targetMs = (newValue * duration).toLong()
                        onSeekingProgress(targetMs)
                    },
                    onValueChangeFinished = {
                        sliderDragging = false
                        val targetMs = (localSliderValue * duration).toLong()
                        onSeekingFinished(targetMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Transparent, // Clean Apple Music track style
                        activeTrackColor = Color.White.copy(alpha = 0.9f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .testTag("player_duration_slider")
                )

                // Time labels + Dolby Atmos badge in center (Screenshot 3)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentDisplayMs = if (sliderDragging) (localSliderValue * duration).toLong() else currentPosition
                    Text(
                        text = formatTime(currentDisplayMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )

                    // Dolby Atmos / Lossless Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = track.audioQuality,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = "-" + formatTime(if (duration > currentDisplayMs) duration - currentDisplayMs else 0L),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Playback Controls Row: <<, Solid Play/Pause, >> (Screenshot 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind <<
                    IconButton(
                        onClick = onPreviousClick,
                        modifier = Modifier.size(54.dp).testTag("btn_previous")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Sebelumnya",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Large Play / Pause
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable { onPlayPauseClick() }
                            .testTag("btn_player_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" else "Putar",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Fast Forward >>
                    IconButton(
                        onClick = onNextClick,
                        modifier = Modifier.size(54.dp).testTag("btn_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Selanjutnya",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Volume Control Row: Speaker low, Slider, Speaker High (Screenshot 3)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeDown,
                        contentDescription = "Volume Kecil",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )

                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Transparent,
                            activeTrackColor = Color.White.copy(alpha = 0.85f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .height(16.dp)
                            .testTag("player_volume_slider")
                    )

                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Volume Besar",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Row: Speech bubble (Lyrics), Airplay, Queue (Screenshot 3)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lyrics button (Speech bubble with quote icon)
                    IconButton(
                        onClick = onToggleLyrics,
                        modifier = Modifier
                            .size(40.dp)
                            .then(
                                if (isLyricsViewOpen) Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f))
                                else Modifier
                            )
                            .testTag("btn_toggle_lyrics")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Lirik",
                            tint = if (isLyricsViewOpen) Color.White else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // AirPlay / Speaker circle icon
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SpeakerGroup,
                            contentDescription = "AirPlay",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Queue button (3 horizontal lines / bullet list)
                    IconButton(
                        onClick = { showQueueSheet = true },
                        modifier = Modifier.size(40.dp).testTag("btn_open_queue")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = "Daftar Antrean",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Queue Modal Bottom Sheet
        if (showQueueSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                sheetState = sheetState,
                containerColor = DarkSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Berikutnya dalam Antrean",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )

                    if (queue.isEmpty()) {
                        Text(
                            text = "Tidak ada lagu dalam antrean",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    } else {
                        queue.forEachIndexed { index, queueTrack ->
                            val isItemCurrent = queueTrack.id == track.id
                            TrackRow(
                                track = queueTrack,
                                isPlaying = isPlaying && isItemCurrent,
                                isCurrent = isItemCurrent,
                                onClick = {
                                    onPlayQueueItem(index)
                                    showQueueSheet = false
                                },
                                onFavoriteClick = { onToggleFavorite(queueTrack) },
                                onPlayNext = {},
                                onAddToQueue = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
