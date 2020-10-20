package lighttunnel.cmd.client

import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.ssl.SslContext
import lighttunnel.*
import lighttunnel.TunnelClient.Companion.RETRY_CONNECT_POLICY_ERROR
import lighttunnel.TunnelClient.Companion.RETRY_CONNECT_POLICY_LOSE
import lighttunnel.cmd.AbstractApplication
import lighttunnel.cmd.asInt
import lighttunnel.cmd.localIpV4
import lighttunnel.conn.TunnelConnection
import lighttunnel.ext.*
import lighttunnel.internal.base.util.loggerDelegate
import lighttunnel.listener.OnRemoteConnectionListener
import lighttunnel.listener.OnTunnelConnectionListener
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.apache.log4j.Level
import org.apache.log4j.helpers.OptionConverter
import org.ini4j.Ini
import org.ini4j.Profile
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
            System.out.printf("%s(%d)%n", LightTunnelConfig.VERSION_NAME, LightTunnelConfig.VERSION_CODE)
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
            val httpRpcUsername = basic["http_rpc_username"]
            val httpRpcPassword = basic["http_rpc_password"]
            val bossGroup = NioEventLoopGroup(1)
            val workerGroup = if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
            val httpRpcServer = tunnelClient.newHttpRpcServer(
                bossGroup = bossGroup,
                workerGroup = workerGroup,
                bindAddr = null,
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
                localAddr = tunnel["local_addr"] ?: localIpV4 ?: "127.0.0.1",
                localPort = tunnel["local_port"].asInt() ?: 80,
                remotePort = tunnel["remote_port"].asInt() ?: 0
            ) {
                name = tunnel.name
                version = LightTunnelConfig.VERSION_NAME
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
                localAddr = tunnel["local_addr"] ?: localIpV4 ?: "127.0.0.1",
                localPort = tunnel["local_port"].asInt() ?: 80,
                host = tunnel["host"] ?: return null
            ) {
                name = tunnel.name
                version = LightTunnelConfig.VERSION_NAME
                authToken = basic["auth_token"]
                pxySetHeaders = proxySetHeaders
                pxyAddHeaders = proxyAddHeaders
                enableBasicAuth = tunnel["auth_username"] != null && tunnel["auth_password"] != null
                if (enableBasicAuth) {
                    basicAuthRealm = tunnel["auth_realm"] ?: "."
                    basicAuthUsername = tunnel["auth_username"]
                    basicAuthPassword = tunnel["auth_password"]
                }
            }
        }

        private fun setupLogConf(basic: Profile.Section) {
            val logLevel = Level.toLevel(basic["log_level"], Level.INFO)
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