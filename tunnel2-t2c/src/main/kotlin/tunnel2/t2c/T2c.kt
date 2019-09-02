package tunnel2.t2c


import io.netty.handler.ssl.SslContext
import org.apache.log4j.Level
import org.apache.log4j.helpers.OptionConverter
import org.yaml.snakeyaml.Yaml
import tunnel2.client.TunnelClient
import tunnel2.client.TunnelClientDescriptor
import tunnel2.common.TunnelRequest
import tunnel2.common.TunnelType
import tunnel2.common.logging.LoggerFactory
import tunnel2.common.ssl.SslContexts
import tunnel2.t2cli.*
import java.io.FileReader

@Suppress("UNCHECKED_CAST")
class T2c : AbstractApp<RunOptions>() {

    companion object {
        private val logger = LoggerFactory.getLogger(T2c::class.java)
    }


    private val tunnelClientListener = object : TunnelClient.Listener {
        override fun onConnecting(descriptor: TunnelClientDescriptor, reconnect: Boolean) {
            logger.debug("onConnecting: {}", descriptor)
        }

        override fun onConnected(descriptor: TunnelClientDescriptor) {
            logger.info("onConnected: {}", descriptor)
        }

        override fun onDisconnect(descriptor: TunnelClientDescriptor, err: Boolean, errCause: Throwable?) {
            logger.info("onDisconnect: {}, err: {}", descriptor, err, errCause)
        }
    }

    override fun newRunOptions(): RunOptions {
        return RunOptions()
    }

    private fun createTunnelClient(workerThreads: Int): TunnelClient {
        return TunnelClient(
            workerThreads, true, tunnelClientListener
        )
    }

    @Throws(Exception::class)
    override fun runApp(runOptions: RunOptions) {

        if (runOptions.configFile.isNotEmpty()) runAppAtCfg(runOptions.configFile)
        else runAppAtArgs(runOptions)
        Thread.currentThread().join()
    }

    @Throws(Exception::class)
    private fun runAppAtCfg(cfgFile: String) {
        val globalCfg = Yaml().loadAs(FileReader(cfgFile), Map::class.java)
        // ================================== logCfg ================================== //
        val logCfg = globalCfg.getMap("log")
        LoggerFactory.configConsole(Level.OFF, names = *LoggerFactory.thirdLibs).apply()
        // 配置控制台日志
        LoggerFactory.configConsole(
            level = LogLevel.valueOf(logCfg.getString("level", "OFF").toUpperCase()).level
        ).apply()
        // 配置文件日志
        LoggerFactory.configFile(
            level = LogLevel.valueOf(logCfg.getString("level", "OFF").toUpperCase()).level,
            file = logCfg.getString("file", "./logs/t2c.log"),
            maxFileSize = OptionConverter.toFileSize(logCfg.getString("max_file_size", "1MB"), 1),
            maxBackupIndex = logCfg.getInt("max_backup_index", 3)
        ).apply()


        // ================================== common ================================== //
        val serverAddr = globalCfg.getString("server_addr", "0.0.0.0")
        val serverPort = globalCfg.getInt("server_addr", 5000)
        val authToken = globalCfg.getString("auth_token", "")
        val workerThreads = globalCfg.getInt("worker_threads", -1)
        val tunnelClient = createTunnelClient(workerThreads)

        // ================================== sslCfg ================================== //
        var sslContext: SslContext? = null
        var sslServerPort = serverPort
        val sslCfg = globalCfg.getMap("ssl")

        if (sslCfg.isNotEmpty()) {
            sslContext = SslContexts.forClient(
                sslCfg.getString("jks", ""),
                sslCfg.getString("storepass", "")
            )
            sslServerPort = sslCfg.getInt("server_port", 5001)
        }


        // ================================== tunnelsCfg ================================== //

        val tunnelsCfg = globalCfg.getListMap("tunnels")
        for (tunnelCfg in tunnelsCfg) {
            val type = TunnelType.valueOf(tunnelCfg.getString("type", "unknown").toUpperCase())
            val enableSsl = tunnelCfg.getBoolean("enable_ssl", false)
            val localAddr = tunnelCfg.getString("local_addr", "")
            val localPort = tunnelCfg.getInt("local_port", 0)

            val tunnelRequest = when (type) {
                TunnelType.TCP -> {
                    TunnelRequest.ofTcp(
                        localAddr = localAddr,
                        localPort = localPort,
                        remotePort = tunnelCfg.getInt("remote_port", 0),
                        authToken = authToken
                    )
                }
                TunnelType.HTTP -> {
                    val authCfg = tunnelCfg.getMap("auth")
                    TunnelRequest.ofHttp(
                        localAddr = localAddr,
                        localPort = localPort,
                        vhost = tunnelCfg.getString("vhost", ""),
                        authToken = authToken,
                        proxySetHeaders = tunnelCfg.getMap("proxy_set_headers") as Map<String, String>,
                        proxyAddHeaders = tunnelCfg.getMap("proxy_add_headers") as Map<String, String>,
                        enableBasicAuth = authCfg.getBoolean("enable", false),
                        basicAuthRealm = authCfg.getString("realm", "."),
                        basicAuthUsername = authCfg.getString("username", ""),
                        basicAuthPassword = authCfg.getString("password", "")
                    )
                }
                TunnelType.HTTPS -> {
                    val authCfg = tunnelCfg.getMap("auth")
                    TunnelRequest.ofHttps(
                        localAddr = localAddr,
                        localPort = localPort,
                        vhost = tunnelCfg.getString("vhost", ""),
                        authToken = authToken,
                        proxySetHeaders = tunnelCfg.getMap("proxy_set_headers") as Map<String, String>,
                        proxyAddHeaders = tunnelCfg.getMap("proxy_add_headers") as Map<String, String>,
                        enableBasicAuth = authCfg.getBoolean("enable", false),
                        basicAuthRealm = authCfg.getString("realm", "."),
                        basicAuthUsername = authCfg.getString("username", ""),
                        basicAuthPassword = authCfg.getString("password", "")
                    )
                }
                else -> null
            }
            if (tunnelRequest != null) {
                tunnelClient.connect(
                    serverAddr,
                    if (enableSsl) sslServerPort else serverPort,
                    tunnelRequest,
                    if (enableSsl) sslContext else null
                )
            }
        }
    }

