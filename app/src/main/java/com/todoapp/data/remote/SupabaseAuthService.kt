package com.todoapp.data.remote

import android.content.Context
import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Anonymous
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "SupabaseAuthService"

object SupabaseAuthService {

    suspend fun ensureSession(context: Context): String? {
        if (!SupabaseConfig.isConfigured) return null

        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.getClient()

                // Try to restore persisted session (SDK handles token storage)
                val existing = client.auth.currentSessionOrNull()

                if (existing != null) {
                    if (!existing.isExpired()) {
                        return@withContext existing.user?.id
                    }

                    // Access token expired — attempt refresh
                    try {
                        val refreshed = client.auth.refreshSession()
                        Log.d(TAG, "Session refreshed successfully")
                        return@withContext refreshed.user?.id
                    } catch (e: Exception) {
                        // Refresh failed. The refresh token may itself be expired.
                        // Check if the session is still usable after refresh attempt.
                        val retrySession = client.auth.currentSessionOrNull()
                        if (retrySession != null && !retrySession.isExpired()) {
                            return@withContext retrySession.user?.id
                        }
                        // Session is genuinely unrecoverable.
                        // Do NOT create a new anonymous user — that would lose the identity.
                        // Return null to signal no session. Caller must handle this.
                        Log.w(TAG, "Session unrecoverable after refresh failure — not creating replacement identity")
                        return@withContext null
                    }
                }

                // No session exists at all — create new anonymous session
                client.auth.signIn(Anonymous)
                val session = client.auth.currentSessionOrNull()
                session?.user?.id
            } catch (e: Exception) {
                Log.e(TAG, "ensureSession failed", e)
                null
            }
        }
    }

    fun getCurrentUserId(context: Context): String? {
        if (!SupabaseConfig.isConfigured) return null
        val client = SupabaseClientProvider.getClientOrNull() ?: return null
        val session = client.auth.currentSessionOrNull() ?: return null
        return if (!session.isExpired()) session.user?.id else null
    }

    fun signOut(context: Context) {
        try {
            SupabaseClientProvider.getClientOrNull()?.auth?.signOut()
        } catch (_: Exception) {}
    }

    private fun io.github.jan.supabase.gotrue.Session.isExpired(): Boolean {
        return expiresAt <= System.currentTimeMillis() / 1000
    }
}
