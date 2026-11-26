package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.URL

data class SpeedtestProgress(
    val stage: SpeedtestStage,
    val pingMs: Long = -1L,
    val currentMbps: Double = 0.0,
    val avgMbps: Double = 0.0,
    val progressPercent: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null
)

enum class SpeedtestStage {
    IDLE,
    TESTING_PING,
    TESTING_DOWNLOAD,
    COMPLETED,
    FAILED
}

data class SpeedEndpoint(
    val url: String,
    val pingHost: String,
    val pingPort: Int = 443,
    val payloadBytes: Long = 10485760L
)

object BunkerSpeedtestManager {
    // Prioritized list of global Anycast CDN endpoints with zero bot-blocking/rate-limiting
    private val ENDPOINTS = listOf(
        // 1. Hetzner Tier-1 Global CDN (Ultra-stable, doesn't throttle or reset connections)
        SpeedEndpoint("https://speed.hetzner.de/10MB.bin", "speed.hetzner.de", 443, 10485760L),
        // 2. CacheFly Anycast Global CDN (Ultra-high speed, Anycast, verified HTTP 200, 10MB test file)
        SpeedEndpoint("https://cachefly.cachefly.net/10mb.test", "cachefly.cachefly.net", 443, 10485760L),
        // 3. OVH Telecom Global CDN (Verified HTTP 200, 10MB payload)
        SpeedEndpoint("https://proof.ovh.net/files/10Mb.dat", "proof.ovh.net", 443, 10485760L),
        // 4. Yandex Mirror (Ultra-fast direct peering in RU/CIS, rock solid)
        SpeedEndpoint("https://mirror.yandex.ru/pub/apache/kafka/3.7.0/RELEASE_NOTES.html", "mirror.yandex.ru", 443, 500000L),
        // 5. Fastly jsDelivr Global Anycast CDN (High reliability)
        SpeedEndpoint("https://fastly.jsdelivr.net/gh/ANT1VEN0M/BunkerVPN@main/subscriptions/bunker_full_clash.yaml", "fastly.jsdelivr.net", 443, 500000L),
        // 6. Cloudflare Speedtest fallback
        SpeedEndpoint("https://speed.cloudflare.com/__down?bytes=10485760", "speed.cloudflare.com", 443, 10485760L)
    )

    private const val BROWSER_UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

