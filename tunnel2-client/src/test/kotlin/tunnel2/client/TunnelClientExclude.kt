package tunnel2.client

import org.apache.log4j.Level
import org.junit.After
import org.junit.Before
import org.junit.Test
import tunnel2.common.TunnelRequest
import tunnel2.common.logging.LoggerFactory
import tunnel2.common.ssl.SslContexts

class TunnelClientExclude {

    private lateinit var client: TunnelClient
    private lateinit var tcphttp: TunnelRequest
    private lateinit var vnc: TunnelRequest
    private lateinit var ssh: TunnelRequest
    private lateinit var httpT1: TunnelRequest
    private lateinit var httpT2: TunnelRequest
    private lateinit var httpsT3: TunnelRequest


    @Test
    @Throws(Exception::class)
    fun start() {
        val serverAddr = "127.0.0.1"
        val serverPort = 5001
        val sslContext = SslContexts.forClient(
            "../resources/jks/t2c.jks",
            "t2cpass"
        )
        // tcp
        client.connect(serverAddr, serverPort, vnc, sslContext)
        client.connect(serverAddr, serverPort, ssh, sslContext)
        client.connect(serverAddr, serverPort, tcphttp, sslContext)
        // Http(s)
        client.connect(serverAddr, serverPort, httpT1, sslContext)
        client.connect(serverAddr, serverPort, httpT2, sslContext)
        client.connect(serverAddr, serverPort, httpsT3, sslContext)

        Thread.currentThread().join()
    }

    @Before
    fun setUp() {
        LoggerFactory.configConsole(level = Level.WARN, names = *LoggerFactory.thirdLibs).apply()
        LoggerFactory.configConsole(level = Level.ALL).apply()
        LoggerFactory.configFile(level = Level.ALL, file = "../logs/t2c.log").apply()

        client = TunnelClient(
            autoReconnect = true,
            workerThreads = 4,
            listener = object : TunnelClient.Listener {
                private val logger = LoggerFactory.getLogger(TunnelClient.Listener::class.java)

                override fun onConnecting(descriptor: TunnelClientDescriptor, reconnect: Boolean) {
                    logger.warn("tunnel: {}, reconnect: {}", descriptor, reconnect)
                }

                override fun onConnected(descriptor: TunnelClientDescriptor) {
                    logger.error("{}", descriptor)
                }

                override fun onDisconnect(descriptor: TunnelClientDescriptor, err: Boolean, errCause: Throwable?) {
                    logger.error("tunnel: {}, err: {}", descriptor, err, errCause)
                }
            }
        )


        vnc = TunnelRequest.ofTcp(
            localAddr = "192.168.1.33",
            localPort = 5900,
            remotePort = 15900,
            authToken = "tk123456"
        )
        ssh = TunnelRequest.ofTcp(
            localAddr = "192.168.1.23",
            localPort = 22,
            remotePort = 10022,
            authToken = "tk123456"
        )
        tcphttp = TunnelRequest.ofTcp(
            localAddr = "192.168.1.1",
            localPort = 80,
            remotePort = 10080,
            authToken = "tk123456"
        )

        httpT1 = TunnelRequest.ofHttp(
            localAddr = "192.168.1.1",
            localPort = 80,
            host = "t1.tunnel.lo",
            authToken = "tk123456",
            proxySetHeaders = mapOf(Pair("X-Real-IP", "\$remote_addr")),
            proxyAddHeaders = mapOf(Pair("X-User-Agent", "Tunnel"))
        )
        httpT2 = TunnelRequest.ofHttp(
            localAddr = "111.230.198.37",
            localPort = 10080,
            host = "t2.tunnel.lo",
            enableBasicAuth = true,
            basicAuthRealm = "OCR",
            basicAuthUsername = "admin",
            basicAuthPassword = "admin",
            authToken = "tk123456",
            proxySetHeaders = mapOf(Pair("X-Real-IP", "\$remote_addr")),
            proxyAddHeaders = mapOf(Pair("X-User-Agent", "Tunnel"))
        )
        httpsT3 = TunnelRequest.ofHttps(
            localAddr = "111.230.198.37",
            localPort = 10080,
            host = "t3.tunnel.lo",
            enableBasicAuth = true,
            basicAuthRealm = "OCR",
            basicAuthUsername = "admin",
            basicAuthPassword = "admin",
            authToken = "tk123456",
            proxySetHeaders = mapOf(Pair("X-Real-IP", "\$remote_addr")),
            proxyAddHeaders = mapOf(Pair("X-User-Agent", "Tunnel"))
        )

    }

    @After
    fun shutDown() {
        client.destroy()
    }
}