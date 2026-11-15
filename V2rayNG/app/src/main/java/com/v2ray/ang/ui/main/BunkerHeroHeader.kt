package com.v2ray.ang.ui.main

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.extension.toSpeedString
import com.v2ray.ang.ui.compose.colorFabActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun BunkerHeroHeader(
    isRunning: Boolean,
    statusText: String,
    currentServerRemark: String? = null,
    onToggle: () -> Unit,
    onTestPing: () -> Unit,
    onSortByPing: () -> Unit,
    onUpdateSub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val infiniteTransition = rememberInfiniteTransition(label = "bunker_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Real-time Live Speed & Session Duration
    var downSpeedText by remember { mutableStateOf("0 B/s") }
    var upSpeedText by remember { mutableStateOf("0 B/s") }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isRunning) {
        if (!isRunning) {
            downSpeedText = "0 B/s"
            upSpeedText = "0 B/s"
            elapsedSeconds = 0L
            return@LaunchedEffect
        }
        var lastTime = System.currentTimeMillis()
        var lastDown = 0L
        var lastUp = 0L

        runCatching {
            CoreServiceManager.queryAllOutboundTrafficStats().forEach { stat ->
                if (stat.tag != AppConfig.TAG_BLOCKED) {
                    when (stat.direction) {
                        AppConfig.UPLINK -> lastUp += stat.value
                        AppConfig.DOWNLINK -> lastDown += stat.value
                    }
                }
            }
        }

        while (isActive && isRunning) {
            delay(1000L)
            elapsedSeconds += 1L
            val now = System.currentTimeMillis()
            val dt = (now - lastTime).coerceAtLeast(100L) / 1000.0
            var curDown = 0L
            var curUp = 0L

            runCatching {
                CoreServiceManager.queryAllOutboundTrafficStats().forEach { stat ->
                    if (stat.tag != AppConfig.TAG_BLOCKED) {
                        when (stat.direction) {
                            AppConfig.UPLINK -> curUp += stat.value
                            AppConfig.DOWNLINK -> curDown += stat.value
                        }
                    }
                }
            }

            val dDown = (curDown - lastDown).coerceAtLeast(0L)
            val dUp = (curUp - lastUp).coerceAtLeast(0L)
            lastDown = curDown
            lastUp = curUp
            lastTime = now

            downSpeedText = (dDown / dt).toLong().toSpeedString()
            upSpeedText = (dUp / dt).toLong().toSpeedString()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Interactive Cat Button with Cyber Glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(92.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle()
                    }
            ) {
                // Outer glow pulse
                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(colorFabActive.copy(alpha = pulseAlpha * 0.25f))
                            .border(
                                width = 2.dp,
                                color = colorFabActive.copy(alpha = pulseAlpha),
                                shape = CircleShape
                            )
                    )
                }

                // Inner Cat Emblem
                Image(
                    painter = painterResource(R.drawable.bunker_logo),
                    contentDescription = "Bunker Cat Button",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .shadow(elevation = if (isRunning) 8.dp else 2.dp, shape = CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Status Banner
            Text(
                text = if (isRunning) "⚡ БУНКЕР АКТИВЕН" else "🛡 ЗАЩИТА ОТКЛЮЧЕНА",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isRunning) colorFabActive else MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Server node or instructions
            if (isRunning && !currentServerRemark.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)) // glowing green dot
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentServerRemark,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = if (isRunning) "Туннель зашифрован • Нажмите на кота для отключения"
                    else "Нажмите на кота для мгновенного подключения",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }

            // Realtime Live Traffic & Timer HUD
            if (isRunning) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, colorFabActive.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Down speed
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "↓",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981), // Neon Green
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = downSpeedText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Session Duration
                        val hours = elapsedSeconds / 3600
                        val minutes = (elapsedSeconds % 3600) / 60
                        val seconds = elapsedSeconds % 60
                        val durationStr = if (hours > 0) {
                            String.format("%02d:%02d:%02d", hours, minutes, seconds)
                        } else {
                            String.format("%02d:%02d", minutes, seconds)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⏱",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = durationStr,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF06B6D4) // Neon Cyan
                            )
                        }

                        // Up speed
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "↑",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA855F7), // Neon Purple
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = upSpeedText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Control Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                // Ping Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    modifier = Modifier
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTestPing()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_flash_on_24dp),
                            contentDescription = null,
                            tint = colorFabActive,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Пинг",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Sort By Ping Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    modifier = Modifier
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSortByPing()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_restore_24dp),
                            contentDescription = null,
                            tint = colorFabActive,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Топ пинг",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Update Subscriptions Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    modifier = Modifier
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onUpdateSub()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh_24dp),
                            contentDescription = null,
                            tint = colorFabActive,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Обновить",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
