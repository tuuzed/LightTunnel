package lighttunnel.openapi

import lighttunnel.base.logger.LoggerFactory
import lighttunnel.openapi.args.HttpTunnelArgs
import lighttunnel.openapi.args.HttpsTunnelArgs
import lighttunnel.openapi.args.SslTunnelDaemonArgs
import lighttunnel.openapi.args.TunnelDaemonArgs
import org.apache.log4j.Level
import org.junit.Before
import org.junit.Test

class TunnelServerTest {

    private lateinit var tunnelServer: TunnelServer

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
        val tunnelDaemonArgs = TunnelDaemonArgs(
            bindPort = 5080
        )
        val sslTunnelDaemonArgs = SslTunnelDaemonArgs(
            bindPort = 5443,
            sslContext = SslContextUtil.forBuiltinServer()
        )
        val httpTunnelArgs = HttpTunnelArgs(
            bindPort = 8080
        )
        val httpsTunnelArgs = HttpsTunnelArgs(
            bindPort = 8443,
            sslContext = SslContextUtil.forBuiltinServer()
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