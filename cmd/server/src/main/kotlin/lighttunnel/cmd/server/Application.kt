@file:Suppress("DuplicatedCode")

package lighttunnel.cmd.server

import io.netty.channel.nio.NioEventLoopGroup
import lighttunnel.base.util.loggerDelegate
import lighttunnel.cmd.AbstractApplication
import lighttunnel.cmd.asInt
import lighttunnel.openapi.BuildConfig
import lighttunnel.openapi.SslContextUtil
import lighttunnel.openapi.TunnelRequestInterceptor
import lighttunnel.openapi.TunnelServer
import lighttunnel.openapi.args.HttpTunnelArgs
import lighttunnel.openapi.args.HttpsTunnelArgs
import lighttunnel.openapi.args.SslTunnelDaemonArgs
import lighttunnel.openapi.args.TunnelDaemonArgs
import lighttunnel.openapi.ext.*
import lighttunnel.openapi.http.HttpFd
import lighttunnel.openapi.http.HttpPlugin
import lighttunnel.openapi.listener.OnHttpTunnelStateListener
import lighttunnel.openapi.listener.OnTcpTunnelStateListener
import lighttunnel.openapi.tcp.TcpFd
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.apache.log4j.Level
import org.apache.log4j.helpers.OptionConverter
import org.ini4j.Ini
import org.ini4j.Profile
import java.io.File

class Application : AbstractApplication(), OnTcpTunnelStateListener, OnHttpTunnelStateListener {

    override val options: Options = Options().apply {
        addOption("h", "help", false, "帮助信息")
        addOption("v", "version", false, "版本信息")
        addOption("c", "config", true, "配置文件, 默认为lts.ini")
    }

