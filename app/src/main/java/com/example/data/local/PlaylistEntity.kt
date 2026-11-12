package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(trackCount: Int): Playlist = Playlist(
        id = id,
        name = name,
        trackCount = trackCount,
        createdAt = createdAt
    )
}
