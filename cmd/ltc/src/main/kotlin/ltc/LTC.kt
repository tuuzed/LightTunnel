package ltc

import io.netty.handler.ssl.SslContext
import org.apache.log4j.Level
import org.apache.log4j.helpers.OptionConverter
import com.tuuzed.lighttunnel.client.OnLTClientStateListener
import com.tuuzed.lighttunnel.client.LTClient
import com.tuuzed.lighttunnel.client.LTClientDescriptor
import com.tuuzed.lighttunnel.common.*
import com.tuuzed.lighttunnel.cmd.CmdLineParser
import com.tuuzed.lighttunnel.cmd.Yaml
import java.io.File

class LTC {
    private val logger by logger()
    private val onTPClientStateListener = object : OnLTClientStateListener {
        override fun onConnecting(descriptor: LTClientDescriptor, reconnect: Boolean) {
            logger.debug("onConnecting: {}", descriptor)
        }

        override fun onConnected(descriptor: LTClientDescriptor) {
            logger.info("onConnected: {}", descriptor)
        }

        override fun onDisconnect(descriptor: LTClientDescriptor, err: Boolean, errCause: Throwable?) {
            logger.info("onDisconnect: {}, err: {}", descriptor, err, errCause)
        }
    }

    fun doMain(args: Array<String>) {
        val config = getConfig(args) ?: return
        configureLogger(config.log)
        val options = LTClient.Options()
        with(options) {
            workerThreads = config.basic.workerThreads
            listener = onTPClientStateListener
            autoReconnect = true
        }
        val client = LTClient(options)
        var sslContext: SslContext? = null
        for (it in config.tunnels) {
            val request = when (it.typeEume) {
                LTRequest.Type.TCP -> {
                    LTRequest.ofTcp(
                        authToken = config.basic.authToken,
                        localAddr = it.localAddr,
                        localPort = it.localPort,
                        remotePort = it.remotePort
                    )
                }
                LTRequest.Type.HTTP,  LTRequest.Type.HTTPS -> {
                    LTRequest.ofHttp(
                        https = it.typeEume != LTRequest.Type.HTTP,
                        authToken = config.basic.authToken,
                        localAddr = it.localAddr,
                        localPort = it.localPort,
                        host = it.httpHost,
                        proxySetHeaders = it.httpSetHeaders ?: emptyMap(),
                        proxyAddHeaders = it.httpAddHeaders ?: emptyMap(),
                        enableBasicAuth = it.httpAuth.enable,
                        basicAuthRealm = it.httpAuth.realm,
                        basicAuthUsername = it.httpAuth.username,
                        basicAuthPassword = it.httpAuth.password
                    )
                }
                else -> null
            }
            if (request != null) {
                if (it.enableSsl && sslContext == null) {
                    val jks = config.ssl.jks
                    val storepass = config.ssl.storepass
                    requireNotNull(jks)
                    requireNotNull(storepass)
                    sslContext = SslContextUtil.forClient(jks, storepass)
                }
                client.connect(
                    config.basic.serverAddr,
                    if (it.enableSsl) config.ssl.serverPort else config.basic.serverPort,
                    request,
                    if (it.enableSsl) sslContext else null
                )
            }
        }
    }

    private fun getConfig(args: Array<String>): LTCConfig? {
        val cmdLine = LTCCmdLine()
        if (args.isEmpty()) {
            return LTCConfig()
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
            System.err.printf(Yaml.dump(LTCConfig().also {
                it.tunnels = listOf(LTCConfig.Tunnel(), LTCConfig.Tunnel())
            }))
            return null
        } else {
            if (cmdLine.yaml.isNotEmpty()) {
                Yaml.load(cmdLine.yaml)
            } else {
                Yaml.load(File(cmdLine.configFile).readText(Charsets.UTF_8))
            }
        }
    }

    private fun configureLogger(logConfig: LTCConfig.Log) {
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