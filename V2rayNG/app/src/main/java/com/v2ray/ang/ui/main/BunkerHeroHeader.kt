package com.v2ray.ang.ui.main

import android.net.TrafficStats
import android.os.Process
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.luminance
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
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.extension.toSpeedString
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.compose.colorFabActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class BunkerServiceItem(
    val id: String,
    val name: String,
    val icon: String,
    val color: Color,
    val isAvailable: Boolean
)

private fun hasServiceToken(remark: String?, token: String, altKeywords: List<String>): Boolean {
    if (remark.isNullOrBlank()) return false
    val upper = remark.uppercase()
    val pattern = Regex("""(^|[\s|×+,;\-_])${Regex.escape(token.uppercase())}([\s|×+,;\-_]|$)""")
    if (pattern.containsMatchIn(upper)) return true
    for (alt in altKeywords) {
        if (upper.contains(alt.uppercase())) return true
    }
    return false
}

private fun detectBunkerServices(
    currentRemark: String?,
    serversInGroup: List<ServersCache>,
    isRunning: Boolean,
    workingCount: Int
): List<BunkerServiceItem> {
    val serviceDefs = listOf(
        Triple("yt", "YouTube" to "▶", 0xFFEF4444 to listOf("YT", "YouTube")),
        Triple("tg", "Telegram" to "✈", 0xFF06B6D4 to listOf("TG", "Telegram")),
        Triple("gpt", "ChatGPT" to "🤖", 0xFF10B981 to listOf("GPT", "ChatGPT", "OpenAI")),
        Triple("gem", "Gemini" to "✦", 0xFFA855F7 to listOf("GEM", "Gemini"))
    )

    val remarkHasAnyTag = serviceDefs.any { (_, _, meta) ->
        val (_, tags) = meta
        hasServiceToken(currentRemark, tags[0], tags.drop(1))
    }

    val groupHasAnyTag = serversInGroup.any { server ->
        val rem = server.profile.remarks
        serviceDefs.any { (_, _, meta) ->
            val (_, tags) = meta
            hasServiceToken(rem, tags[0], tags.drop(1))
        }
    }

    val workingServers = serversInGroup.filter { it.testDelayMillis > 0L }
    val candidateServers = if (workingServers.isNotEmpty()) workingServers else serversInGroup

    return serviceDefs.map { (id, display, meta) ->
        val (name, icon) = display
        val (colorHex, tags) = meta
        val color = Color(colorHex)
        val mainTag = tags[0]
        val altTags = tags.drop(1)

        val isAvailable = when {
            remarkHasAnyTag -> hasServiceToken(currentRemark, mainTag, altTags)
            groupHasAnyTag -> {
                candidateServers.any { hasServiceToken(it.profile.remarks, mainTag, altTags) }
            }
            else -> isRunning || workingCount > 0
        }

        BunkerServiceItem(
            id = id,
            name = name,
            icon = icon,
            color = color,
            isAvailable = isAvailable
        )
    }
}

@Composable
private fun ServiceBadge(
    service: BunkerServiceItem,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (service.isAvailable) service.color.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f),
        border = BorderStroke(
            1.dp,
            if (service.isAvailable) service.color.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.12f)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = service.icon,
                fontSize = 9.sp,
                color = if (service.isAvailable) service.color else Color.White.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.width(2.5.dp))
            Text(
                text = service.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (service.isAvailable) FontWeight.Bold else FontWeight.Normal,
                fontSize = 9.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                color = if (service.isAvailable) service.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
            if (service.isAvailable) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "✓",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = service.color
                )
            }
        }
    }
}

