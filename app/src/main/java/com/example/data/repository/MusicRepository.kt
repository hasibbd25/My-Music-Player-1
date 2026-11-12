package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.local.MusicDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.TrackEntity
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.playback.AudioSynthesizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(
    private val musicDao: MusicDao,
    private val context: Context
) {

    val allTracks: Flow<List<Track>> = musicDao.getAllTracks().map { list ->
        list.map { it.toDomain() }
    }

    val favoriteTracks: Flow<List<Track>> = musicDao.getFavoriteTracks().map { list ->
        list.map { it.toDomain() }
    }

    val playlists: Flow<List<Playlist>> = musicDao.getAllPlaylists().map { list ->
        list.map { entity ->
            // For count we can query or calculate
            entity.toDomain(0)
        }
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        return musicDao.getTracksForPlaylist(playlistId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun initializeBuiltInTracks() = withContext(Dispatchers.IO) {
        val count = musicDao.getTrackCount()
        if (count == 0) {
            val demoFiles = AudioSynthesizer.generateDemoTracks(context)
            val demoEntities = demoFiles.mapIndexed { index, file ->
                val meta = AudioSynthesizer.DEMO_TRACKS.getOrNull(index)
                TrackEntity(
                    title = meta?.title ?: file.nameWithoutExtension,
                    artist = meta?.artist ?: "SonicFlow Studio",
                    album = meta?.album ?: "Originals",
                    durationMs = (meta?.durationSeconds ?: 15) * 1000L,
                    uriString = Uri.fromFile(file).toString(),
                    isBuiltIn = true,
                    isFavorite = index == 0 // Default first track favorite
                )
            }
            musicDao.insertTracks(demoEntities)

            // Seed default playlists
            val chillPlaylistId = musicDao.insertPlaylist(PlaylistEntity(name = "Chill Vibes"))
            val synthPlaylistId = musicDao.insertPlaylist(PlaylistEntity(name = "Electronic & Synth"))

            // Add demo tracks to playlists
            val insertedTracks = musicDao.getAllTracks().first()
            if (insertedTracks.isNotEmpty()) {
                insertedTracks.take(2).forEachIndexed { idx, t ->
                    musicDao.addTrackToPlaylist(PlaylistTrackCrossRef(chillPlaylistId, t.id, idx))
                }
                insertedTracks.drop(1).take(2).forEachIndexed { idx, t ->
                    musicDao.addTrackToPlaylist(PlaylistTrackCrossRef(synthPlaylistId, t.id, idx))
                }
            }
        }
    }

    suspend fun toggleFavorite(trackId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        musicDao.setFavorite(trackId, isFavorite)
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        musicDao.insertPlaylist(PlaylistEntity(name = name.trim()))
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        musicDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        musicDao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId = playlistId, trackId = trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        musicDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun deleteTrack(trackId: Long) = withContext(Dispatchers.IO) {
        musicDao.deleteTrack(trackId)
    }

    suspend fun scanDeviceAudio(): Int = withContext(Dispatchers.IO) {
        var addedCount = 0
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                val entities = mutableListOf<TrackEntity>()
                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)
                    val uriString = contentUri.toString()

                    val existing = musicDao.getTrackByUri(uriString)
                    if (existing == null) {
                        val title = cursor.getString(titleCol) ?: "Unknown Track"
                        val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                        val album = cursor.getString(albumCol) ?: "Unknown Album"
                        val duration = cursor.getLong(durationCol)
                        val albumId = cursor.getLong(albumIdCol)

                        val albumArtUri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()

                        entities.add(
                            TrackEntity(
                                title = title,
                                artist = if (artist.contains("<unknown>", ignoreCase = true)) "Unknown Artist" else artist,
                                album = album,
                                durationMs = duration,
                                uriString = uriString,
                                isBuiltIn = false,
                                albumArtUri = albumArtUri
                            )
                        )
                    }
                }

                if (entities.isNotEmpty()) {
                    musicDao.insertTracks(entities)
                    addedCount = entities.size
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        addedCount
    }

    suspend fun importAudioUris(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var count = 0
        val entities = mutableListOf<TrackEntity>()

        for (uri in uris) {
            try {
                val uriString = uri.toString()
                val existing = musicDao.getTrackByUri(uriString)
                if (existing != null) continue

                var displayName = "Imported Audio"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                    }
                }

                var title = displayName.substringBeforeLast(".")
                var artist = "Imported Song"
                var album = "Local Storage"
                var durationMs = 0L

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                        if (it.isNotBlank()) title = it
                    }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                        if (it.isNotBlank()) artist = it
                    }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                        if (it.isNotBlank()) album = it
                    }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                        durationMs = it.toLongOrNull() ?: 0L
                    }
                } catch (e: Exception) {
                    // Ignore metadata fallback
                } finally {
                    try { retriever.release() } catch (ignored: Exception) {}
                }

                entities.add(
                    TrackEntity(
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = durationMs,
                        uriString = uriString,
                        isBuiltIn = false
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (entities.isNotEmpty()) {
            musicDao.insertTracks(entities)
            count = entities.size
        }
        count
    }
}
