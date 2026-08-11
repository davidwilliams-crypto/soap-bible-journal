package com.soapjournal.app.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
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
import java.security.MessageDigest

data class AvailableUpdate(
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val apkAssetApiUrl: String?,
    val releaseNotes: String,
    val htmlUrl: String
)

sealed class UpdateCheckResult {
    data class Available(val update: AvailableUpdate) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

sealed class InstallPrepResult {
    data class Ready(val file: File) : InstallPrepResult()
    data class SignatureMismatch(
        val file: File,
        val update: AvailableUpdate,
        val message: String
    ) : InstallPrepResult()
    data class Error(val message: String) : InstallPrepResult()
}

/**
 * Checks GitHub Releases for a newer APK and installs it via the system package installer.
 *
 * Publish flow:
 * 1. Bump versionCode / versionName in app/build.gradle.kts
 * 2. Build a distribute-signed APK and create a GitHub Release tagged like v1.2.0
 * 3. Put `versionCode=N` in the release body
 * 4. Attach the `.apk` as a release asset
 */
class AppUpdateManager(
    private val context: Context,
    private val gson: Gson = Gson(),
    private val owner: String = BuildConfig.UPDATE_GITHUB_OWNER,
    private val repo: String = BuildConfig.UPDATE_GITHUB_REPO,
    private val tokenProvider: () -> String = { "" }
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

    fun latestReleasePageUrl(): String =
        "https://github.com/$owner/$repo/releases/latest"

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val token = tokenProvider().trim()
            val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
            val connection = openGithub(url, token, accept = "application/vnd.github+json")
            try {
                when (connection.responseCode) {
                    401, 403 -> {
                        return@withContext UpdateCheckResult.Error(
                            "GitHub auth failed. Add a read-only GitHub token in Settings."
                        )
                    }
                    404 -> {
                        return@withContext UpdateCheckResult.Error(
                            "No GitHub release found (404)."
                        )
                    }
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
                        (!it.browserDownloadUrl.isNullOrBlank() || it.url != null)
                } ?: return@withContext UpdateCheckResult.Error(
                    "Latest GitHub release has no APK asset attached"
                )

                val remoteCode = parseVersionCode(release.body, apk.name, release.tagName)
                    ?: return@withContext UpdateCheckResult.Error(
                        "Add versionCode=N to the release body (e.g. versionCode=3)"
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
                        apkUrl = apk.browserDownloadUrl.orEmpty(),
                        apkAssetApiUrl = apk.url,
                        releaseNotes = release.body.orEmpty().trim(),
                        htmlUrl = release.htmlUrl.orEmpty().ifBlank { latestReleasePageUrl() }
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
        val token = tokenProvider().trim()
        val dir = File(context.cacheDir, "updates").also { it.mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val outFile = File(dir, "soap-journal-update-${update.versionCode}.apk")

        val downloadUrl = when {
            token.isNotBlank() && !update.apkAssetApiUrl.isNullOrBlank() -> update.apkAssetApiUrl
            update.apkUrl.isNotBlank() -> update.apkUrl
            !update.apkAssetApiUrl.isNullOrBlank() -> update.apkAssetApiUrl
            else -> error("No APK download URL on the release")
        }

        val connection = openGithub(
            url = URL(downloadUrl),
            token = token,
            accept = "application/octet-stream"
        ).apply {
            instanceFollowRedirects = true
            readTimeout = 120_000
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
            if (outFile.length() < 1_000_000L) {
                error("Downloaded file looks too small to be an APK (${outFile.length()} bytes).")
            }
            outFile
        } finally {
            connection.disconnect()
        }
    }

    suspend fun prepareInstall(
        update: AvailableUpdate,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): InstallPrepResult = withContext(Dispatchers.IO) {
        runCatching {
            val file = downloadApk(update, onProgress)
            if (!signaturesCompatible(file)) {
                InstallPrepResult.SignatureMismatch(
                    file = file,
                    update = update,
                    message = "Android can’t upgrade this install because the signing key changed. " +
                        "Back up to Google Drive, uninstall SOAP Journal, then install the APK from the release page."
                )
            } else {
                InstallPrepResult.Ready(file)
            }
        }.getOrElse {
            InstallPrepResult.Error(it.message ?: "Download failed")
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

    fun openInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun installApk(file: File) {
        // Prefer PackageInstaller sessions — more reliable on Samsung / Android 13+.
        runCatching { installWithPackageInstaller(file) }
            .recoverCatching { installWithViewIntent(file) }
            .getOrThrow()
    }

    private fun installWithPackageInstaller(file: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        if (Build.VERSION.SDK_INT >= 31) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            file.inputStream().use { input ->
                session.openWrite("soap-update.apk", 0, file.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            val callback = Intent(context, UpdateInstallReceiver::class.java).apply {
                action = UpdateInstallReceiver.ACTION_INSTALL_COMPLETE
            }
            val pending = PendingIntent.getBroadcast(
                context,
                sessionId,
                callback,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            session.commit(pending.intentSender)
        }
    }

    private fun installWithViewIntent(file: File) {
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
        // Explicitly grant to common package-installer packages (Samsung/Pixel/etc.).
        listOf(
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.samsung.android.packageinstaller"
        ).forEach { pkg ->
            runCatching {
                context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(intent)
    }

    fun signaturesCompatible(apkFile: File): Boolean {
        val installed = installedSignatureDigests()
        if (installed.isEmpty()) return true
        val incoming = archiveSignatureDigests(apkFile)
        if (incoming.isEmpty()) return true
        return installed.intersect(incoming).isNotEmpty()
    }

    private fun installedSignatureDigests(): Set<String> {
        return runCatching {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= 28) {
                val info = pm.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = info.signingInfo ?: return emptySet()
                val sigs = if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
                sigs.map { sha256(it.toByteArray()) }.toSet()
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                info.signatures.orEmpty().map { sha256(it.toByteArray()) }.toSet()
            }
        }.getOrDefault(emptySet())
    }

    private fun archiveSignatureDigests(apkFile: File): Set<String> {
        return runCatching {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= 28) {
                val info = pm.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.GET_SIGNING_CERTIFICATES
                ) ?: return emptySet()
                val signingInfo = info.signingInfo ?: return emptySet()
                val sigs = if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
                sigs.map { sha256(it.toByteArray()) }.toSet()
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.GET_SIGNATURES
                ) ?: return emptySet()
                @Suppress("DEPRECATION")
                info.signatures.orEmpty().map { sha256(it.toByteArray()) }.toSet()
            }
        }.getOrDefault(emptySet())
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun openGithub(url: URL, token: String, accept: String): HttpURLConnection {
        return (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("Accept", accept)
            setRequestProperty("User-Agent", "SOAP-Bible-Journal/${BuildConfig.VERSION_NAME}")
            if (token.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }
        }
    }

    companion object {
        private val versionCodeLine = Regex("""(?im)^\s*versionCode\s*[:=]\s*(\d+)\s*$""")

        fun parseVersionCode(body: String?, assetName: String?, tagName: String?): Long? {
            versionCodeLine.find(body.orEmpty())?.groupValues?.getOrNull(1)?.toLongOrNull()
                ?.let { return it }

            Regex("""(?i)SOAPBibleJournal-v(\d+)""")
                .find(assetName.orEmpty())
                ?.groupValues?.getOrNull(1)?.toLongOrNull()
                ?.let { return it }

            val fromName = Regex("""(?i)versionCode[_-]?(\d+)""")
                .find(assetName.orEmpty())
                ?.groupValues?.getOrNull(1)?.toLongOrNull()
            if (fromName != null) return fromName

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
        @SerializedName("url") val url: String?,
        @SerializedName("browser_download_url") val browserDownloadUrl: String?
    )
}
