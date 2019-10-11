package com.tuuzed.lighttunnel.server

import org.apache.log4j.Level
import org.junit.Before
import org.junit.Test
import com.tuuzed.lighttunnel.common.LoggerFactory
import com.tuuzed.lighttunnel.common.SslContextUtil

class LTServerTestSkip {

    @Before
    fun setup() {
        LoggerFactory.configConsole(level = Level.OFF, names = *LoggerFactory.thirdLibs).apply()
        LoggerFactory.configConsole(level = Level.ALL).apply()
    }

    @Test
    fun startTest() {
        val context = SslContextUtil.forServer(
            javaClass.getResource("/lts.jks").file, "ltspass", "ltspass"
        )
        val options = LTServer.Options()
        with(options) {
            sslEnable = true
            sslContext = context
            tcpEnable = true
            httpEnable = true
            httpsEnable = true
            httpsContext = context
        }
        LTServer(options).start()
        Thread.currentThread().join()
    }

}