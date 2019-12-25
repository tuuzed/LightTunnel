package lighttunnel.client

import lighttunnel.logger.LoggerFactory
import lighttunnel.proto.TunnelRequest
import org.apache.log4j.Level
import org.junit.Before
import org.junit.Test

class TunnelClientTest {

    lateinit var tunnelClient: TunnelClient

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
        tunnelClient = TunnelClient()
    }

    @Test
    fun connect() {
        tunnelClient.connect("127.0.0.1", 5080, TunnelRequest.forTcp(
            localAddr = "192.168.1.100",
            localPort = 22,
            remotePort = 0
        ), sslContext = null)
        tunnelClient.connect("127.0.0.1", 5080, TunnelRequest.forTcp(
            localAddr = "192.168.1.100",
            localPort = 22,
            remotePort = 0
        ), sslContext = null)
        tunnelClient.connect("127.0.0.1", 5080, TunnelRequest.forTcp(
            localAddr = "192.168.1.100",
            localPort = 22,
            remotePort = 0
        ), sslContext = null)
        Thread.currentThread().join()
    }
}