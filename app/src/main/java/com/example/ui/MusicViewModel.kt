package com.example.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.SonicFlowApp
import com.example.data.model.EqualizerPreset
import com.example.data.model.Playlist
import com.example.data.model.RepeatMode
import com.example.data.model.Track
import com.example.data.repository.MusicRepository
import com.example.playback.MusicPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
    TITLE("Song Title (A-Z)"),
    ARTIST("Artist Name"),
    DURATION("Duration (Longest)"),
    DATE_ADDED("Recently Added")
}

class MusicViewModel(
    private val repository: MusicRepository = SonicFlowApp.instance.repository,
    private val playerManager: MusicPlayerManager = SonicFlowApp.instance.playerManager
) : ViewModel() {

    // Player state delegated from player manager
    val currentTrack: StateFlow<Track?> = playerManager.currentTrack
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val duration: StateFlow<Long> = playerManager.duration
    val playbackQueue: StateFlow<List<Track>> = playerManager.playbackQueue
    val isShuffled: StateFlow<Boolean> = playerManager.isShuffled
    val repeatMode: StateFlow<RepeatMode> = playerManager.repeatMode
    val playbackSpeed: StateFlow<Float> = playerManager.playbackSpeed
    val equalizerPreset: StateFlow<EqualizerPreset> = playerManager.equalizerPreset
    val sleepTimerSecondsLeft: StateFlow<Int?> = playerManager.sleepTimerSecondsLeft
    val visualizerAmplitudes: StateFlow<List<Float>> = playerManager.visualizerAmplitudes

    // Search and Sort
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _scanStatusMessage = MutableStateFlow<String?>(null)
    val scanStatusMessage: StateFlow<String?> = _scanStatusMessage.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val rawTracks = repository.allTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTracks: StateFlow<List<Track>> = combine(rawTracks, _searchQuery, _sortOption) { tracks, query, sort ->
        val filtered = if (query.isBlank()) {
            tracks
        } else {
            tracks.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }

        when (sort) {
            SortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
            SortOption.DURATION -> filtered.sortedByDescending { it.durationMs }
            SortOption.DATE_ADDED -> filtered.sortedByDescending { it.dateAdded }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteTracks: StateFlow<List<Track>> = repository.favoriteTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playlists: StateFlow<List<Playlist>> = repository.playlists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sort: SortOption) {
        _sortOption.value = sort
    }

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
    }

    fun getPlaylistTracks(playlistId: Long): StateFlow<List<Track>> {
        return repository.getTracksForPlaylist(playlistId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Playback commands
    fun playTrack(track: Track, queue: List<Track>? = null) {
        playerManager.playTrack(track, queue ?: allTracks.value)
    }

    fun playOrPause() {
        playerManager.playOrPause()
    }

    fun resume() {
        playerManager.resume()
    }

    fun pause() {
        playerManager.pause()
    }

    fun skipToNext() {
        playerManager.skipToNext()
    }

    fun skipToPrevious() {
        playerManager.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun seekRelative(offsetMs: Long) {
        playerManager.seekRelative(offsetMs)
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun cycleRepeatMode() {
        playerManager.cycleRepeatMode()
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        playerManager.setEqualizerPreset(preset)
    }

    fun setSleepTimer(minutes: Int) {
        playerManager.setSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        playerManager.cancelSleepTimer()
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val newFav = !track.isFavorite
            repository.toggleFavorite(track.id, newFav)
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = null
            }
            repository.deletePlaylist(playlistId)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun deleteTrack(trackId: Long) {
        viewModelScope.launch {
            repository.deleteTrack(trackId)
        }
    }

    fun scanPhoneAudio() {
        viewModelScope.launch {
            _scanStatusMessage.value = "Scanning device for audio files..."
            val count = repository.scanDeviceAudio()
            _scanStatusMessage.value = if (count > 0) {
                "Found & added $count new song${if (count > 1) "s" else ""}!"
            } else {
                "Scan complete. No new songs found."
            }
        }
    }

    fun importAudioUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _scanStatusMessage.value = "Importing ${uris.size} audio file(s)..."
            val count = repository.importAudioUris(uris)
            _scanStatusMessage.value = "Successfully imported $count track${if (count > 1) "s" else ""}!"
        }
    }

    fun dismissStatusMessage() {
        _scanStatusMessage.value = null
    }
}
