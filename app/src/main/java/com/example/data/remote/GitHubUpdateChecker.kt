package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val tagName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val releasePageUrl: String,
    val apkDownloadUrl: String?,
    val publishedAt: String
) {
    /** Direct APK asset URL if attached to the GitHub Release; otherwise the release page URL. */
    val preferredDownloadUrl: String
        get() = apkDownloadUrl?.takeIf { it.isNotBlank() } ?: releasePageUrl
}

sealed interface AppUpdateStatus {
    data object Unconfigured : AppUpdateStatus
    data object Idle : AppUpdateStatus
    data object Checking : AppUpdateStatus
    data class UpToDate(val currentVersion: String) : AppUpdateStatus
    data class UpdateAvailable(val info: AppUpdateInfo) : AppUpdateStatus
    data class Error(val message: String) : AppUpdateStatus
}

/**
 * Deterministic, zero-permission GitHub Releases update checker.
 * Queries the public GitHub Releases API and opens the official release/APK URL
 * in the system browser when a newer semantic version is available.
 */
object GitHubUpdateChecker {

    private val REPO_PATTERN = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")

    /**
     * Accepts either "owner/repo" or a full GitHub URL (e.g., "https://github.com/owner/repo.git")
     * and returns a clean "owner/repo" slug, or null if invalid.
     */
    fun normalizeRepoInput(raw: String): String? {
        val trimmed = raw.trim()
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
            .removeSuffix("/")
            .removeSuffix(".git")
        val segments = trimmed.split("/").filter { it.isNotBlank() }
        if (segments.size < 2) return null
        val candidate = "${segments[0]}/${segments[1]}"
        return candidate.takeIf { REPO_PATTERN.matches(it) }
    }

    /**
     * Compares semantic version tags (e.g., "v1.2.0" vs "1.0" or "1.0.0").
     * Returns true strictly when [latestTag] represents a higher version than [currentVersion].
     */
    fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
        val latestParts = parseVersionParts(latestTag)
        val currentParts = parseVersionParts(currentVersion)
        if (latestParts.isEmpty() || currentParts.isEmpty()) return false

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun parseVersionParts(raw: String): List<Int> {
        val clean = raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
        if (clean.isBlank()) return emptyList()
        return clean.split(".").mapNotNull { it.trim().toIntOrNull() }
    }

    suspend fun checkLatestRelease(
        repoSlug: String,
        currentVersionName: String
    ): AppUpdateStatus = withContext(Dispatchers.IO) {
        val normalized = normalizeRepoInput(repoSlug)
            ?: return@withContext AppUpdateStatus.Unconfigured

        val apiUrl = "https://api.github.com/repos/$normalized/releases/latest"
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "FitTrack-Android/$currentVersionName")
            }

            val code = connection.responseCode
            when (code) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val tagName = json.optString("tag_name", "").trim()
                    val releaseName = json.optString("name", tagName).ifBlank { tagName }
                    val releaseNotes = json.optString("body", "").trim()
                    val htmlUrl = json.optString("html_url", "https://github.com/$normalized/releases")
                    val publishedAt = json.optString("published_at", "")

                    var apkUrl: String? = null
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.optJSONObject(i) ?: continue
                            val assetName = asset.optString("name", "")
                            val downloadUrl = asset.optString("browser_download_url", "")
                            if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                                apkUrl = downloadUrl
                                break
                            }
                        }
                    }

                    if (tagName.isNotBlank() && isNewerVersion(tagName, currentVersionName)) {
                        AppUpdateStatus.UpdateAvailable(
                            AppUpdateInfo(
                                tagName = tagName,
                                releaseTitle = releaseName,
                                releaseNotes = releaseNotes,
                                releasePageUrl = htmlUrl,
                                apkDownloadUrl = apkUrl,
                                publishedAt = publishedAt
                            )
                        )
                    } else {
                        AppUpdateStatus.UpToDate(currentVersionName)
                    }
                }

                HttpURLConnection.HTTP_NOT_FOUND -> {
                    AppUpdateStatus.Error("No published GitHub release found for $normalized yet.")
                }

                403 -> {
                    AppUpdateStatus.Error("GitHub API rate limit reached or repository is private.")
                }

                else -> {
                    AppUpdateStatus.Error("Update check failed (HTTP $code).")
                }
            }
        } catch (e: IOException) {
            AppUpdateStatus.Error("Offline or unreachable. Check your internet connection.")
        } catch (e: Exception) {
            AppUpdateStatus.Error("Could not parse release info: ${e.localizedMessage ?: "unknown error"}")
        } finally {
            connection?.disconnect()
        }
    }
}
