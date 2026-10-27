package com.david.notebook.data.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** The newest published release on GitHub, as far as the app cares. */
data class Release(
    val version: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String,
    val apkSize: Long,
    /** Hex SHA-256 of the APK as GitHub reports it, when it does. */
    val sha256: String?,
)

/**
 * Checks GitHub Releases for a newer version and downloads its APK.
 *
 * Installing is safe to offer: Android only installs an update signed with the same
 * key as the installed app, so an APK from anywhere else is rejected by the system
 * installer. On top of that the download is HTTPS-only and its SHA-256 is checked
 * against the digest GitHub publishes for the asset.
 */
class UpdateRepository(private val context: Context) {

    val currentVersion: String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"

    /** The latest non-draft, non-prerelease release. */
    suspend fun latestRelease(): Release = withContext(Dispatchers.IO) {
        val connection = open(LATEST_RELEASE_URL).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        val json = try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("GitHub answered ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }

        val assets = json.getJSONArray("assets")
        val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
            .firstOrNull { it.getString("name").endsWith(".apk", ignoreCase = true) }
            ?: throw IOException("The latest release has no APK")
        Release(
            version = json.getString("tag_name").removePrefix("v"),
            notes = json.optString("body"),
            pageUrl = json.getString("html_url"),
            apkUrl = apk.getString("browser_download_url"),
            apkSize = apk.optLong("size"),
            sha256 = apk.optString("digest").takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:"),
        )
    }

    fun isNewer(release: Release): Boolean = compareVersions(release.version, currentVersion) > 0

    /** Downloads the APK to the cache, reporting progress from 0 to 1. */
    suspend fun download(release: Release, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, UPDATES_DIR).apply { deleteRecursively(); mkdirs() }
        val part = File(dir, "Notebook-${release.version}.apk.part")
        val digest = MessageDigest.getInstance("SHA-256")

        val connection = open(release.apkUrl)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Download failed: ${connection.responseCode}")
            }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        ensureActive() // leaving the screen cancels the download
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        read += n
                        if (total > 0) onProgress((read.toFloat() / total).coerceAtMost(1f))
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (release.sha256 != null && !release.sha256.equals(actual, ignoreCase = true)) {
            part.delete()
            throw IOException("Checksum mismatch")
        }
        File(dir, "Notebook-${release.version}.apk").also { part.renameTo(it) }
    }

    /** Hands the APK to Android's package installer (which asks the user to confirm). */
    fun installIntent(apk: File): Intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk),
            "application/vnd.android.package-archive",
        )
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true // asset downloads redirect to GitHub's CDN
            setRequestProperty("User-Agent", "Notebook-Android/$currentVersion")
        }

    companion object {
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/sleepdeprived-dev/Notebook/releases/latest"
        const val UPDATES_DIR = "updates"

        /** Compares "1.10.0" vs "1.9.2" numerically; a "-beta" suffix is ignored. */
        fun compareVersions(a: String, b: String): Int {
            fun parts(v: String) = v.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            val pa = parts(a)
            val pb = parts(b)
            for (i in 0 until maxOf(pa.size, pb.size)) {
                val diff = pa.getOrElse(i) { 0 } - pb.getOrElse(i) { 0 }
                if (diff != 0) return diff
            }
            return 0
        }
    }
}