    @Throws(Exception::class)
    private fun runAppAtArgs(runOptions: RunOptions) {
        val tunnelClient = createTunnelClient(runOptions.workerThreads)

        LoggerFactory.configConsole(Level.OFF, names = *LoggerFactory.thirdLibs).apply()
        // 配置控制台日志
        LoggerFactory.configConsole(
            level = runOptions.logLevel.level
        ).apply()
        // 配置文件日志
        LoggerFactory.configFile(
            level = runOptions.logLevel.level,
            file = runOptions.logFile,
            maxFileSize = OptionConverter.toFileSize(runOptions.logMaxFileSize, 1),
            maxBackupIndex = runOptions.logMaxBackupIndex
        ).apply()

        val tunnelRequest = when (runOptions.type) {
            TunnelType.TCP -> {
                TunnelRequest.ofTcp(
                    localAddr = runOptions.localAddr,
                    localPort = runOptions.localPort,
                    remotePort = runOptions.remotePort,
                    authToken = runOptions.authToken
                )
            }
            TunnelType.HTTP -> {
                TunnelRequest.ofHttp(
                    localAddr = runOptions.localAddr,
                    localPort = runOptions.localPort,
                    vhost = runOptions.vhost,
                    authToken = runOptions.authToken,
                    proxySetHeaders = runOptions.proxySetHeaders,
                    proxyAddHeaders = runOptions.proxyAddHeaders,
                    enableBasicAuth = runOptions.httpAuthEnable,
                    basicAuthRealm = runOptions.httpAuthRealm,
                    basicAuthUsername = runOptions.httpAuthUsername,
                    basicAuthPassword = runOptions.httpAuthPassword
                )
            }
            TunnelType.HTTPS -> {
                TunnelRequest.ofHttps(
                    localAddr = runOptions.localAddr,
                    localPort = runOptions.localPort,
                    vhost = runOptions.vhost,
                    authToken = runOptions.authToken,
                    proxySetHeaders = runOptions.proxySetHeaders,
                    proxyAddHeaders = runOptions.proxyAddHeaders,
                    enableBasicAuth = runOptions.httpAuthEnable,
                    basicAuthRealm = runOptions.httpAuthRealm,
                    basicAuthUsername = runOptions.httpAuthUsername,
                    basicAuthPassword = runOptions.httpAuthPassword
                )
            }
            else -> null
        }
        var sslContext: SslContext? = null
        if (runOptions.sslEnable) {
            sslContext = SslContexts.forClient(runOptions.sslJks, runOptions.sslStorepass)
        }
        if (tunnelRequest != null) {
            tunnelClient.connect(runOptions.serverAddr, runOptions.serverPort, tunnelRequest, sslContext)
        }
    }


}
