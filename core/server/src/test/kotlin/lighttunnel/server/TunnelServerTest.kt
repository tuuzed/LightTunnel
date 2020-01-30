package lighttunnel.server

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate
import lighttunnel.logger.LoggerFactory
import lighttunnel.server.http.DefaultStaticFilePlugin
import lighttunnel.server.util.PortUtil
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
        val ssc = SelfSignedCertificate()
        tunnelServer = TunnelServer(
            httpBindPort = 80,
            httpsBindPort = 443,
            httpsContext = SslContextBuilder.forServer(ssc.key(), ssc.cert()).build(),
            dashboardBindPort = 4000,
            staticFilePlugin = DefaultStaticFilePlugin(
                rootPathList = listOf("C:\\", "D:\\"),
                domainPrefixList = listOf("127.0.0.1")
            )
        )
    }

    @Test
    fun start() {
        tunnelServer.start()
        Thread.currentThread().join()
    }


    @Test
    fun getRandomPort() {
        for (i in 1..10000) {
            val port = PortUtil.getAvailableTcpPort("4000-21000,30000,30001,30003")
            print("$port,")
            if (i != 0 && i % 10 == 0) {
                println()
            }
        }
    }


}