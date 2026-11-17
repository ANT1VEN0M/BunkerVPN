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

object BunkerSpeedtestManager {
    private const val SPEED_TEST_URL = "https://speed.cloudflare.com/__down?bytes=10485760" // 10MB test payload
    private const val PING_HOST = "speed.cloudflare.com"
    private const val PING_PORT = 443

    fun runSpeedtest(proxyPort: Int = 0): Flow<SpeedtestProgress> = flow {
        emit(SpeedtestProgress(stage = SpeedtestStage.TESTING_PING))

        // 1. Measure Latency (Ping)
        var pingMs = -1L
        try {
            val start = System.currentTimeMillis()
            val socket = Socket()
            socket.tcpNoDelay = true
            socket.soTimeout = 3000
            socket.connect(InetSocketAddress(PING_HOST, PING_PORT), 3000)
            pingMs = System.currentTimeMillis() - start
            socket.close()
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Speedtest ping failed: ${e.message}")
            pingMs = 999L
        }

        emit(SpeedtestProgress(
            stage = SpeedtestStage.TESTING_DOWNLOAD,
            pingMs = pingMs,
            progressPercent = 5
        ))

        // 2. Measure Download Throughput
        var urlConnection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        val totalExpectedBytes = 10485760L // 10MB

        try {
            val url = URL(SPEED_TEST_URL)
            urlConnection = if (proxyPort > 0) {
                val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", proxyPort))
                url.openConnection(proxy) as HttpURLConnection
            } else {
                url.openConnection() as HttpURLConnection
            }

            urlConnection.connectTimeout = 8000
            urlConnection.readTimeout = 12000
            urlConnection.setRequestProperty("User-Agent", "BunkerVPN-Speedtest/1.2.3")
            urlConnection.setRequestProperty("Accept-Encoding", "identity")
            urlConnection.instanceFollowRedirects = true

            val responseCode = urlConnection.responseCode
            if (responseCode !in 200..299) {
                emit(SpeedtestProgress(
                    stage = SpeedtestStage.FAILED,
                    pingMs = pingMs,
                    errorMessage = "HTTP $responseCode"
                ))
                return@flow
            }

            inputStream = urlConnection.inputStream
            val buffer = ByteArray(32768)
            var totalRead = 0L
            val testStart = System.currentTimeMillis()
            var lastEmitTime = testStart
            var lastReadBytes = 0L

            while (true) {
                val read = inputStream.read(buffer)
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

        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Speedtest download failed: ${e.message}")
            emit(SpeedtestProgress(
                stage = SpeedtestStage.FAILED,
                pingMs = pingMs,
                errorMessage = e.message ?: "Ошибка соединения"
            ))
        } finally {
            try {
                inputStream?.close()
                urlConnection?.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }.flowOn(Dispatchers.IO)
}
