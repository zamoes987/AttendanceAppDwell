package com.attendancetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendancetracker.data.models.AppSettings
import com.attendancetracker.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen.
 *
 * Manages app settings and preferences.
 *
 * @property preferencesRepository Repository for managing preferences
 */
class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    /**
     * Current app settings.
     */
    val settings: StateFlow<AppSettings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    /**
     * Updates the Google Spreadsheet ID.
     */
    fun updateSpreadsheetId(id: String) {
        viewModelScope.launch {
            preferencesRepository.updateSpreadsheetId(id)
        }
    }

    /**
     * Toggles morning notification setting.
     */
    fun toggleMorningNotification(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateMorningNotification(enabled)
            // TODO: Schedule or cancel morning notification
        }
    }

    /**
     * Toggles evening notification setting.
     */
    fun toggleEveningNotification(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateEveningNotification(enabled)
            // TODO: Schedule or cancel evening notification
        }
    }
}
