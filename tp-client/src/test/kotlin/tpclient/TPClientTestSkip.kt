package tpclient

import io.netty.handler.ssl.SslContext
import org.apache.log4j.Level
import org.junit.Before
import org.junit.Test
import tpcommon.LoggerFactory
import tpcommon.SslContexts
import tpcommon.TPRequest
import tpcommon.logger


class TPClientTestSkip {
    private val logger by logger()
    private lateinit var tpClient: TPClient
    private lateinit var context: SslContext

    @Before
    fun setup() {
        LoggerFactory.configConsole(level = Level.OFF, names = *LoggerFactory.thirdLibs).apply()
        LoggerFactory.configConsole(level = Level.ALL).apply()
        val options = TPClient.Options()
        with(options) {
            autoReconnect = true
            listener = object : TPClient.Listener {
                override fun onConnecting(descriptor: TPClientDescriptor, reconnect: Boolean) {
                    logger.info("onConnecting: {}, reconnect: {}", descriptor, reconnect)
                }

                override fun onConnected(descriptor: TPClientDescriptor) {
                    logger.info("onConnected: {}", descriptor)
                }

                override fun onDisconnect(descriptor: TPClientDescriptor, err: Boolean, errCause: Throwable?) {
                    logger.info("onDisconnect: {}, err: {}", descriptor, err)
                    errCause?.printStackTrace()
                }
            }
        }
        tpClient = TPClient(options)
        context = SslContexts.forClient(javaClass.getResource("/tpc.jks").file, "tpcpass")

    }

    @Test
    fun tcpTest() {
        val tpRequest = TPRequest.ofTcp(
            localAddr = "192.168.43.205",
            localPort = 22,
            remotePort = 10022
        )
        tpClient.connect("127.0.0.1", 5080, tpRequest, null)
        Thread.currentThread().join()
    }

    @Test
    fun tcpWithSslTest() {
        val tpRequest = TPRequest.ofTcp(
            localAddr = "192.168.43.205",
            localPort = 22,
            remotePort = 20022
        )
        tpClient.connect("127.0.0.1", 5443, tpRequest, context)
        Thread.currentThread().join()
    }

    @Test
    fun httpTest() {
        val tpRequest = TPRequest.ofHttp(
            false,
            localAddr = "192.168.43.205",
            localPort = 80,
            host = "t1.tunnel.lo"
        )
        val tpRequest2 = TPRequest.ofHttp(
            false,
            localAddr = "192.168.43.86",
            localPort = 80,
            host = "t2.tunnel.lo"
        )
        tpClient.connect("127.0.0.1", 5080, tpRequest, null)
        tpClient.connect("127.0.0.1", 5080, tpRequest2, null)
        Thread.currentThread().join()
    }

    @Test
    fun httpWithSslTest() {
        val tpRequest = TPRequest.ofHttp(
            false,
            localAddr = "192.168.43.205",
            localPort = 80,
            host = "t3.tunnel.lo"
        )
        val tpRequest2 = TPRequest.ofHttp(
            false,
            localAddr = "192.168.43.86",
            localPort = 80,
            host = "t4.tunnel.lo"
        )
        tpClient.connect("127.0.0.1", 5443, tpRequest, context)
        tpClient.connect("127.0.0.1", 5443, tpRequest2, context)
        Thread.currentThread().join()
    }

    @Test
    fun httpsTest() {
        val tpRequest = TPRequest.ofHttp(
            true,
            localAddr = "192.168.43.205",
            localPort = 80,
            host = "t1.tunnel.lo"
        )
        val tpRequest2 = TPRequest.ofHttp(
            true,
            localAddr = "192.168.43.86",
            localPort = 80,
            host = "t2.tunnel.lo"
        )
        tpClient.connect("127.0.0.1", 5080, tpRequest, null)
        tpClient.connect("127.0.0.1", 5080, tpRequest2, null)
        Thread.currentThread().join()
    }

    @Test
    fun httpsWithSslTest() {
        val tpRequest = TPRequest.ofHttp(
            true,
            localAddr = "192.168.43.205",
            localPort = 80,
            host = "t3.tunnel.lo"
        )
        val tpRequest2 = TPRequest.ofHttp(
            true,
            localAddr = "192.168.43.86",
            localPort = 80,
            host = "t4.tunnel.lo"
        )
        tpClient.connect("127.0.0.1", 5443, tpRequest, context)
        tpClient.connect("127.0.0.1", 5443, tpRequest2, context)
        Thread.currentThread().join()
    }


}