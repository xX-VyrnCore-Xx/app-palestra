package com.vyrncore.palestra.data.remote

import com.vyrncore.palestra.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.HttpTimeout

/** Single Supabase client, configured with the Auth/Postgrest/Realtime/Storage/Functions plugins used across the app. */
object SupabaseProvider {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            // Without explicit timeouts a request on a bad connection hangs until the OS socket
            // timeout kicks in (minutes), leaving the UI stuck on a spinner instead of surfacing
            // friendlyError()'s "impossibile connettersi" message in a reasonable time.
            httpConfig {
                install(HttpTimeout) {
                    requestTimeoutMillis = 15_000
                    connectTimeoutMillis = 10_000
                    socketTimeoutMillis = 15_000
                }
            }
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
            install(Functions)
        }
    }
}
