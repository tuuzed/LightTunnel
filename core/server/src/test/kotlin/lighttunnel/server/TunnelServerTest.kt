package lighttunnel.server

import lighttunnel.logger.LoggerFactory
import lighttunnel.server.interceptor.SimpleRequestInterceptor
import lighttunnel.server.util.PortRangeUtil
import lighttunnel.util.Manifest
import org.apache.log4j.Level
import org.junit.Before
import org.junit.Test

class TunnelServerTest {

    lateinit var tunnelServer: TunnelServer

    @Before
    fun setUp() {
        LoggerFactory.configConsole(Level.OFF, names = *Manifest.thirdPartyLibs)
        LoggerFactory.configConsole(level = Level.ALL)
        tunnelServer = TunnelServer(
            tunnelRequestInterceptor = SimpleRequestInterceptor.defaultImpl
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
            val port = PortRangeUtil.getAvailableTcpPort("4000-21000,30000,30001,30003")
            print("$port,")
            if (i != 0 && i % 10 == 0) {
                println()
            }
        }
    }


}