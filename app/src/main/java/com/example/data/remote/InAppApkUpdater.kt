package com.example.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface ApkDownloadState {
    data object Idle : ApkDownloadState
    data class Downloading(
        val tagName: String,
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : ApkDownloadState
    data class ReadyToInstall(
        val tagName: String,
        val apkFilePath: String
    ) : ApkDownloadState
    data class NeedsInstallPermission(
        val tagName: String,
        val apkFilePath: String
    ) : ApkDownloadState
    data class Error(val message: String) : ApkDownloadState
}

/**
 * Downloads a signed GitHub Release APK into private app cache storage and invokes
 * the Android OS Package Installer directly via FileProvider (no Chrome redirect).
 */
object InAppApkUpdater {

    private const val MAX_REDIRECTS = 6
    private const val BUFFER_SIZE = 16 * 1024

    fun canRequestPackageInstalls(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
    }

    fun openInstallUnknownAppsSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }
    }

    fun launchSystemPackageInstaller(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() <= 0L) return false
        return runCatching {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            true
        }.getOrDefault(false)
    }

    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        tagName: String,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        // Clean up any stale APK files to keep cache tidy
        updatesDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk") || file.name.endsWith(".part")) {
                file.delete()
            }
        }
        val safeTag = tagName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val tempFile = File(updatesDir, "fittrack-$safeTag.apk.part")
        val finalFile = File(updatesDir, "fittrack-$safeTag.apk")

        var connection: HttpURLConnection? = null
        try {
            connection = openFollowingRedirects(apkUrl)
            val code = connection.responseCode
            if (code !in 200..299) {
                return@withContext Result.failure(IOException("Download failed (HTTP $code)."))
            }

            val totalBytes = connection.contentLengthLong.coerceAtLeast(0L)
            var downloadedBytes = 0L
            var lastReportedPct = -1

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val pct = if (totalBytes > 0L) {
                            ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            -1
                        }
                        if (pct != lastReportedPct || (pct == -1 && downloadedBytes % (256 * 1024) < BUFFER_SIZE)) {
                            lastReportedPct = pct
                            onProgress(pct.coerceAtLeast(0), downloadedBytes, totalBytes)
                        }
                    }
                    output.flush()
                }
            }

            if (downloadedBytes <= 0L) {
                tempFile.delete()
                return@withContext Result.failure(IOException("Downloaded APK file was empty."))
            }

            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }
            onProgress(100, downloadedBytes, totalBytes.takeIf { it > 0L } ?: downloadedBytes)
            Result.success(finalFile)
        } catch (e: CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun openFollowingRedirects(initialUrl: String): HttpURLConnection {
        var currentUrl = initialUrl
        repeat(MAX_REDIRECTS) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/octet-stream, */*")
                setRequestProperty("User-Agent", "FitTrack-Android-Updater")
            }
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == HttpURLConnection.HTTP_SEE_OTHER ||
                code == 307 ||
                code == 308
            ) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IOException("Redirect missing Location header.")
                }
                currentUrl = URL(URL(currentUrl), location).toString()
            } else {
                return conn
            }
        }
        throw IOException("Too many redirects while downloading update.")
    }

    fun formatBytesMb(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format("%.1f MB", mb)
    }
}
