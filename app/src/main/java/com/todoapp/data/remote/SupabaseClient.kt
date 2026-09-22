package com.todoapp.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient

object SupabaseClientProvider {
    private var client: SupabaseClient? = null

    fun getClient(): SupabaseClient {
        return client ?: synchronized(this) {
            val existing = client
            if (existing != null) return existing

            val newClient = createSupabaseClient(
                supabaseUrl = SupabaseConfig.url,
                supabaseKey = SupabaseConfig.anonKey
            ) {
                install(io.github.jan.supabase.auth.Auth)
                install(io.github.jan.supabase.postgrest.Postgrest)
                install(io.github.jan.supabase.realtime.Realtime)
            }
            client = newClient
            newClient
        }
    }

    fun getClientOrNull(): SupabaseClient? = client
}
