package com.v2ray.ang.ui.main

import android.content.Context
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.extension.nullIfBlank
import com.v2ray.ang.handler.AngConfigManager

internal data class ServerRowUiModel(
    val guid: String,
    val profile: ProfileItem,
    val remarks: String,
    val statistics: String,
    val typeDescription: String,
    val testDelayMillis: Long,
    val subscriptionBadge: String,
)

internal data class ServerGroupUiState(
    val servers: List<ServersCache> = emptyList(),
    val rows: List<ServerRowUiModel> = emptyList(),
)

internal fun buildServerRowUiModel(
    server: ServersCache,
    subscriptionRemarks: String,
    context: Context? = null
): ServerRowUiModel {
    val profile = server.profile
    val stats = if (profile.configType == EConfigType.POLICYGROUP) {
        context?.getString(R.string.bunker_policy_desc) ?: "All subscription servers • Automatic selection"
    } else {
        profile.description.nullIfBlank()
            ?: AngConfigManager.generateDescription(profile)
    }
    return ServerRowUiModel(
        guid = server.guid,
        profile = profile,
        remarks = profile.remarks,
        statistics = stats,
        typeDescription = serverProtocolDescription(profile, context),
        testDelayMillis = server.testDelayMillis,
        subscriptionBadge = subscriptionRemarks.firstOrNull()?.toString().orEmpty(),
    )
}

private fun serverProtocolDescription(profile: ProfileItem, context: Context? = null): String {
    if (profile.configType == EConfigType.POLICYGROUP) {
        return context?.getString(R.string.bunker_balancer_type) ?: "⚡ Smart balancer (lowest ping)"
    }
    if (profile.configType.isComplexType()) return profile.configType.name
    val parts = mutableListOf(profile.configType.name)
    profile.network?.let { network ->
        if (network.isNotBlank() && !network.equals("tcp", ignoreCase = true)) {
            parts.add(network)
        }
    }
    profile.security?.let { security ->
        if (security.isNotBlank()) {
            parts.add(
                if (profile.insecure == true && security.equals("tls", ignoreCase = true)) {
                    "$security insecure"
                } else {
                    security
                }
            )
        }
    }
    return parts.joinToString(" / ")
}
