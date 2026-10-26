package com.david.notebook.data.schedule

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * The timetable is a single PDF or image, copied into the app's private storage
 * (files/schedule/) so it survives the original being deleted or moved.
 */
class ScheduleRepository(private val context: Context) {
    private val dir = File(context.filesDir, "schedule")

    suspend fun current(): File? = withContext(Dispatchers.IO) {
        dir.listFiles { file -> file.name.startsWith(PREFIX) }?.maxByOrNull { it.lastModified() }
    }

    /** Copies [uri] in and deletes the previous schedule, only once the copy has succeeded. */
    suspend fun import(uri: Uri): File = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val type = resolver.getType(uri)
        val extension = when {
            type == "application/pdf" -> "pdf"
            type?.startsWith("image/") == true ->
                MimeTypeMap.getSingleton().getExtensionFromMimeType(type) ?: "img"
            else -> throw IOException("Unsupported file type: $type")
        }

        dir.mkdirs()
        val temp = File(dir, "import.tmp")
        try {
            val input = resolver.openInputStream(uri) ?: throw IOException("Cannot read $uri")
            input.use { source -> temp.outputStream().use { source.copyTo(it) } }

            // A new name each time, so the screen sees a different file and re-renders.
            val target = File(dir, "$PREFIX${System.currentTimeMillis()}.$extension")
            if (!temp.renameTo(target)) throw IOException("Cannot save schedule")
            dir.listFiles()?.filter { it != target }?.forEach { it.delete() }
            target
        } finally {
            temp.delete()
        }
    }

    suspend fun remove() = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
        Unit
    }

    private companion object {
        const val PREFIX = "schedule-"
    }
}
