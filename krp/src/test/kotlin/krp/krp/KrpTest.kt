package krp.krp

import krp.common.entity.TunnelRequest
import krp.logimpl.LoggerConfigure
import org.apache.log4j.Level
import org.junit.After
import org.junit.Before
import org.junit.Test

class KrpTest {
    private lateinit var krp: Krp

    @Before
    fun setUp() {
        LoggerConfigure.configConsole(Level.WARN, names = arrayOf("io.netty"))
        LoggerConfigure.configConsole(Level.ALL)
        krp = Krp()
    }

    @After
    fun shutDown() {
        krp.depose()
    }

    @Test
    fun `Start Krp`() {
        krp.connect(
            serverIp = "127.0.0.1",
            serverPort = 7080,
            tunnelRequest = TunnelRequest.forTcp(
                localIp = "81.71.23.44",
                localPort = 50001,
                remotePort = 10001,
            ),
            useEncryption = false,
        )
        krp.connect(
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
