package com.david.notebook.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.david.notebook.data.courses.CourseFile
import com.david.notebook.data.courses.CoursesDao
import com.david.notebook.data.courses.Folder
import com.david.notebook.data.courses.FolderTag
import com.david.notebook.data.courses.Subject
import com.david.notebook.data.courses.SubjectTag
import com.david.notebook.data.courses.Tag
import com.david.notebook.data.tests.Assessment
import com.david.notebook.data.tests.AssessmentDao
import com.david.notebook.data.todo.Todo
import com.david.notebook.data.todo.TodoDao

/**
 * Version history (schemas in app/schemas/):
 * 1. Subjects, folders, PDFs.
 * 2. Pinning, subject logos, coloured tags. Only adds tables and columns, so Room
 *    migrates it automatically by diffing the two exported schemas — existing
 *    subjects and PDFs are kept.
 * 3. To Do tasks (new table, auto-migrated).
 * 4. Tests: exams, colloquia and deadlines (new table, auto-migrated).
 * 5. Grades on tests; a time and a linked test on tasks (new columns, auto-migrated).
 */
@Database(
    entities = [
        Subject::class, Folder::class, CourseFile::class,
        Tag::class, SubjectTag::class, FolderTag::class,
        Todo::class, Assessment::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
abstract class NotebookDatabase : RoomDatabase() {
    abstract fun coursesDao(): CoursesDao
    abstract fun todoDao(): TodoDao
    abstract fun assessmentDao(): AssessmentDao

    companion object {
        fun create(context: Context): NotebookDatabase =
            Room.databaseBuilder(context, NotebookDatabase::class.java, "notebook.db").build()
    }
}
