package com.watzem.nativeapp.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.watzem.nativeapp.data.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class AppUpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "AppUpdateManager"
        const val UPDATE_JSON_URL = "https://raw.githubusercontent.com/kingpin222/watzem/main/version.json"

        @Volatile
        private var instance: AppUpdateManager? = null

        fun getInstance(context: Context): AppUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: AppUpdateManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo = _updateInfo.asStateFlow()

    private val _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable = _isUpdateAvailable.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking = _isChecking.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _downloadBytesText = MutableStateFlow("")
    val downloadBytesText = _downloadBytesText.asStateFlow()

    private val _downloadStatusText = MutableStateFlow("")
    val downloadStatusText = _downloadStatusText.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    var isDismissedForSession: Boolean = false
        private set

    val currentVersionName: String by lazy {
        try {
            val pInfo = getPackageInfo()
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    val currentVersionCode: Long by lazy {
        try {
            val pInfo = getPackageInfo()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    private fun getPackageInfo() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }

    suspend fun checkForUpdate(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (_isChecking.value) return@withContext _isUpdateAvailable.value
        _isChecking.value = true
        _error.value = null

        try {
            val request = Request.Builder()
                .url("$UPDATE_JSON_URL?t=${System.currentTimeMillis()}")
                .header("User-Agent", "WATZEM-Native-Android")
                .header("Cache-Control", "no-cache")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch update info: HTTP ${response.code}")
            }

            val bodyString = response.body?.string() ?: throw Exception("Empty update response")
            val remoteInfo = gson.fromJson(bodyString, AppUpdateInfo::class.java)
            _updateInfo.value = remoteInfo

            val remoteVersion = remoteInfo.displayVersion
            val remoteCode = remoteInfo.versionCode.toLong()

            val hasCodeUpdate = remoteCode > currentVersionCode
            val hasSemverUpdate = compareSemver(remoteVersion, currentVersionName) > 0
            val isNewer = hasCodeUpdate || hasSemverUpdate

            Log.d(TAG, "Current: v$currentVersionName ($currentVersionCode) vs Remote: v$remoteVersion ($remoteCode) -> isNewer: $isNewer")
            _isUpdateAvailable.value = isNewer
            return@withContext isNewer
        } catch (e: Exception) {
            Log.w(TAG, "Check for update failed: ${e.message}")
            _error.value = e.message ?: "Failed to check for updates"
            return@withContext false
        } finally {
            _isChecking.value = false
        }
    }

    suspend fun startUpdateDownload(): Unit = withContext(Dispatchers.IO) {
        val info = _updateInfo.value ?: return@withContext
        val rawUrl = info.downloadUrl
        if (rawUrl.isNullOrBlank()) {
            _error.value = "Invalid download URL"
            return@withContext
        }

        _isDownloading.value = true
        _downloadProgress.value = 0
        _downloadBytesText.value = ""
        _downloadStatusText.value = "Connecting to server..."
        _error.value = null

        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            val request = Request.Builder()
                .url(rawUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; WATZEM App)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Server returned HTTP ${response.code}")
            }

            val body = response.body ?: throw Exception("Empty download body")
            val totalBytes = body.contentLength()

            val updatesDir = File(context.getExternalFilesDir(null) ?: context.cacheDir, "updates")
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
            }

            val apkFile = File(updatesDir, "app-update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            inputStream = body.byteStream()
            outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(16384)
            var bytesRead: Int
            var totalDownloaded: Long = 0
            var lastEmitTime = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalDownloaded += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastEmitTime > 120 || (totalBytes > 0 && totalDownloaded == totalBytes)) {
                    lastEmitTime = now
                    val progress = if (totalBytes > 0) ((totalDownloaded * 100) / totalBytes).toInt() else 0
                    _downloadProgress.value = progress.coerceIn(0, 100)

                    val downloadedMB = String.format("%.1f", totalDownloaded / (1024f * 1024f))
                    val totalMB = if (totalBytes > 0) {
                        String.format("%.1f", totalBytes / (1024f * 1024f))
                    } else {
                        info.fileSize ?: "4.0 MB"
                    }

                    _downloadBytesText.value = "$downloadedMB MB / $totalMB"
                    _downloadStatusText.value = "Downloading update... $progress%"
                }
            }

            outputStream.flush()
            _downloadProgress.value = 100
            _downloadStatusText.value = "Download complete! Opening installer..."

            // Trigger install
            withContext(Dispatchers.Main) {
                installApk(apkFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _error.value = e.message ?: "Download failed"
            _downloadStatusText.value = "Download failed: ${e.message}"
        } finally {
            try {
                outputStream?.close()
                inputStream?.close()
            } catch (ignored: Exception) {}
            _isDownloading.value = false
        }
    }

    fun installApk(apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Log.e(TAG, "APK file does not exist: ${apkFile.absolutePath}")
                _error.value = "Installer package not found"
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(permissionIntent)
                }
            }

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
        } catch (e: Exception) {
            Log.e(TAG, "Error launching package installer", e)
            _error.value = "Failed to launch installer: ${e.message}"
        }
    }

    fun dismissForSession() {
        isDismissedForSession = true
    }

    private fun compareSemver(v1: String, v2: String): Int {
        val parts1 = v1.removePrefix("v").removePrefix("V").split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.removePrefix("v").removePrefix("V").split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }
            if (num1 > num2) return 1
            if (num1 < num2) return -1
        }
        return 0
    }
}
