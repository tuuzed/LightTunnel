package krp.krpd

import krp.krpd.args.HttpTunnelArgs
import krp.krpd.args.HttpsTunnelArgs
import krp.krpd.args.TunnelDaemonArgs
import krp.krpd.args.TunnelSslDaemonArgs
import krp.logimpl.LoggerConfigure
import org.apache.log4j.Level
import org.junit.After
import org.junit.Before
import org.junit.Test

class KrpdTest {

    private lateinit var krpd: Krpd

    @Before
    fun setUp() {
        LoggerConfigure.configConsole(Level.WARN, names = arrayOf("io.netty"))
        LoggerConfigure.configConsole(Level.ALL)
        krpd = Krpd(
            tunnelDaemonArgs = TunnelDaemonArgs(),
            tunnelSslDaemonArgs = TunnelSslDaemonArgs(),
            httpTunnelArgs = HttpTunnelArgs(bindPort = 9000),
            httpsTunnelArgs = HttpsTunnelArgs(bindPort = 9443)
        )
    }

    @After
    fun shutDown() {
        krpd.depose()
    }

    @Test
    fun `Start Krpd`() {
        krpd.start()
        Thread.currentThread().join()
    }

}
