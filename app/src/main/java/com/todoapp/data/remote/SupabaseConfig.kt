package com.todoapp.data.remote

import com.todoapp.BuildConfig

object SupabaseConfig {
    val url: String = BuildConfig.SUPABASE_URL
    val anonKey: String = BuildConfig.SUPABASE_ANON_KEY

    val isConfigured: Boolean
        get() = url.isNotBlank() && anonKey.isNotBlank() &&
                url != "https://your-project.supabase.co"
}
