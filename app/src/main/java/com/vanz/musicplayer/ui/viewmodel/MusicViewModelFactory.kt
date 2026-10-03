package com.vanz.musicplayer.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vanz.musicplayer.data.local.MusicDatabase
import com.vanz.musicplayer.data.remote.NetworkClient
import com.vanz.musicplayer.data.repository.MusicRepository
import com.vanz.musicplayer.player.MusicPlayerManager

class MusicViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            val database = MusicDatabase.getInstance(context)
            val repository = MusicRepository(
                youTubeApiService = NetworkClient.youTubeApiService,
                itunesApiService = NetworkClient.itunesApiService,
                lrcApiService = NetworkClient.lrcApiService,
                audioStreamResolver = NetworkClient.audioStreamResolver,
                database = database
            )
            val playerManager = MusicPlayerManager(context.applicationContext)
            return MusicViewModel(repository, playerManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
