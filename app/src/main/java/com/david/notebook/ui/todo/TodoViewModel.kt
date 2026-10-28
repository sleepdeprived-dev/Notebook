package com.david.notebook.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.david.notebook.NotebookApplication
import com.david.notebook.data.tests.AssessmentDao
import com.david.notebook.data.tests.AssessmentWithSubject
import com.david.notebook.data.todo.Todo
import com.david.notebook.data.todo.TodoDao
import com.david.notebook.data.todo.TodoWithSubject
import com.david.notebook.ui.components.SubjectOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TodoViewModel(
    private val dao: TodoDao,
    assessments: AssessmentDao,
    subjects: Flow<List<SubjectOption>>,
) : ViewModel() {
    /** null until the first database read, so the empty state doesn't flash. */
    val todos: StateFlow<List<TodoWithSubject>?> =
        dao.todos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Tests in the next week, shown above the tasks. */
    val comingUp: StateFlow<List<AssessmentWithSubject>> = assessments.all()
        .map { list ->
            val today = LocalDate.now().toEpochDay()
            list.filter { it.assessment.date in today..today + 7 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subjects: StateFlow<List<SubjectOption>> =
        subjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Creates a task, or updates [existing] keeping its done state, dates and linked test. */
    fun save(existing: Todo?, title: String, subjectId: Long?, dueDate: Long?, time: Int?) = viewModelScope.launch {
        val clean = title.trim()
        if (existing == null) {
            dao.insert(Todo(title = clean, subjectId = subjectId, dueDate = dueDate, time = time))
        } else {
            dao.update(existing.copy(title = clean, subjectId = subjectId, dueDate = dueDate, time = time))
        }
    }

    fun setDone(todo: Todo, done: Boolean) = viewModelScope.launch {
        dao.setDone(todo.id, done, if (done) System.currentTimeMillis() else null)
    }

    fun delete(todo: Todo) = viewModelScope.launch { dao.delete(todo.id) }

    /**
     * Undo for a delete. The task comes back as a new row (new id): the list remembers
     * each row's swipe state by id, and reusing the old id would bring the row back
     * still "swiped away", which deletes it again.
     */
    fun restore(todo: Todo) = viewModelScope.launch { dao.insert(todo.copy(id = 0)) }

    fun clearCompleted() = viewModelScope.launch { dao.deleteCompleted() }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NotebookApplication
                TodoViewModel(
                    app.todos,
                    app.assessments,
                    app.courses.subjects.map { list -> list.map { SubjectOption(it.summary.id, it.summary.name) } },
                )
            }
        }
    }
}
