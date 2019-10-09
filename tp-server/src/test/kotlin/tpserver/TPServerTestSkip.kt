package tpserver

import org.apache.log4j.Level
import org.junit.Before
import org.junit.Test
import tpcommon.LoggerFactory
import tpcommon.SslContextUtil

class TPServerTestSkip {

    @Before
    fun setup() {
        LoggerFactory.configConsole(level = Level.OFF, names = *LoggerFactory.thirdLibs).apply()
        LoggerFactory.configConsole(level = Level.ALL).apply()
    }

    @Test
    fun startTest() {
        val context = SslContextUtil.forServer(
            javaClass.getResource("/tps.jks").file, "tpspass", "tpspass"
        )
        val options = TPServer.Options()
        with(options) {
            sslEnable = true
            sslContext = context
            tcpEnable = true
            httpEnable = true
            httpsEnable = true
            httpsContext = context
        }
        TPServer(options).start()
        Thread.currentThread().join()
    }

}