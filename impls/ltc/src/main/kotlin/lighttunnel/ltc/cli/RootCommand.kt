package lighttunnel.ltc.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import lighttunnel.client.Client
import lighttunnel.common.entity.TunnelRequest
import lighttunnel.common.extensions.asInt
import lighttunnel.common.utils.*
import lighttunnel.extras.*
import lighttunnel.logger.LoggerConfigure
import lighttunnel.ltc.internal.DefaultClientListener
import lighttunnel.ltc.internal.LtcOpenApi
import org.apache.log4j.Level
import org.ini4j.Ini
import org.ini4j.Profile
import java.io.File
import kotlin.experimental.or

internal class RootCommand : CliktCommand(name = ManifestUtils.appName, invokeWithoutSubcommand = true) {

    private val config by findOrSetObject { mutableMapOf<String, Any>() }

    private val cmdShowVersion: Boolean by option("-v", "--version", help = "Show version").flag()

    private val cmdWorkerThreads: Int by option(
        "--worker-threads",
        help = "Number of worker threads, there is no upper limit by default"
    )
        .int()
        .default(-1)

    private val cmdConfig: String? by option("-c", "--config", help = "config file")
        .default("")

    private val cmdServerAddr: Pair<String, Int> by option(
        "-s",
        "--server-addr",
        help = "Server address, default: 127.0.0.1:7080"
    )
        .convert { IpUtils.parseAddr(it) }
        .default("127.0.0.1" to 7080)

    private val cmdLocalAddr: Pair<String, Int> by option(
        "-l",
        "--local-addr",
        help = "Local address, default: 127.0.0.1:80"
    )
        .convert { IpUtils.parseAddr(it) }
        .default("127.0.0.1" to 80)

    private val cmdAuthToken: String? by option("--auth-token", help = "Auth token")

    private val cmdUseEncryption: Boolean by option("--use-encryption", help = "Data encryption transmission")
        .flag()

    private val cmdLogLevel: Level by option(
        "--log-level",
        help = "Log level, e.g. [OFF|FATAL|ERROR|WARN|INFO|DEBUG|TRACE|ALL], default: INFO"
    )
        .convert { Level.toLevel(it, Level.INFO) }
        .default(Level.INFO)

    private val cmdLogFile: String? by option("--log-file", help = "Save log to file, e.g. ltc.log")

    private val cmdLogFileCount: Int by option("--log-file-count", help = "Log file count, default: 10")
        .int()
        .default(10)

    private val cmdLogFileSize: String by option("--log-file-size", help = "Save log file size, e.g. 1MB")
        .default("1MB")


    /** 是否从命令参数启动 */
    private val isStartFromCommand: Boolean get() = cmdConfig.isNullOrEmpty()

    private val ini: Ini by lazy { Ini().also { it.load(File(cmdConfig ?: "ltc.ini")) } }

    override fun run() {
        // 调试内存泄漏
        // ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED)
        if (cmdShowVersion && currentContext.invokedSubcommand == null) {
            echo("   App-Name: ${ManifestUtils.appName}")
            echo("App-Version: ${ManifestUtils.version}")
            echo(" Build-Date: ${ManifestUtils.buildDate}")
            echo("Commit-Date: ${ManifestUtils.commitDate}")
            echo("Commit-Hash: ${ManifestUtils.commitHash}")
            return
        }
        configureLogger()
        config["WORKER_THREADS"] = cmdWorkerThreads
        config["SERVER_ADDR"] = cmdServerAddr
        config["LOCAL_ADDR"] = cmdLocalAddr
        config["USE_ENCRYPTION"] = cmdUseEncryption
        config["AUTH_TOKEN"] = cmdAuthToken ?: ""
        if (!isStartFromCommand && currentContext.invokedSubcommand == null) {
            startupClient()
            Thread.currentThread().join()
        }
    }