@Composable
private fun CyberTrafficWave(
    isRunning: Boolean,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_motion")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (intensity > 0.35f) 700 else 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val barCount = 14
    val cyanColor = Color(0xFF06B6D4)
    val purpleColor = MaterialTheme.colorScheme.primary
    val emeraldColor = Color(0xFF10B981)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val distFromCenter = kotlin.math.abs(i - (barCount - 1) / 2.0f) / ((barCount - 1) / 2.0f)
            val centerWeight = 1f - 0.35f * distFromCenter

            val sineVal = kotlin.math.sin(wavePhase + i * 0.45f).toFloat()
            val normalizedSine = (sineVal + 1f) / 2f

            val targetHeightDp = if (!isRunning) {
                3.dp
            } else {
                val minH = 6f + normalizedSine * 6f
                val maxH = 9f + normalizedSine * 17f
                (minH + (maxH - minH) * intensity * centerWeight).dp
            }

            val barAlpha = if (!isRunning) 0.25f else (0.55f + 0.45f * normalizedSine)
            val barColor = when {
                i % 3 == 0 -> cyanColor
                i % 3 == 1 -> purpleColor
                else -> emeraldColor
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(targetHeightDp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor.copy(alpha = barAlpha))
            )
        }
    }
}

@Composable
fun BunkerHeroHeader(
    isRunning: Boolean,
    statusText: String,
    currentServerRemark: String? = null,
    isTesting: Boolean = false,
    workingServersCount: Int = 0,
    totalServersCount: Int = 0,
    testedServersCount: Int = 0,
    serversInGroup: List<ServersCache> = emptyList(),
    isAntiRknEnabled: Boolean = false,
    onToggleAntiRkn: () -> Unit = {},
    availableUpdateVersion: String? = null,
    onUpdateClick: () -> Unit = {},
    onToggle: () -> Unit,
    onTestPing: () -> Unit,
    onAutoSelect: () -> Unit = {},
    onSortByPing: () -> Unit = {},
    onSpeedtestClick: () -> Unit = {},
    onUpdateSub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isCollapsed by rememberSaveable { mutableStateOf(false) }
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

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
    var trafficIntensity by remember { mutableFloatStateOf(0f) }
    var showThemeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        if (!isRunning) {
            downSpeedText = "0 B/s"
            upSpeedText = "0 B/s"
            elapsedSeconds = 0L
            trafficIntensity = 0f
            return@LaunchedEffect
        }
        var lastTime = System.currentTimeMillis()
        val uid = Process.myUid()
        var lastRx = TrafficStats.getUidRxBytes(uid).takeIf { it >= 0 } ?: TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getUidTxBytes(uid).takeIf { it >= 0 } ?: TrafficStats.getTotalTxBytes()

        while (isActive && isRunning) {
            delay(1000L)
            elapsedSeconds += 1L
            val now = System.currentTimeMillis()
            val dt = (now - lastTime).coerceAtLeast(100L) / 1000.0
            lastTime = now

            val curRx = TrafficStats.getUidRxBytes(uid).takeIf { it >= 0 } ?: TrafficStats.getTotalRxBytes()
            val curTx = TrafficStats.getUidTxBytes(uid).takeIf { it >= 0 } ?: TrafficStats.getTotalTxBytes()

            var speedDown = 0L
            var speedUp = 0L

            if (curRx >= 0 && lastRx >= 0 && curRx >= lastRx) {
                speedDown = ((curRx - lastRx) / dt).toLong().coerceAtLeast(0L)
            }
            if (curTx >= 0 && lastTx >= 0 && curTx >= lastTx) {
                speedUp = ((curTx - lastTx) / dt).toLong().coerceAtLeast(0L)
            }

            // Cross-check with core stats shared through MMKV by daemon
            val (coreDown, coreUp) = MmkvManager.decodeTrafficSpeed()
            val finalDown = maxOf(speedDown, coreDown)
            val finalUp = maxOf(speedUp, coreUp)

            lastRx = curRx
            lastTx = curTx

            downSpeedText = finalDown.toSpeedString()
            upSpeedText = finalUp.toSpeedString()

            val totalSpeed = finalDown + finalUp
            trafficIntensity = if (totalSpeed <= 0L) {
                0.2f
            } else {
                (0.25f + (totalSpeed / (1024.0 * 1024.0 * 2.0)).toFloat() * 0.75f).coerceIn(0.2f, 1f)
            }
        }
    }

    if (isCollapsed) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Cat Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggle()
                        }
                ) {
                    if (isRunning) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, colorFabActive.copy(alpha = pulseAlpha), CircleShape)
                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.bunker_logo),
                        contentDescription = "Bunker Cat Button",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Middle Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) Color(0xFF10B981) else Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRunning) "⚡ БУНКЕР АКТИВЕН" else "🛡 ЗАЩИТА ВЫКЛ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isRunning) colorFabActive else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isRunning && downSpeedText != "0 B/s") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "↓$downSpeedText",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                    Text(
                        text = currentServerRemark ?: "Нажмите на кота для подключения",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Quick Auto Button in collapsed mode
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, colorFabActive.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAutoSelect()
                    }
                ) {
                    Text(
                        text = "🎯 Авто",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorFabActive,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))

                // Expand toggle button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isCollapsed = false
                    }
                ) {
                    Text(
                        text = "▼ Развернуть",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                    )
                }
            }
        }
    } else {
        // Expanded View
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Mini Header with Collapse Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BUNKER CYBER SHIELD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isCollapsed = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "▲ Свернуть",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Interactive Cat Button with Cyber Glow
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(66.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggle()
                        }
                ) {
                    // Outer glow pulse
                    if (isRunning) {
                        Box(
                            modifier = Modifier
                                .size(66.dp)
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
                            .size(56.dp)
                            .clip(CircleShape)
                            .shadow(elevation = if (isRunning) 6.dp else 2.dp, shape = CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Main Status Banner
                Text(
                    text = if (isRunning) "⚡ БУНКЕР АКТИВЕН" else "🛡 ЗАЩИТА ОТКЛЮЧЕНА",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = if (isRunning) colorFabActive else MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Server node or instructions
                if (!currentServerRemark.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) Color(0xFF10B981) else Color(0xFF06B6D4))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = currentServerRemark,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = if (isRunning) "Туннель зашифрован • Нажмите на кота для отключения"
                        else "Нажмите на кота для мгновенного подключения",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Real-time Server Testing & Health Counter
                if (isTesting) {
                    val testingBg = if (isDarkTheme) Color(0xFF0F172A).copy(alpha = 0.85f) else Color(0xFFE0F2FE)
                    val testingText = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = testingBg,
                        border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = pulseAlpha)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF06B6D4))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "⚡ Проверка серверов: $testedServersCount / $totalServersCount • Онлайн: $workingServersCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                color = testingText
                            )
                        }
                    }
                } else if (testedServersCount > 0) {
                    val healthBg = when {
                        workingServersCount > 0 && isDarkTheme -> Color(0xFF064E3B).copy(alpha = 0.5f)
                        workingServersCount > 0 && !isDarkTheme -> Color(0xFFDCFCE7)
                        isDarkTheme -> Color(0xFF7F1D1D).copy(alpha = 0.5f)
                        else -> Color(0xFFFEE2E2)
                    }
                    val healthBorder = when {
                        workingServersCount > 0 && isDarkTheme -> Color(0xFF10B981).copy(alpha = 0.6f)
                        workingServersCount > 0 && !isDarkTheme -> Color(0xFF16A34A).copy(alpha = 0.7f)
                        isDarkTheme -> Color(0xFFEF4444).copy(alpha = 0.6f)
                        else -> Color(0xFFDC2626).copy(alpha = 0.7f)
                    }
                    val healthText = when {
                        workingServersCount > 0 && isDarkTheme -> Color(0xFF34D399)
                        workingServersCount > 0 && !isDarkTheme -> Color(0xFF14532D)
                        isDarkTheme -> Color(0xFFF87171)
                        else -> Color(0xFF991B1B)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = healthBg,
                        border = BorderStroke(1.dp, healthBorder),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (workingServersCount > 0) Color(0xFF10B981) else Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            val percent = if (totalServersCount > 0) (workingServersCount * 100 / totalServersCount) else 0
                            Text(
                                text = if (workingServersCount > 0) {
                                    "⚡ Рабочих узлов: $workingServersCount из $totalServersCount онлайн ($percent%)"
                                } else {
                                    "✕ Нет доступных серверов (0 из $totalServersCount)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                color = healthText
                            )
                        }
                    }
                } else if (totalServersCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "📡 Серверов в группе: $totalServersCount • Нажмите «Пинг» для замера",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Supported Services Cyber Row
                val services = remember(currentServerRemark, serversInGroup, isRunning, workingServersCount) {
                    detectBunkerServices(currentServerRemark, serversInGroup, isRunning, workingServersCount)
                }
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    services.forEach { service ->
                        ServiceBadge(
                            service = service,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Anti-RKN Mode Toggle & Theme Palette Quick Row
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rknBg = when {
                        isAntiRknEnabled && isDarkTheme -> Color(0xFF064E3B).copy(alpha = 0.5f)
                        isAntiRknEnabled && !isDarkTheme -> Color(0xFFDCFCE7)
                        isDarkTheme -> Color(0xFF1E1B4B).copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    val rknBorder = when {
                        isAntiRknEnabled && isDarkTheme -> Color(0xFF10B981).copy(alpha = 0.7f)
                        isAntiRknEnabled && !isDarkTheme -> Color(0xFF16A34A).copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    }
                    val rknText = when {
                        isAntiRknEnabled && isDarkTheme -> Color(0xFF34D399)
                        isAntiRknEnabled && !isDarkTheme -> Color(0xFF14532D)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    // Anti-RKN Mode Toggle Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = rknBg,
                        border = BorderStroke(1.dp, rknBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleAntiRkn()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isAntiRknEnabled) "⚡" else "🌐",
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAntiRknEnabled) "Анти-РКН 🇷🇺 (РФ прямо)" else "Полный туннель",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = rknText,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Theme Quick Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showThemeDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🎨", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Тема",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // In-App Update Banner
                if (!availableUpdateVersion.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onUpdateClick()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🚀", fontSize = 10.5.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Обновление $availableUpdateVersion!",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Обновить ➔",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                // Realtime Live Traffic & Timer HUD with Cyber Wave
                if (isRunning) {
                    Spacer(modifier = Modifier.height(6.dp))
                    CyberTrafficWave(
                        isRunning = isRunning,
                        intensity = trafficIntensity
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, colorFabActive.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Down speed
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "↓",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = downSpeedText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp,
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
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = durationStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF06B6D4)
                                )
                            }

                            // Up speed
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "↑",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = upSpeedText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(7.dp))

                // Quick Control Pills: Пинг | Авто | Тест | Обновить
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Ping Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTestPing()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_flash_on_24dp),
                                contentDescription = null,
                                tint = colorFabActive,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.5.dp))
                            Text(
                                text = "Пинг",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // 2. Auto Select Lowest Ping Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, colorFabActive.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAutoSelect()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🎯", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(2.5.dp))
                            Text(
                                text = "Авто",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = colorFabActive
                            )
                        }
                    }

                    // 3. Speedtest Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSpeedtestClick()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🚀", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(2.5.dp))
                            Text(
                                text = "Тест",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // 4. Update Subscriptions Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateSub()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_refresh_24dp),
                                contentDescription = null,
                                tint = colorFabActive,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.5.dp))
                            Text(
                                text = "Обновить",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        BunkerThemeDialog(onDismissRequest = { showThemeDialog = false })
    }
}

    if (showThemeDialog) {
        BunkerThemeDialog(onDismissRequest = { showThemeDialog = false })
    }
}
