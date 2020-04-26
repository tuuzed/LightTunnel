package lighttunnel.client

import lighttunnel.logger.LoggerFactory
import lighttunnel.proto.TunnelRequest
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
        tunnelClient = TunnelClient(dashBindAddr = "::", dashboardBindPort = 5081)
    }

    @Test
    fun connect() {
        tunnelClient.connect(
            serverAddr = "127.0.0.1",
            serverPort = 5080,
            tunnelRequest = TunnelRequest.forTcp(
                localAddr = "192.168.1.1",
                localPort = 80,
                remotePort = 10080
            ),
            sslContext = null
        )
        Thread.currentThread().join()
    }
}