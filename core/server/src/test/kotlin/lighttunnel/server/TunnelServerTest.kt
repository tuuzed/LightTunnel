package lighttunnel.server

import lighttunnel.logger.LoggerFactory
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
        tunnelServer = TunnelServer()
    }

    @Test
    fun start() {
        tunnelServer.start()
        Thread.currentThread().join()
    }
}