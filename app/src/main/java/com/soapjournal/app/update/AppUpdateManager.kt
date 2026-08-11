package com.soapjournal.app.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.soapjournal.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AvailableUpdate(
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val releaseNotes: String,
    val htmlUrl: String
)

sealed class UpdateCheckResult {
    data class Available(val update: AvailableUpdate) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * Checks GitHub Releases for a newer APK and installs it via the system package installer.
 *
 * Publish flow:
 * 1. Bump versionCode / versionName in app/build.gradle.kts
 * 2. Build an APK and create a GitHub Release tagged like v1.1.0
 * 3. Put `versionCode=2` in the release body
 * 4. Attach the `.apk` as a release asset
 */
class AppUpdateManager(
    private val context: Context,
    private val gson: Gson = Gson(),
    private val owner: String = BuildConfig.UPDATE_GITHUB_OWNER,
    private val repo: String = BuildConfig.UPDATE_GITHUB_REPO
) {
    fun currentVersionName(): String = runCatching {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        info.versionName ?: BuildConfig.VERSION_NAME
    }.getOrDefault(BuildConfig.VERSION_NAME)

    fun currentVersionCode(): Long = runCatching {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }.getOrDefault(BuildConfig.VERSION_CODE.toLong())

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 12_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "SOAP-Bible-Journal/${BuildConfig.VERSION_NAME}")
            }
            try {
                when (connection.responseCode) {
                    404 -> return@withContext UpdateCheckResult.UpToDate
                    !in 200..299 -> {
                        return@withContext UpdateCheckResult.Error(
                            "GitHub returned HTTP ${connection.responseCode}"
                        )
                    }
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val release = gson.fromJson(body, GithubRelease::class.java)
                    ?: return@withContext UpdateCheckResult.Error("Could not parse release info")
                if (release.draft == true || release.prerelease == true) {
                    return@withContext UpdateCheckResult.UpToDate
                }
                val apk = release.assets.orEmpty().firstOrNull {
                    it.name?.endsWith(".apk", ignoreCase = true) == true &&
                        !it.browserDownloadUrl.isNullOrBlank()
                } ?: return@withContext UpdateCheckResult.Error(
                    "Latest GitHub release has no APK asset attached"
                )

                val remoteCode = parseVersionCode(release.body, apk.name, release.tagName)
                    ?: return@withContext UpdateCheckResult.Error(
                        "Add versionCode=N to the release body (e.g. versionCode=2)"
                    )
                val current = currentVersionCode()
                if (remoteCode <= current) {
                    return@withContext UpdateCheckResult.UpToDate
                }

                UpdateCheckResult.Available(
                    AvailableUpdate(
                        versionName = release.tagName?.removePrefix("v").orEmpty()
                            .ifBlank { release.name.orEmpty().ifBlank { remoteCode.toString() } },
                        versionCode = remoteCode.toInt(),
                        apkUrl = apk.browserDownloadUrl!!,
                        releaseNotes = release.body.orEmpty().trim(),
                        htmlUrl = release.htmlUrl.orEmpty()
                    )
                )
            } finally {
                connection.disconnect()
            }
        } catch (t: Throwable) {
            UpdateCheckResult.Error(t.message ?: "Update check failed")
        }
    }

    suspend fun downloadApk(
        update: AvailableUpdate,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").also { it.mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val outFile = File(dir, "soap-journal-update-${update.versionCode}.apk")
        val connection = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("User-Agent", "SOAP-Bible-Journal/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("Download failed (HTTP ${connection.responseCode})")
            }
            val total = connection.contentLengthLong.coerceAtLeast(-1L)
            connection.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                    output.flush()
                }
            }
            outFile
        } finally {
            connection.disconnect()
        }
    }

    fun canInstallPackages(): Boolean =
        if (Build.VERSION.SDK_INT >= 26) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun permissionIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    companion object {
        private val versionCodeLine = Regex("""(?im)^\s*versionCode\s*[:=]\s*(\d+)\s*$""")

        fun parseVersionCode(body: String?, assetName: String?, tagName: String?): Long? {
            versionCodeLine.find(body.orEmpty())?.groupValues?.getOrNull(1)?.toLongOrNull()
                ?.let { return it }

            // Prefer explicit "-v2" / "versionCode_2" patterns in the APK filename.
            val fromName = Regex("""(?i)(?:versionCode[_-]?|v)(\d+)""").find(assetName.orEmpty())
                ?.groupValues?.getOrNull(1)?.toLongOrNull()
            if (fromName != null) return fromName

            // Last resort: numeric tag like v2
            val tag = tagName?.removePrefix("v").orEmpty()
            return tag.toLongOrNull()
        }
    }

    private data class GithubRelease(
        @SerializedName("tag_name") val tagName: String?,
        @SerializedName("name") val name: String?,
        @SerializedName("body") val body: String?,
        @SerializedName("html_url") val htmlUrl: String?,
        @SerializedName("draft") val draft: Boolean?,
        @SerializedName("prerelease") val prerelease: Boolean?,
        @SerializedName("assets") val assets: List<GithubAsset>?
    )

    private data class GithubAsset(
        @SerializedName("name") val name: String?,
        @SerializedName("browser_download_url") val browserDownloadUrl: String?
    )
}
