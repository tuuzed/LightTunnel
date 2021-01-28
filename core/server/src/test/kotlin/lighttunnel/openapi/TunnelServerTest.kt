package lighttunnel.openapi

import lighttunnel.SslContextUtils
import lighttunnel.TunnelServer
import lighttunnel.args.HttpTunnelArgs
import lighttunnel.args.HttpsTunnelArgs
import lighttunnel.args.SslTunnelDaemonArgs
import lighttunnel.args.TunnelDaemonArgs
import org.junit.Before
import org.junit.Test

class TunnelServerTest {

    private lateinit var tunnelServer: TunnelServer

    @Before
    fun setUp() {
        val tunnelDaemonArgs = TunnelDaemonArgs(
            bindPort = 5080
        )
        val sslTunnelDaemonArgs = SslTunnelDaemonArgs(
            bindPort = 5443,
            sslContext = SslContextUtils.forBuiltinServer()
        )
        val httpTunnelArgs = HttpTunnelArgs(
            bindPort = 8080
        )
        val httpsTunnelArgs = HttpsTunnelArgs(
            bindPort = 8443,
            sslContext = SslContextUtils.forBuiltinServer()
        )
        tunnelServer = TunnelServer(
            tunnelDaemonArgs = tunnelDaemonArgs,
            sslTunnelDaemonArgs = sslTunnelDaemonArgs,
            httpTunnelArgs = httpTunnelArgs,
            httpsTunnelArgs = httpsTunnelArgs
        )
    }

    @Test
    fun start() {
        tunnelServer.start()
        Thread.currentThread().join()
    }


}
