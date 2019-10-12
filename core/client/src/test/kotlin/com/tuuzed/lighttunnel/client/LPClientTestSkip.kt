package com.tuuzed.lighttunnel.client

import com.tuuzed.lighttunnel.common.*
import io.netty.handler.ssl.SslContext
import org.apache.log4j.Level
import org.junit.Before
import org.junit.Test


class LPClientTestSkip {
    private val logger by logger()
    private lateinit var client: LTClient
    private lateinit var context: SslContext

    @Before
    fun setup() {
        LoggerFactory.configConsole(level = Level.OFF, names = *LTManifest.thirdLibs)
        LoggerFactory.configConsole(level = Level.ALL)
        val options = LTClient.Options()
        with(options) {
            autoReconnect = true
            listener = object : OnLTClientStateListener {
                override fun onConnecting(descriptor: LTConnDescriptor, reconnect: Boolean) {
                    logger.info("onConnecting: {}, reconnect: {}", descriptor, reconnect)
                }

                override fun onConnected(descriptor: LTConnDescriptor) {
                    logger.info("onConnected: {}", descriptor)
                }

                override fun onDisconnect(descriptor: LTConnDescriptor, err: Boolean, errCause: Throwable?) {
                    logger.info("onDisconnect: {}, err: {}", descriptor, err)
                    errCause?.printStackTrace()
                }
            }
        }
        client = LTClient(options)
        context = SslContextUtil.forClient(javaClass.getResource("/ltc.jks").file, "ltcpass")

    }

    @Test
    fun tcpTest() {
        val tpRequest = LTRequest.ofTcp(
            localAddr = "192.168.43.205",
            localPort = 22,
            remotePort = 10022
        )
        client.connect("127.0.0.1", 5080, tpRequest, null)
        Thread.currentThread().join()
    }

    @Test
    fun tcpWithSslTest() {
        val tpRequest = LTRequest.ofTcp(
            localAddr = "192.168.43.205",
            localPort = 22,
            remotePort = 20022
        )
        client.connect("127.0.0.1", 5443, tpRequest, context)
        Thread.currentThread().join()
    }

    @Test
    fun httpTest() {
        val tpRequest = LTRequest.ofHttp(
            false,
            localAddr = "192.168.43.205",
            localPort = 80,
            host = "t1.tunnel.lo"
        )
        val tpRequest2 = LTRequest.ofHttp(
            false,
            localAddr = "192.168.43.86",
            localPort = 80,
            host = "t2.tunnel.lo"
        )
        client.connect("127.0.0.1", 5080, tpRequest, null)
        client.connect("127.0.0.1", 5080, tpRequest2, null)
        Thread.currentThread().join()
    }

    @Test
    fun httpWithSslTest() {
        val tpRequest = LTRequest.ofHttp(
            false,
            localAddr = "192.168.43.205",
            localPort = 80,
            host = "t3.tunnel.lo"
        )
        val tpRequest2 = LTRequest.ofHttp(
            false,
            localAddr = "192.168.43.86",
            localPort = 80,
            host = "t4.tunnel.lo"
        )
        client.connect("127.0.0.1", 5443, tpRequest, context)
        client.connect("127.0.0.1", 5443, tpRequest2, context)
        Thread.currentThread().join()
    }

    @Test
    fun httpsTest() {
        val tpRequest = LTRequest.ofHttp(
            true,
            localAddr = "192.168.43.205",
            localPort = 80,
            host = "t1.tunnel.lo"
        )
        val tpRequest2 = LTRequest.ofHttp(
            true,
            localAddr = "192.168.43.86",
            localPort = 80,
            host = "t2.tunnel.lo"
        )
        client.connect("127.0.0.1", 5080, tpRequest, null)
        client.connect("127.0.0.1", 5080, tpRequest2, null)
        Thread.currentThread().join()
    }

    @Test
    fun httpsWithSslTest() {
        val tpRequest = LTRequest.ofHttp(
            true,
            localAddr = "192.168.43.205",
            localPort = 80,
            host = "t3.tunnel.lo"
        )
        val tpRequest2 = LTRequest.ofHttp(
            true,
            localAddr = "192.168.43.86",
            localPort = 80,
            host = "t4.tunnel.lo"
        )
        client.connect("127.0.0.1", 5443, tpRequest, context)
        client.connect("127.0.0.1", 5443, tpRequest2, context)
        Thread.currentThread().join()
    }


}