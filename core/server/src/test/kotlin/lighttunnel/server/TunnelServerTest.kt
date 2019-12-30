package lighttunnel.server

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate
import lighttunnel.logger.LoggerFactory
import lighttunnel.server.util.PortUtil
import org.apache.log4j.Level
import org.junit.Before
import org.junit.Test

class TunnelServerTest {

    lateinit var tunnelServer: TunnelServer

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
            httpsBindPort = 443,
            httpsContext = SslContextBuilder.forServer(ssc.key(), ssc.cert()).build(),
            dashBindPort = 4000
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