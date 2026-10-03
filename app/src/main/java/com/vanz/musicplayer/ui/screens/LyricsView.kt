package com.vanz.musicplayer.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanz.musicplayer.data.model.LyricLine
import com.vanz.musicplayer.ui.theme.AppleMusicRed
import com.vanz.musicplayer.ui.theme.TextPrimary
import com.vanz.musicplayer.ui.theme.TextSecondary

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    plainLyrics: String?,
    currentLyricIndex: Int,
    currentPositionMs: Long,
    isLoading: Boolean,
    onLineClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentLyricIndex) {
        if (currentLyricIndex in lyrics.indices) {
            try {
                listState.animateScrollToItem(
                    index = currentLyricIndex,
                    scrollOffset = -220
                )
            } catch (ignored: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("lyrics_view_container"),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = AppleMusicRed,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Memuat lirik sinkron...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            lyrics.isNotEmpty() -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 180.dp, bottom = 260.dp, start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(lyrics, key = { index, item -> "${index}_${item.timestampMs}" }) { index, line ->
                        val isActive = index == currentLyricIndex
                        val isInstrumentalBreak = if (index > 0) {
                            (line.timestampMs - lyrics[index - 1].endTimeMs) > 8000L
                        } else false

                        if (isInstrumentalBreak && !isActive) {
                            Text(
                                text = "••• Instrumental •••",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.25f),
                                letterSpacing = 2.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }

                        LyricLineItem(
                            line = line,
                            isActive = isActive,
                            onClick = { onLineClick(line.timestampMs) }
                        )
                    }
                }
            }

            !plainLyrics.isNullOrBlank() -> {
                LazyColumn(
                    contentPadding = PaddingValues(top = 100.dp, bottom = 200.dp, start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "Lirik Lagu",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    itemsIndexed(plainLyrics.lines()) { _, rawLine ->
                        Text(
                            text = rawLine,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 28.sp
                        )
                    }
                }
            }

            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "Lirik Tidak Tersedia",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Lirik tersinkronisasi belum tersedia untuk lagu ini di katalog LRCLIB.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun LyricLineItem(
    line: LyricLine,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFFFFFFFF) else Color(0x66FFFFFF),
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "lyric_color_anim"
    )

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 0.96f,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "lyric_scale_anim"
    )

    val blurRadius by animateFloatAsState(
        targetValue = if (isActive) 0f else 2.5f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "lyric_blur_anim"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (isActive) 1.0f else 0.45f
            }
            .then(if (blurRadius > 0.1f) Modifier.blur(blurRadius.dp) else Modifier)
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = line.text.ifBlank { "♫" },
            fontSize = if (isActive) 26.sp else 23.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            lineHeight = 36.sp,
            letterSpacing = (-0.3).sp,
            textAlign = TextAlign.Start
        )
    }
}
