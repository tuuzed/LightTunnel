package tpc

import io.netty.handler.ssl.SslContext
import org.apache.log4j.Level
import org.apache.log4j.helpers.OptionConverter
import tpclient.OnTPClientStateListener
import tpclient.TPClient
import tpclient.TPClientDescriptor
import tpcommon.*
import tpstarter.CmdLineParser
import tpstarter.Yaml
import java.io.File

class TPC {
    private val logger by logger()
    private val onTPClientStateListener = object : OnTPClientStateListener {
        override fun onConnecting(descriptor: TPClientDescriptor, reconnect: Boolean) {
            logger.debug("onConnecting: {}", descriptor)
        }

        override fun onConnected(descriptor: TPClientDescriptor) {
            logger.info("onConnected: {}", descriptor)
        }

        override fun onDisconnect(descriptor: TPClientDescriptor, err: Boolean, errCause: Throwable?) {
            logger.info("onDisconnect: {}, err: {}", descriptor, err, errCause)
        }
    }

    fun doMain(args: Array<String>) {
        val config = getConfig(args) ?: return
        configureLogger(config.log)
        val options = TPClient.Options()
        with(options) {
            workerThreads = config.basic.workerThreads
            listener = onTPClientStateListener
            autoReconnect = true
        }
        val tpClient = TPClient(options)
        var sslContext: SslContext? = null
        for (it in config.tunnels) {
            val tpRequest = when (it.tpType) {
                TPType.TCP -> {
                    TPRequest.ofTcp(
                        authToken = config.basic.authToken,
                        localAddr = it.localAddr,
                        localPort = it.localPort,
                        remotePort = it.remotePort
                    )
                }
                TPType.HTTP, TPType.HTTPS -> {
                    TPRequest.ofHttp(
                        https = it.tpType != TPType.HTTP,
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
            if (tpRequest != null) {
                if (it.enableSsl && sslContext == null) {
                    val jks = config.ssl.jks
                    val storepass = config.ssl.storepass
                    requireNotNull(jks)
                    requireNotNull(storepass)
                    sslContext = SslContexts.forClient(jks, storepass)
                }
                tpClient.connect(
                    config.basic.serverAddr,
                    if (it.enableSsl) config.ssl.serverPort else config.basic.serverPort,
                    tpRequest,
                    if (it.enableSsl) sslContext else null
                )
            }
        }
    }

    private fun getConfig(args: Array<String>): TPCConfig? {
        val cmdLine = TPCCmdLine()
        if (args.isEmpty()) {
            return TPCConfig()
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
            System.err.printf(Yaml.dump(TPCConfig().also {
                it.tunnels = listOf(TPCConfig.Tunnel(), TPCConfig.Tunnel())
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

    private fun configureLogger(logConfig: TPCConfig.Log) {
        LoggerFactory.configConsole(Level.OFF, names = *LoggerFactory.thirdLibs).apply()
        LoggerFactory.configConsole(level = logConfig.logLevel).apply()
        LoggerFactory.configFile(
            level = logConfig.logLevel,
            file = logConfig.file,
            maxBackupIndex = logConfig.maxBackupIndex,
            maxFileSize = OptionConverter.toFileSize(logConfig.maxFileSize, 1)
        ).apply()
    }

}