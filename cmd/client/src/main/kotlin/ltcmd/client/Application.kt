package ltcmd.client

import io.netty.handler.ssl.SslContext
import lighttunnel.BuildConfig
import lighttunnel.client.TunnelClient
import lighttunnel.client.TunnelClient.Companion.RETRY_CONNECT_POLICY_ERROR
import lighttunnel.client.TunnelClient.Companion.RETRY_CONNECT_POLICY_LOSE
import lighttunnel.client.conn.TunnelConnection
import lighttunnel.cmd.AbstractApplication
import lighttunnel.cmd.IpAddressUtil
import lighttunnel.cmd.asInt
import lighttunnel.logger.LoggerFactory
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.RemoteConnection
import lighttunnel.proto.TunnelRequest
import lighttunnel.util.SslContextUtil
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Options
import org.apache.log4j.Level
import org.apache.log4j.helpers.OptionConverter
import org.ini4j.Ini
import org.ini4j.Profile
import java.io.File
import kotlin.experimental.or

class Application : AbstractApplication(), TunnelClient.OnTunnelConnectionListener, TunnelClient.OnRemoteConnectionListener {

    override val options: Options
        get() = Options().apply {
            addOption("h", "help", false, "帮助信息")
            addOption("v", "version", false, "版本信息")
            addOption("c", "config", true, "配置文件, 默认为ltc.ini")
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
        val configFilePath = commandLine.getOptionValue("c") ?: "ltc.ini"
        val ini = Ini()
        ini.load(File(configFilePath))
        val basic = ini["basic"] ?: return
        //
        setupLogConf(basic)
        val tunnelClient = getTunnelClient(basic)
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
                httpRpcBindPort = basic["http_rpc_port"].asInt(),
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
                remotePort = tunnel["remote_port"].asInt() ?: 0,
                name = tunnel.name,
                authToken = basic["auth_token"]
            )
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
                localAddr = tunnel["local_addr"] ?: IpAddressUtil.localIpV4 ?: "127.0.0.1",
                localPort = tunnel["local_port"].asInt() ?: 80,
                https = https,
                name = tunnel.name,
                authToken = basic["auth_token"],
                host = tunnel["host"] ?: return null,
                pxySetHeaders = proxySetHeaders,
                pxyAddHeaders = proxyAddHeaders,
                enableBasicAuth = tunnel["auth_enable"]?.toUpperCase() == "TRUE",
                basicAuthRealm = tunnel["auth_realm"] ?: ".",
                basicAuthUsername = tunnel["auth_username"] ?: "guest",
                basicAuthPassword = tunnel["auth_password"] ?: "guest"
            )
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