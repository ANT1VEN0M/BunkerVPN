package com.v2ray.ang.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.v2ray.ang.R
import com.v2ray.ang.handler.BunkerSpeedtestManager
import com.v2ray.ang.handler.SpeedtestProgress
import com.v2ray.ang.handler.SpeedtestStage
import com.v2ray.ang.ui.compose.colorFabActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun BunkerSpeedtestDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    serverName: String,
    proxyPort: Int = 0
) {
    if (!show) return

    val scope = rememberCoroutineScope()
    var progressState by remember { mutableStateOf(SpeedtestProgress(stage = SpeedtestStage.IDLE)) }
    var testJob by remember { mutableStateOf<Job?>(null) }

    fun startTest() {
        testJob?.cancel()
        testJob = scope.launch {
            BunkerSpeedtestManager.runSpeedtest(proxyPort).collect { progress ->
                progressState = progress
            }
        }
    }

    LaunchedEffect(Unit) {
        startTest()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressState.progressPercent / 100f,
        label = "speedtest_progress"
    )

    Dialog(onDismissRequest = {
        testJob?.cancel()
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF140D24),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF7C3AED).copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_flash_on_24dp),
                        contentDescription = null,
                        tint = colorFabActive,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "БУНКЕР СПИДТЕСТ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE9D5FF),
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Server Subtitle
                Text(
                    text = serverName.ifBlank { "Сервер Бункера" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Big Speed Display
                val displaySpeed = when (progressState.stage) {
                    SpeedtestStage.COMPLETED -> String.format(Locale.US, "%.1f", progressState.avgMbps)
                    SpeedtestStage.TESTING_DOWNLOAD -> String.format(Locale.US, "%.1f", progressState.currentMbps)
                    SpeedtestStage.FAILED -> "—"
                    else -> "0.0"
                }

                val speedColor = when (progressState.stage) {
                    SpeedtestStage.COMPLETED -> Color(0xFF10B981)
                    SpeedtestStage.TESTING_DOWNLOAD -> Color(0xFF38BDF8)
                    SpeedtestStage.FAILED -> Color(0xFFEF4444)
                    else -> Color(0xFFA78BFA)
                }

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(75.dp))
                        .background(Color(0xFF1C1333))
                        .border(2.dp, speedColor.copy(alpha = 0.4f), RoundedCornerShape(75.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = displaySpeed,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = speedColor
                        )
                        Text(
                            text = "Мбит/с",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCBD5E1)
                        )
                        if (progressState.pingMs > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Пинг: ${progressState.pingMs} ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorFabActive,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = speedColor,
                    trackColor = Color(0xFF261846)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status text below bar
                val statusText = when (progressState.stage) {
                    SpeedtestStage.IDLE -> "Инициализация..."
                    SpeedtestStage.TESTING_PING -> "Замер задержки..."
                    SpeedtestStage.TESTING_DOWNLOAD -> {
                        val mbDownloaded = progressState.downloadedBytes / (1024.0 * 1024.0)
                        val mbTotal = progressState.totalBytes / (1024.0 * 1024.0)
                        String.format(Locale.US, "Загрузка: %.1f / %.1f МБ (%d%%)", mbDownloaded, mbTotal, progressState.progressPercent)
                    }
                    SpeedtestStage.COMPLETED -> "Тест завершён успешно! ⚡"
                    SpeedtestStage.FAILED -> "Ошибка: ${progressState.errorMessage ?: "Сбой соединения"}"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (progressState.stage == SpeedtestStage.FAILED) Color(0xFFF87171) else Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (progressState.stage == SpeedtestStage.TESTING_DOWNLOAD || progressState.stage == SpeedtestStage.TESTING_PING) {
                        OutlinedButton(
                            onClick = {
                                testJob?.cancel()
                                progressState = SpeedtestProgress(stage = SpeedtestStage.IDLE)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Отмена", color = Color(0xFFE2E8F0))
                        }
                    } else {
                        Button(
                            onClick = { startTest() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Повторить", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = {
                            testJob?.cancel()
                            onDismiss()
                        }
                    ) {
                        Text("Закрыть", color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }
}
