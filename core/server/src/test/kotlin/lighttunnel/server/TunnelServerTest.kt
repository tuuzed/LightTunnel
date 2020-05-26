package lighttunnel.server

import lighttunnel.logger.LoggerFactory
import lighttunnel.server.http.HttpPlugin
import lighttunnel.util.SslContextUtil
import org.apache.log4j.Level
import org.junit.Before
import org.junit.Test

class TunnelServerTest {


    @Test
    fun start() {
        tunnelServer.start()
        Thread.currentThread().join()
    }

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
        val tunnelServiceArgs = TunnelServer.TunnelServiceArgs(
            bindPort = 5080
        )
        val sslTunnelServiceArgs = TunnelServer.SslTunnelServiceArgs(
            bindPort = 5443,
            sslContext = SslContextUtil.forBuiltinServer())
        val httpServerArgs = TunnelServer.HttpServerArgs(
            bindPort = 8080,
            httpPlugin = HttpPlugin.staticFileImpl(
                paths = listOf("C:\\", "D:\\"),
                hosts = listOf("tunnel.lo")
            )
        )
        val httpsServerArgs = TunnelServer.HttpsServerArgs(
            bindPort = 8443,
            httpPlugin = HttpPlugin.staticFileImpl(
                paths = listOf("C:\\", "D:\\"),
                hosts = listOf("tunnel.lo")
            ),
            sslContext = SslContextUtil.forBuiltinServer()
        )
        val webServerArgs = TunnelServer.WebServerArgs(
            bindPort = 5081

        )
        tunnelServer = TunnelServer(
            tunnelServiceArgs = tunnelServiceArgs,
            sslTunnelServiceArgs = sslTunnelServiceArgs,
            httpServerArgs = httpServerArgs,
            httpsServerArgs = httpsServerArgs,
            webServerArgs = webServerArgs
        )
    }


}