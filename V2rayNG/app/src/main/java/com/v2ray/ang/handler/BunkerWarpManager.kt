package com.v2ray.ang.handler

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyPairGenerator
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object BunkerWarpManager {

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
            val conn = (regUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("User-Agent", "okhttp/3.12.1")
                connectTimeout = 8000
                readTimeout = 8000
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
                LogUtil.e(AppConfig.TAG, "WARP registration error: code ${conn.responseCode}")
                return@withContext false
            }

            val respText = conn.inputStream.bufferedReader().use { it.readText() }
            val respJson = JSONObject(respText)

            val configObj = respJson.getJSONObject("config")
            val peersArr = configObj.getJSONArray("peers")
            val peer0 = peersArr.getJSONObject(0)
            val peerPubKey = peer0.getString("public_key")
            val endpointObj = peer0.getJSONObject("endpoint")
            val endpointHost = endpointObj.optString("host", "engage.cloudflareclient.com:2408")
            val hostParts = endpointHost.split(":")
            val serverIp = if (hostParts.isNotEmpty() && hostParts[0] != "engage.cloudflareclient.com") hostParts[0] else "162.159.192.1"
            val serverPort = if (hostParts.size > 1) hostParts[1] else "2408"

            val interfaceObj = configObj.getJSONObject("interface")
            val addressesObj = interfaceObj.getJSONObject("addresses")
            val v4 = addressesObj.optString("v4", "172.16.0.2")
            val v6 = addressesObj.optString("v6", "")
            val localAddress = if (v6.isNotEmpty()) "$v4/32,$v6/128" else "$v4/32"

            // 3. Create WireGuard profile item
            val profile = ProfileItem.create(EConfigType.WIREGUARD).apply {
                remarks = "🚀 Бункер Личный WARP+ | Cloudflare"
                server = serverIp
                this.serverPort = serverPort
                secretKey = privB64
                publicKey = peerPubKey
                this.localAddress = localAddress
                mtu = 1280
                remoteDNS = "77.88.8.8,1.1.1.1"
            }

            // 4. Save to MMKV
            val guid = MmkvManager.encodeServerConfig("", profile)
            if (guid.isNotEmpty()) {
                MmkvManager.encodeSelectServer(guid)
                return@withContext true
            }
            false
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to generate WARP config", e)
            false
        }
    }
}
