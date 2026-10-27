package com.david.notebook

import android.app.Application
import com.david.notebook.data.NotebookDatabase
import com.david.notebook.data.courses.CoursesRepository
import com.david.notebook.data.schedule.ScheduleRepository
import com.david.notebook.data.settings.SettingsRepository
import com.david.notebook.data.update.UpdateRepository

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
    val updates by lazy { UpdateRepository(this) }
}
