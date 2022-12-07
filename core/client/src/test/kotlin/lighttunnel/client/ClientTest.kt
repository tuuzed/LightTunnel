package lighttunnel.client

import lighttunnel.logimpl.LoggerConfigure
import lighttunnel.common.entity.TunnelRequest
import org.apache.log4j.Level
import org.junit.After
import org.junit.Before
import org.junit.Test

class ClientTest {
    private lateinit var client: Client

    @Before
    fun setUp() {
        LoggerConfigure.configConsole(Level.WARN, names = arrayOf("io.netty"))
        LoggerConfigure.configConsole(Level.ALL)
        client = Client()
    }

    @After
    fun shutDown() {
        client.depose()
    }

    @Test
    fun `Start Client`() {
        client.connect(
            serverIp = "127.0.0.1",
            serverPort = 7080,
            tunnelRequest = TunnelRequest.forTcp(
                localIp = "81.71.23.44",
                localPort = 50001,
                remotePort = 10001,
            ),
            useEncryption = false,
        )
        client.connect(
            serverIp = "127.0.0.1",
            serverPort = 7080,
            tunnelRequest = TunnelRequest.forTcp(
                localIp = "192.168.1.102",
                localPort = 22,
                remotePort = 10002,
            ),
            useEncryption = false,
        )
        Thread.currentThread().join()
    }


}
