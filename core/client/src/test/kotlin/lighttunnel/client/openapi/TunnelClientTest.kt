package lighttunnel.client.openapi

import lighttunnel.base.logger.LoggerFactory
import lighttunnel.base.openapi.TunnelRequest
import org.apache.log4j.Level
import org.junit.Before
import org.junit.Test

class TunnelClientTest {

    private lateinit var tunnelClient: TunnelClient

    @Before
    fun setUp() {
        LoggerFactory.configConsole(Level.OFF, names = *arrayOf(
            "io.netty",
            "org.ini4j",
            "org.slf4j",
            "org.json",
            "org.apache.commons.cli"
        ))
        LoggerFactory.configConsole(level = Level.ALL)
        tunnelClient = TunnelClient(httpRpcBindPort = 5081)
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
            tunnelRequest = TunnelRequest.forHttp(
                localAddr = "139.199.221.244", localPort = 80, host = "t1.tunnel.lo",
                enableBasicAuth = true
            ),
            sslContext = null
        )
        Thread.currentThread().join()
    }


}