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

    // Simple CookieJar to persist cookies and act like a real Session in python
    private val cookieStore = HashMap<String, List<Cookie>>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
                Log.d(TAG, "Saved cookies: $cookies")
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        })
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    fun clearSession() {
        cookieStore.clear()
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
