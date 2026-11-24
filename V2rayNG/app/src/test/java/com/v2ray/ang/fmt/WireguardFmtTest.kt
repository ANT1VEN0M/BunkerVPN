package com.v2ray.ang.fmt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class WireguardFmtTest {

    @Test
    fun parseWireguardConfFileWithCommentsAndAmneziaFields() {
        val confText = """
            # ==============================================================
            #  BUNKER VPN | VIP ESPORTS FreeFire
            # ==============================================================
            [Interface]
            PrivateKey = UPP6Czo/dfhBKKMFMnedCQpb6l3bLvmO+hJzwucAeHU=
            Address = 172.16.0.2/32
            DNS = 1.1.1.1
            MTU = 1280
            S1 = 0
            Jc = 3
            
            [Peer]
            PublicKey = bmXOC+F1FxEMF9dyiK2H5/1SUtzH0JuVo51h2wPfgyo=
            Endpoint = 162.159.195.1:500
            AllowedIPs = 0.0.0.0/0
            PersistentKeepalive = 10
        """.trimIndent()

        val profile = WireguardFmt.parseWireguardConfFile(confText)
        assertNotNull(profile)
        assertEquals("UPP6Czo/dfhBKKMFMnedCQpb6l3bLvmO+hJzwucAeHU=", profile.secretKey)
        assertEquals("bmXOC+F1FxEMF9dyiK2H5/1SUtzH0JuVo51h2wPfgyo=", profile.publicKey)
        assertEquals("162.159.195.1", profile.server)
        assertEquals("500", profile.serverPort)
        assertEquals("172.16.0.2/32", profile.localAddress)
        assertEquals(1280, profile.mtu)
        assertEquals("1.1.1.1", profile.remoteDNS)
        assertEquals("BUNKER VPN | VIP ESPORTS FreeFire", profile.remarks)
    }

    @Test
    fun parseWireguardUriWithUnencodedSlashInKey() {
        // Raw key contains '/' and '+'
        val rawUri = "wireguard://UPP6Czo/dfhBKKMFMnedCQpb6l3bLvmO+hJzwucAeHU=@162.159.195.1:500?publickey=bmXOC%2BF1FxEMF9dyiK2H5%2F1SUtzH0JuVo51h2wPfgyo%3D&address=172.16.0.2%2F32&mtu=1280&dns=1.1.1.1#FreeFire"

        val profile = WireguardFmt.parse(rawUri)
        assertNotNull(profile)
        assertEquals("UPP6Czo/dfhBKKMFMnedCQpb6l3bLvmO+hJzwucAeHU=", profile?.secretKey)
        assertEquals("162.159.195.1", profile?.server)
        assertEquals("500", profile?.serverPort)
        assertEquals("FreeFire", profile?.remarks)
    }
}
