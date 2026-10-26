package com.david.notebook.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.david.notebook.NotebookApplication
import com.david.notebook.data.settings.Accent
import com.david.notebook.data.settings.AppSettings
import com.david.notebook.data.settings.SettingsRepository
import com.david.notebook.data.settings.ThemeMode
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings

    fun setThemeMode(mode: ThemeMode) = repository.setThemeMode(mode)
    fun setAccent(accent: Accent) = repository.setAccent(accent)

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel((this[APPLICATION_KEY] as NotebookApplication).settings) }
        }
    }
}
