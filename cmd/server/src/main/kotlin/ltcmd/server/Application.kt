package ltcmd.server

import lighttunnel.BuildConfig
import lighttunnel.cmd.AbstractApplication
import lighttunnel.logger.LoggerFactory
import lighttunnel.logger.loggerDelegate
import lighttunnel.server.TunnelRequestInterceptor
import lighttunnel.server.TunnelServer
import lighttunnel.server.http.HttpFd
import lighttunnel.server.http.HttpPlugin
import lighttunnel.server.http.HttpRequestInterceptor
import lighttunnel.server.tcp.TcpFd
import lighttunnel.util.SslContextUtil
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Options
import org.apache.log4j.Level
import org.apache.log4j.helpers.OptionConverter
import org.ini4j.Ini
import org.ini4j.Profile
import java.io.File

class Application : AbstractApplication() {
    private val logger by loggerDelegate()
    private var tunnelServer: TunnelServer? = null
    private val onTcpTunnelStateListener = object : TunnelServer.OnTcpTunnelStateListener {
        override fun onConnected(fd: TcpFd) {
            logger.info("onConnected: {}", fd)
        }

        override fun onDisconnect(fd: TcpFd) {
            logger.info("onDisconnect: {}", fd)
        }
    }
    private val onHttpTunnelStateListener = object : TunnelServer.OnHttpTunnelStateListener {
        override fun onConnected(fd: HttpFd) {
            logger.info("onConnected: {}", fd)
        }

        override fun onDisconnect(fd: HttpFd) {
            logger.info("onDisconnect: {}", fd)
        }
    }

    override val options: Options
        get() = Options().apply {
            addOption("h", "help", false, "帮助信息")
            addOption("v", "version", false, "版本信息")
            addOption("c", "config", true, "配置文件, 默认为lts.ini")
        }

    override fun main(commandLine: CommandLine) {
        if (commandLine.hasOption("h")) {
            printUsage()
            return
        }
        if (commandLine.hasOption("v")) {
            System.out.printf("%s%n", BuildConfig.VERSION_NAME)
            return
        }
        val configFilePath = commandLine.getOptionValue("c") ?: "lts.ini"
        val ini = Ini()
        ini.load(File(configFilePath))
        val basic = ini["basic"] ?: return
        loadLogConf(basic)
        tunnelServer = newTunnelServer(basic)
        start()
    }

    private fun start() {
        tunnelServer?.start()
    }

    private fun newTunnelServer(basic: Profile.Section): TunnelServer {
        val authToken = basic["auth_token"]
        val allowPorts = basic["allow_ports"]
        val httpRequestInterceptor = HttpRequestInterceptor.defaultImpl
        val tunnelRequestInterceptor = if (authToken != null || allowPorts != null) {
            TunnelRequestInterceptor.defaultImpl(authToken, allowPorts)
        } else {
            TunnelRequestInterceptor.emptyImpl
        }
        val pluginSfPaths = basic["plugin_sf_paths"]?.split(',')
        val pluginSfHosts = basic["plugin_sf_hosts"]?.split(',')
        var sfHttpPlugin: HttpPlugin? = null
        if (!pluginSfPaths.isNullOrEmpty() && !pluginSfHosts.isNullOrEmpty()) {
            sfHttpPlugin = HttpPlugin.StaticFileImpl(
                paths = pluginSfPaths,
                hosts = pluginSfHosts
            )
        }
        return TunnelServer(
            bossThreads = basic["boss_threads"].asInt() ?: -1,
            workerThreads = basic["worker_threads"].asInt() ?: -1,
            // tunnel
            bindAddr = basic["bind_addr"],
            bindPort = basic["bind_port"].asInt() ?: 5080,
            // ssl tunnel
            sslBindPort = basic["ssl_bind_port"].asInt(),
            sslContext = if (basic["ssl_bind_port"] != null) {
                try {
                    val jks = basic["ssl_jks"] ?: "lts.jks"
                    val storePassword = basic["ssl_key_password"] ?: "ltspass"
                    val keyPassword = basic["ssl_store_password"] ?: "ltspass"
                    SslContextUtil.forServer(jks, storePassword, keyPassword)
                } catch (e: Exception) {
                    logger.warn("tunnel ssl used builtin jks.")
                    SslContextUtil.forBuiltinServer()
                }
            } else {
                null
            },
            tunnelRequestInterceptor = tunnelRequestInterceptor,
            // http
            httpBindPort = basic["vhost_http_port"].asInt(),
            httpRequestInterceptor = httpRequestInterceptor,
            // https
            httpsBindPort = basic["vhost_https_port"].asInt(),
            httpsContext = if (basic["vhost_https_port"] != null) {
                try {
                    val jks = basic["ssl_jks"] ?: "lts.jks"
                    val storePassword = basic["ssl_key_password"] ?: "ltspass"
                    val keyPassword = basic["ssl_store_password"] ?: "ltspass"
                    SslContextUtil.forServer(jks, storePassword, keyPassword)
                } catch (e: Exception) {
                    logger.warn("tunnel https used builtin jks.")
                    SslContextUtil.forBuiltinServer()
                }
            } else {
                null
            },
            httpsRequestInterceptor = httpRequestInterceptor,
            // plugin
            httpPlugin = sfHttpPlugin,
            // dashboard
            dashboardBindPort = basic["dashboard_bind_port"].asInt(),
            // listener
            onTcpTunnelStateListener = onTcpTunnelStateListener,
            onHttpTunnelStateListener = onHttpTunnelStateListener
        )
    }

    private fun loadLogConf(basic: Profile.Section) {
        val logLevel = Level.toLevel(basic["log_level"], Level.INFO)
        val logFile = basic["log_file"]
        val logCount = basic["log_count"].asInt() ?: 3
        val logSize = basic["log_size"] ?: "1MB"
        LoggerFactory.configConsole(Level.OFF, names = *arrayOf(
            "io.netty",
            "org.ini4j",
            "org.slf4j",
            "org.json",
            "org.apache.commons.cli"
        ))
        LoggerFactory.configConsole(level = logLevel, conversionPattern = "%-d{yyyy-MM-dd HH:mm:ss} - [ %p ] %m%n")
        if (logFile != null) {
            LoggerFactory.configFile(
                level = logLevel,
                file = logFile,
                maxBackupIndex = logCount,
                maxFileSize = OptionConverter.toFileSize(logSize, 1)
            )
        }
    }

    private fun String?.asInt(): Int? {
        return try {
            this?.toInt()
        } catch (e: NumberFormatException) {
            return null
        }
    }

}