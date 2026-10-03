package com.vanz.musicplayer.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class AudioStreamResolver(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "AudioStreamResolver"

        private val PIPED_INSTANCES = listOf(
            "https://pipedapi.kavin.rocks",
            "https://api.piped.private.coffee",
            "https://piped-api.garudalinux.org",
            "https://cf.pipedapi.kavin.rocks",
            "https://pa.il.ax"
        )

        private val INVIDIOUS_INSTANCES = listOf(
            "https://inv.nadeko.net",
            "https://yewtu.be",
            "https://invidious.nerdvpn.de",
            "https://inv.tux.pizza"
        )

        private val FALLBACK_AUDIO_STREAMS = listOf(
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
        )
    }

    suspend fun resolveAudioUrl(videoId: String): String = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) {
            return@withContext FALLBACK_AUDIO_STREAMS.first()
        }

        // Strategy 1: Piped API instances for direct Opus / M4A audio streams
        for (instance in PIPED_INSTANCES) {
            try {
                val url = "$instance/streams/$videoId"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank()) {
                        val json = JSONObject(responseBody)
                        val audioStreams = json.optJSONArray("audioStreams")
                        if (audioStreams != null && audioStreams.length() > 0) {
                            var bestUrl: String? = null
                            var bestBitrate = 0

                            for (i in 0 until audioStreams.length()) {
                                val item = audioStreams.getJSONObject(i)
                                val streamUrl = item.optString("url")
                                val bitrate = item.optInt("bitrate", 0)
                                if (streamUrl.isNotBlank() && (bestUrl == null || bitrate > bestBitrate)) {
                                    bestUrl = streamUrl
                                    bestBitrate = bitrate
                                }
                            }

                            if (!bestUrl.isNullOrBlank()) {
                                Log.d(TAG, "Successfully resolved Piped audio stream for $videoId")
                                return@withContext bestUrl
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Piped instance $instance failed for $videoId: ${e.message}")
            }
        }

        // Strategy 2: Invidious API instances
        for (instance in INVIDIOUS_INSTANCES) {
            try {
                val url = "$instance/api/v1/videos/$videoId"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank()) {
                        val json = JSONObject(responseBody)
                        val adaptiveFormats = json.optJSONArray("adaptiveFormats")
                        if (adaptiveFormats != null && adaptiveFormats.length() > 0) {
                            for (i in 0 until adaptiveFormats.length()) {
                                val item = adaptiveFormats.getJSONObject(i)
                                val type = item.optString("type", "")
                                if (type.startsWith("audio/")) {
                                    val streamUrl = item.optString("url")
                                    if (streamUrl.isNotBlank()) {
                                        Log.d(TAG, "Successfully resolved Invidious audio stream for $videoId")
                                        return@withContext streamUrl
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious instance $instance failed for $videoId: ${e.message}")
            }
        }

        // Strategy 3: Cobal / Invidious proxy stream URL
        return@withContext "https://inv.nadeko.net/latest_version?id=$videoId&itag=140"
    }
}
