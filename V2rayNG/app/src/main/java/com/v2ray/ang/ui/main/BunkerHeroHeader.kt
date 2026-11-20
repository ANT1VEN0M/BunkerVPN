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
import androidx.compose.ui.res.stringResource
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
import com.v2ray.ang.ui.compose.LocalStealthMode
import com.v2ray.ang.ui.compose.ThemeManager
import com.v2ray.ang.ui.compose.colorFabActive
import androidx.compose.runtime.collectAsState
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

internal fun detectBunkerServices(
    currentRemark: String?,
    serversInGroup: List<ServersCache>,
    isRunning: Boolean,
    workingCount: Int
): List<BunkerServiceItem> {
    val serviceDefs = listOf(
        Triple("yt", "YouTube" to "▶", 0xFFEF4444 to listOf("YT", "YouTube")),
        Triple("tg", "Telegram" to "✈", 0xFF06B6D4 to listOf("TG", "Telegram")),
        Triple("ai", "AI (GPT•Gemini)" to "🤖", 0xFF10B981 to listOf("GPT", "ChatGPT", "OpenAI", "GEM", "Gemini", "AI"))
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
internal fun ServiceBadge(
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
    hideOfflineServers: Boolean = false,
    onToggleHideOffline: () -> Unit = {},
    doubleColumnDisplay: Boolean = false,
    onToggleDoubleColumn: () -> Unit = {},
    onRemoveInvalid: () -> Unit = {},
    onTelegramSos: () -> Unit = {},
    onGenerateWarp: () -> Unit = {},
    onSmartClean: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isStealth = LocalStealthMode.current || ThemeManager.isStealthMode.collectAsState().value
    var isCollapsed by rememberSaveable { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
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

    if (isCollapsed || isStealth) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 3.dp),
            shape = RoundedCornerShape(14.dp),
            color = if (isStealth) Color(0xFF0F131A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, if (isStealth) Color(0xFF222834) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggle()
                        }
                ) {
                    if (isStealth) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) Color(0x2610B981) else MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Icon(
                                painter = painterResource(if (isRunning) R.drawable.ic_action_done else R.drawable.ic_lock_24dp),
                                contentDescription = "VPN Status",
                                tint = if (isRunning) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        if (isRunning) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, colorFabActive.copy(alpha = pulseAlpha), CircleShape)
                            )
                        }
                        Image(
                            painter = painterResource(R.drawable.bunker_logo),
                            contentDescription = "Bunker Cat Button",
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Middle Info - Safe single-line texts to prevent character wrapping
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) Color(0xFF10B981) else Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val statusTitle = if (isRunning) {
                            if (downSpeedText != "0 B/s") stringResource(R.string.bunker_hero_status_active_speed, downSpeedText)
                            else stringResource(R.string.bunker_hero_status_active)
                        } else {
                            stringResource(R.string.bunker_hero_status_off)
                        }
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isRunning) (if (isStealth) Color(0xFF10B981) else colorFabActive) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = currentServerRemark ?: stringResource(R.string.bunker_hero_tap_to_connect),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Quick Auto Button in collapsed mode
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, if (isStealth) Color(0xFF263042) else colorFabActive.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAutoSelect()
                    }
                ) {
                    Text(
                        text = if (isStealth) stringResource(R.string.bunker_btn_auto) else ("🎯 " + stringResource(R.string.bunker_btn_auto)),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isStealth) Color(0xFF38BDF8) else colorFabActive,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Filter button in collapsed mode
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (hideOfflineServers || isAntiRknEnabled) BorderStroke(1.dp, Color(0xFF10B981)) else null,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showFilterDialog = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = if (isStealth) "⚙" else "🎛️", fontSize = 11.sp)
                        if (hideOfflineServers || isAntiRknEnabled) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                        }
                    }
                }

                if (!isStealth) {
                    Spacer(modifier = Modifier.width(4.dp))

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
                            text = "▼",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    } else {
        // Expanded View - Compact, Clean & Fast
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 3.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Mini Header with active indicators and Collapse action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "BUNKER CYBER SHIELD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        if (hideOfflineServers) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.bunker_hero_badge_online),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                        if (isAntiRknEnabled) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.bunker_hero_badge_anti_rkn),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isCollapsed = true
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.bunker_hero_collapse),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Main Identity Row: Cat + Status + Current Server
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cat Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggle()
                            }
                    ) {
                        if (isRunning) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(colorFabActive.copy(alpha = pulseAlpha * 0.2f))
                                    .border(1.5.dp, colorFabActive.copy(alpha = pulseAlpha), CircleShape)
                            )
                        }
                        Image(
                            painter = painterResource(R.drawable.bunker_logo),
                            contentDescription = "Bunker Cat Button",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .shadow(elevation = if (isRunning) 4.dp else 1.dp, shape = CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Status and Server Info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRunning) stringResource(R.string.bunker_hero_status_active) else stringResource(R.string.bunker_hero_status_protection_off),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isRunning) colorFabActive else MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.3.sp
                        )

                        Spacer(modifier = Modifier.height(1.dp))

                        if (!currentServerRemark.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isRunning) Color(0xFF10B981) else Color(0xFF06B6D4))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentServerRemark,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.bunker_hero_tap_to_connect),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // If running: Single-line clean Live HUD
                if (isRunning) {
                    Spacer(modifier = Modifier.height(5.dp))
                    val hours = elapsedSeconds / 3600
                    val minutes = (elapsedSeconds % 3600) / 60
                    val seconds = elapsedSeconds % 60
                    val durationStr = if (hours > 0) {
                        String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    } else {
                        String.format("%02d:%02d", minutes, seconds)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, colorFabActive.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "↓ $downSpeedText",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = "⏱ $durationStr",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF06B6D4)
                            )
                            Text(
                                text = "↑ $upSpeedText",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else if (isTesting) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0xFF0F172A).copy(alpha = 0.85f) else Color(0xFFE0F2FE),
                        border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = pulseAlpha)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.bunker_hero_testing_progress, testedServersCount, totalServersCount, workingServersCount),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            )
                        }
                    }
                } else if (testedServersCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val percent = if (totalServersCount > 0) (workingServersCount * 100 / totalServersCount) else 0
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (workingServersCount > 0 && isDarkTheme) Color(0xFF064E3B).copy(alpha = 0.45f)
                        else if (workingServersCount > 0) Color(0xFFDCFCE7)
                        else if (isDarkTheme) Color(0xFF7F1D1D).copy(alpha = 0.45f)
                        else Color(0xFFFEE2E2),
                        border = BorderStroke(
                            1.dp,
                            if (workingServersCount > 0) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = workingServersCount > 0) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleHideOffline()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (workingServersCount > 0) {
                                    if (!hideOfflineServers) stringResource(R.string.bunker_hero_working_nodes_filter, workingServersCount, totalServersCount, percent)
                                    else stringResource(R.string.bunker_hero_working_nodes, workingServersCount, totalServersCount, percent)
                                } else {
                                    stringResource(R.string.bunker_hero_no_servers, totalServersCount)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (workingServersCount > 0 && isDarkTheme) Color(0xFF34D399)
                                else if (workingServersCount > 0) Color(0xFF14532D)
                                else if (isDarkTheme) Color(0xFFF87171)
                                else Color(0xFF991B1B)
                            )
                        }
                    }
                }

                // In-App Update Banner
                if (!availableUpdateVersion.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onUpdateClick()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🚀", fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.bunker_hero_update_available, availableUpdateVersion),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.bunker_hero_update_btn),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Essential Clean Action Row: Auto | Ping | Filters | Test
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Quick Auto 1-Click
                    Surface(
                        shape = RoundedCornerShape(9.dp),
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
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🎯", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = stringResource(R.string.bunker_btn_auto),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = colorFabActive
                            )
                        }
                    }

                    // 2. Ping Test
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTestPing()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_flash_on_24dp),
                                contentDescription = null,
                                tint = colorFabActive,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = stringResource(R.string.bunker_btn_ping),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // 3. Filters & Settings Sheet Button (Alexander's filter button!)
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        border = if (hideOfflineServers || isAntiRknEnabled) BorderStroke(1.dp, Color(0xFF10B981)) else null,
                        modifier = Modifier
                            .weight(1.1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showFilterDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🎛️", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = stringResource(R.string.bunker_btn_filters),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = if (hideOfflineServers || isAntiRknEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                            )
                            if (hideOfflineServers || isAntiRknEnabled) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }
                    }

                    // 4. Speedtest
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSpeedtestClick()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🚀", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = stringResource(R.string.bunker_btn_test),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
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

    if (showFilterDialog) {
        BunkerFilterDialog(
            hideOfflineServers = hideOfflineServers,
            onToggleHideOffline = onToggleHideOffline,
            doubleColumnDisplay = doubleColumnDisplay,
            onToggleDoubleColumn = onToggleDoubleColumn,
            isAntiRknEnabled = isAntiRknEnabled,
            onToggleAntiRkn = onToggleAntiRkn,
            onSortByPing = onSortByPing,
            onRemoveInvalid = onRemoveInvalid,
            onTelegramSos = onTelegramSos,
            onGenerateWarp = onGenerateWarp,
            onSmartClean = onSmartClean,
            onSpeedtestClick = onSpeedtestClick,
            onUpdateSub = onUpdateSub,
            currentServerRemark = currentServerRemark,
            serversInGroup = serversInGroup,
            isRunning = isRunning,
            workingServersCount = workingServersCount,
            onDismissRequest = { showFilterDialog = false }
        )
    }
}
