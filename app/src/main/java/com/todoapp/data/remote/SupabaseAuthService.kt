package com.todoapp.data.remote

import android.content.Context
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "SupabaseAuthService"
private const val PREFS_NAME = "supabase_session"
private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_USER_ID = "user_id"
private const val KEY_EXPIRES_AT = "expires_at"

@Serializable
private data class AuthResponse(
    val access_token: String = "",
    val refresh_token: String = "",
    val expires_in: Long = 0,
    val expires_at: Long = 0,
    val user: User? = null
)

@Serializable
private data class User(val id: String = "")

object SupabaseAuthService {

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    suspend fun ensureSession(context: Context): String? {
        if (!SupabaseConfig.isConfigured) return null

        return withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                val existingToken = prefs.getString(KEY_ACCESS_TOKEN, null)
                val existingUserId = prefs.getString(KEY_USER_ID, null)
                val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)

                if (existingToken != null && existingUserId != null) {
                    if (expiresAt > System.currentTimeMillis() / 1000) {
                        return@withContext existingUserId
                    }

                    val refreshed = refreshSession(context)
                    if (refreshed != null) return@withContext refreshed

                    Log.w(TAG, "Session unrecoverable after refresh failure")
                    return@withContext null
                }

                anonymousSignIn(context)
            } catch (e: Exception) {
                Log.e(TAG, "ensureSession failed", e)
                null
            }
        }
    }

    fun getCurrentUserId(context: Context): String? {
        if (!SupabaseConfig.isConfigured) return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        return if (expiresAt > System.currentTimeMillis() / 1000) userId else null
    }

    fun signOut(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val token = prefs.getString(KEY_ACCESS_TOKEN, null)
            prefs.edit().clear().apply()

            if (token != null) {
                kotlinx.coroutines.runBlocking {
                    try {
                        httpClient.post("${SupabaseConfig.url}/auth/v1/logout") {
                            header("apikey", SupabaseConfig.anonKey)
                            header("Authorization", "Bearer $token")
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }

    private suspend fun anonymousSignIn(context: Context): String? {
        val response = httpClient.post("${SupabaseConfig.url}/auth/v1/signup?grant_type=anonymous") {
            header("apikey", SupabaseConfig.anonKey)
            header("Content-Type", "application/json")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        val body = response.bodyAsText()
        val json = Json.parseToJsonElement(body).jsonObject

        val accessToken = json["access_token"]?.jsonPrimitive?.content ?: return null
        val refreshToken = json["refresh_token"]?.jsonPrimitive?.content ?: return null
        val expiresAt = json["expires_at"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        val userId = json["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content

        if (userId == null) {
            Log.e(TAG, "anonymousSignIn: no user id in response")
            return null
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()

        Log.d(TAG, "Anonymous sign-in successful: $userId")
        return userId
    }

    private suspend fun refreshSession(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null

        val response = httpClient.post("${SupabaseConfig.url}/auth/v1/token?grant_type=refresh_token") {
            header("apikey", SupabaseConfig.anonKey)
            header("Content-Type", "application/json")
            contentType(ContentType.Application.Json)
            setBody("""{"refresh_token":"$refreshToken"}""")
        }

        val body = response.bodyAsText()
        val json = Json.parseToJsonElement(body).jsonObject

        val accessToken = json["access_token"]?.jsonPrimitive?.content ?: return null
        val newRefreshToken = json["refresh_token"]?.jsonPrimitive?.content ?: refreshToken
        val expiresAt = json["expires_at"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        val userId = json["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content

        if (userId == null) return null

        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, newRefreshToken)
            .putString(KEY_USER_ID, userId)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()

        Log.d(TAG, "Session refreshed successfully")
        return userId
    }
}
