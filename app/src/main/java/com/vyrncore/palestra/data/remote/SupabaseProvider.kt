package com.vyrncore.palestra.data.remote

import com.vyrncore.palestra.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/** Single Supabase client, configured with the Auth/Postgrest/Realtime/Storage/Functions plugins used across the app. */
object SupabaseProvider {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
            install(Functions)
        }
    }
}
