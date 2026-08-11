package com.github.im.group.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class AndroidAppUpdateService(private val context: Context) : AppUpdateService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(AndroidUpdateEngine.readState(context))
    override val state: StateFlow<AppUpdateUiState> = _state.asStateFlow()

    override fun checkForUpdate() {
        if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) return
        scope.launch {
            _state.value = _state.value.copy(phase = AppUpdatePhase.CHECKING, message = null)
            runCatching {
                val update = AppUpdateApi.checkAndroid(context.packageName, AndroidUpdateEngine.currentVersionCode(context))
                AndroidUpdateEngine.onChecked(context, update)
                if (update != null && AndroidUpdateEngine.autoDownloadOnWifi(context) && AndroidUpdateEngine.isOnWifi(context)) {
                    AndroidUpdateEngine.startDownload(context, update, wifiOnly = true)
                }
            }.onFailure { error -> AndroidUpdateEngine.setFailure(context, error.message ?: "Unable to check for updates") }
            _state.value = AndroidUpdateEngine.readState(context)
        }
    }

    override fun downloadOrInstall() {
        scope.launch {
            val update = _state.value.update ?: return@launch
            AndroidUpdateEngine.downloadOrInstall(context, update)
            _state.value = AndroidUpdateEngine.readState(context)
        }
    }

    override fun setAutoDownloadOnWifi(enabled: Boolean) {
        AndroidUpdateEngine.setAutoDownloadOnWifi(context, enabled)
        _state.value = AndroidUpdateEngine.readState(context)
    }

    fun refresh() { scope.launch { _state.value = AndroidUpdateEngine.refresh(context) } }
}

/** Android-only persistence, transport, and verification. Never trust a completed download without all checks. */
object AndroidUpdateEngine {
    private const val PREFS = "app_update"
    private const val ID = "download_id"
    private const val VERSION_CODE = "version_code"
    private const val VERSION_NAME = "version_name"
    private const val URL = "url"
    private const val SHA256 = "sha256"
    private const val SIZE = "size"
    private const val CHANGELOG = "changelog"
    private const val FORCE = "force"
    private const val MIN_SUPPORTED = "min_supported"
    private const val READY = "ready"
    private const val AUTO_WIFI = "auto_wifi"
    private const val MESSAGE = "message"
    private const val DIRECTORY = "Group/updates"

    fun readState(context: Context): AppUpdateUiState {
        val prefs = prefs(context)
        val update = storedManifest(context)
        val progress = downloadProgress(context)
        val phase = when {
            update == null -> AppUpdatePhase.IDLE
            prefs.getBoolean(READY, false) -> AppUpdatePhase.READY_TO_INSTALL
            progress?.first == DownloadManager.STATUS_RUNNING || progress?.first == DownloadManager.STATUS_PENDING || progress?.first == DownloadManager.STATUS_PAUSED -> AppUpdatePhase.DOWNLOADING
            else -> AppUpdatePhase.AVAILABLE
        }
        return AppUpdateUiState(
            currentVersion = currentVersionName(context),
            update = update, phase = phase,
            downloadedBytes = progress?.second ?: 0, totalBytes = progress?.third ?: update?.sizeBytes ?: 0,
            autoDownloadOnWifi = autoDownloadOnWifi(context), message = prefs.getString(MESSAGE, null)
        )
    }

    fun onChecked(context: Context, update: AppUpdateManifest?) {
        if (update == null) { prefs(context).edit().remove(MESSAGE).apply(); return }
        if (!isValid(context, update)) { setFailure(context, "The update release metadata is invalid"); return }
        val stored = storedManifest(context)
        if (stored?.versionCode != update.versionCode) clearDownload(context)
        prefs(context).edit()
            .putInt(VERSION_CODE, update.versionCode).putString(VERSION_NAME, update.versionName)
            .putString(URL, update.apkUrl).putString(SHA256, update.sha256).putLong(SIZE, update.sizeBytes)
            .putString(CHANGELOG, update.changelog).putBoolean(FORCE, update.forceUpdate)
            .putInt(MIN_SUPPORTED, update.minSupportedVersionCode).remove(MESSAGE).apply()
    }

    fun startDownload(context: Context, update: AppUpdateManifest, wifiOnly: Boolean = false) {
        if (isReady(context)) return
        val manager = context.getSystemService(DownloadManager::class.java)
        clearDownload(context, manager)
        val file = updateFile(context, update)
        file.parentFile?.mkdirs(); file.delete()
        val request = DownloadManager.Request(Uri.parse(update.apkUrl))
            .setTitle("Group ${update.versionName}").setDescription("Downloading app update")
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(!wifiOnly).setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$DIRECTORY/${file.name}")
        prefs(context).edit().putLong(ID, manager.enqueue(request)).putBoolean(READY, false).remove(MESSAGE).apply()
    }

