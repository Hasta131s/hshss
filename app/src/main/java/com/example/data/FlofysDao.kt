package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY addedAt DESC")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE isDownloaded = 1 ORDER BY addedAt DESC")
    fun getDownloadedTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): Track?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track)

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(relation: PlaylistTrack)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)

    @Query("SELECT * FROM tracks INNER JOIN playlist_tracks ON tracks.id = playlist_tracks.trackId WHERE playlist_tracks.playlistId = :playlistId ORDER BY addedAt DESC")
    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>>

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId)")
    suspend fun isTrackInPlaylist(playlistId: Long, trackId: String): Boolean
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY playedAt DESC")
    fun getHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: HistoryItem)

    @Query("DELETE FROM history WHERE trackId = :trackId")
    suspend fun deleteHistoryItem(trackId: String)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}
