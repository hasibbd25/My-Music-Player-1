package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val isFavorite: Boolean = false,
    val isBuiltIn: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val albumArtUri: String? = null
) {
    fun toDomain(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        uriString = uriString,
        isFavorite = isFavorite,
        isBuiltIn = isBuiltIn,
        dateAdded = dateAdded,
        albumArtUri = albumArtUri
    )

    companion object {
        fun fromDomain(track: Track): TrackEntity = TrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationMs = track.durationMs,
            uriString = track.uriString,
            isFavorite = track.isFavorite,
            isBuiltIn = track.isBuiltIn,
            dateAdded = track.dateAdded,
            albumArtUri = track.albumArtUri
        )
    }
}
