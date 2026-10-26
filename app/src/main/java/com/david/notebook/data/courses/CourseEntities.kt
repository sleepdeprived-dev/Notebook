package com.david.notebook.data.courses

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.File

// New columns carry a defaultValue so the 1 → 2 auto-migration can add them to
// rows that already exist on the phone.

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
    /** File name of the subject's logo inside files/logos/, or null for the letter tile. */
    val logoPath: String? = null,
)

/** Deleting a subject deletes its folders (CASCADE); the repository removes the files on disk. */
@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("subjectId")],
)
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val name: String,
    /** Display order: Curs and Seminar first, then folders in the order they were added. */
    val position: Int,
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
)

/**
 * An imported PDF. The bytes live at files/courses/[storedPath], where storedPath is
 * "<random id>/<name>": the random folder keeps names unique, and keeping the real
 * name means the PDF viewer shows "Curs 3.pdf" instead of a random string.
 */
@Entity(
    tableName = "course_files",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("folderId")],
)
data class CourseFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val name: String,
    val storedPath: String,
    val sizeBytes: Long,
    val addedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
)

/** One shared set of tags, used on both subjects and folders. [color] is a [TagColor] name. */
@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: String,
)

enum class TagColor { Red, Orange, Yellow, Green, Teal, Blue, Purple, Pink, Grey }

fun Tag.tagColor(): TagColor = TagColor.entries.find { it.name == color } ?: TagColor.Grey

@Entity(
    tableName = "subject_tags",
    primaryKeys = ["subjectId", "tagId"],
    foreignKeys = [
        ForeignKey(Subject::class, ["id"], ["subjectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Tag::class, ["id"], ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tagId")],
)
data class SubjectTag(val subjectId: Long, val tagId: Long)

@Entity(
    tableName = "folder_tags",
    primaryKeys = ["folderId", "tagId"],
    foreignKeys = [
        ForeignKey(Folder::class, ["id"], ["folderId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Tag::class, ["id"], ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tagId")],
)
data class FolderTag(val folderId: Long, val tagId: Long)

/** A tag together with the subject or folder it's attached to, for grouping. */
data class OwnedTag(
    val ownerId: Long,
    val id: Long,
    val name: String,
    val color: String,
) {
    fun toTag() = Tag(id, name, color)
}

/** A subject row for the list, with counts so the card can say "2 folders · 7 PDFs". */
data class SubjectSummary(
    val id: Long,
    val name: String,
    val pinned: Boolean,
    val logoPath: String?,
    val folderCount: Int,
    val fileCount: Int,
)

data class FolderSummary(
    val id: Long,
    val name: String,
    val pinned: Boolean,
    val fileCount: Int,
)

/** What the folder screen's top bar needs: the folder and the subject it belongs to. */
data class FolderHeader(
    val id: Long,
    val name: String,
    val subjectName: String,
)

// What the screens get from the repository: rows with their tags (and logo file) attached.

data class SubjectItem(val summary: SubjectSummary, val logo: File?, val tags: List<Tag>)

data class FolderItem(val summary: FolderSummary, val tags: List<Tag>)

/** Something tags can be put on. */
sealed interface TagOwner {
    data class OfSubject(val id: Long) : TagOwner
    data class OfFolder(val id: Long) : TagOwner
}
