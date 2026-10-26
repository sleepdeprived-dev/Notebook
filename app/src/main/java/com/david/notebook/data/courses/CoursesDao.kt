package com.david.notebook.data.courses

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CoursesDao {

    // Subjects (pinned first, then by name)

    @Query(
        """
        SELECT s.id, s.name, s.pinned, s.logoPath,
            (SELECT COUNT(*) FROM folders f WHERE f.subjectId = s.id) AS folderCount,
            (SELECT COUNT(*) FROM course_files c JOIN folders f ON c.folderId = f.id
                WHERE f.subjectId = s.id) AS fileCount
        FROM subjects s
        ORDER BY s.pinned DESC, s.name COLLATE NOCASE
        """,
    )
    fun subjects(): Flow<List<SubjectSummary>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun subject(id: Long): Flow<Subject?>

    @Insert
    suspend fun insertSubject(subject: Subject): Long

    /** A new subject always starts with the two folders every ASE course has. */
    @Transaction
    suspend fun createSubject(name: String, defaultFolders: List<String>): Long {
        val id = insertSubject(Subject(name = name))
        defaultFolders.forEachIndexed { index, folder ->
            insertFolder(Folder(subjectId = id, name = folder, position = index))
        }
        return id
    }

    @Query("UPDATE subjects SET name = :name WHERE id = :id")
    suspend fun renameSubject(id: Long, name: String)

    @Query("UPDATE subjects SET pinned = :pinned WHERE id = :id")
    suspend fun setSubjectPinned(id: Long, pinned: Boolean)

    @Query("SELECT logoPath FROM subjects WHERE id = :id")
    suspend fun subjectLogo(id: Long): String?

    @Query("UPDATE subjects SET logoPath = :logoPath WHERE id = :id")
    suspend fun setSubjectLogo(id: Long, logoPath: String?)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: Long)

    // Folders (pinned first, then Curs, Seminar, and the rest in the order added)

    @Query(
        """
        SELECT f.id, f.name, f.pinned,
            (SELECT COUNT(*) FROM course_files c WHERE c.folderId = f.id) AS fileCount
        FROM folders f
        WHERE f.subjectId = :subjectId
        ORDER BY f.pinned DESC, f.position, f.id
        """,
    )
    fun folders(subjectId: Long): Flow<List<FolderSummary>>

    @Query(
        """
        SELECT f.id, f.name, s.name AS subjectName
        FROM folders f JOIN subjects s ON s.id = f.subjectId
        WHERE f.id = :id
        """,
    )
    fun folderHeader(id: Long): Flow<FolderHeader?>

    @Insert
    suspend fun insertFolder(folder: Folder): Long

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM folders WHERE subjectId = :subjectId")
    suspend fun nextFolderPosition(subjectId: Long): Int

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun renameFolder(id: Long, name: String)

    @Query("UPDATE folders SET pinned = :pinned WHERE id = :id")
    suspend fun setFolderPinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: Long)

    // Files (pinned first, then by name)

    @Query("SELECT * FROM course_files WHERE folderId = :folderId ORDER BY pinned DESC, name COLLATE NOCASE")
    fun files(folderId: Long): Flow<List<CourseFile>>

    @Insert
    suspend fun insertFile(file: CourseFile): Long

    @Query("UPDATE course_files SET name = :name, storedPath = :storedPath WHERE id = :id")
    suspend fun renameFile(id: Long, name: String, storedPath: String)

    @Query("UPDATE course_files SET pinned = :pinned WHERE id = :id")
    suspend fun setFilePinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM course_files WHERE id = :id")
    suspend fun deleteFile(id: Long)

    @Query(
        """
        SELECT c.storedPath FROM course_files c JOIN folders f ON c.folderId = f.id
        WHERE f.subjectId = :subjectId
        """,
    )
    suspend fun storedPathsForSubject(subjectId: Long): List<String>

    @Query("SELECT storedPath FROM course_files WHERE folderId = :folderId")
    suspend fun storedPathsForFolder(folderId: Long): List<String>

    // Tags

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun tags(): Flow<List<Tag>>

    @Insert
    suspend fun insertTag(tag: Tag): Long

    @Query("UPDATE tags SET name = :name, color = :color WHERE id = :id")
    suspend fun updateTag(id: Long, name: String, color: String)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    @Query(
        """
        SELECT st.subjectId AS ownerId, t.id, t.name, t.color
        FROM subject_tags st JOIN tags t ON t.id = st.tagId
        ORDER BY t.name COLLATE NOCASE
        """,
    )
    fun subjectTags(): Flow<List<OwnedTag>>

    @Query(
        """
        SELECT ft.folderId AS ownerId, t.id, t.name, t.color
        FROM folder_tags ft
            JOIN tags t ON t.id = ft.tagId
            JOIN folders f ON f.id = ft.folderId
        WHERE f.subjectId = :subjectId
        ORDER BY t.name COLLATE NOCASE
        """,
    )
    fun folderTags(subjectId: Long): Flow<List<OwnedTag>>

    @Query("SELECT tagId FROM subject_tags WHERE subjectId = :subjectId")
    fun subjectTagIds(subjectId: Long): Flow<List<Long>>

    @Query("SELECT tagId FROM folder_tags WHERE folderId = :folderId")
    fun folderTagIds(folderId: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSubjectTag(link: SubjectTag)

    @Query("DELETE FROM subject_tags WHERE subjectId = :subjectId AND tagId = :tagId")
    suspend fun removeSubjectTag(subjectId: Long, tagId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFolderTag(link: FolderTag)

    @Query("DELETE FROM folder_tags WHERE folderId = :folderId AND tagId = :tagId")
    suspend fun removeFolderTag(folderId: Long, tagId: Long)
}
