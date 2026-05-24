package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: String, // YouTube video_id
    val title: String,
    val author: String,
    val durationText: String, // e.g., "04:15"
    val thumbnailUrl: String,
    val localFilePath: String? = null, // Path to local MP3 if downloaded
    val isDownloaded: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val iconIdentifier: String = "playlist", // e.g., "playlist", "favorite", "frequent"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrack(
    val playlistId: Long,
    val trackId: String
)

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey val trackId: String,
    val title: String,
    val author: String,
    val durationText: String,
    val thumbnailUrl: String,
    val localFilePath: String?,
    val playedAt: Long = System.currentTimeMillis()
)
