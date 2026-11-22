package com.v2ray.ang.handler

import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreConfigManager
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.dto.SubscriptionUpdateResult
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.extension.isNotNullEmpty
import com.v2ray.ang.fmt.CustomFmt
import com.v2ray.ang.fmt.CustomOutboundFmt
import com.v2ray.ang.fmt.Hysteria2Fmt
import com.v2ray.ang.fmt.MasqueFmt
import com.v2ray.ang.fmt.ShadowsocksFmt
import com.v2ray.ang.fmt.SocksFmt
import com.v2ray.ang.fmt.TrojanFmt
import com.v2ray.ang.fmt.V2rayNFmt
import com.v2ray.ang.fmt.VlessFmt
import com.v2ray.ang.fmt.VmessFmt
import com.v2ray.ang.fmt.WireguardFmt
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.QRCodeDecoder
import com.v2ray.ang.util.Utils
import okhttp3.Headers
import java.net.URI

object AngConfigManager {

    private data class ParsedProfile(
        val profile: ProfileItem,
        val rawConfig: String? = null,
    )

    // Parser mapping for different config types (lazy initialized)
    private val configFmtParsers: Map<String, (String) -> ProfileItem?> by lazy {
        mapOf(
            EConfigType.VMESS.protocolScheme to VmessFmt::parse,
            EConfigType.SHADOWSOCKS.protocolScheme to ShadowsocksFmt::parse,
            EConfigType.SOCKS.protocolScheme to SocksFmt::parse,
            AppConfig.SOCKS4 to SocksFmt::parse,
            AppConfig.SOCKS5 to SocksFmt::parse,
            EConfigType.TROJAN.protocolScheme to TrojanFmt::parse,
            EConfigType.VLESS.protocolScheme to VlessFmt::parse,
            EConfigType.WIREGUARD.protocolScheme to WireguardFmt::parse,
            EConfigType.HYSTERIA2.protocolScheme to Hysteria2Fmt::parse,
            EConfigType.MASQUE.protocolScheme to MasqueFmt::parse,
            AppConfig.HY2 to Hysteria2Fmt::parse,
        )
    }

    /**
     * Shares the configuration to the clipboard.
     *
     * @param context The context.
     * @param guid The GUID of the configuration.
     * @return The result code.
     */
    fun share2Clipboard(context: Context, guid: String): Int {
        try {
            val conf = shareConfig(guid)
            if (TextUtils.isEmpty(conf)) {
                return -1
            }

            Utils.setClipboard(context, conf)

        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to share config to clipboard", e)
            return -1
        }
        return 0
    }

    /**
     * Shares non-custom configurations to the clipboard.
     *
     * @param context The context.
     * @param serverList The list of server GUIDs.
     * @return The number of configurations shared.
     */
    fun shareNonCustomConfigsToClipboard(context: Context, serverList: List<String>): Int {
        try {
            val sb = StringBuilder()
            for (guid in serverList) {
                val url = shareConfig(guid)
                if (TextUtils.isEmpty(url)) {
                    continue
                }
                sb.append(url)
                sb.appendLine()
            }
            if (sb.count() > 0) {
                Utils.setClipboard(context, sb.toString())
            }
            return sb.lines().count() - 1
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to share non-custom configs to clipboard", e)
            return -1
        }
    }

    /**
     * Shares the configuration as a QR code.
     *
     * @param guid The GUID of the configuration.
     * @return The QR code bitmap.
     */
    fun share2QRCode(guid: String): Bitmap? {
        try {
            val conf = shareConfig(guid)
            if (TextUtils.isEmpty(conf)) {
                return null
            }
            return QRCodeDecoder.createQRCode(conf)

        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to share config as QR code", e)
            return null
        }
    }

