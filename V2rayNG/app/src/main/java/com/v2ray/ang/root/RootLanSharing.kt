package com.v2ray.ang.root

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


object RootLanSharing {

    private var lanSharingStarted = false
    private var lanShareJob: Job? = null

    /**
     * Optional root feature: share the proxy with tethered LAN/USB clients while the
     * device itself stays on the VpnService, or enforce TTL fixation (64) for carrier bypass.
     * Runs a dedicated client hev-socks5-tunnel / TTL mangle script off the main thread
     * so the su calls don't block service startup.
     */
    fun startClientSharing(context: Context): Boolean {
        val lanShare = MmkvManager.decodeSettingsBool(AppConfig.PREF_ROOT_LAN_SHARING)
        val fixTtl = MmkvManager.decodeSettingsBool(AppConfig.PREF_ROOT_FIX_TTL)

        if (lanShare || fixTtl) {
            if (lanShareJob != null) return false

            lanSharingStarted = true
            lanShareJob = CoroutineScope(Dispatchers.IO).launch {
                if (RootManager.isRootAvailable()) {
                    RootProxyManager.startClientSharing(context)
                }
            }
        }

        return true
    }

    /**
     * Remove LAN/tethering sharing rules + helper before stopping the core. Wait for the
     * async setup to finish first, otherwise a stop during setup tears down before the
     * rules are installed and they leak (orphan FORWARD/policy-routing rules + client tun).
     */
    fun stopClientSharing(context: Context) {
        if (!lanSharingStarted) return

        lanSharingStarted = false
        runBlocking { lanShareJob?.cancelAndJoin() }
        lanShareJob = null
        RootProxyManager.stop(context)
    }
}
