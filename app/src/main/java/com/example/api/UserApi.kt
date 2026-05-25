package com.example.api

import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object UserApi {
    private const val TAG = "UserApi"
    private const val BASE_URL = "http://bosforlab.online"

    private val cookieStore = HashMap<String, List<Cookie>>()
    private var sharedPreferences: android.content.SharedPreferences? = null

    fun init(context: android.content.Context) {
        sharedPreferences = context.getSharedPreferences("flofys_session_prefs", android.content.Context.MODE_PRIVATE)
        loadCookiesFromPrefs()
    }

    private fun saveCookiesToPrefs(host: String, cookies: List<Cookie>) {
        val prefs = sharedPreferences ?: return
        val set = cookies.map { cookie ->
            val builder = StringBuilder()
            builder.append(cookie.name).append("=")
                   .append(cookie.value).append(";")
                   .append(cookie.domain).append(";")
                   .append(cookie.path).append(";")
                   .append(cookie.expiresAt).append(";")
                   .append(if (cookie.secure) "1" else "0").append(";")
                   .append(if (cookie.httpOnly) "1" else "0")
            builder.toString()
        }.toSet()
        prefs.edit().putStringSet("cookies_$host", set).apply()
    }

    private fun loadCookiesFromPrefs() {
        val prefs = sharedPreferences ?: return
        try {
            val keys = prefs.all.keys
            for (key in keys) {
                if (key.startsWith("cookies_")) {
                    val host = key.substring("cookies_".length)
                    val set = prefs.getStringSet(key, null) ?: continue
                    val loaded = mutableListOf<Cookie>()
                    for (serialized in set) {
                        try {
                            val parts = serialized.split(";")
                            val nameValue = parts[0].split("=")
                            val name = nameValue[0]
                            val value = nameValue[1]
                            val domain = parts[1]
                            val path = parts[2]
                            val expiresAt = parts[3].toLong()
                            val secure = parts[4] == "1"
                            val httpOnly = parts[5] == "1"

                            val builder = Cookie.Builder()
                                .name(name)
                                .value(value)
                                .domain(domain)
                                .path(path)
                            if (expiresAt > System.currentTimeMillis()) {
                                builder.expiresAt(expiresAt)
                            }
                            if (secure) builder.secure()
                            if (httpOnly) builder.httpOnly()

                            loaded.add(builder.build())
                        } catch (e: Exception) {
                            Log.e(TAG, "parse cookie failure", e)
                        }
                    }
                    cookieStore[host] = loaded
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cookies from prefs", e)
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                if (cookies.isNotEmpty()) {
                    val existing = cookieStore[url.host]?.toMutableList() ?: mutableListOf()
                    for (newCookie in cookies) {
                        existing.removeAll { it.name == newCookie.name }
                        existing.add(newCookie)
                    }
                    cookieStore[url.host] = existing
                    saveCookiesToPrefs(url.host, existing)
                    Log.d(TAG, "Saved and merged cookies: $existing")
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val list = cookieStore[url.host] ?: emptyList()
                Log.d(TAG, "Loading cookies for ${url.host}: $list")
                return list
            }
        })
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    fun clearSession() {
        cookieStore.clear()
        sharedPreferences?.edit()?.clear()?.apply()
    }

    /**
     * Registers a new user.
     * POST http://bosforlab.online/register.php
     * Payload: {"username": "foo", "email": "foo@test.com", "password": "xxx"}
     */
    suspend fun register(username: String, email: String, password: String): JSONObject {
        return withContextIO {
            val json = JSONObject().apply {
                put("username", username)
                put("email", email)
                put("password", password)
            }
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE_URL/register.php")
                .post(body)
                .build()

            executeRequest(request)
        }
    }

    /**
     * Logins user.
     * POST http://bosforlab.online/login.php
     * Payload: {"email": "foo@test.com", "password": "xxx"}
     */
    suspend fun login(email: String, password: String): JSONObject {
        return withContextIO {
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE_URL/login.php")
                .post(body)
                .build()

            executeRequest(request)
        }
    }

    /**
     * Gets List of Users.
     * GET http://bosforlab.online/get_users.php
     */
    suspend fun getUsers(): JSONObject {
        return withContextIO {
            val request = Request.Builder()
                .url("$BASE_URL/get_users.php")
                .get()
                .build()

            executeRequest(request)
        }
    }

    /**
     * Updates Profile of the logged-in user.
     * POST http://bosforlab.online/settings.php?action=update_profile
     * Payload: {"username": "new_username", "email": "new_email"}
     */
    suspend fun updateProfile(username: String?, email: String?): JSONObject {
        return withContextIO {
            val json = JSONObject().apply {
                if (username != null) put("username", username)
                if (email != null) put("email", email)
            }
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE_URL/settings.php?action=update_profile")
                .post(body)
                .build()

            executeRequest(request)
        }
    }

    /**
     * Changes Password of the logged-in user.
     * POST http://bosforlab.online/settings.php?action=change_password
     * Payload: {"old_password": "...", "new_password": "..."}
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): JSONObject {
        return withContextIO {
            val json = JSONObject().apply {
                put("old_password", oldPassword)
                put("new_password", newPassword)
            }
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE_URL/settings.php?action=change_password")
                .post(body)
                .build()

            executeRequest(request)
        }
    }

    /**
     * Deletes user account.
     * DELETE http://bosforlab.online/delete_user.php
     * Payload: {"user_id": X}
     */
    suspend fun deleteUser(userId: Int): JSONObject {
        return withContextIO {
            val json = JSONObject().apply {
                put("user_id", userId)
            }
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE_URL/delete_user.php")
                .delete(body)
                .build()

            executeRequest(request)
        }
    }

    private fun executeRequest(request: Request): JSONObject {
        return try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "URL: ${request.url}, status code: ${response.code}, response: $bodyStr")
                if (response.isSuccessful && bodyStr.trim().startsWith("{")) {
                    JSONObject(bodyStr)
                } else if (response.isSuccessful && bodyStr.trim().startsWith("[")) {
                    JSONObject().apply {
                        put("success", true)
                        put("users", JSONArray(bodyStr))
                    }
                } else {
                    val fallbackMsg = "İşlem başarısız oldu (Hata ${response.code})."
                    try {
                        val parsed = JSONObject(bodyStr)
                        if (parsed.has("message") || parsed.has("msg")) {
                            parsed
                        } else {
                            JSONObject().apply {
                                put("success", false)
                                put("message", fallbackMsg)
                            }
                        }
                    } catch (e: Exception) {
                        JSONObject().apply {
                            put("success", false)
                            put("message", bodyStr.ifEmpty { fallbackMsg })
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network IO failure: ", e)
            JSONObject().apply {
                put("success", false)
                put("message", "Ağ bağlantı hatası: ${e.localizedMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "API call failure: ", e)
            JSONObject().apply {
                put("success", false)
                put("message", "Beklenmeyen hata: ${e.localizedMessage}")
            }
        }
    }

    private suspend inline fun <T> withContextIO(crossinline block: () -> T): T {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            block()
        }
    }
}