    /**
     * Shares the full content of the configuration to the clipboard.
     *
     * @param context The context.
     * @param guid The GUID of the configuration.
     * @return The result code.
     */
    fun shareFullContent2Clipboard(context: Context, guid: String?): Int {
        try {
            if (guid == null) return -1
            val result = CoreConfigManager.getV2rayConfig(context, guid)
            if (result.status) {
                Utils.setClipboard(context, result.content)
            } else {
                return -1
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to share full content to clipboard", e)
            return -1
        }
        return 0
    }

    /**
     * Shares the configuration.
     *
     * @param guid The GUID of the configuration.
     * @return The configuration string.
     */
    private fun shareConfig(guid: String): String {
        try {
            val config = MmkvManager.decodeServerConfig(guid) ?: return ""

            return config.configType.protocolScheme + when (config.configType) {
                EConfigType.VMESS -> VmessFmt.toUri(config)
                EConfigType.SHADOWSOCKS -> ShadowsocksFmt.toUri(config)
                EConfigType.SOCKS -> SocksFmt.toUri(config)
                EConfigType.VLESS -> VlessFmt.toUri(config)
                EConfigType.TROJAN -> TrojanFmt.toUri(config)
                EConfigType.WIREGUARD -> WireguardFmt.toUri(config)
                EConfigType.HYSTERIA2 -> Hysteria2Fmt.toUri(config)
                EConfigType.MASQUE -> MasqueFmt.toUri(config)
                else -> {}
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to share config for GUID: $guid", e)
            return ""
        }
    }

    /**
     * Imports a batch of configurations.
     *
     * @param server The server string.
     * @param subid The subscription ID.
     * @param append Whether to append the configurations.
     * @return A pair containing the number of configurations and subscriptions imported.
     */
    fun importBatchConfig(server: String?, subid: String, append: Boolean): Pair<Int, Int> {
        return try {
            var count = parseBatchConfig(Utils.decode(server), subid, append)
            if (count <= 0) {
                count = parseBatchConfig(server, subid, append)
            }
            if (count <= 0) {
                count = parseCustomConfigServer(server, subid, append)
            }
            if (count <= 0) {
                val extracted = BunkerUniversalImporter.extractConfigs(server)
                if (extracted.isNotEmpty()) {
                    count = parseBatchConfig(extracted.joinToString("\n"), subid, append)
                }
            }

            var countSub = parseBatchSubscription(server)
            if (countSub <= 0) {
                countSub = parseBatchSubscription(Utils.decode(server))
            }
            if (countSub <= 0) {
                val extractedSubs = BunkerUniversalImporter.extractSubscriptionUrls(server)
                for (subUrl in extractedSubs) {
                    countSub += importUrlAsSubscription(subUrl)
                }
            }
            if (countSub > 0) {
                updateConfigViaSubAll()
            }

            count to countSub
        } catch (e: ProfileStorageException) {
            LogUtil.e(AppConfig.TAG, "Failed to store imported profiles", e)
            0 to 0
        }
    }

    /**
     * Parses a batch of subscriptions.
     *
     * @param servers The servers string.
     * @return The number of subscriptions parsed.
     */
    private fun parseBatchSubscription(servers: String?): Int {
        try {
            if (servers.isNullOrBlank()) {
                return 0
            }

            var count = 0
            servers.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .forEach { str ->
                    if (Utils.isValidSubUrl(str)) {
                        count += importUrlAsSubscription(str)
                    }
                }
            return count
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to parse batch subscription", e)
        }
        return 0
    }

    /**
     * Parses a batch of configurations.
     *
     * @param servers The servers string.
     * @param subid The subscription ID.
     * @param append Whether to append the configurations.
     * @return The number of configurations parsed.
     */
    private fun parseBatchConfig(servers: String?, subid: String, append: Boolean): Int {
        try {
            if (servers.isNullOrBlank()) {
                return 0
            }
            val subItem = MmkvManager.decodeSubscription(subid)

            // Parse all configs first (no I/O during parsing)
            val configs = mutableListOf<ProfileItem>()
            val v2raynLines = mutableListOf<String>()

            servers.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .reversed()
                .forEach {
                    if (it.startsWith(AppConfig.V2RAYNFMTS, ignoreCase = true)) {
                        v2raynLines.add(it)
                    } else {
                        val config = parseConfig(it, subid, subItem)
                        if (config != null) {
                            configs.add(config)
                        }
                    }
                }

            val v2raynConfigs = V2rayNFmt.parse(v2raynLines, subid)
            val allConfigs = v2raynConfigs + configs

            if (allConfigs.isNotEmpty()) {
                commitProfiles(
                    configs = allConfigs.map(::ParsedProfile),
                    subid = subid,
                    append = append,
                )
            }

            return allConfigs.size
        } catch (e: ProfileStorageException) {
            throw e
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to parse batch config", e)
        }
        return 0
    }

    /**
     * Commits parsed profiles before removing the profiles they replace.
     *
     * @param configs The parsed profiles to save.
     * @param subid The subscription ID.
     * @param append Whether to append to the existing server list.
     */
    private fun commitProfiles(
        configs: List<ParsedProfile>,
        subid: String,
        append: Boolean,
    ) {
        val keyToProfile = linkedMapOf<String, ProfileItem>()
        val rawConfigs = mutableMapOf<String, String>()

        configs.forEach { parsed ->
            val key = Utils.getUuid()
            keyToProfile[key] = parsed.profile
            parsed.rawConfig?.let { raw -> rawConfigs[key] = raw }
        }

        MmkvManager.saveServerProfiles(
            profiles = keyToProfile,
            rawConfigs = rawConfigs,
            subscriptionId = subid,
            append = append,
        )
    }

    /**
     * Parses a custom configuration server.
     *
     * @param server The server string.
     * @param subid The subscription ID.
     * @param append Whether to append the configurations.
     * @return The number of configurations parsed.
     */
    private fun parseCustomConfigServer(server: String?, subid: String, append: Boolean): Int {
        if (server == null) {
            return 0
        }
        if (server.contains("inbounds")
            && server.contains("outbounds")
            && server.contains("routing")
        ) {
            try {
                val serverList: Array<Any> =
                    JsonUtil.fromJson(server, Array<Any>::class.java) ?: arrayOf()

                if (serverList.isNotEmpty()) {
                    val configs = serverList.reversed().map { srv ->
                        val config = CustomFmt.parse(JsonUtil.toJson(srv))
                        config.subscriptionId = subid
                        config.description = generateDescription(config)
                        ParsedProfile(
                            profile = config,
                            rawConfig = JsonUtil.toJsonPretty(srv) ?: "",
                        )
                    }
                    commitProfiles(configs, subid, append)
                    return configs.size
                }
            } catch (e: ProfileStorageException) {
                throw e
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to parse custom config server JSON array", e)
            }

            try {
                // For compatibility
                val config = CustomFmt.parse(server)
                config.subscriptionId = subid
                config.description = generateDescription(config)
                commitProfiles(
                    configs = listOf(ParsedProfile(config, server)),
                    subid = subid,
                    append = append,
                )
                return 1
            } catch (e: ProfileStorageException) {
                throw e
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to parse custom config server as single config", e)
            }
            return 0
        } else if (!server.contains("outbounds") && server.contains("protocol")) {
            try {
                val (config, rawOutbound) = CustomOutboundFmt.parse(server)
                config.subscriptionId = subid
                config.description = generateDescription(config)
                commitProfiles(
                    configs = listOf(ParsedProfile(config, rawOutbound)),
                    subid = subid,
                    append = append,
                )
                return 1
            } catch (e: ProfileStorageException) {
                throw e
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to parse custom outbound", e)
            }
            return 0
        } else if (server.startsWith("[Interface]") && server.contains("[Peer]")) {
            try {
                val config = WireguardFmt.parseWireguardConfFile(server)
                config.subscriptionId = subid
                config.description = generateDescription(config)
                commitProfiles(
                    configs = listOf(ParsedProfile(config, server)),
                    subid = subid,
                    append = append,
                )
                return 1
            } catch (e: ProfileStorageException) {
                throw e
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to parse WireGuard config file", e)
            }
            return 0
        } else {
            return 0
        }
    }

    /**
     * Parses the configuration from a QR code or string.
     * Only parses and returns ProfileItem, does not save.
     *
     * @param str The configuration string.
     * @param subid The subscription ID.
     * @param subItem The subscription item.
     * @return The parsed ProfileItem or null if parsing fails or filtered out.
     */
    private fun parseConfig(
        str: String?,
        subid: String,
        subItem: SubscriptionItem?
    ): ProfileItem? {
        try {
            if (str == null || TextUtils.isEmpty(str)) {
                return null
            }

            val config = configFmtParsers.firstNotNullOfOrNull { (scheme, parser) ->
                if (str.startsWith(scheme)) parser(str) else null
            }

            if (config == null) {
                return null
            }

            // Apply filter
            if (subItem?.filter.isNotNullEmpty() && config.remarks.isNotNullEmpty()) {
                val matched = Regex(pattern = subItem?.filter.orEmpty())
                    .containsMatchIn(input = config.remarks)
                if (!matched) return null
            }

            config.subscriptionId = subid
            config.description = generateDescription(config)

            return config
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to parse config", e)
            return null
        }
    }

    /**
     * Updates the configuration via all subscriptions.
     *
     * @return Detailed result of the subscription update operation.
     */
    fun updateConfigViaSubAll(): SubscriptionUpdateResult {
        return try {
            val subscriptions = MmkvManager.decodeSubscriptions()
            subscriptions.fold(SubscriptionUpdateResult()) { acc, subscription ->
                acc + updateConfigViaSub(subscription)
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to update config via all subscriptions", e)
            SubscriptionUpdateResult()
        }
    }

    /**
     * Updates the configuration via a subscription.
     *
     * @param it The subscription item.
     * @return Subscription update result.
     */
    fun updateConfigViaSub(it: SubscriptionCache): SubscriptionUpdateResult {
        try {
            // Check if disabled
            if (!it.subscription.enabled) {
                return SubscriptionUpdateResult(skipCount = 1)
            }

            // Validate subscription info
            if (TextUtils.isEmpty(it.guid)
                || TextUtils.isEmpty(it.subscription.remarks)
                || TextUtils.isEmpty(it.subscription.url)
            ) {
                return SubscriptionUpdateResult(skipCount = 1)
            }

            val url = HttpUtil.toIdnUrl(it.subscription.url.trim())
            if (!Utils.isValidUrl(url)) {
                return SubscriptionUpdateResult(failureCount = 1)
            }
            if (!it.subscription.allowInsecureUrl) {
                if (!Utils.isValidSubUrl(url)) {
                    return SubscriptionUpdateResult(failureCount = 1)
                }
            }
            LogUtil.i(AppConfig.TAG, url)
            val userAgent = it.subscription.userAgent
            val requestHeaders = it.subscription.requestHeaders
            val proxyUsername = SettingsManager.getSocksUsername()
            val proxyPassword = SettingsManager.getSocksPassword()

            // Account info (traffic, expiry, support link) the panel sends in the response headers.
            var usageInfo: SubscriptionUsageInfo? = null
            val captureUsageInfo: (Headers) -> Unit = { headers ->
                usageInfo = SubscriptionInfoParser.parse(
                    userInfo = headers["Subscription-Userinfo"],
                    profileTitle = headers["Profile-Title"],
                    supportUrl = headers["Support-Url"],
                    webPageUrl = headers["Profile-Web-Page-Url"],
                )
            }

            var configText = ""
            val candidateUrls = getSubscriptionMirrors(url)

            for (candidateUrl in candidateUrls) {
                if (CoreServiceManager.isRunning()) {
                    configText = try {
                        val httpPort = SettingsManager.getHttpPort()
                        HttpUtil.getUrlContentWithUserAgent(
                            UrlContentRequest(
                                url = candidateUrl,
                                userAgent = userAgent,
                                requestHeaders = requestHeaders,
                                timeout = 8000,
                                httpPort = httpPort,
                                proxyUsername = proxyUsername,
                                proxyPassword = proxyPassword
                            ),
                            captureUsageInfo
                        )
                    } catch (e: Exception) {
                        LogUtil.e(AppConfig.ANG_PACKAGE, "Update subscription: proxy attempt failed for $candidateUrl", e)
                        ""
                    }
                }

                if (configText.isEmpty()) {
                    configText = try {
                        HttpUtil.getUrlContentWithUserAgent(
                            UrlContentRequest(
                                url = candidateUrl,
                                userAgent = userAgent,
                                requestHeaders = requestHeaders,
                                timeout = 10000
                            ),
                            captureUsageInfo
                        )
                    } catch (e: Exception) {
                        LogUtil.e(AppConfig.TAG, "Update subscription: direct attempt failed for $candidateUrl", e)
                        ""
                    }
                }

                if (configText.isNotEmpty()) {
                    LogUtil.i(AppConfig.TAG, "Successfully downloaded subscription from mirror: $candidateUrl")
                    break
                }
            }
            if (configText.isEmpty()) {
                return SubscriptionUpdateResult(failureCount = 1)
            }

            val count = parseConfigViaSub(configText, it.guid, false)
            if (count > 0) {
                try {
                    ensureBunkerAutoPolicyGroup(it.guid)
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "ensureBunkerAutoPolicyGroup failed", e)
                }
                it.subscription.lastUpdated = System.currentTimeMillis()
                // Replaced on every successful update so the box disappears if the panel stops sending the headers.
                it.subscription.usageInfo = usageInfo
                MmkvManager.encodeSubscription(it.guid, it.subscription)
                LogUtil.i(AppConfig.TAG, "Subscription updated: ${it.subscription.remarks}, $count configs")
                return SubscriptionUpdateResult(
                    configCount = count,
                    successCount = 1
                )
            } else {
                // Got response but no valid configs parsed
                return SubscriptionUpdateResult(failureCount = 1)
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to update config via subscription", e)
            return SubscriptionUpdateResult(failureCount = 1)
        }
    }

    /**
     * Ensures there is a single dynamic auto-select POLICYGROUP node for the subscription.
     * Links all subscription servers to Xray's leastPing balancer with observatory.
     * NEVER deletes normal servers. Only removes duplicate POLICYGROUP nodes if any.
     *
     * @return true if any modifications to profiles or server list were made.
     */
    fun ensureBunkerAutoPolicyGroup(subId: String): Boolean {
        if (subId.isEmpty()) return false
        try {
            val serverGuids = MmkvManager.decodeServerList(subId).toMutableList()
            if (serverGuids.isEmpty()) return false

            var modified = false
            var existingPolicyGroupGuid: String? = null
            val duplicatePolicyGroups = mutableListOf<String>()

            for (guid in serverGuids) {
                val profile = MmkvManager.decodeServerConfig(guid) ?: continue
                if (profile.configType == EConfigType.POLICYGROUP) {
                    if (existingPolicyGroupGuid == null) {
                        existingPolicyGroupGuid = guid
                    } else {
                        duplicatePolicyGroups.add(guid)
                    }
                }
            }

            if (duplicatePolicyGroups.isNotEmpty()) {
                MmkvManager.removeServers(duplicatePolicyGroups, subId)
                serverGuids.removeAll(duplicatePolicyGroups.toSet())
                modified = true
            }

            val normalServers = serverGuids.filter { guid ->
                val p = MmkvManager.decodeServerConfig(guid)
                p != null && !p.configType.isComplexType()
            }
            if (normalServers.size < 2) {
                // No need for balancer if fewer than 2 normal servers
                return modified
            }

            val policyProfile = if (existingPolicyGroupGuid != null) {
                MmkvManager.decodeServerConfig(existingPolicyGroupGuid) ?: ProfileItem.create(EConfigType.POLICYGROUP)
            } else {
                ProfileItem.create(EConfigType.POLICYGROUP)
            }

            val targetRemarks = "⚡ АВТОВЫБОР | BUNKER ⚡"
            if (policyProfile.remarks != targetRemarks ||
                policyProfile.policyGroupSubscriptionId != subId ||
                policyProfile.policyGroupType != "0" ||
                policyProfile.policyGroupFilter != ""
            ) {
                policyProfile.remarks = targetRemarks
                policyProfile.subscriptionId = subId
                policyProfile.policyGroupSubscriptionId = subId
                policyProfile.policyGroupFilter = ""
                policyProfile.policyGroupType = "0" // leastPing
                policyProfile.policyGroupTestOutbounds = true
                policyProfile.policyGroupFallbackTag = ""
                policyProfile.description = "⚡ Автовыбор лучшего сервера (наименьший пинг)"
                modified = true
            }

            val finalGuid = MmkvManager.encodeServerConfig(existingPolicyGroupGuid ?: "", policyProfile)

            val updatedList = MmkvManager.decodeServerList(subId).toMutableList()
            if (updatedList.firstOrNull() != finalGuid) {
                updatedList.remove(finalGuid)
                updatedList.add(0, finalGuid)
                MmkvManager.encodeServerList(updatedList, subId)
                modified = true
            }

            val currentSelected = MmkvManager.getSelectServer()
            if (currentSelected.isNullOrEmpty() || currentSelected in duplicatePolicyGroups) {
                MmkvManager.setSelectServer(finalGuid)
                modified = true
            }

            return modified
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "ensureBunkerAutoPolicyGroup failed", e)
            return false
        }
    }

    /**
     * Removes invalid server configurations for a subscription.
     *
     * @param subId The subscription ID.
     */
    fun removeInvalidServer(subId: String) {
        val serverList = MmkvManager.decodeServerList(subId)
        val invalidServers = serverList.filter {
            val aff = MmkvManager.decodeServerAffiliationInfo(it)
            aff != null && aff.testDelayMillis < 0L
        }
        MmkvManager.removeServers(invalidServers, subId)
    }

    /**
     * Sorts servers by test results for a subscription.
     *
     * @param subId The subscription ID.
     */
    fun sortByTestResultsForSub(subId: String) {
        val serverList = MmkvManager.decodeServerList(subId)
        if (serverList.isEmpty()) return

        val sorted = serverList
            .map { guid ->
                val profile = MmkvManager.decodeServerConfig(guid)
                val isPolicyGroup = profile?.configType == EConfigType.POLICYGROUP
                val delay =
                    MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis ?: 0L
                val sortKey = when {
                    isPolicyGroup -> -1L // Always keep auto-select balancer at the top
                    delay <= 0L -> Long.MAX_VALUE // Offline/untested at bottom
                    else -> delay
                }
                guid to sortKey
            }
            .sortedBy { it.second }
            .map { it.first }
            .toMutableList()
        MmkvManager.encodeServerList(sorted, subId)
    }

    /**
     * Parses the configuration via a subscription.
     *
     * @param server The server string.
     * @param subid The subscription ID.
     * @param append Whether to append the configurations.
     * @return The number of configurations parsed.
     */
    private fun parseConfigViaSub(server: String?, subid: String, append: Boolean): Int {
        if (server.isNullOrBlank()) return 0
        val decoded = Utils.decode(server)
        var count = 0
        if (decoded.isNotBlank()) {
            count = parseBatchConfig(decoded, subid, append)
        }
        if (count <= 0) {
            count = parseBatchConfig(server, subid, append)
        }
        if (count <= 0) {
            count = parseCustomConfigServer(server, subid, append)
        }
        if (count <= 0) {
            val extracted = BunkerUniversalImporter.extractConfigs(server)
            if (extracted.isNotEmpty()) {
                count = parseBatchConfig(extracted.joinToString("\n"), subid, append)
            }
        }
        return count
    }

    /**
     * Imports a URL as a subscription.
     *
     * @param url The URL.
     * @return The number of subscriptions imported.
     */
    private fun importUrlAsSubscription(url: String): Int {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return 0

        val subscriptions = MmkvManager.decodeSubscriptions()
        val existing = subscriptions.firstOrNull { it.subscription.url.trim() == cleanUrl }
        if (existing != null) {
            existing.subscription.lastUpdated = 0
            existing.subscription.allowInsecureUrl = true
            MmkvManager.encodeSubscription(existing.guid, existing.subscription)
            return 1
        }

        val subRemarks = runCatching {
            val uri = java.net.URI(Utils.fixIllegalUrl(cleanUrl))
            uri.fragment?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: cleanUrl.substringAfterLast('#', "").takeIf { it.isNotBlank() } ?: "Бункер Подписка"

        val subItem = SubscriptionItem().apply {
            remarks = subRemarks
            this.url = cleanUrl
            allowInsecureUrl = true
            enabled = true
            autoUpdate = true
        }
        MmkvManager.encodeSubscription("", subItem)
        return 1
    }

    /** Generates a description for the profile.
     *
     * @param profile The profile item.
     * @return The generated description.
     */
    fun generateDescription(profile: ProfileItem): String {
        // Hide xxx:xxx:***/xxx.xxx.xxx.***
        val server = profile.server
        val port = profile.serverPort
        if (server.isNullOrBlank() && port.isNullOrBlank()) return ""

        val addrPart = server?.let {
            if (it.contains(":"))
                it.split(":").take(2).joinToString(":", postfix = ":***")
            else
                it.split('.').dropLast(1).joinToString(".", postfix = ".***")
        } ?: ""

        return "$addrPart : ${port ?: ""}"
    }

    /**
     * Resolves a cascade of reliable mirror URLs for a subscription to guarantee 100% delivery.
     */
    fun getSubscriptionMirrors(primaryUrl: String): List<String> {
        val mirrors = mutableListOf<String>()
        val cleanUrl = primaryUrl.trim()
        mirrors.add(cleanUrl)

        val bunkerSubFiles = listOf(
            "mix_b64.txt", "bs_b64.txt", "factory_b64.txt",
            "fast_b64.txt", "tg_b64.txt", "discord_b64.txt",
            "warp_masque_b64.txt", "warp_b64.txt",
            "mix.txt", "bs.txt", "factory.txt", "fast.txt", "tg.txt", "discord.txt"
        )
        val matchedFile = bunkerSubFiles.firstOrNull { cleanUrl.contains(it) }

        if (matchedFile != null || cleanUrl.contains("Bunker-Auto-Engine")) {
            val fileName = matchedFile ?: cleanUrl.substringAfterLast('/')
            val ts = System.currentTimeMillis()
            val candidateMirrors = listOf(
                "https://ghfast.top/https://raw.githubusercontent.com/ANT1VEN0M/Bunker-Auto-Engine/main/sub/$fileName",
                "https://fastly.jsdelivr.net/gh/ANT1VEN0M/Bunker-Auto-Engine@main/sub/$fileName?t=$ts",
                "https://cdn.jsdelivr.net/gh/ANT1VEN0M/Bunker-Auto-Engine@main/sub/$fileName?t=$ts",
                "https://ghproxy.net/https://raw.githubusercontent.com/ANT1VEN0M/Bunker-Auto-Engine/main/sub/$fileName",
                "https://raw.githubusercontent.com/ANT1VEN0M/Bunker-Auto-Engine/main/sub/$fileName"
            )
            for (m in candidateMirrors) {
                if (!mirrors.contains(m)) {
                    mirrors.add(m)
                }
            }
        } else if (cleanUrl.contains("raw.githubusercontent.com")) {
            val ghFast = if (cleanUrl.startsWith("https://ghfast.top/")) cleanUrl else "https://ghfast.top/$cleanUrl"
            val ghProxy = if (cleanUrl.startsWith("https://ghproxy.net/")) cleanUrl else "https://ghproxy.net/$cleanUrl"
            if (!mirrors.contains(ghFast)) {
                mirrors.add(0, ghFast)
            }
            if (!mirrors.contains(ghProxy)) {
                mirrors.add(ghProxy)
            }
        }

        return mirrors
    }
}
