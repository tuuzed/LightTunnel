package lts

import com.tuuzed.lighttunnel.cmd.CmdLineParser
import com.tuuzed.lighttunnel.cmd.Yaml
import com.tuuzed.lighttunnel.common.LTManifest
import com.tuuzed.lighttunnel.common.LoggerFactory
import com.tuuzed.lighttunnel.common.SslContextUtil
import com.tuuzed.lighttunnel.server.LTServer
import com.tuuzed.lighttunnel.server.LTSimpleRequestInterceptor
import org.apache.log4j.Level
import org.apache.log4j.helpers.OptionConverter
import java.io.File

class LTS {

    private lateinit var config: LTSConfig
    private val sslContext by lazy {
        if (config.ssl.enable) {
            val jks = config.ssl.jks
            val storepass = config.ssl.storepass
            val keypass = config.ssl.keypass
            requireNotNull(jks)
            requireNotNull(storepass)
            requireNotNull(keypass)
            SslContextUtil.forServer(jks, storepass, keypass)
        } else {
            null
        }
    }
    private val httpsContext by lazy {
        if (config.https.enable) {
            val jks = config.https.jks
            val storepass = config.https.storepass
            val keypass = config.https.keypass
            requireNotNull(jks)
            requireNotNull(storepass)
            requireNotNull(keypass)
            SslContextUtil.forServer(jks, storepass, keypass)
        } else {
            null
        }
    }

    fun doMain(args: Array<String>) {
        config = getConfig(args) ?: return
        configureLogger(config.log)
        val interceptor = LTSimpleRequestInterceptor(
            config.basic.authToken, config.tcp.allowPorts
        )
        val options = LTServer.Options()
        with(options) {
            bossThreads = config.basic.bossThreads
            workerThreads = config.basic.workerThreads
            tpRequestInterceptor = interceptor

            bindAddr = config.basic.bindAddr
            bindPort = config.basic.bindPort

            sslEnable = config.ssl.enable
            sslContext = this@LTS.sslContext
            sslBindAddr = config.ssl.bindAddr
            sslBindPort = config.ssl.bindPort

            tcpEnable = config.tcp.enable

            httpEnable = config.http.enable
            httpBindAddr = config.http.bindAddr
            httpBindPort = config.http.bindPort
            tpHttpRequestInterceptor = interceptor

            httpsEnable = config.https.enable
            httpsContext = this@LTS.httpsContext
            httpsBindAddr = config.https.bindAddr
            httpsBindPort = config.https.bindPort
            tpHttpsRequestInterceptor = interceptor
        }
        LTServer(options).start()
    }

    private fun getConfig(args: Array<String>): LTSConfig? {
        val cmdLine = LTSCmdLine()
        if (args.isEmpty()) {
            return LTSConfig()
        }
        try {
            CmdLineParser.parse(cmdLine, args)
        } catch (e: Exception) {
            cmdLine.help = true
        }
        return if (cmdLine.help) {
            System.err.printf("%n%nUsage: %n")
            CmdLineParser.printHelp(cmdLine, System.err, "    ")
            System.err.printf("%n%nConfig Example: %n%n")
            System.err.printf(Yaml.dump(LTSConfig()))
            return null
        } else {
            if (cmdLine.yaml.isNotEmpty()) {
                Yaml.load(cmdLine.yaml)
            } else {
                Yaml.load(File(cmdLine.configFile).readText(Charsets.UTF_8))
            }
        }
    }

    private fun configureLogger(logConfig: LTSConfig.Log) {
        LoggerFactory.configConsole(Level.OFF, names = *LTManifest.thirdLibs)
        LoggerFactory.configConsole(level = logConfig.logLevel)
        LoggerFactory.configFile(
            level = logConfig.logLevel,
            file = logConfig.file,
            maxBackupIndex = logConfig.maxBackupIndex,
            maxFileSize = OptionConverter.toFileSize(logConfig.maxFileSize, 1)
        )
    }

}