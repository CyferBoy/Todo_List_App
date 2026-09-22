package com.todoapp.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

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
                install(Postgrest)
                install(Realtime)
            }
            client = newClient
            newClient
        }
    }

    fun getClientOrNull(): SupabaseClient? = client
}
