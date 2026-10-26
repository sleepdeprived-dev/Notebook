package com.david.notebook.ui.schedule

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.david.notebook.NotebookApplication
import com.david.notebook.R
import com.david.notebook.data.schedule.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

data class ScheduleUiState(
    val loading: Boolean = true,
    val importing: Boolean = false,
    val file: File? = null,
    @StringRes val message: Int? = null,
)

class ScheduleViewModel(private val repository: ScheduleRepository) : ViewModel() {
    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val file = repository.current()
            _state.update { it.copy(loading = false, file = file) }
        }
    }

    fun import(uri: Uri) {
        _state.update { it.copy(importing = true) }
        viewModelScope.launch {
            try {
                val file = repository.import(uri)
                _state.update { it.copy(importing = false, file = file) }
            } catch (e: IOException) {
                _state.update { it.copy(importing = false, message = R.string.schedule_import_failed) }
            } catch (e: SecurityException) {
                _state.update { it.copy(importing = false, message = R.string.schedule_import_failed) }
            }
        }
    }

    fun remove() {
        viewModelScope.launch {
            repository.remove()
            _state.update { it.copy(file = null) }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer { ScheduleViewModel((this[APPLICATION_KEY] as NotebookApplication).schedule) }
        }
    }
}
