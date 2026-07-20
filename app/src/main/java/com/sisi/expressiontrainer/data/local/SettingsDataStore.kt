package com.sisi.expressiontrainer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sisi.expressiontrainer.data.model.CustomPrompt
import com.sisi.expressiontrainer.data.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "expression_trainer")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val SETTINGS_KEY = stringPreferencesKey("settings")
        private val CUSTOM_PROMPT_KEY = stringPreferencesKey("custom_prompt")
        private val json = Json { ignoreUnknownKeys = true }
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        prefs[SETTINGS_KEY]?.let { json.decodeFromString(it) } ?: Settings()
    }

    val customPrompt: Flow<CustomPrompt> = context.dataStore.data.map { prefs ->
        prefs[CUSTOM_PROMPT_KEY]?.let { json.decodeFromString(it) } ?: CustomPrompt()
    }

    suspend fun saveSettings(settings: Settings) {
        context.dataStore.edit { prefs ->
            prefs[SETTINGS_KEY] = json.encodeToString(settings)
        }
    }

    suspend fun saveCustomPrompt(customPrompt: CustomPrompt) {
        context.dataStore.edit { prefs ->
            prefs[CUSTOM_PROMPT_KEY] = json.encodeToString(customPrompt)
        }
    }
}
