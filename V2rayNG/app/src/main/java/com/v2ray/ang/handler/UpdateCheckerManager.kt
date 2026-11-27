package com.v2ray.ang.handler

import android.os.Build
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.dto.CheckUpdateResult
import com.v2ray.ang.dto.GitHubRelease
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.extension.concatUrl
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BunkerVersionPayload(
    val version: String = "",
    val versionCode: Int = 0,
    val tagName: String = "",
    val downloadUrl: String = "",
    val releaseNotes: String = ""
)

object UpdateCheckerManager {
    private val VERSION_JSON_URLS = listOf(
        AppConfig.VERSION_INFO_URL,
        "https://cdn.jsdelivr.net/gh/ANT1VEN0M/BunkerVPN@main/version.json",
        "https://raw.githubusercontent.com/ANT1VEN0M/BunkerVPN/main/version.json"
    )

    suspend fun checkForUpdate(includePreRelease: Boolean = false): CheckUpdateResult = withContext(Dispatchers.IO) {
        val proxyUsername = SettingsManager.getSocksUsername()
        val proxyPassword = SettingsManager.getSocksPassword()
        val httpPort = SettingsManager.getHttpPort()

        // 1. Fast direct check via raw version.json through fast CDN mirrors (no GitHub API rate limits)
        for (versionUrl in VERSION_JSON_URLS) {
            runCatching {
                var rawJson = HttpUtil.getUrlContent(
                    UrlContentRequest(
                        url = versionUrl,
                        timeout = 3500,
                        userAgent = "BunkerVPN/${BuildConfig.VERSION_NAME}"
                    )
                )
                if (rawJson.isNullOrBlank() && httpPort != 0) {
                    rawJson = HttpUtil.getUrlContent(
                        UrlContentRequest(
                            url = versionUrl,
                            timeout = 3500,
                            httpPort = httpPort,
                            proxyUsername = proxyUsername,
                            proxyPassword = proxyPassword,
                            userAgent = "BunkerVPN/${BuildConfig.VERSION_NAME}"
                        )
                    )
                }
                if (!rawJson.isNullOrBlank()) {
                    val info = JsonUtil.fromJsonSafe(rawJson, BunkerVersionPayload::class.java)
                    if (info != null && compareVersions(info.version, BuildConfig.VERSION_NAME) > 0) {
                        LogUtil.i(AppConfig.TAG, "UpdateChecker: Found newer version via $versionUrl: ${info.version}")
                        return@withContext CheckUpdateResult(
                            hasUpdate = true,
                            latestVersion = info.tagName.ifBlank { "v${info.version}" },
                            releaseNotes = info.releaseNotes,
                            downloadUrl = info.downloadUrl
                        )
                    }
                }
            }
        }

        // 2. Fallback to GitHub Releases API with User-Agent
        val url = if (includePreRelease) {
            AppConfig.APP_API_URL
        } else {
            AppConfig.APP_API_URL.concatUrl("latest")
        }

        var response = HttpUtil.getUrlContent(
            UrlContentRequest(
                url = url,
                timeout = 5000,
                userAgent = "BunkerVPN/${BuildConfig.VERSION_NAME} (Android; Linux)"
            )
        )
        if (response.isNullOrEmpty() && httpPort != 0) {
            response = HttpUtil.getUrlContent(
                UrlContentRequest(
                    url = url,
                    timeout = 5000,
                    httpPort = httpPort,
                    proxyUsername = proxyUsername,
                    proxyPassword = proxyPassword,
                    userAgent = "BunkerVPN/${BuildConfig.VERSION_NAME} (Android; Linux)"
                )
            )
        }
        if (response.isNullOrEmpty()) {
            return@withContext CheckUpdateResult(hasUpdate = false)
        }

        val latestRelease = if (includePreRelease) {
            JsonUtil.fromJsonSafe(response, Array<GitHubRelease>::class.java)
                ?.firstOrNull()
        } else {
            JsonUtil.fromJsonSafe(response, GitHubRelease::class.java)
        }
        if (latestRelease == null) {
            return@withContext CheckUpdateResult(hasUpdate = false)
        }

        val latestVersion = latestRelease.tagName.removePrefix("v").substringBefore("-").trim()
        LogUtil.i(
            AppConfig.TAG,
            "Found new version: $latestVersion (current: ${BuildConfig.VERSION_NAME})"
        )

        return@withContext if (compareVersions(latestVersion, BuildConfig.VERSION_NAME) > 0) {
            val downloadUrl = getDownloadUrl(latestRelease, Build.SUPPORTED_ABIS[0])
            CheckUpdateResult(
                hasUpdate = true,
                latestVersion = latestRelease.tagName,
                releaseNotes = latestRelease.body,
                downloadUrl = downloadUrl,
                isPreRelease = latestRelease.prerelease
            )
        } else {
            CheckUpdateResult(hasUpdate = false)
        }
    }

    private fun compareVersions(version1: String, version2: String): Int {
        val clean1 = version1.removePrefix("v").substringBefore("-").trim()
        val clean2 = version2.removePrefix("v").substringBefore("-").trim()
        val v1 = clean1.split(".")
        val v2 = clean2.split(".")

        for (i in 0 until maxOf(v1.size, v2.size)) {
            val num1 = if (i < v1.size) v1[i].filter { it.isDigit() }.toIntOrNull() ?: 0 else 0
            val num2 = if (i < v2.size) v2[i].filter { it.isDigit() }.toIntOrNull() ?: 0 else 0
            if (num1 != num2) return num1 - num2
        }
        return 0
    }

    private fun getDownloadUrl(release: GitHubRelease, abi: String): String {
        val asset = release.assets.firstOrNull { it.name.contains(abi, ignoreCase = true) }
            ?: release.assets.firstOrNull { it.name.contains("arm64", ignoreCase = true) }
            ?: release.assets.firstOrNull { it.name.contains("universal", ignoreCase = true) }
            ?: release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }

        val rawUrl = asset?.browserDownloadUrl
            ?: release.htmlUrl.takeIf { it.isNotBlank() }
            ?: "https://github.com/ANT1VEN0M/BunkerVPN/releases/latest"

        // For Russian users, GitHub Release Assets (*.githubusercontent.com) are blocked by RKN/TSPU.
        // Wrap with fast CDN mirror by default so in-app update always succeeds without VPN.
        return if (rawUrl.startsWith("https://github.com/")) {
            "https://ghfast.top/$rawUrl"
        } else {
            rawUrl
        }
    }
}
