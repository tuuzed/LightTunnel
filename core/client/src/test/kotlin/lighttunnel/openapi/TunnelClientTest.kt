package lighttunnel.openapi

import org.junit.Before
import org.junit.Test

class TunnelClientTest {

    private lateinit var tunnelClient: TunnelClient

    @Before
    fun setUp() {
        tunnelClient = TunnelClient()
    }

    @Test
    fun connect() {
        tunnelClient.connect(
            serverAddr = "127.0.0.1",
            serverPort = 5080,
            tunnelRequest = TunnelRequest.forTcp(localAddr = "139.199.221.244", localPort = 22, remotePort = 10022),
            sslContext = null
        )
        tunnelClient.connect(
            serverAddr = "127.0.0.1",
            serverPort = 5080,
            tunnelRequest = TunnelRequest.forTcp(localAddr = "139.199.221.244", localPort = 80, remotePort = 10080),
            sslContext = null
        )
        tunnelClient.connect(
            serverAddr = "127.0.0.1",
            serverPort = 5080,
            tunnelRequest = TunnelRequest.forHttp(localAddr = "139.199.221.244", localPort = 80, host = "t1.tunnel.lo"),
            sslContext = null
        )
        Thread.currentThread().join()
    }


}