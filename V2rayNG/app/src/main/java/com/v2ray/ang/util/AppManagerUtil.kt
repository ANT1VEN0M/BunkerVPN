package com.v2ray.ang.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.v2ray.ang.dto.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppManagerUtil {
    /**
     * Load the list of network applications safely without Binder transaction overflows,
     * duplicate package names (e.g. from Android 15/16/17 Private Space or Dual Apps),
     * or crash on corrupted resource loading.
     *
     * @param context The context to use.
     * @return A list of unique AppInfo objects representing the applications.
     */
    suspend fun loadNetworkAppList(context: Context): ArrayList<AppInfo> =
        withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            val apps = ArrayList<AppInfo>()
            val seen = HashSet<String>()

            // 1. Try getInstalledPackages(0) - avoids fetching bulky permission arrays over IPC
            try {
                val packages = packageManager.getInstalledPackages(0)
                for (pkg in packages) {
                    val packageName = pkg.packageName ?: continue
                    if (packageName.isBlank() || !seen.add(packageName)) continue

                    val applicationInfo = pkg.applicationInfo ?: continue
                    val appName = try {
                        applicationInfo.loadLabel(packageManager).toString()
                    } catch (_: Throwable) {
                        applicationInfo.nonLocalizedLabel?.toString() ?: packageName
                    }
                    val isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    apps.add(AppInfo(appName, packageName, isSystemApp, 0))
                }
            } catch (e: Throwable) {
                LogUtil.w("AppManagerUtil", "getInstalledPackages failed, falling back to getInstalledApplications: ${e.message}")
                try {
                    val applications = packageManager.getInstalledApplications(0)
                    for (appInfo in applications) {
                        val packageName = appInfo.packageName ?: continue
                        if (packageName.isBlank() || !seen.add(packageName)) continue

                        val appName = try {
                            appInfo.loadLabel(packageManager).toString()
                        } catch (_: Throwable) {
                            appInfo.nonLocalizedLabel?.toString() ?: packageName
                        }
                        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                        apps.add(AppInfo(appName, packageName, isSystemApp, 0))
                    }
                } catch (e2: Throwable) {
                    LogUtil.e("AppManagerUtil", "Failed to load installed applications fallback: ${e2.message}")
                }
            }

            return@withContext apps
        }

    fun getLastUpdateTime(context: Context): Long =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
        } catch (_: Throwable) {
            0L
        }

}
