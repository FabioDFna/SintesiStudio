package com.sintesistudio.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sintesistudio.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settings: SettingsStore) : ViewModel() {
    val apiKey = settings.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val geminiModel = settings.geminiModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun saveKey(key: String) {
        viewModelScope.launch { settings.setGeminiApiKey(key) }
    }

    fun saveModel(model: String) {
        viewModelScope.launch { settings.setGeminiModel(model) }
    }
}

class SettingsViewModelFactory(private val settings: SettingsStore) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SettingsViewModel(settings) as T
}
