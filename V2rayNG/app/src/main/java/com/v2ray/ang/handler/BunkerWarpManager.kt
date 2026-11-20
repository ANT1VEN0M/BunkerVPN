package com.v2ray.ang.handler

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.security.KeyPairGenerator
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object BunkerWarpManager {

    private val EMBEDDED_WARP_POOL = listOf(
        Triple("OAQNqqSYW896A5fpOp26BtmIUCv6mz4aR19RPX2mWGo=", "1,255,117", "162.159.192.1"),
        Triple("AENE4iNCD/eY6UCLVHfCFXLb7bpStzpFi/3Q+EKU+0E=", "47,231,208", "162.159.193.1"),
        Triple("ABmxmVxLkiR59vGlb2/W2kEOY3T4y2PgywkNrPrOf1g=", "87,188,150", "162.159.195.1"),
        Triple("ODaJIHDFprxr6xNwbplneqIegMpjtbIME7gPTztAvkE=", "214,190,145", "188.114.96.1"),
        Triple("CL2Rkq2AjFpy4FbQVmbJOz/ERr1kEzQZDgEg1dkCpn4=", "93,159,246", "188.114.97.1"),
        Triple("8OsHjfdv8YYCzY66JeL7vTE4ZCpPl4BX4Zq+dbznyFU=", "160,98,123", "162.159.192.2")
    )

    suspend fun generateWarpConfig(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Generate standard X25519 keypair
            val kpg = KeyPairGenerator.getInstance("X25519")
            val kp = kpg.generateKeyPair()
            val pubBytes = kp.public.encoded
            val privBytes = kp.private.encoded

            val rawPub = ByteArray(32)
            val rawPriv = ByteArray(32)
            System.arraycopy(pubBytes, pubBytes.size - 32, rawPub, 0, 32)
            System.arraycopy(privBytes, privBytes.size - 32, rawPriv, 0, 32)

            val pubB64 = Base64.getEncoder().encodeToString(rawPub)
            val privB64 = Base64.getEncoder().encodeToString(rawPriv)

            // 2. Register via Cloudflare API
            val regUrl = URL("https://api.cloudflareclient.com/v0a2158/reg")
            
            // If BunkerVPN is active, route through internal SOCKS5 proxy (127.0.0.1:10808)
            val proxy = if (CoreServiceManager.isRunning()) {
                try {
                    Proxy(Proxy.Type.SOCKS, InetSocketAddress(AppConfig.LOOPBACK, AppConfig.PORT_SOCKS.toInt()))
                } catch (e: Exception) {
                    Proxy.NO_PROXY
                }
            } else {
                Proxy.NO_PROXY
            }

            val conn = try {
                (regUrl.openConnection(proxy) as HttpURLConnection)
            } catch (e: Exception) {
                (regUrl.openConnection(Proxy.NO_PROXY) as HttpURLConnection)
            }.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("User-Agent", "okhttp/3.12.1")
                connectTimeout = 6000
                readTimeout = 6000
                doOutput = true
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val nowIso = sdf.format(Date())

            val reqObj = JSONObject().apply {
                put("key", pubB64)
                put("install_id", "")
                put("fcm_token", "")
                put("tos", nowIso)
                put("model", "Android")
                put("serial_number", "")
                put("locale", "ru_RU")
            }

            OutputStreamWriter(conn.outputStream).use { it.write(reqObj.toString()) }

            if (conn.responseCode !in 200..299) {
                LogUtil.w(AppConfig.TAG, "WARP API status ${conn.responseCode}, switching to cloud pool")
                return@withContext fetchFallbackWarpFromPool()
            }

            val respText = conn.inputStream.bufferedReader().use { it.readText() }
            val respJson = JSONObject(respText)

            val configObj = respJson.getJSONObject("config")
            
            // Extract client_id (3 reserved bytes required by Cloudflare WireGuard handshake)
            val clientId = configObj.optString("client_id", "")
            val reservedBytes = if (clientId.isNotEmpty()) {
                try {
                    val dec = Base64.getDecoder().decode(clientId)
                    dec.map { it.toInt() and 0xFF }.joinToString(",")
                } catch (e: Exception) {
                    ""
                }
            } else ""

            val peersArr = configObj.getJSONArray("peers")
            val peer0 = peersArr.getJSONObject(0)
            val peerPubKey = peer0.getString("public_key")
            
            // Port 880 bypasses Russian TSPU WireGuard blocks with ultra-low latency
            val serverIp = "162.159.192.1"
            val serverPort = "880"

            val interfaceObj = configObj.getJSONObject("interface")
            val addressesObj = interfaceObj.getJSONObject("addresses")
            val v4 = addressesObj.optString("v4", "172.16.0.2")
            val v6 = addressesObj.optString("v6", "")
            val localAddress = if (v6.isNotEmpty()) "$v4/32,$v6/128" else "$v4/32"

            // 3. Create WireGuard profile item
            val profile = ProfileItem.create(EConfigType.WIREGUARD).apply {
                remarks = "Cloudflare WARP+ (880)"
                server = serverIp
                this.serverPort = serverPort
                secretKey = privB64
                publicKey = peerPubKey
                this.localAddress = localAddress
                this.reserved = reservedBytes
                mtu = 1280
                remoteDNS = "77.88.8.8,1.1.1.1"
            }

            // 4. Save to MMKV
            val guid = MmkvManager.encodeServerConfig("", profile)
            if (guid.isNotEmpty()) {
                MmkvManager.setSelectServer(guid)
                return@withContext true
            }
            fetchFallbackWarpFromPool()
        } catch (e: Exception) {
            LogUtil.w(AppConfig.TAG, "WARP API error, falling back to cloud pool: ${e.message}")
            fetchFallbackWarpFromPool()
        }
    }

    private fun fetchFallbackWarpFromPool(): Boolean {
        val urls = listOf(
            "https://ghfast.top/https://raw.githubusercontent.com/ANT1VEN0M/Bunker-Auto-Engine/main/sub/warp_pool.json",
            "https://raw.gitmirror.com/ANT1VEN0M/Bunker-Auto-Engine/main/sub/warp_pool.json",
            "https://fastly.jsdelivr.net/gh/ANT1VEN0M/Bunker-Auto-Engine@latest/sub/warp_pool.json",
            "https://cdn.jsdelivr.net/gh/ANT1VEN0M/Bunker-Auto-Engine@latest/sub/warp_pool.json",
            "https://raw.githubusercontent.com/ANT1VEN0M/Bunker-Auto-Engine/main/sub/warp_pool.json"
        )
        for (u in urls) {
            try {
                val conn = (URL(u).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "BunkerVPN/1.3.9")
                }
                if (conn.responseCode in 200..299) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONArray(text)
                    if (arr.length() > 0) {
                        val idx = (0 until arr.length()).random()
                        val item = arr.getJSONObject(idx)
                        val profile = ProfileItem.create(EConfigType.WIREGUARD).apply {
                            remarks = "Cloudflare WARP+ (880)"
                            server = item.optString("server", "162.159.192.1")
                            serverPort = item.optString("server_port", "880")
                            secretKey = item.getString("private_key")
                            publicKey = item.getString("public_key")
                            localAddress = item.optString("local_address", "172.16.0.2/32")
                            reserved = item.optString("reserved", "0,0,0")
                            mtu = item.optInt("mtu", 1280)
                            remoteDNS = item.optString("remote_dns", "77.88.8.8,1.1.1.1")
                        }
                        val guid = MmkvManager.encodeServerConfig("", profile)
                        if (guid.isNotEmpty()) {
                            MmkvManager.setSelectServer(guid)
                            LogUtil.i(AppConfig.TAG, "Successfully acquired cloud pool WARP profile from $u")
                            return true
                        }
                    }
                }
            } catch (e: Exception) {
                LogUtil.w(AppConfig.TAG, "Failed to load warp from $u: ${e.message}")
            }
        }
        return applyEmbeddedWarpProfile()
    }

    private fun applyEmbeddedWarpProfile(): Boolean {
        return try {
            val selected = EMBEDDED_WARP_POOL.random()
            val profile = ProfileItem.create(EConfigType.WIREGUARD).apply {
                remarks = "Cloudflare WARP+ (880)"
                server = selected.third
                serverPort = "880"
                secretKey = selected.first
                publicKey = "bmXOC+F1FxEMF9dyiK2H5/1SUtzH0JuVo51h2wPfgyo="
                localAddress = "172.16.0.2/32"
                reserved = selected.second
                mtu = 1280
                remoteDNS = "77.88.8.8,1.1.1.1"
            }
            val guid = MmkvManager.encodeServerConfig("", profile)
            if (guid.isNotEmpty()) {
                MmkvManager.setSelectServer(guid)
                LogUtil.i(AppConfig.TAG, "Successfully acquired embedded offline WARP profile")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "applyEmbeddedWarpProfile error", e)
            false
        }
    }
}
