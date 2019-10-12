package com.tuuzed.lighttunnel.server

import com.tuuzed.lighttunnel.common.LTManifest
import com.tuuzed.lighttunnel.common.LoggerFactory
import com.tuuzed.lighttunnel.common.SslContextUtil
import org.apache.log4j.Level
import org.junit.Before
import org.junit.Test

class LTServerTestSkip {

    @Before
    fun setup() {
        LoggerFactory.configConsole(level = Level.OFF, names = *LTManifest.thirdLibs)
        LoggerFactory.configConsole(level = Level.ALL)
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