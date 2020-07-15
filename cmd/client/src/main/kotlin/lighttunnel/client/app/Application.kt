package lighttunnel.client.app

import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.*
import io.netty.handler.ssl.SslContext
import lighttunnel.base.logger.LoggerFactory
import lighttunnel.base.logger.loggerDelegate
import lighttunnel.base.util.SslContextUtil
import lighttunnel.cmd.AbstractApplication
import lighttunnel.cmd.ext.*
import lighttunnel.cmd.http.server.HttpServer
import lighttunnel.cmd.util.IpAddressUtil
import lighttunnel.cmd.util.asInt
import lighttunnel.openapi.BuildConfig
import lighttunnel.openapi.RemoteConnection
import lighttunnel.openapi.TunnelClient
import lighttunnel.openapi.TunnelClient.Companion.RETRY_CONNECT_POLICY_ERROR
import lighttunnel.openapi.TunnelClient.Companion.RETRY_CONNECT_POLICY_LOSE
import lighttunnel.openapi.TunnelRequest
import lighttunnel.openapi.conn.TunnelConnection
import lighttunnel.openapi.listener.OnRemoteConnectionListener
import lighttunnel.openapi.listener.OnTunnelConnectionListener
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
import kotlin.experimental.or

class Application : AbstractApplication(), OnTunnelConnectionListener, OnRemoteConnectionListener {

    override val options: Options = Options().apply {
        addOption("h", "help", false, "帮助信息")
        addOption("v", "version", false, "版本信息")
        addOption("c", "config", true, "配置文件, 默认为ltc.ini")
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
        val configFilePath = commandLine.getOptionValue("c") ?: "ltc.ini"
        val ini = Ini()
        ini.load(File(configFilePath))
        val basic = ini["basic"] ?: return
        //
        setupLogConf(basic)
        val tunnelClient = getTunnelClient(basic)
        val httpRpcPort = basic["http_rpc_port"].asInt()
        if (httpRpcPort != null) {
            val workerThreads = basic["worker_threads"].asInt() ?: -1
            val bossGroup = NioEventLoopGroup(1)
            val workerGroup = if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
            val httpRpcServer = getHttpRpcServer(
                bossGroup = bossGroup,
                workerGroup = workerGroup,
                bindAddr = null,
                bindPort = httpRpcPort,
                tunnelClient = tunnelClient
            )
            httpRpcServer.start()
        }


        val serverAddr = basic["server_addr"] ?: "127.0.0.1"
        val serverPort = basic["server_port"].asInt() ?: 5080
        val sslContext = getSslContext(basic)
        val sslServerPort = basic["ssl_server_port"].asInt() ?: 5443
        ini.entries
            .filter { it.key != "basic" }
            .map { Pair(it.value["ssl"]?.toUpperCase() == "TRUE", getTunnelRequest(basic, it.value)) }
            .forEach {
                val ssl = it.first
                val tunnelRequest = it.second
                if (tunnelRequest != null) {
                    if (ssl) {
                        tunnelClient.connect(serverAddr, sslServerPort, tunnelRequest, sslContext)
                    } else {
                        tunnelClient.connect(serverAddr, serverPort, tunnelRequest, null)
                    }
                }
            }
    }

    override fun onRemoteConnected(conn: RemoteConnection) {
        logger.info("onRemoteConnected: {}", conn)
    }

    override fun onRemoteDisconnect(conn: RemoteConnection) {
        logger.info("onRemoteDisconnect: {}", conn)
    }

    override fun onTunnelConnecting(conn: TunnelConnection, retryConnect: Boolean) {
        logger.info("onTunnelConnecting: {}, retryConnect: {}", conn, retryConnect)
    }

    override fun onTunnelConnected(conn: TunnelConnection) {
        logger.info("onTunnelConnected: {}", conn)
    }

    override fun onTunnelDisconnect(conn: TunnelConnection, cause: Throwable?) {
        logger.info("onTunnelDisconnect: {}, cause: {}", conn, cause)
    }

