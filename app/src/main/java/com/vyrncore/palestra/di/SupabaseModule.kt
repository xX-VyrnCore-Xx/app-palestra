package com.vyrncore.palestra.di

import com.vyrncore.palestra.data.remote.SupabaseProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = SupabaseProvider.client

    @Provides
    fun provideAuth(client: SupabaseClient): Auth = client.auth

    @Provides
    fun providePostgrest(client: SupabaseClient): Postgrest = client.postgrest

    @Provides
    fun provideRealtime(client: SupabaseClient): Realtime = client.realtime

    @Provides
    fun provideStorage(client: SupabaseClient): Storage = client.storage

    @Provides
    fun provideFunctions(client: SupabaseClient): Functions = client.functions
}