    private fun startupClient() {
        val basic = ini["basic"] ?: return
        val workerThreads = basic["worker_threads"].asInt() ?: -1
        val serverAddr: Pair<String, Int> = basic["server_addr"]?.let { IpUtils.parseAddr(it) } ?: ("127.0.0.1" to 7080)
        val serverSslAddr = basic["server_ssl_addr"]?.let { IpUtils.parseAddr(it) } ?: ("127.0.0.1" to 7443)
        val sslContext = runCatching {
            SslContextUtils.forClient(
                jks = basic["ssl_jks"] ?: "ltc.jks",
                storePassword = basic["ssl_store_password"] ?: "ltcpass",
            )
        }.getOrNull() ?: SslContextUtils.forBuiltinClient()
        val client = Client(
            workerThreads = workerThreads,
            retryConnectPolicy = Client.RETRY_CONNECT_POLICY_LOSE or Client.RETRY_CONNECT_POLICY_ERROR,
            clientListener = DefaultClientListener(),
        )
        ini.entries.filter { it.key != "basic" }.forEach {
            val ssl = it.value["ssl"].toBoolean()
            val useEncryption = it.value["ssl"].toBoolean()
            val tunnelRequest = getTunnelRequest(basic, it.value)
            if (tunnelRequest != null) {
                if (ssl) {
                    client.connect(
                        serverIp = serverSslAddr.first,
                        serverPort = serverSslAddr.second,
                        tunnelRequest = tunnelRequest,
                        useEncryption = useEncryption,
                        sslContext = sslContext,
                    )
                } else {
                    client.connect(
                        serverIp = serverAddr.first,
                        serverPort = serverAddr.second,
                        tunnelRequest = tunnelRequest,
                        useEncryption = useEncryption,
                        sslContext = null,
                    )
                }
            }
        }

        val httpApiBindAddr = basic["bind_http_api_addr"]?.let { IpUtils.parseAddr(it) }
        val httpApiUsername = basic["http_api_username"]
        val httpApiPassword = basic["http_api_password"]
        if (httpApiBindAddr != null) {
            LtcOpenApi(client).start(
                bindIp = httpApiBindAddr.first,
                bindPort = httpApiBindAddr.second,
                authProvider = if (httpApiUsername.isNullOrEmpty() || httpApiPassword.isNullOrEmpty()) {
                    null
                } else {
                    { username, password -> httpApiUsername == username && httpApiPassword == password }
                },
            )
        }
    }

    private fun configureLogger() {
        val level: Level
        val file: String?
        val maxBackupIndex: Int
        val maxFileSize: String
        if (isStartFromCommand) {
            level = cmdLogLevel
            file = cmdLogFile
            maxBackupIndex = cmdLogFileCount
            maxFileSize = cmdLogFileSize
        } else {
            val basic = ini["basic"] ?: return
            level = Level.toLevel(basic["log_level"], Level.INFO)
            file = basic["log_file"]
            maxBackupIndex = basic["log_count"].asInt() ?: 3
            maxFileSize = basic["log_size"] ?: "1MB"
        }
        LoggerConfigure.configConsole(
            Level.WARN, names = arrayOf(
                "io.netty",
                "org.ini4j",
                "org.slf4j",
                "org.json",
            )
        )
        LoggerConfigure.configConsole(cmdLogLevel, conversionPattern = "%-d{yyyy-MM-dd HH:mm:ss} - [%5p] %m%n")
        if (file != null) {
            LoggerConfigure.configFile(
                file = file,
                maxBackupIndex = maxBackupIndex,
                level = level,
                maxFileSize = FileUtils.toFileSize(maxFileSize, 1024 * 1024)
            )
        }
    }

    private fun getTunnelRequest(basic: Profile.Section, tunnel: Profile.Section): TunnelRequest? {
        val type = tunnel["type"] ?: "tcp"
        return when (type.uppercase()) {
            "TCP" -> getTcpTunnelRequest(basic, tunnel)
            "HTTP" -> getHttpOrHttpsTunnelRequest(basic, tunnel, false)
            "HTTPS" -> getHttpOrHttpsTunnelRequest(basic, tunnel, true)
            else -> null
        }
    }

    private fun getTcpTunnelRequest(basic: Profile.Section, tunnel: Profile.Section): TunnelRequest {
        return TunnelRequest.forTcp(
            localIp = tunnel["local_addr"] ?: IpUtils.localIpV4 ?: "127.0.0.1",
            localPort = tunnel["local_port"].asInt() ?: 80,
            remotePort = tunnel["remote_port"].asInt() ?: 0
        ) {
            name = tunnel.name
            os = "${System.getProperty("os.name")}-${System.getProperty("os.arch")}-${System.getProperty("os.version")}"
            version = ManifestUtils.version
            authToken = basic["auth_token"]
        }
    }

    private fun getHttpOrHttpsTunnelRequest(
        basic: Profile.Section,
        tunnel: Profile.Section,
        https: Boolean
    ): TunnelRequest? {
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
            localIp = tunnel["local_addr"] ?: IpUtils.localIpV4 ?: "127.0.0.1",
            localPort = tunnel["local_port"].asInt() ?: 80,
            vhost = tunnel["vhost"] ?: return null
        ) {
            name = tunnel.name
            os = "${System.getProperty("os.name")}-${System.getProperty("os.arch")}-${System.getProperty("os.version")}"
            version = ManifestUtils.version
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

}
