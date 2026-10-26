package com.david.notebook.ui.courses

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.david.notebook.NotebookApplication
import com.david.notebook.R
import com.david.notebook.data.courses.CourseFile
import com.david.notebook.data.courses.CoursesRepository
import com.david.notebook.data.courses.FolderHeader
import com.david.notebook.data.courses.FolderItem
import com.david.notebook.data.courses.SubjectItem
import com.david.notebook.data.courses.Tag
import com.david.notebook.data.courses.TagColor
import com.david.notebook.data.courses.TagOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

private val CreationExtras.courses: CoursesRepository
    get() = (this[APPLICATION_KEY] as NotebookApplication).courses

/** null until the first database read, so screens don't flash the empty state. */
private fun <T> Flow<T>.asState(vm: ViewModel): StateFlow<T?> =
    stateIn(vm.viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

/** Logo picking is offered on two screens, so both ViewModels share this. */
private fun ViewModel.setLogo(
    repository: CoursesRepository,
    subjectId: Long,
    uri: Uri,
    onError: (Int) -> Unit,
) = viewModelScope.launch {
    try {
        repository.setSubjectLogo(subjectId, uri)
    } catch (e: IOException) {
        onError(R.string.logo_failed)
    } catch (e: SecurityException) {
        onError(R.string.logo_failed)
    }
}

class CoursesViewModel(private val repository: CoursesRepository) : ViewModel() {
    private val allSubjects = repository.subjects
    val tags: StateFlow<List<Tag>> =
        repository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The tag chosen in the filter row, or null for "all subjects". */
    private val _filter = MutableStateFlow<Long?>(null)
    val filter: StateFlow<Long?> = _filter.asStateFlow()

    val subjects: StateFlow<List<SubjectItem>?> =
        combine(allSubjects, _filter, repository.tags) { subjects, filter, tags ->
            // A deleted tag can't stay selected.
            val active = filter?.takeIf { id -> tags.any { it.id == id } }
            if (active == null) subjects else subjects.filter { s -> s.tags.any { it.id == active } }
        }.asState(this)

    private val _message = MutableStateFlow<Int?>(null)
    /** A string resource to show in a snackbar. */
    val message: StateFlow<Int?> = _message.asStateFlow()

    fun setFilter(tagId: Long?) = _filter.update { if (it == tagId) null else tagId }

    fun createSubject(name: String) = viewModelScope.launch { repository.createSubject(name) }
    fun renameSubject(id: Long, name: String) = viewModelScope.launch { repository.renameSubject(id, name) }
    fun setPinned(id: Long, pinned: Boolean) = viewModelScope.launch { repository.setSubjectPinned(id, pinned) }
    fun deleteSubject(id: Long) = viewModelScope.launch { repository.deleteSubject(id) }
    fun setLogo(id: Long, uri: Uri) = setLogo(repository, id, uri) { _message.value = it }
    fun removeLogo(id: Long) = viewModelScope.launch { repository.removeSubjectLogo(id) }
    fun messageShown() = _message.update { null }

    companion object {
        val Factory = viewModelFactory { initializer { CoursesViewModel(courses) } }
    }
}

class SubjectViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CoursesRepository,
) : ViewModel() {
    val subjectId: Long = checkNotNull(savedStateHandle[SUBJECT_ID_ARG])

    val subject: StateFlow<SubjectItem?> =
        repository.subjects.map { list -> list.find { it.summary.id == subjectId } }.asState(this)
    val folders: StateFlow<List<FolderItem>?> = repository.folders(subjectId).asState(this)

    private val _message = MutableStateFlow<Int?>(null)
    /** A string resource to show in a snackbar. */
    val message: StateFlow<Int?> = _message.asStateFlow()

    fun renameSubject(name: String) = viewModelScope.launch { repository.renameSubject(subjectId, name) }
    fun deleteSubject() = viewModelScope.launch { repository.deleteSubject(subjectId) }
    fun setLogo(uri: Uri) = setLogo(repository, subjectId, uri) { _message.value = it }
    fun removeLogo() = viewModelScope.launch { repository.removeSubjectLogo(subjectId) }
    fun addFolder(name: String) = viewModelScope.launch { repository.addFolder(subjectId, name) }
    fun renameFolder(id: Long, name: String) = viewModelScope.launch { repository.renameFolder(id, name) }
    fun setFolderPinned(id: Long, pinned: Boolean) = viewModelScope.launch { repository.setFolderPinned(id, pinned) }
    fun deleteFolder(id: Long) = viewModelScope.launch { repository.deleteFolder(id) }
    fun messageShown() = _message.update { null }

    companion object {
        val Factory = viewModelFactory {
            initializer { SubjectViewModel(createSavedStateHandle(), courses) }
        }
    }
}

sealed interface FolderMessage {
    data class Imported(val count: Int) : FolderMessage
    data class ImportFailed(val count: Int) : FolderMessage
    data object RenameFailed : FolderMessage
}

class FolderViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CoursesRepository,
) : ViewModel() {
    private val folderId: Long = checkNotNull(savedStateHandle[FOLDER_ID_ARG])

    val header: StateFlow<FolderHeader?> = repository.folderHeader(folderId).asState(this)
    val files: StateFlow<List<CourseFile>?> = repository.files(folderId).asState(this)

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _message = MutableStateFlow<FolderMessage?>(null)
    val message: StateFlow<FolderMessage?> = _message.asStateFlow()

    fun importPdfs(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _importing.value = true
        viewModelScope.launch {
            val failed = repository.importPdfs(folderId, uris)
            val imported = uris.size - failed
            _importing.value = false
            _message.value = if (failed > 0) FolderMessage.ImportFailed(failed) else FolderMessage.Imported(imported)
        }
    }

    fun renameFile(file: CourseFile, name: String) = viewModelScope.launch {
        try {
            repository.renameFile(file, name)
        } catch (e: IOException) {
            _message.value = FolderMessage.RenameFailed
        }
    }

    fun setPinned(file: CourseFile, pinned: Boolean) = viewModelScope.launch { repository.setFilePinned(file, pinned) }

    fun deleteFile(file: CourseFile) = viewModelScope.launch { repository.deleteFile(file) }

    fun shareableUri(file: CourseFile): Uri = repository.shareableUri(file)

    fun messageShown() = _message.update { null }

    companion object {
        val Factory = viewModelFactory {
            initializer { FolderViewModel(createSavedStateHandle(), courses) }
        }
    }
}

/** Backs [TagsSheet] for one subject or folder. */
class TagsViewModel(
    private val owner: TagOwner,
    private val repository: CoursesRepository,
) : ViewModel() {
    val tags: StateFlow<List<Tag>> =
        repository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selected: StateFlow<Set<Long>> = repository.tagIds(owner).map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun setTagged(tag: Tag, tagged: Boolean) = viewModelScope.launch { repository.setTagged(owner, tag.id, tagged) }
    fun create(name: String, color: TagColor) = viewModelScope.launch { repository.createTag(name, color, owner) }
    fun update(tag: Tag, name: String, color: TagColor) = viewModelScope.launch { repository.updateTag(tag, name, color) }
    fun delete(tag: Tag) = viewModelScope.launch { repository.deleteTag(tag) }

    companion object {
        fun factory(owner: TagOwner) = viewModelFactory { initializer { TagsViewModel(owner, courses) } }
    }
}
