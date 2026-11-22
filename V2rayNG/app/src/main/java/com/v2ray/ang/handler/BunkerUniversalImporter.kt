package com.v2ray.ang.handler

import android.util.Base64
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object BunkerUniversalImporter {

    private val PROTOCOL_URI_REGEX = Regex(
        "(?:vless|vmess|trojan|ss|ssr|hysteria2|hy2|wireguard|wg|socks|socks5|tuic)://[^\\s<>\"']+",
        RegexOption.IGNORE_CASE
    )

    private val SUB_URL_REGEX = Regex(
        "https?://[^\\s<>\"']+(?:sub|subs|subscribe|/all|\\.txt|\\.yaml|\\.json|raw\\.githubusercontent\\.com|codeberg\\.org|pastebin\\.com|jsdelivr\\.net)[^\\s<>\"']*",
        RegexOption.IGNORE_CASE
    )

    /**
     * Extracts all valid proxy config links from arbitrary raw input (text, base64, json, wrappers).
     */
    fun extractConfigs(rawInput: String?): List<String> {
        if (rawInput.isNullOrBlank()) return emptyList()

        val results = linkedSetOf<String>()
        val candidates = mutableListOf<String>()
        candidates.add(rawInput.trim())

        // 1. Recursive Base64 unwrap up to 4 levels
        var current = rawInput.trim()
        for (i in 0 until 4) {
            val unwrapped = tryUnwrapBase64(current) ?: break
            if (unwrapped.isNotBlank() && unwrapped != current) {
                candidates.add(unwrapped)
                current = unwrapped
            } else {
                break
            }
        }

        // 2. Scan each candidate string for protocol URIs and wrappers
        for (text in candidates) {
            // Direct protocol regex match
            PROTOCOL_URI_REGEX.findAll(text).forEach { match ->
                val uri = cleanUri(match.value)
                if (uri.isNotBlank()) results.add(uri)
            }

            // V2RayTun deep links: v2raytun://import/<base64>
            if (text.contains("v2raytun://import/", ignoreCase = true)) {
                val matches = Regex("v2raytun://import/([^\\s<>\"']+)", RegexOption.IGNORE_CASE).findAll(text)
                for (m in matches) {
                    val payload = m.groupValues[1]
                    val decoded = safeBase64Decode(payload)
                    if (decoded.isNotBlank()) {
                        extractConfigs(decoded).forEach { results.add(it) }
                    }
                }
            }

            // INCY deep links: incy://crypt1/<base64>
            if (text.contains("incy://", ignoreCase = true)) {
                val matches = Regex("incy://[^/]+/([^\\s<>\"']+)", RegexOption.IGNORE_CASE).findAll(text)
                for (m in matches) {
                    val payload = m.groupValues[1]
                    val decoded = safeBase64Decode(payload)
                    if (decoded.isNotBlank()) {
                        extractConfigs(decoded).forEach { results.add(it) }
                    }
                }
            }

            // Happ links: happ://crypt...
            if (text.contains("happ://", ignoreCase = true)) {
                val matches = Regex("happ://[^\\s<>\"']+", RegexOption.IGNORE_CASE).findAll(text)
                for (m in matches) {
                    val happLink = m.value
                    val unwrapped = unwrapHappLink(happLink)
                    if (unwrapped.isNotBlank()) {
                        extractConfigs(unwrapped).forEach { results.add(it) }
                    }
                }
            }

            // NapsternetV open format: NPVO1<base64>
            if (text.contains("NPVO1")) {
                val idx = text.indexOf("NPVO1")
                val token = text.substring(idx + 5).trim().takeWhile { !it.isWhitespace() }
                val decoded = safeBase64Decode(token)
                if (decoded.isNotBlank()) {
                    parseNapsternetJson(decoded)?.let { results.add(it) }
                }
            }

            // Sing-box JSON / Xray JSON configurations containing outbounds
            if (text.contains("\"outbounds\"")) {
                parseSingboxOutbounds(text).forEach { results.add(it) }
            }
        }

        return results.toList()
    }

    /**
     * Extracts candidate subscription URLs from raw text.
     */
    fun extractSubscriptionUrls(rawInput: String?): List<String> {
        if (rawInput.isNullOrBlank()) return emptyList()

        val results = linkedSetOf<String>()
        SUB_URL_REGEX.findAll(rawInput).forEach { match ->
            val u = match.value.trim().trimEnd(',', ';', ')', ']', '}', '\'', '"')
            if (u.startsWith("http://") || u.startsWith("https://")) {
                results.add(u)
            }
        }
        return results.toList()
    }

    private fun cleanUri(uri: String): String {
        return uri.trim().trimEnd(',', ';', ')', ']', '}', '\'', '"')
    }

    private fun tryUnwrapBase64(input: String): String? {
        val clean = input.trim()
        if (clean.length < 8) return null

        // If it starts with a protocol scheme, don't base64 unwrap the whole string
        if (clean.startsWith("vless://") || clean.startsWith("vmess://") ||
            clean.startsWith("trojan://") || clean.startsWith("ss://") ||
            clean.startsWith("hysteria2://") || clean.startsWith("wireguard://")
        ) {
            return null
        }

        return try {
            val flags = Base64.DEFAULT
            val decodedBytes = Base64.decode(clean, flags)
            val decodedStr = String(decodedBytes, StandardCharsets.UTF_8).trim()
            if (decodedStr.isNotBlank() && decodedStr.any { !it.isISOControl() }) {
                decodedStr
            } else {
                null
            }
        } catch (e: Exception) {
            try {
                val decodedBytes = Base64.decode(clean, Base64.URL_SAFE)
                val decodedStr = String(decodedBytes, StandardCharsets.UTF_8).trim()
                if (decodedStr.isNotBlank() && decodedStr.any { !it.isISOControl() }) {
                    decodedStr
                } else {
                    null
                }
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun safeBase64Decode(str: String): String {
        return try {
            val decoded = Base64.decode(str.trim(), Base64.DEFAULT)
            String(decoded, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            try {
                val decoded = Base64.decode(str.trim(), Base64.URL_SAFE)
                String(decoded, StandardCharsets.UTF_8)
            } catch (e2: Exception) {
                ""
            }
        }
    }

    private fun unwrapHappLink(happLink: String): String {
        return try {
            val payload = happLink.substringAfter("://").substringAfter('/')
            val decoded = safeBase64Decode(payload)
            if (decoded.isNotBlank()) decoded else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseNapsternetJson(jsonText: String): String? {
        return try {
            val json = JSONObject(jsonText)
            val server = json.optString("serverAddress", json.optString("host", ""))
            val port = json.optInt("port", 0)
            val id = json.optString("id", json.optString("uuid", json.optString("password", "")))
            val name = json.optString("name", "NapsternetV")
            val proto = json.optString("protocol", "vless").lowercase()

            if (server.isBlank() || port <= 0 || id.isBlank()) return null

            val encName = URLEncoder.encode(name, "UTF-8")
            when (proto) {
                "vless" -> "vless://$id@$server:$port?encryption=none&security=none#$encName"
                "trojan" -> "trojan://$id@$server:$port?security=tls#$encName"
                "shadowsocks", "ss" -> {
                    val method = json.optString("method", "aes-256-gcm")
                    val authB64 = Base64.encodeToString("$method:$id".toByteArray(), Base64.NO_WRAP)
                    "ss://$authB64@$server:$port#$encName"
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSingboxOutbounds(jsonText: String): List<String> {
        val outLinks = mutableListOf<String>()
        try {
            val root = JSONObject(jsonText)
            val outbounds = root.optJSONArray("outbounds") ?: return emptyList()

            for (i in 0 until outbounds.length()) {
                val ob = outbounds.optJSONObject(i) ?: continue
                val type = ob.optString("type", "").lowercase()
                val tag = ob.optString("tag", "Bunker Server")
                val encTag = URLEncoder.encode(tag, "UTF-8")
                val server = ob.optString("server", "")
                val serverPort = ob.optInt("server_port", 0)

                if (server.isBlank() || serverPort <= 0) continue

                when (type) {
                    "vless" -> {
                        val uuid = ob.optString("uuid", "")
                        if (uuid.isBlank()) continue
                        val flow = ob.optString("flow", "")
                        val tls = ob.optJSONObject("tls")
                        var security = "none"
                        var sni = ""
                        var pbk = ""
                        var sid = ""
                        var fp = ""

                        if (tls != null && tls.optBoolean("enabled", false)) {
                            sni = tls.optString("server_name", "")
                            val reality = tls.optJSONObject("reality")
                            if (reality != null && reality.optBoolean("enabled", false)) {
                                security = "reality"
                                pbk = reality.optString("public_key", "")
                                sid = reality.optString("short_id", "")
                            } else {
                                security = "tls"
                            }
                            val utls = tls.optJSONObject("utls")
                            if (utls != null && utls.optBoolean("enabled", false)) {
                                fp = utls.optString("fingerprint", "chrome")
                            }
                        }

                        val transport = ob.optJSONObject("transport")
                        val netType = transport?.optString("type", "tcp") ?: "tcp"
                        val path = transport?.optString("path", "") ?: ""
                        val host = transport?.optJSONObject("headers")?.optString("Host", "") ?: ""

                        val q = mutableListOf<String>()
                        if (netType != "tcp") q.add("type=$netType")
                        if (security != "none") q.add("security=$security")
                        if (flow.isNotBlank()) q.add("flow=$flow")
                        if (sni.isNotBlank()) q.add("sni=$sni")
                        if (pbk.isNotBlank()) q.add("pbk=$pbk")
                        if (sid.isNotBlank()) q.add("sid=$sid")
                        if (fp.isNotBlank()) q.add("fp=$fp")
                        if (path.isNotBlank()) q.add("path=${URLEncoder.encode(path, "UTF-8")}")
                        if (host.isNotBlank()) q.add("host=${URLEncoder.encode(host, "UTF-8")}")

                        val queryStr = if (q.isNotEmpty()) "?" + q.joinToString("&") else ""
                        outLinks.add("vless://$uuid@$server:$serverPort$queryStr#$encTag")
                    }

                    "trojan" -> {
                        val password = ob.optString("password", "")
                        if (password.isBlank()) continue
                        val tls = ob.optJSONObject("tls")
                        val sni = tls?.optString("server_name", "") ?: ""
                        val q = mutableListOf("security=tls")
                        if (sni.isNotBlank()) q.add("sni=$sni")
                        outLinks.add("trojan://$password@$server:$serverPort?${q.joinToString("&")}#$encTag")
                    }

                    "shadowsocks" -> {
                        val method = ob.optString("method", "aes-256-gcm")
                        val password = ob.optString("password", "")
                        if (password.isBlank()) continue
                        val auth = Base64.encodeToString("$method:$password".toByteArray(), Base64.NO_WRAP)
                        outLinks.add("ss://$auth@$server:$serverPort#$encTag")
                    }

                    "hysteria2" -> {
                        val password = ob.optString("password", "")
                        val tls = ob.optJSONObject("tls")
                        val sni = tls?.optString("server_name", "") ?: ""
                        val insecure = if (tls?.optBoolean("insecure", false) == true) "1" else "0"
                        val q = mutableListOf<String>()
                        if (sni.isNotBlank()) q.add("sni=$sni")
                        if (insecure == "1") q.add("insecure=1")
                        val obfs = ob.optJSONObject("obfs")
                        if (obfs != null) {
                            val obfsType = obfs.optString("type", "")
                            val obfsPass = obfs.optString("password", "")
                            if (obfsType.isNotBlank()) q.add("obfs=$obfsType")
                            if (obfsPass.isNotBlank()) q.add("obfs-password=$obfsPass")
                        }
                        val queryStr = if (q.isNotEmpty()) "?" + q.joinToString("&") else ""
                        outLinks.add("hysteria2://$password@$server:$serverPort$queryStr#$encTag")
                    }
                }
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "parseSingboxOutbounds error", e)
        }
        return outLinks
    }
}
