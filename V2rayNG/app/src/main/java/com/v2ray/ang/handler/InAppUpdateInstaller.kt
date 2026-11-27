package com.v2ray.ang.handler

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.colorFabActive
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

enum class UpdateDownloadState {
    IDLE,
    DOWNLOADING,
    READY,
    ERROR
}

object InAppUpdateInstaller {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
        }
    }

    fun installApk(context: Context, file: File): Boolean {
        return try {
            if (!file.exists() || file.length() == 0L) {
                LogUtil.e(AppConfig.TAG, "InAppUpdateInstaller: APK file does not exist or empty")
                return false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !canInstallPackages(context)) {
                openInstallPermissionSettings(context)
                return false
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.cache",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val resInfoList = context.packageManager.queryIntentActivities(installIntent, 0)
            for (resolveInfo in resInfoList) {
                context.grantUriPermission(resolveInfo.activityInfo.packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "InAppUpdateInstaller: Failed to launch PackageInstaller", e)
            false
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (percent: Int, downloadedMb: Float, totalMb: Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        cacheDir.listFiles { f -> f.name.startsWith("bunker_update") }?.forEach { it.delete() }

        val targetFile = File(cacheDir, "bunker_update_${System.currentTimeMillis()}.apk")

        // Build list of high-speed anti-blocking mirrors for Russian users
        val urlsToTry = mutableListOf<String>()

        val rawGithubUrl = when {
            downloadUrl.contains("ghfast.top/") -> downloadUrl.substringAfter("ghfast.top/")
            downloadUrl.contains("ghproxy.net/") -> downloadUrl.substringAfter("ghproxy.net/")
            downloadUrl.contains("mirror.ghproxy.com/") -> downloadUrl.substringAfter("mirror.ghproxy.com/")
            else -> downloadUrl
        }

        if (rawGithubUrl.contains("github.com/")) {
            urlsToTry.add("https://ghfast.top/$rawGithubUrl")
            urlsToTry.add("https://ghproxy.net/$rawGithubUrl")
            urlsToTry.add("https://mirror.ghproxy.com/$rawGithubUrl")
        }
        if (!urlsToTry.contains(downloadUrl)) {
            urlsToTry.add(downloadUrl)
        }
        if (!urlsToTry.contains(rawGithubUrl)) {
            urlsToTry.add(rawGithubUrl)
        }

        for (candidateUrl in urlsToTry) {
            if (!isActive) break
            LogUtil.i(AppConfig.TAG, "InAppUpdateInstaller: Attempting download via: $candidateUrl")
            val downloaded = tryDownloadCandidate(candidateUrl, targetFile, onProgress)
            if (downloaded) {
                return@withContext targetFile
            }
            targetFile.delete()
        }

        return@withContext null
    }

    private suspend fun tryDownloadCandidate(
        url: String,
        targetFile: File,
        onProgress: (percent: Int, downloadedMb: Float, totalMb: Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "BunkerVPN/${BuildConfig.VERSION_NAME} (Android; Linux)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                LogUtil.e(AppConfig.TAG, "Download attempt failed: HTTP ${response.code} from $url")
                return@withContext false
            }

            val contentType = response.header("Content-Type")
            if (contentType != null && (contentType.contains("text/html") || contentType.contains("application/json"))) {
                LogUtil.e(AppConfig.TAG, "Download URL returned non-APK content-type: $contentType ($url)")
                return@withContext false
            }

            val body = response.body ?: return@withContext false
            val contentLength = body.contentLength()

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(8192)
            var totalRead = 0L
            var lastReportTime = 0L
            var completedCleanly = false

            inputStream.use { input ->
                outputStream.use { output ->
                    while (isActive) {
                        val read = input.read(buffer)
                        if (read == -1) {
                            completedCleanly = true
                            break
                        }
                        output.write(buffer, 0, read)
                        totalRead += read

                        val now = System.currentTimeMillis()
                        if (now - lastReportTime > 100 || (contentLength > 0 && totalRead == contentLength)) {
                            lastReportTime = now
                            val percent = if (contentLength > 0) ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100) else 0
                            val downloadedMb = totalRead / (1024f * 1024f)
                            val totalMb = if (contentLength > 0) contentLength / (1024f * 1024f) else downloadedMb
                            withContext(Dispatchers.Main) {
                                onProgress(percent, downloadedMb, totalMb)
                            }
                        }
                    }
                }
            }

            completedCleanly && targetFile.exists() && targetFile.length() > 5 * 1024 * 1024
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Error downloading from $url: ${e.message}")
            false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppUpdateDialog(
    latestVersion: String,
    releaseNotes: String? = null,
    downloadUrl: String,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf(UpdateDownloadState.IDLE) }
    var progressPercent by remember { mutableFloatStateOf(0f) }
    var downloadedMbText by remember { mutableStateOf("0.0 MB") }
    var totalMbText by remember { mutableStateOf("0.0 MB") }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    val startDownload: () -> Unit = {
        state = UpdateDownloadState.DOWNLOADING
        progressPercent = 0f
        errorMessage = ""
        downloadJob = scope.launch {
            val file = InAppUpdateInstaller.downloadApk(
                context = context,
                downloadUrl = downloadUrl,
                onProgress = { percent, downloadedMb, totalMb ->
                    progressPercent = percent / 100f
                    downloadedMbText = String.format("%.1f MB", downloadedMb)
                    totalMbText = String.format("%.1f MB", totalMb)
                }
            )

            if (file != null && file.exists()) {
                downloadedFile = file
                state = UpdateDownloadState.READY
                InAppUpdateInstaller.installApk(context, file)
            } else {
                state = UpdateDownloadState.ERROR
                errorMessage = "Не удалось загрузить файл обновления"
            }
        }
    }

    BasicAlertDialog(
        onDismissRequest = {
            if (state == UpdateDownloadState.DOWNLOADING) {
                downloadJob?.cancel()
            }
            onDismissRequest()
        }
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, colorFabActive.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cyber Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(colorFabActive.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = when (state) {
                            UpdateDownloadState.READY -> "✅"
                            UpdateDownloadState.ERROR -> "⚠️"
                            else -> "🚀"
                        },
                        fontSize = 26.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.bunker_update_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorFabActive,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = stringResource(R.string.bunker_update_dialog_version, latestVersion),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                when (state) {
                    UpdateDownloadState.IDLE -> {
                        if (!releaseNotes.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                            ) {
                                Text(
                                    text = releaseNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Install Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colorFabActive,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { startDownload() }
                        ) {
                            Text(
                                text = stringResource(R.string.bunker_update_dialog_btn_now),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.bunker_update_dialog_btn_later),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { onDismissRequest() }
                                .padding(8.dp)
                        )
                    }

                    UpdateDownloadState.DOWNLOADING -> {
                        Text(
                            text = stringResource(R.string.bunker_update_downloading),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = colorFabActive,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(progressPercent * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorFabActive
                            )
                            Text(
                                text = "$downloadedMbText / $totalMbText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable {
                                downloadJob?.cancel()
                                state = UpdateDownloadState.IDLE
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }

                    UpdateDownloadState.READY -> {
                        val hasPermission = InAppUpdateInstaller.canInstallPackages(context)
                        Text(
                            text = if (hasPermission) stringResource(R.string.bunker_update_download_success) else stringResource(R.string.bunker_update_permission_required),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (hasPermission) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hasPermission) {
                                stringResource(R.string.bunker_update_launching_installer)
                            } else {
                                stringResource(R.string.bunker_update_grant_permission_hint)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colorFabActive,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (hasPermission) {
                                        downloadedFile?.let { InAppUpdateInstaller.installApk(context, it) }
                                    } else {
                                        InAppUpdateInstaller.openInstallPermissionSettings(context)
                                    }
                                }
                        ) {
                            Text(
                                text = if (hasPermission) stringResource(R.string.bunker_update_btn_install) else stringResource(R.string.bunker_update_btn_grant_permission),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.action_cancel),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { onDismissRequest() }
                                .padding(8.dp)
                        )
                    }

                    UpdateDownloadState.ERROR -> {
                        Text(
                            text = errorMessage.ifBlank { "Произошла ошибка при загрузке" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colorFabActive,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { startDownload() }
                            ) {
                                Text(
                                    text = "Повторить",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onDismissRequest()
                                        Utils.openUri(context, downloadUrl)
                                    }
                            ) {
                                Text(
                                    text = "В браузере",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