    @Throws(ParseException::class)
    override fun run(commandLine: CommandLine) {
        if (commandLine.hasOption("h")) {
            throw ParseException("printUsage")
        }
        if (commandLine.hasOption("v")) {
            System.out.printf("%s(%d)%n", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
            return
        }
        val configFilePath = commandLine.getOptionValue("c") ?: "lts.ini"
        val ini = Ini()
        ini.load(File(configFilePath))
        val basic = ini["basic"] ?: return
        setupLogConf(basic)
        val tunnelServer = getTunnelServer(basic)
        val httpRpcPort = basic["http_rpc_port"].asInt()
        if (httpRpcPort != null) {
            val bossThreads = basic["boss_threads"].asInt() ?: -1
            val workerThreads = basic["worker_threads"].asInt() ?: -1
            val bindAddr = basic["bind_addr"]
            val httpRpcUsername = basic["http_rpc_username"]
            val httpRpcPassword = basic["http_rpc_password"]
            val bossGroup = if (bossThreads >= 0) NioEventLoopGroup(bossThreads) else NioEventLoopGroup()
            val workerGroup = if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
            val httpRpcServer = tunnelServer.newHttpRpcServer(
                bossGroup = bossGroup,
                workerGroup = workerGroup,
                bindAddr = bindAddr,
                bindPort = httpRpcPort
            ) { username, password ->
                if (httpRpcUsername != null && httpRpcPassword != null) {
                    httpRpcUsername == username && httpRpcPassword == password
                } else {
                    true
                }
            }
            httpRpcServer.start()
        }
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
        private val httpTunnelRequestInterceptor by lazy { HttpTunnelRequestInterceptorDefaultImpl() }

        private fun Application.getTunnelServer(basic: Profile.Section): TunnelServer {
            val tunnelRequestInterceptor = getTunnelRequestInterceptor(basic)
            return TunnelServer(
                bossThreads = basic["boss_threads"].asInt() ?: -1,
                workerThreads = basic["worker_threads"].asInt() ?: -1,
                tunnelDaemonArgs = getTunnelDaemonArgs(basic, tunnelRequestInterceptor),
                sslTunnelDaemonArgs = getSslTunnelDaemonArgs(basic, tunnelRequestInterceptor),
                httpTunnelArgs = getHttpTunnelArgs(basic),
                httpsTunnelArgs = getHttpsTunnelArgs(basic),
                isHttpAndHttpsShareRegistry = "true".equals(basic["http_https_share_registry"], true),
                onTcpTunnelStateListener = this,
                onHttpTunnelStateListener = this
            )
        }

        private fun getTunnelRequestInterceptor(basic: Profile.Section): TunnelRequestInterceptor {
            val authToken = basic["auth_token"]
            val allowPorts = basic["allow_ports"]
            return TunnelRequestInterceptorDefaultImpl(authToken, allowPorts)
        }

        private fun getTunnelDaemonArgs(basic: Profile.Section, tunnelRequestInterceptor: TunnelRequestInterceptor?): TunnelDaemonArgs {
            return TunnelDaemonArgs(
                bindAddr = basic["bind_addr"],
                bindPort = basic["bind_port"].asInt() ?: 5080,
                tunnelRequestInterceptor = tunnelRequestInterceptor
            )
        }

        private fun getSslTunnelDaemonArgs(basic: Profile.Section, tunnelRequestInterceptor: TunnelRequestInterceptor?): SslTunnelDaemonArgs? {
            return SslTunnelDaemonArgs(
                bindAddr = basic["bind_addr"],
                bindPort = basic["ssl_bind_port"].asInt() ?: return null,
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

        private fun getHttpTunnelArgs(http: Profile.Section): HttpTunnelArgs? {
            val bindPort = http["http_port"].asInt() ?: return null
            val pluginSfPaths = http["plugin_sf_paths"]?.split(',')
            val pluginSfHosts = http["plugin_sf_hosts"]?.split(',')
            var httpPlugin: HttpPlugin? = null
            if (!pluginSfPaths.isNullOrEmpty() && !pluginSfHosts.isNullOrEmpty()) {
                httpPlugin = HttpPluginStaticFileImpl(
                    paths = pluginSfPaths,
                    hosts = pluginSfHosts
                )
            }
            return HttpTunnelArgs(
                bindAddr = http["bind_addr"],
                bindPort = bindPort,
                httpTunnelRequestInterceptor = httpTunnelRequestInterceptor,
                httpPlugin = httpPlugin
            )
        }

        private fun getHttpsTunnelArgs(https: Profile.Section): HttpsTunnelArgs? {
            val bindPort = https["https_port"].asInt() ?: return null
            val pluginSfPaths = https["plugin_sf_paths"]?.split(',')
            val pluginSfHosts = https["plugin_sf_hosts"]?.split(',')
            var httpPlugin: HttpPlugin? = null
            if (!pluginSfPaths.isNullOrEmpty() && !pluginSfHosts.isNullOrEmpty()) {
                httpPlugin = HttpPluginStaticFileImpl(
                    paths = pluginSfPaths,
                    hosts = pluginSfHosts
                )
            }
            return HttpsTunnelArgs(
                bindAddr = https["bind_addr"],
                bindPort = bindPort,
                httpTunnelRequestInterceptor = httpTunnelRequestInterceptor,
                httpPlugin = httpPlugin,
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

        private fun setupLogConf(basic: Profile.Section) {
            val logLevel = Level.toLevel(basic["log_level"], null) ?: Level.INFO
            val logFile = basic["log_file"]
            val logCount = basic["log_count"].asInt() ?: 3
            val logSize = basic["log_size"] ?: "1MB"
            LoggerConfigure.configConsole(Level.OFF, names = arrayOf(
                "io.netty",
                "org.ini4j",
                "org.slf4j",
                "org.json",
                "org.apache.commons.cli"
            ))
            LoggerConfigure.configConsole(level = logLevel, conversionPattern = "%-d{yyyy-MM-dd HH:mm:ss} - [ %p ] %m%n")
            if (logFile != null) {
                LoggerConfigure.configFile(
                    level = logLevel,
                    file = logFile,
                    maxBackupIndex = logCount,
                    maxFileSize = OptionConverter.toFileSize(logSize, 1)
                )
            }
        }
    }

}