    fun downloadOrInstall(context: Context, update: AppUpdateManifest) {
        if (!isReady(context)) { startDownload(context, update); return }
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        val file = updateFile(context, update)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        context.startActivity(Intent(Intent.ACTION_INSTALL_PACKAGE).setData(uri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }

    suspend fun finalizeDownload(context: Context, id: Long): Boolean = withContext(Dispatchers.IO) {
        if (prefs(context).getLong(ID, -1) != id) return@withContext false
        val update = storedManifest(context) ?: return@withContext false
        val file = updateFile(context, update)
        val successful = context.getSystemService(DownloadManager::class.java).query(DownloadManager.Query().setFilterById(id))?.use {
            it.moveToFirst() && it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
        } ?: false
        if (!successful || !file.isFile || file.length() != update.sizeBytes || !matchesSha256(file, update.sha256) || !matchesInstalledApp(context, file)) {
            clearDownload(context); setFailure(context, "Downloaded APK verification failed"); return@withContext false
        }
        prefs(context).edit().putBoolean(READY, true).remove(MESSAGE).apply(); true
    }

    suspend fun refresh(context: Context): AppUpdateUiState {
        val id = prefs(context).getLong(ID, -1)
        if (id >= 0 && !isReady(context)) finalizeDownload(context, id)
        return readState(context)
    }

    fun setFailure(context: Context, message: String) { prefs(context).edit().putString(MESSAGE, message).apply() }
    fun autoDownloadOnWifi(context: Context) = prefs(context).getBoolean(AUTO_WIFI, true)
    fun setAutoDownloadOnWifi(context: Context, enabled: Boolean) { prefs(context).edit().putBoolean(AUTO_WIFI, enabled).apply() }
    fun isOnWifi(context: Context): Boolean = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager)
        .getNetworkCapabilities((context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager).activeNetwork)
        ?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true

    private fun isReady(context: Context) = prefs(context).getBoolean(READY, false)
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun storedManifest(context: Context): AppUpdateManifest? {
        val prefs = prefs(context)
        val code = prefs.getInt(VERSION_CODE, 0); val url = prefs.getString(URL, null); val hash = prefs.getString(SHA256, null)
        if (code <= currentVersionCode(context) || url.isNullOrBlank() || hash.isNullOrBlank()) return null
        return AppUpdateManifest(code, prefs.getString(VERSION_NAME, code.toString()).orEmpty(), url, hash, prefs.getLong(SIZE, 0),
            prefs.getString(CHANGELOG, "").orEmpty(), prefs.getBoolean(FORCE, false), prefs.getInt(MIN_SUPPORTED, 0))
    }
    private fun isValid(context: Context, update: AppUpdateManifest) = update.versionCode > currentVersionCode(context) && update.apkUrl.startsWith("https://") && update.sha256.matches(Regex("^[a-f0-9]{64}$")) && update.sizeBytes > 0
    private fun updateFile(context: Context, update: AppUpdateManifest) = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "$DIRECTORY/group-${update.versionCode}.apk")
    private fun downloadProgress(context: Context): Triple<Int, Long, Long>? {
        val id = prefs(context).getLong(ID, -1); if (id < 0) return null
        return context.getSystemService(DownloadManager::class.java).query(DownloadManager.Query().setFilterById(id))?.use {
            if (!it.moveToFirst()) null else Triple(it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)), it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)), it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)))
        }
    }
    private fun matchesSha256(file: File, expected: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256"); file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE); while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }; return digest.digest().joinToString("") { "%02x".format(it) } == expected
    }
    private fun matchesInstalledApp(context: Context, file: File): Boolean = try {
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags) ?: return false
        val archiveVersion = PackageInfoCompat.getLongVersionCode(archive)
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        val signersMatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archive.signingInfo?.apkContentsSigners.orEmpty().any { archiveSigner ->
                installed.signingInfo?.apkContentsSigners.orEmpty().any { it.toByteArray().contentEquals(archiveSigner.toByteArray()) }
            }
        } else {
            archive.signatures.orEmpty().any { archiveSigner -> installed.signatures.orEmpty().any { it.toByteArray().contentEquals(archiveSigner.toByteArray()) } }
        }
        archive.packageName == context.packageName && archiveVersion > currentVersionCode(context) && signersMatch
    } catch (_: Exception) { false }
    private fun clearDownload(context: Context, manager: DownloadManager = context.getSystemService(DownloadManager::class.java)) {
        prefs(context).getLong(ID, -1).takeIf { it >= 0 }?.let { manager.remove(it) }
        storedManifest(context)?.let { updateFile(context, it).delete() }
        prefs(context).edit().remove(ID).putBoolean(READY, false).apply()
    }

    fun currentVersionCode(context: Context): Int = PackageInfoCompat.getLongVersionCode(
        context.packageManager.getPackageInfo(context.packageName, 0)
    ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    private fun currentVersionName(context: Context): String = context.packageManager
        .getPackageInfo(context.packageName, 0).versionName ?: ""

}
