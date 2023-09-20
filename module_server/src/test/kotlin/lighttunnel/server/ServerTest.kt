package lighttunnel.server

import lighttunnel.logger.LoggerConfigure
import lighttunnel.server.args.HttpTunnelArgs
import lighttunnel.server.args.HttpsTunnelArgs
import lighttunnel.server.args.TunnelDaemonArgs
import lighttunnel.server.args.TunnelSslDaemonArgs
import org.apache.log4j.Level
import org.junit.After
import org.junit.Before
import org.junit.Test

class ServerTest {

    private lateinit var server: Server

    @Before
    fun setUp() {
        LoggerConfigure.configConsole(Level.WARN, names = arrayOf("io.netty"))
        LoggerConfigure.configConsole(Level.ALL)
        server = Server(
            tunnelDaemonArgs = TunnelDaemonArgs(),
            tunnelSslDaemonArgs = TunnelSslDaemonArgs(),
            httpTunnelArgs = HttpTunnelArgs(bindPort = 9000),
            httpsTunnelArgs = HttpsTunnelArgs(bindPort = 9443)
        )
    }

    @After
    fun shutDown() {
        server.depose()
    }

    @Test
    fun `Start Server`() {
        server.start()
        Thread.currentThread().join()
    }

}