    private companion object {
        private val logger by loggerDelegate()

        @Suppress("SameParameterValue")
        private fun getHttpRpcServer(
            bossGroup: NioEventLoopGroup,
            workerGroup: NioEventLoopGroup,
            bindAddr: String?,
            bindPort: Int,
            tunnelClient: TunnelClient
        ): HttpServer {
            return HttpServer(
                bossGroup = bossGroup,
                workerGroup = workerGroup,
                bindAddr = bindAddr,
                bindPort = bindPort
            ) {
                route("/api/version") {
                    val content = JSONObject().apply {
                        put("name", "ltc")
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
                    val content = tunnelClient.getTunnelConnectionList().tunnelConnectionListToJson().let {
                        Unpooled.copiedBuffer(it.toString(2), Charsets.UTF_8)
                    }
                    DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        content
                    ).also {
                        it.headers()
                            .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                            .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
                    }
                }
            }
        }

        private fun List<TunnelConnection>.tunnelConnectionListToJson(): JSONArray {
            return JSONArray(map {
                JSONObject().apply {
                    put("name", it.tunnelRequest.name)
                    put("conn", it.toString())
                }
            })
        }

        private fun getSslContext(basic: Profile.Section): SslContext {
            return try {
                SslContextUtil.forClient(
                    basic["ssl_jks"] ?: "ltc.jks",
                    basic["ssl_store_password"] ?: "ltcpass"
                )
            } catch (e: Exception) {
                logger.warn("tunnel ssl used builtin jks.")
                SslContextUtil.forBuiltinClient()
            }
        }

        private fun Application.getTunnelClient(basic: Profile.Section): TunnelClient {
            return TunnelClient(
                workerThreads = basic["worker_threads"].asInt() ?: -1,
                retryConnectPolicy = RETRY_CONNECT_POLICY_LOSE or RETRY_CONNECT_POLICY_ERROR,
                onTunnelConnectionListener = this,
                onRemoteConnectionListener = this
            )
        }

        private fun getTunnelRequest(basic: Profile.Section, tunnel: Profile.Section): TunnelRequest? {
            val type = tunnel["type"] ?: "tcp"
            return when (type.toUpperCase()) {
                "TCP" -> getTcpTunnelRequest(basic, tunnel)
                "HTTP" -> getHttpOrHttpsTunnelRequest(basic, tunnel, false)
                "HTTPS" -> getHttpOrHttpsTunnelRequest(basic, tunnel, true)
                else -> null
            }
        }

        private fun getTcpTunnelRequest(basic: Profile.Section, tunnel: Profile.Section): TunnelRequest? {
            return TunnelRequest.forTcp(
                localAddr = tunnel["local_addr"] ?: IpAddressUtil.localIpV4 ?: "127.0.0.1",
                localPort = tunnel["local_port"].asInt() ?: 80,
                remotePort = tunnel["remote_port"].asInt() ?: 0
            ) {
                name = tunnel.name
                version = BuildConfig.VERSION_NAME
                authToken = basic["auth_token"]
            }
        }

        private fun getHttpOrHttpsTunnelRequest(basic: Profile.Section, tunnel: Profile.Section, https: Boolean): TunnelRequest? {
            val proxySetHeaders = mapOf(
                *tunnel.entries
                    .filter { it.key.startsWith("pxy_header_set_") && it.value.isNotEmpty() }
                    .map { Pair(it.key.substring("pxy_header_set_".length), it.value.toString()) }.toTypedArray()
            )
            val proxyAddHeaders = mapOf(
                *tunnel.entries
                    .filter { it.key.startsWith("pxy_header_add_") && it.value.isNotEmpty() }
                    .map { Pair(it.key.substring("pxy_header_add_".length), it.value.toString()) }.toTypedArray()
            )
            return TunnelRequest.forHttp(
                https = https,
                localAddr = tunnel["local_addr"] ?: IpAddressUtil.localIpV4 ?: "127.0.0.1",
                localPort = tunnel["local_port"].asInt() ?: 80,
                host = tunnel["host"] ?: return null
            ) {
                name = tunnel.name
                version = BuildConfig.VERSION_NAME
                authToken = basic["auth_token"]
                pxySetHeaders = proxySetHeaders
                pxyAddHeaders = proxyAddHeaders
                enableBasicAuth = tunnel["auth_enable"]?.toUpperCase() == "TRUE"
                basicAuthRealm = tunnel["auth_realm"] ?: "."
                basicAuthUsername = tunnel["auth_username"] ?: "guest"
                basicAuthPassword = tunnel["auth_password"] ?: "guest"
            }
        }

        private fun setupLogConf(basic: Profile.Section) {
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

    }
}