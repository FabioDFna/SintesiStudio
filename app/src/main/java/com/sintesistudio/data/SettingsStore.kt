package com.sintesistudio.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sintesistudio.ai.GeminiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    private val geminiKey = stringPreferencesKey("gemini_api_key")
    private val geminiModelKey = stringPreferencesKey("gemini_model")

    val geminiApiKey: Flow<String?> = context.dataStore.data.map { it[geminiKey] }

    val geminiModel: Flow<String> = context.dataStore.data.map {
        it[geminiModelKey] ?: GeminiService.DEFAULT_MODEL
    }

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { it[geminiKey] = key.trim() }
    }

    suspend fun setGeminiModel(model: String) {
        context.dataStore.edit { it[geminiModelKey] = model }
    }
}
