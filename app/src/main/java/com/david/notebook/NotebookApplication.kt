package com.david.notebook

import android.app.Application
import com.david.notebook.data.NotebookDatabase
import com.david.notebook.data.courses.CoursesRepository
import com.david.notebook.data.reminders.ReminderScheduler
import com.david.notebook.data.schedule.ScheduleRepository
import com.david.notebook.data.settings.SettingsRepository
import com.david.notebook.data.update.UpdateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Holds the app-wide singletons. ViewModels reach them through their factories,
 * so there is exactly one copy of each no matter how many screens use it.
 */
class NotebookApplication : Application() {
    val database by lazy { NotebookDatabase.create(this) }
    val settings by lazy { SettingsRepository(this) }
    val schedule by lazy { ScheduleRepository(this) }
    val courses by lazy { CoursesRepository(this, database.coursesDao()) }
    val todos by lazy { database.todoDao() }
    val assessments by lazy { database.assessmentDao() }
    val updates by lazy { UpdateRepository(this) }
    val reminders by lazy { ReminderScheduler(this, todos, assessments, settings) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
        // Any change to tests, tasks or the Reminders setting re-sets the alarms.
        scope.launch {
            combine(todos.todos(), assessments.all(), settings.settings) { _, _, s -> s.reminders }
                .debounce(500)
                .collectLatest { reminders.reschedule() }
        }
    }
}
