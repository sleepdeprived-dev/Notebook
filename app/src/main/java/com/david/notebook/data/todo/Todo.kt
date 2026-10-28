package com.david.notebook.data.todo

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.david.notebook.data.courses.Subject
import com.david.notebook.data.tests.Assessment
import kotlinx.coroutines.flow.Flow

/**
 * A task. [subjectId] and [dueDate] are optional; deleting the subject keeps the
 * task and just clears its subject (SET_NULL). [dueDate] is a day, not a moment:
 * `LocalDate.toEpochDay()`, so "due Friday" doesn't shift with time zones; [time]
 * (minutes after midnight) only means something with a date. [assessmentId] links a
 * study task to the test it's for, and is cleared if the test is deleted.
 */
@Entity(
    tableName = "todos",
    foreignKeys = [
        ForeignKey(Subject::class, ["id"], ["subjectId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(Assessment::class, ["id"], ["assessmentId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("subjectId"), Index("assessmentId")],
)
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectId: Long? = null,
    val dueDate: Long? = null,
    val time: Int? = null,
    val assessmentId: Long? = null,
    @ColumnInfo(defaultValue = "0") val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
)

/** [testName] and [testKind] describe the linked test, if any. */
data class TodoWithSubject(
    @Embedded val todo: Todo,
    val subjectName: String?,
    val testName: String? = null,
    val testKind: String? = null,
)

@Dao
interface TodoDao {
    /** Open tasks by due date and time (undated last), then completed ones, newest first. */
    @Query(
        """
        SELECT t.*, s.name AS subjectName,
            COALESCE(NULLIF(a.title, ''), sa.name) AS testName, a.kind AS testKind
        FROM todos t
            LEFT JOIN subjects s ON s.id = t.subjectId
            LEFT JOIN assessments a ON a.id = t.assessmentId
            LEFT JOIN subjects sa ON sa.id = a.subjectId
        ORDER BY t.done,
            CASE WHEN t.done = 0 AND t.dueDate IS NULL THEN 1 ELSE 0 END,
            CASE WHEN t.done = 0 THEN t.dueDate END,
            CASE WHEN t.done = 0 THEN t.time IS NULL END,
            CASE WHEN t.done = 0 THEN t.time END,
            t.completedAt DESC,
            t.createdAt
        """,
    )
    fun todos(): Flow<List<TodoWithSubject>>

    /** Open tasks due today or later, for reminders. */
    @Query("SELECT * FROM todos WHERE done = 0 AND dueDate >= :today")
    suspend fun openFrom(today: Long): List<Todo>

    @Insert
    suspend fun insert(todo: Todo): Long

    @Update
    suspend fun update(todo: Todo)

    @Query("UPDATE todos SET done = :done, completedAt = :completedAt WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, completedAt: Long?)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM todos WHERE done = 1")
    suspend fun deleteCompleted()
}
