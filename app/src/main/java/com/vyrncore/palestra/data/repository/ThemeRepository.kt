package com.vyrncore.palestra.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.settingsDataStore by preferencesDataStore(name = "palestra_settings")
private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
private val REMINDERS_ENABLED_KEY = booleanPreferencesKey("workout_reminders_enabled")

/** App-wide local preferences: theme and workout-reminder opt-in. */
@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val themeMode = context.settingsDataStore.data.map { prefs ->
        prefs[THEME_MODE_KEY]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    val remindersEnabled = context.settingsDataStore.data.map { prefs -> prefs[REMINDERS_ENABLED_KEY] ?: true }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[REMINDERS_ENABLED_KEY] = enabled }
    }
}
