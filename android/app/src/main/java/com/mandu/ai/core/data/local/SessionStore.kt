package com.mandu.ai.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class SessionStore(
    private val store: DataStore<Preferences>,
    private val json: Json,
) {
    private val KEY_TOKEN = stringPreferencesKey("token")
    private val KEY_EMAIL = stringPreferencesKey("email")
    private val KEY_NAME = stringPreferencesKey("name")
    private val KEY_DARK = booleanPreferencesKey("dark_theme")
    private val KEY_SYSTEM = stringPreferencesKey("system_prompt")

    val token: Flow<String?> = store.data.map { it[KEY_TOKEN] }
    val email: Flow<String?> = store.data.map { it[KEY_EMAIL] }
    val name: Flow<String?> = store.data.map { it[KEY_NAME] }
    val darkTheme: Flow<Boolean> = store.data.map { it[KEY_DARK] ?: false }
    val systemPrompt: Flow<String> = store.data.map { it[KEY_SYSTEM] ?: DEFAULT_SYSTEM }

    suspend fun saveSession(token: String, email: String, name: String?) {
        store.edit {
            it[KEY_TOKEN] = token
            it[KEY_EMAIL] = email
            if (name != null) it[KEY_NAME] = name
        }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        store.edit { it[KEY_DARK] = dark }
    }

    suspend fun setSystemPrompt(prompt: String) {
        store.edit { it[KEY_SYSTEM] = prompt }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }

    companion object {
        const val DEFAULT_SYSTEM = "You are Mandu AI, a helpful, concise, and accurate assistant."
    }
}
