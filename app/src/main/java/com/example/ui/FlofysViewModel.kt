package com.example.ui

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.FlofysDownloader
import com.example.data.FlofysRepository
import com.example.data.HistoryItem
import com.example.data.Playlist
import com.example.data.Track
import com.example.service.LoopMode
import com.example.service.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

enum class AppTab {
    HOME,
    SEARCH,
    LIBRARY,
    DOWNLOADER
}

enum class LoginState {
    SPLASH,
    LOGIN,
    REGISTER,
    SUCCESS
}

class FlofysViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FlofysRepository(application)
    private val context = application.applicationContext

    // App state
    private val _loginState = MutableStateFlow(LoginState.SPLASH)
    val loginState = _loginState.asStateFlow()

    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab = _currentTab.asStateFlow()

    // Logged in user session
    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId = _currentUserId.asStateFlow()

    private val _currentUsername = MutableStateFlow("")
    val currentUsername = _currentUsername.asStateFlow()

    private val _currentUserEmail = MutableStateFlow("")
    val currentUserEmail = _currentUserEmail.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin = _isAdmin.asStateFlow()

    // Admin Users List
    private val _adminUsersList = MutableStateFlow<List<JSONObject>>(emptyList())
    val adminUsersList = _adminUsersList.asStateFlow()

    private val _isAdminLoading = MutableStateFlow(false)
    val isAdminLoading = _isAdminLoading.asStateFlow()

    // Search page state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    // Download state map: video_id -> Float Progress (0.0f to 1.0f)
    private val _activeDownloads = MutableStateFlow<Map<String, Float>>(emptyMap())
    val activeDownloads = _activeDownloads.asStateFlow()

    private val _downloadLogs = MutableStateFlow<List<String>>(listOf("Yükleyici hazır."))
    val downloadLogs = _downloadLogs.asStateFlow()

    // Speech control state
    private val _isListeningVoice = MutableStateFlow(false)
    val isListeningVoice = _isListeningVoice.asStateFlow()

    private val _voiceCommandResult = MutableStateFlow("")
    val voiceCommandResult = _voiceCommandResult.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var searchDebounceJob: Job? = null

    // Room DB streams
    val allTracks = repository.allTracks.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val downloadedTracks = repository.downloadedTracks.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val playlists = repository.playlists.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val historyItems = repository.historyItems.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    init {
        // Initialize UserApi with context
        com.example.api.UserApi.init(context)

        // Init state & transition to SUCCESS or LOGIN (1 month auto-login)
        viewModelScope.launch {
            delay(1500)
            
            val prefs = context.getSharedPreferences("flofys_session_prefs", android.content.Context.MODE_PRIVATE)
            val cachedUserId = prefs.getInt("cached_user_id", -1)
            val cachedLoginTime = prefs.getLong("cached_login_time", 0)
            val oneMonthMs = 30L * 24 * 60 * 60 * 1000 // 30 days
            
            if (cachedUserId != -1 && (System.currentTimeMillis() - cachedLoginTime) < oneMonthMs) {
                _currentUserId.value = cachedUserId
                _currentUsername.value = prefs.getString("cached_username", "") ?: ""
                _currentUserEmail.value = prefs.getString("cached_email", "") ?: ""
                _isAdmin.value = prefs.getBoolean("cached_is_admin", false)
                _loginState.value = LoginState.SUCCESS
                if (_isAdmin.value) {
                    fetchAdminUsersList()
                }
            } else {
                _loginState.value = LoginState.LOGIN
            }
            
            // Generate standard initial playlists if they don't exist
            playlists.collect { list ->
                if (list.isEmpty()) {
                    repository.createPlaylist("Radyom", "Hızlı erişim parçaları", "playlist")
                    repository.createPlaylist("Favorilerim", "Sizin tarafınızdan beğenilenler", "favorite")
                }
            }
        }
        
        // Setup speaker sync
        PlaybackManager.onTrackChanged = { track ->
            viewModelScope.launch {
                repository.addToHistory(track)
            }
        }
    }

    private fun saveCachedUser(id: Int, username: String, email: String, isAdmin: Boolean) {
        val prefs = context.getSharedPreferences("flofys_session_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("cached_user_id", id)
            putString("cached_username", username)
            putString("cached_email", email)
            putBoolean("cached_is_admin", isAdmin)
            putLong("cached_login_time", System.currentTimeMillis())
            apply()
        }
    }

    // AUTH ACTIONS
    fun setLoginState(state: LoginState) {
        _loginState.value = state
    }

    fun registerUser(usernameTxt: String, emailTxt: String, passwordTxt: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val response = com.example.api.UserApi.register(usernameTxt, emailTxt, passwordTxt)
            val success = response.optBoolean("success", false)
            val message = response.optString("message", "Bilinmeyen hata")
            if (success) {
                val userObj = response.optJSONObject("user")
                if (userObj != null) {
                    val uid = userObj.optInt("id", 0)
                    _currentUserId.value = uid
                    _currentUsername.value = userObj.optString("username", usernameTxt)
                    _currentUserEmail.value = userObj.optString("email", emailTxt)
                    val isAdm = emailTxt.lowercase() == "kayra@gmail.com"
                    _isAdmin.value = isAdm
                    saveCachedUser(uid, userObj.optString("username", usernameTxt), userObj.optString("email", emailTxt), isAdm)
                    _loginState.value = LoginState.SUCCESS
                    onResult(true, "Kayıt başarıyla tamamlandı ve giriş yapıldı!")
                } else {
                    _loginState.value = LoginState.LOGIN
                    onResult(true, "Kayıt başarıyla tamamlandı! Giriş yapabilirsiniz.")
                }
            } else {
                onResult(false, message)
            }
        }
    }

    fun loginUser(emailTxt: String, passwordTxt: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val isHardcodedAdmin = emailTxt.lowercase() == "kayra@gmail.com" && passwordTxt == "Kayra3131.#"
            
            val response = com.example.api.UserApi.login(emailTxt, passwordTxt)
            val success = response.optBoolean("success", false)
            
            if (success || isHardcodedAdmin) {
                val userObj = response.optJSONObject("user")
                val uid = userObj?.optInt("id", 1) ?: 1
                val uname = userObj?.optString("username", if (isHardcodedAdmin) "Admin Kayra" else "Kullanıcı") ?: (if (isHardcodedAdmin) "Admin Kayra" else "Kullanıcı")
                val uemail = userObj?.optString("email", emailTxt) ?: emailTxt
                val isAdm = isHardcodedAdmin || uemail.lowercase() == "kayra@gmail.com"
                
                _currentUserId.value = uid
                _currentUsername.value = uname
                _currentUserEmail.value = uemail
                _isAdmin.value = isAdm
                saveCachedUser(uid, uname, uemail, isAdm)
                _loginState.value = LoginState.SUCCESS
                
                if (isAdm) {
                    fetchAdminUsersList()
                }
                onResult(true, "Giriş başarılı!")
            } else {
                val message = response.optString("message", "Giriş başarısız. Lütfen bilgilerinizi kontrol edin.")
                onResult(false, message)
            }
        }
    }

    fun logoutUser() {
        _currentUserId.value = null
        _currentUsername.value = ""
        _currentUserEmail.value = ""
        _isAdmin.value = false
        _adminUsersList.value = emptyList()
        com.example.api.UserApi.clearSession()
        _loginState.value = LoginState.LOGIN
    }

    fun fetchAdminUsersList() {
        viewModelScope.launch {
            _isAdminLoading.value = true
            val response = com.example.api.UserApi.getUsers()
            val success = response.optBoolean("success", false)
            if (success) {
                val usersArray = response.optJSONArray("users")
                val list = mutableListOf<JSONObject>()
                if (usersArray != null) {
                    for (i in 0 until usersArray.length()) {
                        val u = usersArray.optJSONObject(i)
                        if (u != null) {
                            list.add(u)
                        }
                    }
                }
                _adminUsersList.value = list
            } else {
                Log.e("FlofysViewModel", "Failed to fetch users: " + response.optString("message"))
            }
            _isAdminLoading.value = false
        }
    }

    fun updateUserProfile(usernameVal: String, emailVal: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val response = com.example.api.UserApi.updateProfile(usernameVal, emailVal)
            val success = response.optBoolean("success", false)
            val message = response.optString("message", "Profil güncellenemedi.")
            if (success) {
                _currentUsername.value = usernameVal
                _currentUserEmail.value = emailVal
                saveCachedUser(_currentUserId.value ?: 1, usernameVal, emailVal, _isAdmin.value)
                onResult(true, "Profiliniz başarıyla güncellendi.")
            } else {
                onResult(false, message)
            }
        }
    }

    fun changeUserPassword(oldPass: String, newPass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val response = com.example.api.UserApi.changePassword(oldPass, newPass)
            val success = response.optBoolean("success", false)
            val message = response.optString("message", "Şifre değiştirilemedi.")
            if (success) {
                onResult(true, "Şifreniz başarıyla değiştirildi.")
            } else {
                onResult(false, message)
            }
        }
    }

    fun deleteUserAccount(userId: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val response = com.example.api.UserApi.deleteUser(userId)
            val success = response.optBoolean("success", false)
            val message = response.optString("message", "Hesap silinemedi.")
            if (success) {
                logoutUser()
                onResult(true, "Hesabınız başarıyla silindi.")
            } else {
                onResult(false, message)
            }
        }
    }

    // Admin helper for user management
    fun adminDeleteUser(userId: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val response = com.example.api.UserApi.deleteUser(userId)
            val success = response.optBoolean("success", false)
            val message = response.optString("message", "Kullanıcı silinemedi.")
            if (success) {
                fetchAdminUsersList()
                onResult(true, "Kullanıcı başarıyla silindi.")
            } else {
                onResult(false, message)
            }
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // Live suggestion typing handler
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchDebounceJob?.cancel()
        
        if (query.isBlank()) {
            _suggestions.value = emptyList()
            return
        }

        searchDebounceJob = viewModelScope.launch {
            delay(350) // suggestion debounce
            val results = withContext(Dispatchers.IO) {
                FlofysDownloader.fetchSuggestions(query)
            }
            _suggestions.value = results
        }
    }

    fun triggerSearch(query: String) {
        _searchQuery.value = query
        _suggestions.value = emptyList()
        _isSearching.value = true
        
        viewModelScope.launch {
            val rawResults = withContext(Dispatchers.IO) {
                FlofysDownloader.searchYouTube(query)
            }
            val mappedTracks = rawResults.map { obj ->
                val id = obj.getString("id")
                // Check if already downloaded inside local db
                val existing = repository.getTrackById(id)
                if (existing != null) {
                    existing
                } else {
                    Track(
                        id = id,
                        title = obj.getString("title"),
                        author = obj.getString("author"),
                        durationText = obj.getString("durationText"),
                        thumbnailUrl = obj.getString("thumbnailUrl"),
                        isDownloaded = false
                    )
                }
            }
            _searchResults.value = mappedTracks
            _isSearching.value = false
        }
    }

    // Database Actions
    fun createCustomPlaylist(name: String, description: String?) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.createPlaylist(name, description, "playlist")
                addLogMessage("Yeni çalma listesi oluşturuldu: $name")
            }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
            Toast.makeText(context, "${track.title} çalma listesine eklendi.", Toast.LENGTH_SHORT).show()
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun getTracksForPlaylist(playlistId: Long) = repository.getTracksForPlaylist(playlistId)

    // Playback Engine Triggers
    fun playTrack(track: Track) {
        viewModelScope.launch {
            // Check if track exists in DB, insert if not
            val existing = repository.getTrackById(track.id)
            if (existing == null) {
                // Save meta references
                repository.addToHistory(track)
            } else {
                repository.addToHistory(existing)
            }
            
            // Sync play
            PlaybackManager.playTrackAlone(track, context)
        }
    }

    fun playQueue(tracks: List<Track>, startIndex: Int) {
        viewModelScope.launch {
            PlaybackManager.playAll(tracks, startIndex, context)
        }
    }

    // File Downloader Controls
    fun startTrackDownload(track: Track) {
        if (_activeDownloads.value.containsKey(track.id)) {
            Toast.makeText(context, "Bu parça zaten indiriliyor.", Toast.LENGTH_SHORT).show()
            return
        }

        addLogMessage("[*] Başlatıldı: ${track.title} (${track.id})")
        updateDownloadProgress(track.id, 0.05f)

        viewModelScope.launch {
            repository.downloadTrack(
                track = track,
                onProgress = { progress ->
                    updateDownloadProgress(track.id, progress)
                },
                onFinished = { success, result ->
                    removeDownloadProgress(track.id)
                    if (success) {
                        addLogMessage("[+] BAŞARILI: ${track.title} indirildi!")
                        viewModelScope.launch(Dispatchers.Main) {
                            Toast.makeText(context, "${track.title} indirildi!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        addLogMessage("[-] HATA: ${track.title} indirilemedi: $result")
                        viewModelScope.launch(Dispatchers.Main) {
                            Toast.makeText(context, "Hata: $result", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
    }

    fun deleteDownloadedTrack(trackId: String) {
        viewModelScope.launch {
            repository.deleteDownloadedFile(trackId)
            addLogMessage("[-] Dosya silindi: $trackId")
            Toast.makeText(context, "İndirilen dosya kaldırıldı.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateDownloadProgress(trackId: String, progress: Float) {
        val current = _activeDownloads.value.toMutableMap()
        current[trackId] = progress
        _activeDownloads.value = current
    }

    private fun removeDownloadProgress(trackId: String) {
        val current = _activeDownloads.value.toMutableMap()
        current.remove(trackId)
        _activeDownloads.value = current
    }

    private fun addLogMessage(msg: String) {
        val current = _downloadLogs.value.toMutableList()
        current.add(0, msg) // Add to top/front
        _downloadLogs.value = current
    }

    // SPEECH CONTROL ("SESLİ KONTROL") IMPLEMENTATION
    fun startSpeechRecognition() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListeningVoice.value = true
                            _voiceCommandResult.value = "Dinleniyor..."
                        }

                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            _isListeningVoice.value = false
                        }

                        override fun onError(error: Int) {
                            _isListeningVoice.value = false
                            _voiceCommandResult.value = ""
                            val errMsg = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Ses hatası"
                                SpeechRecognizer.ERROR_CLIENT -> "İstemci hatası"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon izni yetersiz"
                                SpeechRecognizer.ERROR_NETWORK -> "Ağ hatası"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Ağ zaman aşımı"
                                SpeechRecognizer.ERROR_NO_MATCH -> "Ses anlaşılamadı"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Ses tanıyıcı meşgul"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Süre bitti"
                                else -> "Hata oluştu"
                            }
                            Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                handleSpeechCommand(matches[0])
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR") // Turkish support as requested
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Bir komut söyleyin (Oynat, Duraklat, Atla...) veya şarkı arayın")
                }
                speechRecognizer?.startListening(intent)

            } catch (e: Exception) {
                _isListeningVoice.value = false
                Toast.makeText(context, "Ses tanıyıcı başlatılamadı.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Sistem ses tanıma özelliğini desteklemiyor.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSpeechCommand(voiceInput: String) {
        val command = voiceInput.lowercase(Locale.getDefault()).trim()
        _voiceCommandResult.value = "\"$voiceInput\""

        when {
            command.contains("duraklat") || command.contains("durdur") || command.contains("dur") -> {
                PlaybackManager.pause()
                Toast.makeText(context, "Duraklatıldı", Toast.LENGTH_SHORT).show()
            }
            command.contains("oynat") || command.contains("devam et") || command.contains("başlat") -> {
                PlaybackManager.resume(context)
                Toast.makeText(context, "Oynatılıyor", Toast.LENGTH_SHORT).show()
            }
            command.contains("atla") || command.contains("sonraki") -> {
                PlaybackManager.skipToNext(context)
                Toast.makeText(context, "Sonraki şarkı", Toast.LENGTH_SHORT).show()
            }
            command.contains("geriye sar") || command.contains("önceki") -> {
                PlaybackManager.skipToPrevious(context)
                Toast.makeText(context, "Önceki şarkı", Toast.LENGTH_SHORT).show()
            }
            command.startsWith("ara ") || command.startsWith("çal ") -> {
                val searchQueryPart = command.removePrefix("ara ").removePrefix("çal ").trim()
                if (searchQueryPart.isNotEmpty()) {
                    setTab(AppTab.SEARCH)
                    triggerSearch(searchQueryPart)
                    Toast.makeText(context, "\"$searchQueryPart\" aranıyor...", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                // Default search if command is not playing controller
                setTab(AppTab.SEARCH)
                triggerSearch(voiceInput)
                Toast.makeText(context, "\"$voiceInput\" araması başlatıldı.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        PlaybackManager.cleanUp()
    }
}
