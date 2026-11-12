package com.example.data.model

import android.net.Uri

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class EqualizerPreset(val displayName: String, val bassMultiplier: Float, val trebleMultiplier: Float) {
    NORMAL("Normal / Flat", 1.0f, 1.0f),
    BASS_BOOST("Bass Boost", 1.5f, 0.9f),
    VOCAL("Vocal Clarifier", 0.9f, 1.4f),
    ELECTRONIC("Electronic / Dance", 1.4f, 1.3f),
    ROCK("Rock & Metal", 1.3f, 1.2f),
    ACOUSTIC("Acoustic & Warm", 1.1f, 1.1f)
}

data class Track(
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String = "Unknown Album",
    val durationMs: Long = 0,
    val uriString: String,
    val isFavorite: Boolean = false,
    val isBuiltIn: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val albumArtUri: String? = null
) {
    val uri: Uri get() = Uri.parse(uriString)

    fun formattedDuration(): String {
        if (durationMs <= 0) return "--:--"
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}

data class Playlist(
    val id: Long = 0,
    val name: String,
    val trackCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
