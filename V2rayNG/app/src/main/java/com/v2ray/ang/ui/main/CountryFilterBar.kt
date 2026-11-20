package com.v2ray.ang.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CountryFilterHelper {
    // Regex for flag emoji: pair of Regional Indicator Symbols (\uD83C[\uDDE6-\uDDFF])
    private val FLAG_REGEX = Regex("([\\uD83C][\\uDDE6-\\uDDFF]){2}")

    const val ID_ALL = "__ALL__"
    const val ID_ONLINE = "__ONLINE__"
    const val ID_AUTO = "__AUTO__"
    const val ID_OTHER = "__OTHER__"

    fun extractFilterKey(remarks: String): String {
        val flagMatch = FLAG_REGEX.find(remarks)
        if (flagMatch != null) {
            val flag = flagMatch.value
            val code = getCountryCodeFromFlag(flag)
            return if (code.isNotBlank()) "$flag $code" else flag
        }

        val lower = remarks.lowercase()
        if (lower.contains("автовыбор") || lower.contains("auto") || lower.contains("balancer")) {
            return ID_AUTO
        }
        if (lower.contains("герман") || lower.contains("germany") || lower.contains("frankfurt")) return "🇩🇪 DE"
        if (lower.contains("нидерланд") || lower.contains("netherland") || lower.contains("голланди") || lower.contains("amsterdam")) return "🇳🇱 NL"
        if (lower.contains("франц") || lower.contains("france") || lower.contains("paris")) return "🇫🇷 FR"
        if (lower.contains("сша") || lower.contains("usa") || lower.contains("america") || lower.contains("united states")) return "🇺🇸 US"
        if (lower.contains("финлянд") || lower.contains("finland") || lower.contains("helsinki")) return "🇫🇮 FI"
        if (lower.contains("турци") || lower.contains("turkey") || lower.contains("istanbul")) return "🇹🇷 TR"
        if (lower.contains("казахст") || lower.contains("kazakhstan") || lower.contains("almaty")) return "🇰🇿 KZ"
        if (lower.contains("швеци") || lower.contains("sweden") || lower.contains("stockholm")) return "🇸🇪 SE"
        if (lower.contains("росси") || lower.contains("russia") || lower.contains("москва")) return "🇷🇺 RU"
        if (lower.contains("польш") || lower.contains("poland") || lower.contains("warsaw")) return "🇵🇱 PL"
        if (lower.contains("великобрит") || lower.contains("london") || lower.contains("англия")) return "🇬🇧 GB"
        if (lower.contains("сингапур") || lower.contains("singapore")) return "🇸🇬 SG"
        if (lower.contains("япони") || lower.contains("japan") || lower.contains("tokyo")) return "🇯🇵 JP"
        if (lower.contains("швейцар") || lower.contains("switzerland")) return "🇨🇭 CH"
        if (lower.contains("австри") || lower.contains("austria")) return "🇦🇹 AT"

        return ID_OTHER
    }

    private fun getCountryCodeFromFlag(flag: String): String {
        return runCatching {
            val chars = flag.codePoints().toArray()
            if (chars.size == 2) {
                val c1 = (chars[0] - 0x1F1E6 + 'A'.code).toChar()
                val c2 = (chars[1] - 0x1F1E6 + 'A'.code).toChar()
                "$c1$c2"
            } else ""
        }.getOrDefault("")
    }
}

data class CountryFilterItem(
    val id: String,
    val label: String,
    val count: Int
)

@Composable
fun CountryFilterBar(
    filters: List<CountryFilterItem>,
    selectedFilter: String?,
    onSelectFilter: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (filters.size <= 2) return // Don't show if only "All" and 1 country

    val haptic = LocalHapticFeedback.current

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(filters, key = { it.id }) { item ->
            val isSelected = (selectedFilter == null && item.id == CountryFilterHelper.ID_ALL) || (selectedFilter == item.id)

            val bgColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
            }

            val textColor = if (isSelected) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            val countColor = if (isSelected) {
                Color.White.copy(alpha = 0.85f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            }

            val border = if (isSelected) {
                BorderStroke(1.5.dp, Color(0xFFC084FC)) // Light Purple glow border
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bgColor,
                border = border,
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (item.id == CountryFilterHelper.ID_ALL || selectedFilter == item.id) {
                            onSelectFilter(null)
                        } else {
                            onSelectFilter(item.id)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.count}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = countColor
                    )
                }
            }
        }
    }
}
