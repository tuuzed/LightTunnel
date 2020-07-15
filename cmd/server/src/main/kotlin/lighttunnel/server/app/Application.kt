@file:Suppress("DuplicatedCode")

package lighttunnel.server.app

import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.*
import lighttunnel.base.logger.LoggerFactory
import lighttunnel.base.logger.loggerDelegate
import lighttunnel.base.util.SslContextUtil
import lighttunnel.cmd.AbstractApplication
import lighttunnel.cmd.ext.name
import lighttunnel.cmd.http.server.HttpServer
import lighttunnel.cmd.util.asInt
import lighttunnel.cmd.util.format
import lighttunnel.openapi.BuildConfig
import lighttunnel.openapi.TunnelRequestInterceptor
import lighttunnel.openapi.TunnelServer
import lighttunnel.openapi.args.HttpTunnelArgs
import lighttunnel.openapi.args.HttpsTunnelArgs
import lighttunnel.openapi.args.SslTunnelDaemonArgs
import lighttunnel.openapi.args.TunnelDaemonArgs
import lighttunnel.openapi.http.HttpFd
import lighttunnel.openapi.http.HttpPlugin
import lighttunnel.openapi.http.HttpTunnelRequestInterceptor
import lighttunnel.openapi.listener.OnHttpTunnelStateListener
import lighttunnel.openapi.listener.OnTcpTunnelStateListener
import lighttunnel.openapi.tcp.TcpFd
import lighttunnel.server.app.impls.HttpPluginStaticFileImpl
import lighttunnel.server.app.impls.HttpTunnelRequestInterceptorDefaultImpl
import lighttunnel.server.app.impls.TunnelRequestInterceptorDefaultImpl
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.apache.log4j.Level
import org.apache.log4j.helpers.OptionConverter
import org.ini4j.Ini
import org.ini4j.Profile
import org.json.JSONArray
import org.json.JSONObject
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
            val bossGroup = if (bossThreads >= 0) NioEventLoopGroup(bossThreads) else NioEventLoopGroup()
            val workerGroup = if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
            val httpRpcServer = getHttpRpcServer(
                bossGroup = bossGroup,
                workerGroup = workerGroup,
                bindAddr = bindAddr,
                bindPort = httpRpcPort,
                tunnelServer = tunnelServer
            )
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

        private fun getHttpRpcServer(
            bossGroup: NioEventLoopGroup,
            workerGroup: NioEventLoopGroup,
            bindAddr: String?,
            bindPort: Int,
            tunnelServer: TunnelServer
        ): HttpServer {
            return HttpServer(
                bossGroup = bossGroup,
                workerGroup = workerGroup,
                bindAddr = bindAddr,
                bindPort = bindPort
            ) {
                route("/api/version") {
                    val content = JSONObject().apply {
                        put("name", "lts")
                        put("versionName", BuildConfig.VERSION_NAME)
                        put("versionCode", BuildConfig.VERSION_CODE)
                        put("buildDate", BuildConfig.BUILD_DATA)
                        put("commitSha", BuildConfig.LAST_COMMIT_SHA)
                        put("commitDate", BuildConfig.LAST_COMMIT_DATE)
                    }.let { Unpooled.copiedBuffer(it.toString(2), Charsets.UTF_8) }
                    DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        content
                    ).apply {
                        headers()
                            .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                            .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
                    }
                }
                route("/api/snapshot") {
                    val content = JSONObject().apply {
                        put("tcp", tunnelServer.getTcpFdList().tcpFdListToJson())
                        put("http", tunnelServer.getHttpFdList().httpFdListToJson())
                        put("https", tunnelServer.getHttpsFdList().httpFdListToJson())
                    }.let { Unpooled.copiedBuffer(it.toString(2), Charsets.UTF_8) }
                    DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        content
                    ).apply {
                        headers()
                            .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                            .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
                    }
                }
            }
        }


        private fun Application.getTunnelServer(basic: Profile.Section): TunnelServer {
            val tunnelRequestInterceptor = getTunnelRequestInterceptor(basic)
            val httpTunnelRequestInterceptor = HttpTunnelRequestInterceptorDefaultImpl()
            return TunnelServer(
                bossThreads = basic["boss_threads"].asInt() ?: -1,
                workerThreads = basic["worker_threads"].asInt() ?: -1,
                tunnelDaemonArgs = getTunnelDaemonArgs(basic, tunnelRequestInterceptor),
                sslTunnelDaemonArgs = getSslTunnelDaemonArgs(basic, tunnelRequestInterceptor),
                httpTunnelArgs = getHttpTunnelArgs(basic, httpTunnelRequestInterceptor),
                httpsTunnelArgs = getHttpsTunnelArgs(basic, httpTunnelRequestInterceptor),
                onTcpTunnelStateListener = this,
                onHttpTunnelStateListener = this
            )
        }

        private fun getTunnelRequestInterceptor(basic: Profile.Section): TunnelRequestInterceptor {
            val authToken = basic["auth_token"]
            val allowPorts = basic["allow_ports"]
            return if (authToken != null || allowPorts != null) {
                TunnelRequestInterceptorDefaultImpl(authToken, allowPorts)
            } else {
                TunnelRequestInterceptor.emptyImpl
            }
        }

        private fun getTunnelDaemonArgs(basic: Profile.Section, tunnelRequestInterceptor: TunnelRequestInterceptor): TunnelDaemonArgs {
            return TunnelDaemonArgs(
                bindAddr = basic["bind_addr"],
                bindPort = basic["bind_port"].asInt() ?: 5080,
                tunnelRequestInterceptor = tunnelRequestInterceptor
            )
        }

        private fun getSslTunnelDaemonArgs(basic: Profile.Section, tunnelRequestInterceptor: TunnelRequestInterceptor): SslTunnelDaemonArgs {
            return SslTunnelDaemonArgs(
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

        private fun getHttpTunnelArgs(http: Profile.Section, httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor): HttpTunnelArgs {
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
                bindPort = http["http_port"].asInt(),
                httpTunnelRequestInterceptor = httpTunnelRequestInterceptor,
                httpPlugin = httpPlugin
            )
        }

        private fun getHttpsTunnelArgs(https: Profile.Section, httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor): HttpsTunnelArgs {
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
                bindPort = https["https_port"].asInt(),
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


        @Suppress("DuplicatedCode")
        private fun List<TcpFd>.tcpFdListToJson(): JSONArray {
            return JSONArray(
                map { fd ->
                    JSONObject().apply {
                        put("port", fd.tunnelRequest.remotePort)
                        put("conns", fd.connectionCount)
                        put("name", fd.tunnelRequest.name)
                        put("localAddr", fd.tunnelRequest.localAddr)
                        put("localPort", fd.tunnelRequest.localPort)
                        put("inboundBytes", fd.statistics.inboundBytes)
                        put("outboundBytes", fd.statistics.outboundBytes)
                        put("createAt", fd.statistics.createAt.format())
                        put("updateAt", fd.statistics.updateAt.format())
                    }
                }
            )
        }

        @Suppress("DuplicatedCode")
        private fun List<HttpFd>.httpFdListToJson(): JSONArray {
            return JSONArray(
                map { fd ->
                    JSONObject().apply {
                        put("host", fd.tunnelRequest.host)
                        put("conns", fd.connectionCount)
                        put("name", fd.tunnelRequest.name)
                        put("localAddr", fd.tunnelRequest.localAddr)
                        put("localPort", fd.tunnelRequest.localPort)
                        put("inboundBytes", fd.statistics.inboundBytes)
                        put("outboundBytes", fd.statistics.outboundBytes)
                        put("createAt", fd.statistics.createAt.format())
                        put("updateAt", fd.statistics.updateAt.format())
                    }
                }
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