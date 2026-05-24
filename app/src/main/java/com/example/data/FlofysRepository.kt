package com.example.data

import android.content.Context
import android.os.Environment
import com.example.api.FlofysDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

class FlofysRepository(private val context: Context) {
    private val database = FlofysDatabase.getDatabase(context)
    private val trackDao = database.trackDao()
    private val playlistDao = database.playlistDao()
    private val historyDao = database.historyDao()
    private val okHttpClient = OkHttpClient()

    // Flow streams for reactive UI
    val allTracks: Flow<List<Track>> = trackDao.getAllTracks()
    val downloadedTracks: Flow<List<Track>> = trackDao.getDownloadedTracks()
    val playlists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    val historyItems: Flow<List<HistoryItem>> = historyDao.getHistory()

    suspend fun getTrackById(id: String): Track? = trackDao.getTrackById(id)

    suspend fun getPlaylistById(id: Long): Playlist? = playlistDao.getPlaylistById(id)

    suspend fun createPlaylist(name: String, description: String? = null, icon: String = "playlist"): Long {
        return playlistDao.insertPlaylist(Playlist(name = name, description = description, iconIdentifier = icon))
    }

    suspend fun deletePlaylist(id: Long) = playlistDao.deletePlaylistById(id)

    suspend fun addTrackToPlaylist(playlistId: Long, track: Track) {
        // Ensure track is inserted in the database first
        trackDao.insertTrack(track)
        playlistDao.addTrackToPlaylist(PlaylistTrack(playlistId, track.id))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        return playlistDao.getTracksForPlaylist(playlistId)
    }

    suspend fun isTrackInPlaylist(playlistId: Long, trackId: String): Boolean {
        return playlistDao.isTrackInPlaylist(playlistId, trackId)
    }

    suspend fun addToHistory(track: Track) {
        historyDao.insertHistoryItem(
            HistoryItem(
                trackId = track.id,
                title = track.title,
                author = track.author,
                durationText = track.durationText,
                thumbnailUrl = track.thumbnailUrl,
                localFilePath = track.localFilePath,
                playedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearHistory() = historyDao.clearHistory()

    /**
     * Downloads an MP3 file of the track and updates the Room DB accordingly.
     */
    suspend fun downloadTrack(
        track: Track,
        onProgress: (Float) -> Unit,
        onFinished: (Boolean, String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            // 1. Get the direct MP3 link
            val directLink = FlofysDownloader.getMp3DownloadLink(track.id)
            if (directLink == null) {
                onFinished(false, "İndirme linki oluşturulamadı.")
                return@withContext
            }

            // 2. Prepare file destination
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            val destFile = File(musicDir, "${track.id}.mp3")

            // 3. Perform network download
            val request = Request.Builder().url(directLink).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    onFinished(false, "İndirme başarısız oldu (Hata ${response.code}).")
                    return@withContext
                }

                val body = response.body
                if (body == null) {
                    onFinished(false, "Yanıt gövdesi boş.")
                    return@withContext
                }

                val totalBytes = body.contentLength()
                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(destFile)

                val buffer = ByteArray(4096)
                var bytesRead: Int
                var downloadedBytes: Long = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        val progress = downloadedBytes.toFloat() / totalBytes
                        onProgress(progress)
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                // 4. Update track details in Room database
                val updatedTrack = track.copy(
                    localFilePath = destFile.absolutePath,
                    isDownloaded = true,
                    addedAt = System.currentTimeMillis()
                )
                trackDao.insertTrack(updatedTrack)

                onFinished(true, destFile.absolutePath)
            }
        } catch (e: Exception) {
            onFinished(false, e.localizedMessage ?: "Bilinmeyen bir hata oluştu.")
        }
    }

    suspend fun deleteDownloadedFile(trackId: String) = withContext(Dispatchers.IO) {
        val track = trackDao.getTrackById(trackId)
        if (track != null) {
            if (track.localFilePath != null) {
                val file = File(track.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            // Mark as not downloaded, keep details or delete track row depending on references
            val updated = track.copy(localFilePath = null, isDownloaded = false)
            trackDao.insertTrack(updated)
        }
    }

    /**
     * Auto-prepopulate default records on database start if empty
     */
    suspend fun prepopulateDatabaseIfEmpty() {
        val list = mutableListOf<Playlist>()
        // We'll run a quick count check or simply let Flow observer decide.
    }
}
