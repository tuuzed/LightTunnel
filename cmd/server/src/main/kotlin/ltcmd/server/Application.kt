@file:Suppress("DuplicatedCode")

package ltcmd.server

import lighttunnel.cmd.AbstractApplication
import lighttunnel.cmd.asInt
import lighttunnel.logger.LoggerFactory
import lighttunnel.logger.loggerDelegate
import lighttunnel.server.TunnelRequestInterceptor
import lighttunnel.server.TunnelServer
import lighttunnel.server.http.HttpFd
import lighttunnel.server.http.HttpPlugin
import lighttunnel.server.http.HttpRequestInterceptor
import lighttunnel.server.tcp.TcpFd
import lighttunnel.util.BuildConfig
import lighttunnel.util.SslContextUtil
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Options
import org.apache.log4j.Level
import org.apache.log4j.helpers.OptionConverter
import org.ini4j.Ini
import org.ini4j.Profile
import java.io.File

class Application : AbstractApplication(), TunnelServer.OnTcpTunnelStateListener, TunnelServer.OnHttpTunnelStateListener {

    override val options: Options
        get() = Options().apply {
            addOption("h", "help", false, "帮助信息")
            addOption("v", "version", false, "版本信息")
            addOption("c", "config", true, "配置文件, 默认为lts.ini")
        }


    override fun run(commandLine: CommandLine) {
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
        setupLogConf(basic)
        val tunnelServer = getTunnelServer(basic)
        tunnelServer.start()
    }

    override fun onHttpTunnelConnected(fd: HttpFd) {
        logger.info("onConnected: {}", fd)
    }

    override fun onHttpTunnelDisconnect(fd: HttpFd) {
        logger.info("onDisconnect: {}", fd)
    }

    override fun onTcpTunnelConnected(fd: TcpFd) {
        logger.info("onConnected: {}", fd)
    }

    override fun onTcpTunnelDisconnect(fd: TcpFd) {
        logger.info("onDisconnect: {}", fd)
    }

    private companion object {

        private val logger by loggerDelegate()

        private fun Application.getTunnelServer(basic: Profile.Section): TunnelServer {
            val tunnelRequestInterceptor = getTunnelRequestInterceptor(basic)
            return TunnelServer(
                bossThreads = basic["boss_threads"].asInt() ?: -1,
                workerThreads = basic["worker_threads"].asInt() ?: -1,
                tunnelDaemonArgs = getTunnelDaemonArgs(basic, tunnelRequestInterceptor),
                sslTunnelDaemonArgs = getSslTunnelDaemonArgs(basic, tunnelRequestInterceptor),
                httpTunnelArgs = getHttpTunnelArgs(basic, HttpRequestInterceptor.defaultImpl),
                httpsTunnelArgs = getHttpsTunnelArgs(basic, HttpRequestInterceptor.defaultImpl),
                httpRpcServerArgs = getHttpRpcServerArgs(basic),
                onTcpTunnelStateListener = this,
                onHttpTunnelStateListener = this
            )
        }

        private fun getTunnelRequestInterceptor(basic: Profile.Section): TunnelRequestInterceptor {
            val authToken = basic["auth_token"]
            val allowPorts = basic["allow_ports"]
            return if (authToken != null || allowPorts != null) {
                TunnelRequestInterceptor.defaultImpl(authToken, allowPorts)
            } else {
                TunnelRequestInterceptor.emptyImpl
            }
        }

        private fun getTunnelDaemonArgs(basic: Profile.Section, tunnelRequestInterceptor: TunnelRequestInterceptor): TunnelServer.TunnelDaemonArgs {
            return TunnelServer.TunnelDaemonArgs(
                bindAddr = basic["bind_addr"],
                bindPort = basic["bind_port"].asInt() ?: 5080,
                tunnelRequestInterceptor = tunnelRequestInterceptor
            )
        }

        private fun getSslTunnelDaemonArgs(basic: Profile.Section, tunnelRequestInterceptor: TunnelRequestInterceptor): TunnelServer.SslTunnelDaemonArgs {
            return TunnelServer.SslTunnelDaemonArgs(
                bindAddr = basic["bind_addr"],
                bindPort = basic["ssl_bind_port"].asInt() ?: 5443,
                tunnelRequestInterceptor = tunnelRequestInterceptor,
                sslContext = try {
                    val jks = basic["ssl_jks"] ?: "lts.jks"
                    val storePassword = basic["ssl_key_password"] ?: "ltspass"
                    val keyPassword = basic["ssl_store_password"] ?: "ltspass"
                    SslContextUtil.forServer(jks, storePassword, keyPassword)
                } catch (e: Exception) {
                    logger.warn("tunnel ssl used builtin jks.")
                    SslContextUtil.forBuiltinServer()
                }
            )
        }

        private fun getHttpTunnelArgs(http: Profile.Section, httpRequestInterceptor: HttpRequestInterceptor): TunnelServer.HttpTunnelArgs {
            val pluginSfPaths = http["plugin_sf_paths"]?.split(',')
            val pluginSfHosts = http["plugin_sf_hosts"]?.split(',')
            var sfHttpPlugin: HttpPlugin? = null
            if (!pluginSfPaths.isNullOrEmpty() && !pluginSfHosts.isNullOrEmpty()) {
                sfHttpPlugin = HttpPlugin.staticFileImpl(
                    paths = pluginSfPaths,
                    hosts = pluginSfHosts
                )
            }
            return TunnelServer.HttpTunnelArgs(
                bindAddr = http["bind_addr"],
                bindPort = http["http_port"].asInt(),
                httpRequestInterceptor = httpRequestInterceptor,
                httpPlugin = sfHttpPlugin
            )
        }

        private fun getHttpsTunnelArgs(https: Profile.Section, httpRequestInterceptor: HttpRequestInterceptor): TunnelServer.HttpsTunnelArgs {
            val pluginSfPaths = https["plugin_sf_paths"]?.split(',')
            val pluginSfHosts = https["plugin_sf_hosts"]?.split(',')
            var sfHttpPlugin: HttpPlugin? = null
            if (!pluginSfPaths.isNullOrEmpty() && !pluginSfHosts.isNullOrEmpty()) {
                sfHttpPlugin = HttpPlugin.staticFileImpl(
                    paths = pluginSfPaths,
                    hosts = pluginSfHosts
                )
            }
            return TunnelServer.HttpsTunnelArgs(
                bindAddr = https["bind_addr"],
                bindPort = https["https_port"].asInt(),
                httpRequestInterceptor = httpRequestInterceptor,
                httpPlugin = sfHttpPlugin,
                sslContext = try {
                    val jks = https["https_jks"] ?: "lts.jks"
                    val storePassword = https["https_key_password"] ?: "ltspass"
                    val keyPassword = https["https_store_password"] ?: "ltspass"
                    SslContextUtil.forServer(jks, storePassword, keyPassword)
                } catch (e: Exception) {
                    logger.warn("tunnel ssl used builtin jks.")
                    SslContextUtil.forBuiltinServer()
                }
            )
        }

        private fun getHttpRpcServerArgs(web: Profile.Section): TunnelServer.HttpRpcServerArgs {
            return TunnelServer.HttpRpcServerArgs(
                bindAddr = web["bind_addr"],
                bindPort = web["http_rpc_port"].asInt()
            )
        }

        private fun setupLogConf(basic: Profile.Section) {
            val logLevel = Level.toLevel(basic["log_level"], null) ?: Level.INFO
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
    }

}