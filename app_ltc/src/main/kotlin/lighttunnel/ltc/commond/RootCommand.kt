package lighttunnel.ltc.commond

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import lighttunnel.client.Client
import lighttunnel.common.extensions.optOrNull
import lighttunnel.common.utils.FileUtils
import lighttunnel.common.utils.IpUtils
import lighttunnel.common.utils.ManifestUtils
import lighttunnel.common.utils.SslContextUtils
import lighttunnel.logger.LoggerConfigure
import lighttunnel.ltc.extensions.asTunnelRequest
import lighttunnel.ltc.internal.DefaultClientListener
import lighttunnel.ltc.internal.LtcWebApi
import org.apache.log4j.Level
import org.json.JSONArray
import org.json.JSONObject
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

    private val configJson by lazy {
        runCatching { JSONObject(File(cmdConfig ?: "ltc.json")) }.getOrDefault(JSONObject())
    }

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
        val workerThreads = configJson.optOrNull("worker_threads") ?: -1
        val serverAddr = configJson.optOrNull<String>("server_addr")?.let { IpUtils.parseAddr(it) } ?: ("127.0.0.1" to 7080)
        val serverSslAddr = configJson.optOrNull<String>("server_ssl_addr")?.let { IpUtils.parseAddr(it) } ?: ("127.0.0.1" to 7443)
        val sslContext = runCatching {
            SslContextUtils.forClient(
                jks = configJson.optOrNull("ssl_jks") ?: "ltc.jks",
                storePassword = configJson.optOrNull("ssl_store_password") ?: "ltcpass",
            )
        }.getOrNull() ?: SslContextUtils.forBuiltinClient()
        val client = Client(
            workerThreads = workerThreads,
            retryConnectPolicy = Client.RETRY_CONNECT_POLICY_LOSE or Client.RETRY_CONNECT_POLICY_ERROR,
            clientListener = DefaultClientListener(),
        )
        val tunnels = configJson.optOrNull<JSONArray>("tunnels") ?: return
        tunnels.filterIsInstance<JSONObject>().forEach {
            val ssl = it.optOrNull<Boolean>("ssl") ?: false
            val useEncryption = it.optOrNull<Boolean>("use_encryption") ?: false
            val tunnelRequest = it.asTunnelRequest(configJson.optOrNull("auth_token"))
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
        val httpApiBindAddr = configJson.optOrNull<String>("bind_http_api_addr")?.let { IpUtils.parseAddr(it) }
        val httpApiUsername = configJson.optOrNull<String>("http_api_username")
        val httpApiPassword = configJson.optOrNull<String>("http_api_password")
        if (httpApiBindAddr != null) {
            LtcWebApi(client).start(
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
            level = Level.toLevel(configJson.optOrNull("log_level"), Level.INFO)
            file = configJson.optOrNull("log_file")
            maxBackupIndex = configJson.optOrNull("log_count") ?: 3
            maxFileSize = configJson.optOrNull("log_size") ?: "1MB"
        }
        LoggerConfigure.configConsole(
            Level.WARN, names = arrayOf(
                "io.netty",
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
}
