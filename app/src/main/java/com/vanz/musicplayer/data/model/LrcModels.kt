package com.vanz.musicplayer.data.model

import com.squareup.moshi.JsonClass
import java.util.regex.Pattern

@JsonClass(generateAdapter = true)
data class LrcRecord(
    val id: Long? = null,
    val name: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean? = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

object LrcParser {
    private val TIME_TAG_REGEX = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")

    fun parse(rawSyncedLyrics: String?): List<LyricLine> {
        if (rawSyncedLyrics.isNullOrBlank()) return emptyList()

        val lines = rawSyncedLyrics.lines()
        val parsedLines = mutableListOf<LyricLine>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val matcher = TIME_TAG_REGEX.matcher(trimmed)
            val timestamps = mutableListOf<Long>()
            var lastMatchEnd = 0

            while (matcher.find()) {
                val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                val millisString = matcher.group(3)

                val millis = when {
                    millisString == null -> 0L
                    millisString.length == 1 -> millisString.toLong() * 100
                    millisString.length == 2 -> millisString.toLong() * 10
                    else -> millisString.take(3).toLong()
                }

                val totalMs = (minutes * 60 * 1000) + (seconds * 1000) + millis
                timestamps.add(totalMs)
                lastMatchEnd = matcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val text = trimmed.substring(lastMatchEnd).trim()
                for (ts in timestamps) {
                    parsedLines.add(LyricLine(timestampMs = ts, text = text))
                }
            }
        }

        val sorted = parsedLines.sortedBy { it.timestampMs }

        return sorted.mapIndexed { index, item ->
            val nextTime = if (index < sorted.size - 1) sorted[index + 1].timestampMs else Long.MAX_VALUE
            item.copy(endTimeMs = nextTime)
        }
    }
}
