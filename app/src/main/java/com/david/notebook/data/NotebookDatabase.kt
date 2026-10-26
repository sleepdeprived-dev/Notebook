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

/**
 * Version history (schemas in app/schemas/):
 * 1. Subjects, folders, PDFs.
 * 2. Pinning, subject logos, coloured tags. Only adds tables and columns, so Room
 *    migrates it automatically by diffing the two exported schemas — existing
 *    subjects and PDFs are kept.
 */
@Database(
    entities = [
        Subject::class, Folder::class, CourseFile::class,
        Tag::class, SubjectTag::class, FolderTag::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class NotebookDatabase : RoomDatabase() {
    abstract fun coursesDao(): CoursesDao

    companion object {
        fun create(context: Context): NotebookDatabase =
            Room.databaseBuilder(context, NotebookDatabase::class.java, "notebook.db").build()
    }
}
