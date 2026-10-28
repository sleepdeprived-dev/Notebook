package com.david.notebook.ui.tests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.david.notebook.NotebookApplication
import com.david.notebook.data.tests.Assessment
import com.david.notebook.data.tests.AssessmentDao
import com.david.notebook.data.tests.AssessmentKind
import com.david.notebook.data.tests.AssessmentWithSubject
import com.david.notebook.data.todo.Todo
import com.david.notebook.data.todo.TodoDao
import com.david.notebook.ui.components.SubjectOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TestsViewModel(
    private val dao: AssessmentDao,
    private val todos: TodoDao,
    subjects: Flow<List<SubjectOption>>,
) : ViewModel() {
    /** Everything, by date; the screen splits it into upcoming and past. null until loaded. */
    val items: StateFlow<List<AssessmentWithSubject>?> =
        dao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val subjects: StateFlow<List<SubjectOption>> =
        subjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(
        existing: Assessment?,
        title: String,
        kind: AssessmentKind,
        subjectId: Long?,
        date: Long,
        time: Int?,
        notes: String,
        grade: Double?,
    ) = viewModelScope.launch {
        val fields = Assessment(
            title = title.trim(),
            kind = kind.name,
            subjectId = subjectId,
            date = date,
            time = time,
            notes = notes.trim(),
            grade = grade,
        )
        if (existing == null) dao.insert(fields) else dao.update(fields.copy(id = existing.id, createdAt = existing.createdAt))
    }

    /** A [title] task in To Do, linked to [assessment], due the day before it (or today). */
    fun addStudyTask(assessment: Assessment, title: String) = viewModelScope.launch {
        val today = LocalDate.now().toEpochDay()
        todos.insert(
            Todo(
                title = title,
                subjectId = assessment.subjectId,
                dueDate = maxOf(today, assessment.date - 1),
                assessmentId = assessment.id,
            ),
        )
    }

    fun delete(assessment: Assessment) = viewModelScope.launch { dao.delete(assessment.id) }

    /** Undo for a delete, under a new id (see SwipeToDelete for why). */
    fun restore(assessment: Assessment) = viewModelScope.launch { dao.insert(assessment.copy(id = 0)) }

    fun clearPast() = viewModelScope.launch { dao.deletePast(LocalDate.now().toEpochDay()) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NotebookApplication
                TestsViewModel(
                    app.assessments,
                    app.todos,
                    app.courses.subjects.map { list -> list.map { SubjectOption(it.summary.id, it.summary.name) } },
                )
            }
        }
    }
}
