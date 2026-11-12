package com.example

import android.app.Application
import com.example.data.local.MusicDatabase
import com.example.data.repository.MusicRepository
import com.example.playback.MusicPlaybackService
import com.example.playback.MusicPlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SonicFlowApp : Application() {

    lateinit var database: MusicDatabase
        private set
    lateinit var repository: MusicRepository
        private set
    lateinit var playerManager: MusicPlayerManager
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = MusicDatabase.getDatabase(this)
        repository = MusicRepository(database.musicDao(), this)
        playerManager = MusicPlayerManager(this)

        MusicPlaybackService.createNotificationChannel(this)

        // Seed demo music in background if needed
        applicationScope.launch {
            repository.initializeBuiltInTracks()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        playerManager.release()
    }

    companion object {
        lateinit var instance: SonicFlowApp
            private set
    }
}
