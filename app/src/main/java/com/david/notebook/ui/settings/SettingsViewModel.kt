package com.david.notebook.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.david.notebook.NotebookApplication
import com.david.notebook.data.settings.Accent
import com.david.notebook.data.settings.AppSettings
import com.david.notebook.data.settings.SettingsRepository
import com.david.notebook.data.settings.ThemeMode
import com.david.notebook.data.update.Release
import com.david.notebook.data.update.UpdateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.File
import java.io.IOException

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    data class ReadyToInstall(val release: Release, val apk: File) : UpdateState
    data object CheckFailed : UpdateState
    data class DownloadFailed(val release: Release) : UpdateState
}

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val updates: UpdateRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings
    val currentVersion: String = updates.currentVersion

    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update: StateFlow<UpdateState> = _update.asStateFlow()
    private var job: Job? = null

    fun setThemeMode(mode: ThemeMode) = repository.setThemeMode(mode)
    fun setAccent(accent: Accent) = repository.setAccent(accent)
    fun setReminders(on: Boolean) = repository.setReminders(on)

    fun checkForUpdate() {
        job?.cancel()
        _update.value = UpdateState.Checking
        job = viewModelScope.launch {
            _update.value = try {
                val release = updates.latestRelease()
                if (updates.isNewer(release)) UpdateState.Available(release) else UpdateState.UpToDate
            } catch (e: IOException) {
                UpdateState.CheckFailed // offline, GitHub down, rate-limited, no APK attached
            } catch (e: JSONException) {
                UpdateState.CheckFailed
            }
        }
    }

    fun download(release: Release) {
        job?.cancel()
        _update.value = UpdateState.Downloading(release, 0f)
        job = viewModelScope.launch {
            _update.value = try {
                val apk = updates.download(release) { progress ->
                    _update.value = UpdateState.Downloading(release, progress)
                }
                UpdateState.ReadyToInstall(release, apk)
            } catch (e: IOException) {
                UpdateState.DownloadFailed(release)
            }
        }
    }

    fun installIntent(apk: File): Intent = updates.installIntent(apk)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NotebookApplication
                SettingsViewModel(app.settings, app.updates)
            }
        }
    }
}
