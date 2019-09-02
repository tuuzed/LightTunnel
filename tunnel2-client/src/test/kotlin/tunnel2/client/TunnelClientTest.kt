package tunnel2.client

import org.apache.log4j.Level
import org.junit.After
import org.junit.Before
import org.junit.Test
import tunnel2.common.TunnelRequest
import tunnel2.common.logging.LoggerFactory
import tunnel2.common.ssl.SslContexts

class TunnelClientTest {
    private lateinit var client: TunnelClient
    private lateinit var portError: TunnelRequest
    private lateinit var tcpHttp: TunnelRequest
    private lateinit var vnc: TunnelRequest
    private lateinit var ssh: TunnelRequest
    private lateinit var vhostHttp1: TunnelRequest
    private lateinit var vhostHttp2: TunnelRequest
    private lateinit var vhostHttps1: TunnelRequest
    private lateinit var vhostHttps2: TunnelRequest
    private lateinit var portReplaced: TunnelRequest


    @Test
    @Throws(Exception::class)
    fun start() {
        val serverAddr = "127.0.0.1"
        val serverPort = 5001
        val sslContext = SslContexts.forClient(
            "../resources/jks/t2c.jks",
            "t2cpass"
        )
        client.connect(serverAddr, serverPort, portError, sslContext)

        client.connect(serverAddr, serverPort, tcpHttp, sslContext)
        client.connect(serverAddr, serverPort, vnc, sslContext)
        client.connect(serverAddr, serverPort, ssh, sslContext)
        // vhostHttp
        client.connect(serverAddr, serverPort, vhostHttp1, sslContext)
        client.connect(serverAddr, serverPort, vhostHttp2, sslContext)
        // vhostHttps
        client.connect(serverAddr, serverPort, vhostHttps1, sslContext)
        client.connect(serverAddr, serverPort, vhostHttps2, sslContext)

        val portReplacedDescriptor = client.connect(serverAddr, serverPort, portReplaced, sslContext)
        Thread(Runnable {
            try {
                Thread.sleep(6000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            portReplacedDescriptor.shutdown()
        }).start()
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

        portError = TunnelRequest.ofTcp(
            "192.168.1.1",
            80,
            65000,
            "tk123456"
        )
        portReplaced = TunnelRequest.ofTcp(
            "192.168.1.1",
            80,
            20000,
            "tk123456"
        ) //  replaced 20080

        tcpHttp = TunnelRequest.ofTcp(
            "111.230.198.37",
            10080,
            10080,
            "tk123456"
        )

        vnc = TunnelRequest.ofTcp(
            "192.168.1.33",
            5900,
            15900,
            "tk123456"
        )
        ssh = TunnelRequest.ofTcp(
            "192.168.1.10",
            22,
            10022,
            "tk123456"
        )

        vhostHttp1 = TunnelRequest.ofHttp(
            localAddr = "192.168.1.1",
            localPort = 80,
            vhost = "t1.tunnel.lo",
            authToken = "tk123456",
            proxySetHeaders = mapOf(Pair("X-Real-IP", "\$remote_addr")),
            proxyAddHeaders = mapOf(Pair("X-User-Agent", "Tunnel"))
        )

        vhostHttp2 = TunnelRequest.ofHttp(
            localAddr = "111.230.198.37",
            localPort = 10080,
            vhost = "t2.tunnel.lo",
            enableBasicAuth = true,
            basicAuthRealm = "OCR",
            basicAuthUsername = "admin",
            basicAuthPassword = "admin",
            authToken = "tk123456",
            proxySetHeaders = mapOf(Pair("X-Real-IP", "\$remote_addr")),
            proxyAddHeaders = mapOf(Pair("X-User-Agent", "Tunnel"))
        )

        vhostHttps1 = TunnelRequest.ofHttps(
            localAddr = "192.168.1.1",
            localPort = 80,
            vhost = "t1.tunnel.lo",
            authToken = "tk123456",
            proxySetHeaders = mapOf(Pair("X-Real-IP", "\$remote_addr")),
            proxyAddHeaders = mapOf(Pair("X-User-Agent", "Tunnel"))
        )

        vhostHttps2 = TunnelRequest.ofHttps(
            localAddr = "111.230.198.37",
            localPort = 10080,
            vhost = "t2.tunnel.lo",
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