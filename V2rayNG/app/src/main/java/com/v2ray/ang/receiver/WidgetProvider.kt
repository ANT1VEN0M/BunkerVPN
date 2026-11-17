package com.v2ray.ang.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.widget.RemoteViews
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.extension.toSpeedString
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetProvider : AppWidgetProvider() {

    companion object {
        private var liveTickerJob: Job? = null

        fun startLiveTicker(context: Context) {
            if (liveTickerJob?.isActive == true) return
            val appContext = context.applicationContext

            liveTickerJob = CoroutineScope(Dispatchers.Default).launch {
                var lastRx = TrafficStats.getTotalRxBytes()
                var lastTx = TrafficStats.getTotalTxBytes()
                var lastTime = System.currentTimeMillis()

                while (isActive && CoreServiceManager.isRunning()) {
                    delay(1000L)
                    val now = System.currentTimeMillis()
                    val dt = (now - lastTime).coerceAtLeast(100L) / 1000.0
                    lastTime = now

                    val curRx = TrafficStats.getTotalRxBytes()
                    val curTx = TrafficStats.getTotalTxBytes()

                    var speedDown = 0L
                    var speedUp = 0L
                    if (curRx >= 0 && lastRx >= 0 && curRx >= lastRx) {
                        speedDown = ((curRx - lastRx) / dt).toLong().coerceAtLeast(0L)
                    }
                    if (curTx >= 0 && lastTx >= 0 && curTx >= lastTx) {
                        speedUp = ((curTx - lastTx) / dt).toLong().coerceAtLeast(0L)
                    }
                    lastRx = curRx
                    lastTx = curTx

                    val (coreDown, coreUp) = MmkvManager.decodeTrafficSpeed()
                    val finalDown = maxOf(speedDown, coreDown)
                    val finalUp = maxOf(speedUp, coreUp)
                    MmkvManager.encodeTrafficSpeed(finalDown, finalUp)

                    withContext(Dispatchers.Main) {
                        updateAllWidgets(appContext)
                    }
                }
            }
        }

        fun stopLiveTicker(context: Context) {
            liveTickerJob?.cancel()
            liveTickerJob = null
            MmkvManager.encodeTrafficSpeed(0L, 0L)
            updateAllWidgets(context)
        }

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, WidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds.isNotEmpty()) {
                    updateWidgetUi(context, appWidgetManager, appWidgetIds, CoreServiceManager.isRunning())
                }
            } catch (ignored: Exception) {
            }
        }

        private fun formatServerName(raw: String?): String {
            if (raw.isNullOrBlank()) return "Бункер Кота ⚡"
            val parts = raw.split("|")
            if (parts.size >= 2) {
                val candidate = parts[1].trim()
                if (candidate.isNotBlank()) return candidate
            }
            return raw.trim()
        }

        private fun updateWidgetUi(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            isRunning: Boolean
        ) {
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_switch)

            // Open app on card click
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            remoteViews.setOnClickPendingIntent(R.id.layout_widget_card, mainPendingIntent)

            // Toggle VPN on power button click
            val toggleIntent = Intent(context, WidgetProvider::class.java).apply {
                action = AppConfig.BROADCAST_ACTION_WIDGET_CLICK
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                R.id.layout_switch,
                toggleIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            remoteViews.setOnClickPendingIntent(R.id.layout_switch, togglePendingIntent)

            val selectedGuid = MmkvManager.getSelectServer()
            val currentConfig = if (!selectedGuid.isNullOrBlank()) MmkvManager.decodeServerConfig(selectedGuid) else null
            val serverName = formatServerName(currentConfig?.remarks)

            if (isRunning) {
                val (downBps, upBps) = MmkvManager.decodeTrafficSpeed()
                val speedText = "↓ ${downBps.toSpeedString()} • ↑ ${upBps.toSpeedString()}"

                remoteViews.setInt(R.id.layout_widget_card, "setBackgroundResource", R.drawable.bg_cyber_widget_active)
                remoteViews.setTextViewText(R.id.tv_widget_status, "⚡ БУНКЕР АКТИВЕН")
                remoteViews.setTextColor(R.id.tv_widget_status, 0xFF10B981.toInt())
                remoteViews.setTextViewText(R.id.tv_widget_server, serverName)
                remoteViews.setTextViewText(R.id.tv_widget_speed, speedText)
                remoteViews.setInt(R.id.layout_switch, "setBackgroundResource", R.drawable.bg_cyber_button_active)
                remoteViews.setInt(R.id.image_switch, "setImageResource", R.drawable.ic_stop_24dp)
            } else {
                remoteViews.setInt(R.id.layout_widget_card, "setBackgroundResource", R.drawable.bg_cyber_widget_inactive)
                remoteViews.setTextViewText(R.id.tv_widget_status, "⚪ БУНКЕР ОТКЛЮЧЕН")
                remoteViews.setTextColor(R.id.tv_widget_status, 0xFF94A3B8.toInt())
                remoteViews.setTextViewText(R.id.tv_widget_server, serverName)
                remoteViews.setTextViewText(R.id.tv_widget_speed, "Узлы готовы к работе")
                remoteViews.setInt(R.id.layout_switch, "setBackgroundResource", R.drawable.bg_cyber_button_inactive)
                remoteViews.setInt(R.id.image_switch, "setImageResource", R.drawable.ic_play_24dp)
            }

            for (appWidgetId in appWidgetIds) {
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val isRunning = CoreServiceManager.isRunning()
        updateWidgetUi(context, appWidgetManager, appWidgetIds, isRunning)
        if (isRunning) {
            startLiveTicker(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (AppConfig.BROADCAST_ACTION_WIDGET_CLICK == intent.action) {
            if (CoreServiceManager.isRunning()) {
                stopLiveTicker(context)
                LauncherManager.stopService(context)
            } else {
                LauncherManager.startServiceFromToggle(context)
                startLiveTicker(context)
            }
        } else if (AppConfig.BROADCAST_ACTION_ACTIVITY == intent.action) {
            AppWidgetManager.getInstance(context)?.let { manager ->
                val appWidgetIds = manager.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java))
                if (appWidgetIds.isEmpty()) return@let

                when (intent.getIntExtra("key", 0)) {
                    AppConfig.MSG_STATE_RUNNING, AppConfig.MSG_STATE_START_SUCCESS -> {
                        updateWidgetUi(context, manager, appWidgetIds, true)
                        startLiveTicker(context)
                    }
                    AppConfig.MSG_STATE_NOT_RUNNING, AppConfig.MSG_STATE_START_FAILURE, AppConfig.MSG_STATE_STOP_SUCCESS -> {
                        stopLiveTicker(context)
                        updateWidgetUi(context, manager, appWidgetIds, false)
                    }
                }
            }
        }
    }
}
