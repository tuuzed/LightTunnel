package lighttunnel.base.entity

import org.junit.Test

class TunnelRequestTest {

    @Test
    fun `Test TunnelRequest`() {
        println(TunnelRequest.forTcp(localAddr = "139.199.221.244", localPort = 22, remotePort = 10022).toJsonString())
        println(
            TunnelRequest.fromJson(
                """
            {"TUNNEL_TYPE":16,"REMOTE_PORT":10022,"LOCAL_PORT":22,"LOCAL_ADDR":"139.199.221.244","EXTRAS":{}}
        """.trimIndent()
            ).tunnelType
        )
    }
}
