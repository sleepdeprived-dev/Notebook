package com.david.notebook.data.tests

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
import kotlinx.coroutines.flow.Flow

enum class AssessmentKind { Exam, Colloquium, Deadline }

/**
 * An exam, colloquium or deadline (the Tests tab). [date] is a day
 * (`LocalDate.toEpochDay()`), [time] optional minutes after midnight. [grade] is what
 * you got, entered once it's over. Deleting the subject keeps the entry and just
 * clears its subject (SET_NULL).
 */
@Entity(
    tableName = "assessments",
    foreignKeys = [
        ForeignKey(Subject::class, ["id"], ["subjectId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("subjectId"), Index("date")],
)
data class Assessment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** An [AssessmentKind] name. */
    val kind: String,
    val subjectId: Long? = null,
    val date: Long,
    val time: Int? = null,
    val notes: String = "",
    val grade: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun assessmentKind(): AssessmentKind =
        AssessmentKind.entries.find { it.name == kind } ?: AssessmentKind.Exam
}

data class AssessmentWithSubject(
    @Embedded val assessment: Assessment,
    val subjectName: String?,
)

@Dao
interface AssessmentDao {
    /** By date, then time (untimed entries last within their day). */
    @Query(
        """
        SELECT a.*, s.name AS subjectName
        FROM assessments a LEFT JOIN subjects s ON s.id = a.subjectId
        ORDER BY a.date, a.time IS NULL, a.time, a.createdAt
        """,
    )
    fun all(): Flow<List<AssessmentWithSubject>>

    @Insert
    suspend fun insert(assessment: Assessment): Long

    @Update
    suspend fun update(assessment: Assessment)

    @Query("DELETE FROM assessments WHERE id = :id")
    suspend fun delete(id: Long)

    /** Upcoming entries, for reminders: they're scheduled from outside any screen. */
    @Query("SELECT a.*, s.name AS subjectName FROM assessments a LEFT JOIN subjects s ON s.id = a.subjectId WHERE a.date >= :today")
    suspend fun upcoming(today: Long): List<AssessmentWithSubject>

    @Query("DELETE FROM assessments WHERE date < :today")
    suspend fun deletePast(today: Long)
}
