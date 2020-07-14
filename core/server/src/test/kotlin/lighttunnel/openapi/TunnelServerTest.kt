package lighttunnel.openapi

import lighttunnel.base.logger.LoggerFactory
import lighttunnel.base.util.SslContextUtil
import lighttunnel.openapi.args.*
import lighttunnel.openapi.http.HttpPlugin
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
            bindPort = 8080,
            httpPlugin = HttpPlugin.staticFileImpl(
                paths = listOf("C:\\", "D:\\"),
                hosts = listOf("tunnel.lo")
            )
        )
        val httpsTunnelArgs = HttpsTunnelArgs(
            bindPort = 8443,
            httpPlugin = HttpPlugin.staticFileImpl(
                paths = listOf("C:\\", "D:\\"),
                hosts = listOf("tunnel.lo")
            ),
            sslContext = SslContextUtil.forBuiltinServer()
        )
        val httpServerArgs = HttpRpcServerArgs(
            bindPort = 5081
        )
        tunnelServer = TunnelServer(
            tunnelDaemonArgs = tunnelDaemonArgs,
            sslTunnelDaemonArgs = sslTunnelDaemonArgs,
            httpTunnelArgs = httpTunnelArgs,
            httpsTunnelArgs = httpsTunnelArgs,
            httpRpcServerArgs = httpServerArgs
        )
    }

    @Test
    fun start() {
        tunnelServer.start()
        Thread.currentThread().join()
    }


}