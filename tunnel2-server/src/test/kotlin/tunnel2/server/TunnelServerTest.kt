package tunnel2.server

import org.apache.log4j.Level
import org.junit.After
import org.junit.Before
import org.junit.Test
import tunnel2.common.logging.LoggerFactory
import tunnel2.common.ssl.SslContexts
import tunnel2.server.interceptor.SimpleRequestInterceptor

class TunnelServerTest {
    private lateinit var tunnelServer: TunnelServer

    @Test
    @Throws(Exception::class)
    fun start() {
        tunnelServer.start()
        Thread.currentThread().join()
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        LoggerFactory.configConsole(level = Level.WARN, names = *LoggerFactory.thirdLibs).apply()
        LoggerFactory.configConsole(level = Level.ALL).apply()
        LoggerFactory.configFile(level = Level.ALL, file = "../logs/t2s.log").apply()

        val simpleRequestInterceptor = SimpleRequestInterceptor(
            "tk123456", "1024-60000"
        )
        val sslContext = SslContexts.forServer(
            "../resources/jks/tunnels.jks",
            "stunnelpass",
            "stunnelpass"
        )
        this.tunnelServer = TunnelServer(
            tunnelRequestInterceptor = simpleRequestInterceptor,
            httpRequestInterceptor = simpleRequestInterceptor,
            httpsRequestInterceptor = simpleRequestInterceptor,

            sslEnable = true,
            sslContext = sslContext,

            httpEnable = true,
            httpsEnable = true,
            httpsContext = sslContext
        )
    }

    @After
    fun shutDown() {
        this.tunnelServer.destroy()
    }
}