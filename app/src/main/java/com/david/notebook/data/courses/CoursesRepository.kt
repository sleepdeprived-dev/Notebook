package com.david.notebook.data.courses

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.david.notebook.data.images.centerSquare
import com.david.notebook.data.images.decodeBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Subjects, folders and imported PDFs. Room holds the structure; the PDFs themselves
 * are copied to files/courses/ so they survive the originals being deleted. Every
 * delete here removes both the rows and the files, so nothing is left orphaned on disk.
 * Subject logos are small square PNGs in files/logos/.
 */
class CoursesRepository(
    private val context: Context,
    private val dao: CoursesDao,
) {
    private val baseDir = File(context.filesDir, "courses")
    private val logosDir = File(context.filesDir, "logos")

    /** Subjects with their tags and logo, pinned first. */
    val subjects: Flow<List<SubjectItem>> = combine(dao.subjects(), dao.subjectTags()) { subjects, links ->
        val tags = links.groupBy({ it.ownerId }, { it.toTag() })
        subjects.map { SubjectItem(it, it.logoPath?.let { path -> File(logosDir, path) }, tags[it.id].orEmpty()) }
    }

    fun subject(id: Long): Flow<Subject?> = dao.subject(id)

    fun folders(subjectId: Long): Flow<List<FolderItem>> =
        combine(dao.folders(subjectId), dao.folderTags(subjectId)) { folders, links ->
            val tags = links.groupBy({ it.ownerId }, { it.toTag() })
            folders.map { FolderItem(it, tags[it.id].orEmpty()) }
        }

    fun folderHeader(folderId: Long): Flow<FolderHeader?> = dao.folderHeader(folderId)
    fun files(folderId: Long): Flow<List<CourseFile>> = dao.files(folderId)

    suspend fun createSubject(name: String): Long =
        dao.createSubject(name.trim(), DEFAULT_FOLDERS)

    suspend fun renameSubject(id: Long, name: String) = dao.renameSubject(id, name.trim())

    suspend fun setSubjectPinned(id: Long, pinned: Boolean) = dao.setSubjectPinned(id, pinned)

    suspend fun deleteSubject(id: Long) {
        val paths = dao.storedPathsForSubject(id)
        val logo = dao.subjectLogo(id)
        dao.deleteSubject(id)
        deleteStored(paths)
        deleteLogoFile(logo)
    }

    /** Crops the picked image to a square, saves it as the subject's logo, and drops the old one. */
    suspend fun setSubjectLogo(id: Long, uri: Uri) {
        val name = "subject-$id-${System.currentTimeMillis()}.png"
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val picked = decodeBitmap(LOGO_SIZE * 2) {
                resolver.openInputStream(uri) ?: throw IOException("Cannot read $uri")
            }
            logosDir.mkdirs()
            // PNG rather than JPEG so transparent logos stay transparent.
            File(logosDir, name).outputStream().use {
                centerSquare(picked, LOGO_SIZE).compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        val old = dao.subjectLogo(id)
        dao.setSubjectLogo(id, name)
        deleteLogoFile(old)
    }

    suspend fun removeSubjectLogo(id: Long) {
        val old = dao.subjectLogo(id)
        dao.setSubjectLogo(id, null)
        deleteLogoFile(old)
    }

    private suspend fun deleteLogoFile(name: String?) {
        if (name != null) withContext(Dispatchers.IO) { File(logosDir, name).delete() }
    }

    suspend fun addFolder(subjectId: Long, name: String) {
        dao.insertFolder(
            Folder(subjectId = subjectId, name = name.trim(), position = dao.nextFolderPosition(subjectId)),
        )
    }

    suspend fun renameFolder(id: Long, name: String) = dao.renameFolder(id, name.trim())

    suspend fun setFolderPinned(id: Long, pinned: Boolean) = dao.setFolderPinned(id, pinned)

    suspend fun deleteFolder(id: Long) {
        val paths = dao.storedPathsForFolder(id)
        dao.deleteFolder(id)
        deleteStored(paths)
    }

    /** Copies each PDF into the folder. Returns how many failed (unreadable or vanished). */
    suspend fun importPdfs(folderId: Long, uris: List<Uri>): Int {
        var failed = 0
        for (uri in uris) {
            try {
                importPdf(folderId, uri)
            } catch (e: IOException) {
                failed++
            } catch (e: SecurityException) {
                failed++
            }
        }
        return failed
    }

    private suspend fun importPdf(folderId: Long, uri: Uri) {
        val displayName = withContext(Dispatchers.IO) { queryDisplayName(uri) }
        val name = pdfName(displayName ?: "Document")
        val storedPath = "${UUID.randomUUID()}/$name"
        val target = File(baseDir, storedPath)

        val size = withContext(Dispatchers.IO) {
            target.parentFile!!.mkdirs()
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Cannot read $uri")
                input.use { source -> target.outputStream().use { source.copyTo(it) } }
            } catch (e: IOException) {
                target.parentFile!!.deleteRecursively()
                throw e
            }
            target.length()
        }
        dao.insertFile(CourseFile(folderId = folderId, name = name, storedPath = storedPath, sizeBytes = size))
    }

    suspend fun renameFile(file: CourseFile, newName: String) {
        val name = pdfName(newName)
        if (name == file.name) return
        val old = File(baseDir, file.storedPath)
        val renamed = File(old.parentFile, name)
        withContext(Dispatchers.IO) {
            if (!old.renameTo(renamed)) throw IOException("Cannot rename ${file.name}")
        }
        dao.renameFile(file.id, name, "${old.parentFile!!.name}/$name")
    }

    suspend fun setFilePinned(file: CourseFile, pinned: Boolean) = dao.setFilePinned(file.id, pinned)

    suspend fun deleteFile(file: CourseFile) {
        dao.deleteFile(file.id)
        deleteStored(listOf(file.storedPath))
    }

    // Tags

    val tags: Flow<List<Tag>> = dao.tags()

    fun tagIds(owner: TagOwner): Flow<List<Long>> = when (owner) {
        is TagOwner.OfSubject -> dao.subjectTagIds(owner.id)
        is TagOwner.OfFolder -> dao.folderTagIds(owner.id)
    }

    suspend fun setTagged(owner: TagOwner, tagId: Long, tagged: Boolean) = when (owner) {
        is TagOwner.OfSubject ->
            if (tagged) dao.addSubjectTag(SubjectTag(owner.id, tagId)) else dao.removeSubjectTag(owner.id, tagId)
        is TagOwner.OfFolder ->
            if (tagged) dao.addFolderTag(FolderTag(owner.id, tagId)) else dao.removeFolderTag(owner.id, tagId)
    }

    /** Creates a tag and puts it straight on [owner], since that's why it was created. */
    suspend fun createTag(name: String, color: TagColor, owner: TagOwner) {
        val id = dao.insertTag(Tag(name = name.trim(), color = color.name))
        setTagged(owner, id, true)
    }

    suspend fun updateTag(tag: Tag, name: String, color: TagColor) = dao.updateTag(tag.id, name.trim(), color.name)

    /** Removes the tag everywhere it's used (the links go with it via CASCADE). */
    suspend fun deleteTag(tag: Tag) = dao.deleteTag(tag.id)

    /** A content:// link other apps can read once granted; see res/xml/file_paths.xml. */
    fun shareableUri(file: CourseFile): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(baseDir, file.storedPath))

    private suspend fun deleteStored(paths: List<String>) = withContext(Dispatchers.IO) {
        paths.forEach { File(baseDir, it).parentFile?.deleteRecursively() }
    }

    private fun queryDisplayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    companion object {
        val DEFAULT_FOLDERS = listOf("Curs", "Seminar")
        private const val LOGO_SIZE = 256

        /** A safe file name that always ends in .pdf, so viewers recognise it. */
        fun pdfName(raw: String): String {
            val cleaned = raw.trim().replace(Regex("[/\\\\:*?\"<>|\\u0000]"), "_").ifBlank { "Document" }
            return if (cleaned.endsWith(".pdf", ignoreCase = true)) cleaned else "$cleaned.pdf"
        }
    }
}
