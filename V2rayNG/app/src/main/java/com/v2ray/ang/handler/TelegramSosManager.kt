package com.v2ray.ang.handler

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil

object TelegramSosManager {

    private val PROXIES = listOf(
        "tg://proxy?server=89.124.95.185&port=8443&secret=ee6403877430086085167456439119957338392e3132342e39352e313835",
        "tg://proxy?server=146.103.109.134&port=443&secret=a4bc6821c58eee9b48038b104950504a",
        "tg://proxy?server=3.149.86.139&port=8443&secret=ee7f19d4230173736615aca50d4a67aeaa7370656564746573742e6e6574",
        "tg://proxy?server=72.56.94.135&port=9443&secret=58737358dee675be48618b79ece66f8c",
        "tg://proxy?server=188.166.126.151&port=443&secret=ee111111111111111111111111111111117777772e676f6f676c652e636f6d"
    )

    fun launchTelegramProxy(context: Context) {
        try {
            val proxyUrl = PROXIES.random()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(proxyUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to launch Telegram proxy", e)
        }
    }
}
