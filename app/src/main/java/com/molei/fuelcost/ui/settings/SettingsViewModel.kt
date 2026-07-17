package com.molei.fuelcost.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.molei.fuelcost.FuelCostApp
import com.molei.fuelcost.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {

    private val _apiKeyDraft = MutableStateFlow("")
    val apiKeyDraft: StateFlow<String> = _apiKeyDraft.asStateFlow()

    init {
        viewModelScope.launch { _apiKeyDraft.value = settings.apiKey.first() }
    }

    fun onApiKeyChanged(value: String) {
        _apiKeyDraft.value = value
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            settings.setApiKey(_apiKeyDraft.value)
            onSaved()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FuelCostApp
                SettingsViewModel(app.settings)
            }
        }
    }
}