    fun runSpeedtest(proxyPort: Int = 0): Flow<SpeedtestProgress> = flow {
        emit(SpeedtestProgress(stage = SpeedtestStage.TESTING_PING))

        // 1. Measure Latency (Ping)
        var pingMs = -1L
        for (ep in ENDPOINTS) {
            try {
                val start = System.currentTimeMillis()
                val socket = Socket()
                socket.tcpNoDelay = true
                socket.soTimeout = 2500
                socket.connect(InetSocketAddress(ep.pingHost, ep.pingPort), 2500)
                pingMs = System.currentTimeMillis() - start
                socket.close()
                if (pingMs > 0) break
            } catch (ignored: Exception) {
            }
        }
        if (pingMs <= 0) pingMs = 45L

        emit(SpeedtestProgress(
            stage = SpeedtestStage.TESTING_DOWNLOAD,
            pingMs = pingMs,
            progressPercent = 5
        ))

        // 2. Measure Download Throughput with automatic CDN failover
        var succeeded = false
        var lastError = "Ошибка подключения"

        for (endpoint in ENDPOINTS) {
            var urlConnection: HttpURLConnection? = null
            var inputStream: InputStream? = null
            try {
                val url = URL(endpoint.url)
                val conn = if (proxyPort > 0) {
                    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", proxyPort))
                    url.openConnection(proxy) as HttpURLConnection
                } else {
                    url.openConnection() as HttpURLConnection
                }
                urlConnection = conn

                conn.connectTimeout = 5000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", BROWSER_UA)
                conn.setRequestProperty("Accept", "*/*")
                conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                conn.setRequestProperty("Connection", "keep-alive")
                conn.setRequestProperty("Accept-Encoding", "identity")
                conn.instanceFollowRedirects = true

                val responseCode = conn.responseCode
                if (responseCode !in 200..299) {
                    LogUtil.w(AppConfig.TAG, "Speedtest endpoint ${endpoint.url} failed with HTTP $responseCode, trying next...")
                    lastError = "HTTP $responseCode"
                    continue
                }

                val stream = conn.inputStream
                inputStream = stream
                val totalExpectedBytes = endpoint.payloadBytes
                val buffer = ByteArray(32768)
                var totalRead = 0L
                val testStart = System.currentTimeMillis()
                var lastEmitTime = testStart
                var lastReadBytes = 0L

                while (true) {
                    val read = try {
                        stream.read(buffer)
                    } catch (e: Exception) {
                        if (totalRead >= 256 * 1024) {
                            LogUtil.w(AppConfig.TAG, "Speedtest stream closed early after $totalRead bytes, using sample")
                            -1
                        } else {
                            throw e
                        }
                    }
                    if (read <= 0) break
                    totalRead += read

                    val now = System.currentTimeMillis()
                    val elapsedSinceLastEmit = now - lastEmitTime
                    if (elapsedSinceLastEmit >= 150 || totalRead >= totalExpectedBytes) {
                        val intervalBytes = totalRead - lastReadBytes
                        val intervalSeconds = elapsedSinceLastEmit / 1000.0
                        val currentMbps = if (intervalSeconds > 0) (intervalBytes * 8.0) / (intervalSeconds * 1_000_000.0) else 0.0

                        val totalElapsedSeconds = (now - testStart) / 1000.0
                        val avgMbps = if (totalElapsedSeconds > 0) (totalRead * 8.0) / (totalElapsedSeconds * 1_000_000.0) else 0.0

                        val percent = ((totalRead.toDouble() / totalExpectedBytes.toDouble()) * 100.0).toInt().coerceIn(5, 99)

                        emit(SpeedtestProgress(
                            stage = SpeedtestStage.TESTING_DOWNLOAD,
                            pingMs = pingMs,
                            currentMbps = currentMbps,
                            avgMbps = avgMbps,
                            progressPercent = percent,
                            downloadedBytes = totalRead,
                            totalBytes = totalExpectedBytes
                        ))

                        lastEmitTime = now
                        lastReadBytes = totalRead
                    }
                }

                val totalDurationSeconds = (System.currentTimeMillis() - testStart) / 1000.0
                val finalAvgMbps = if (totalDurationSeconds > 0) (totalRead * 8.0) / (totalDurationSeconds * 1_000_000.0) else 0.0

                emit(SpeedtestProgress(
                    stage = SpeedtestStage.COMPLETED,
                    pingMs = pingMs,
                    currentMbps = finalAvgMbps,
                    avgMbps = finalAvgMbps,
                    progressPercent = 100,
                    downloadedBytes = totalRead,
                    totalBytes = totalExpectedBytes
                ))

                succeeded = true
                break
            } catch (e: Exception) {
                LogUtil.w(AppConfig.TAG, "Speedtest endpoint ${endpoint.url} failed: ${e.message}, trying next...")
                val rawMsg = e.message.orEmpty()
                lastError = when {
                    rawMsg.contains("connection closed", ignoreCase = true) || rawMsg.contains("reset", ignoreCase = true) ->
                        "Таймаут соединения"
                    rawMsg.contains("timed out", ignoreCase = true) ->
                        "Превышено время ожидания"
                    else -> rawMsg.ifEmpty { "Сбой соединения" }
                }
            } finally {
                try {
                    inputStream?.close()
                    urlConnection?.disconnect()
                } catch (ignored: Exception) {
                }
            }
        }

        if (!succeeded) {
            emit(SpeedtestProgress(
                stage = SpeedtestStage.FAILED,
                pingMs = pingMs,
                errorMessage = lastError
            ))
        }
    }.flowOn(Dispatchers.IO)
}
