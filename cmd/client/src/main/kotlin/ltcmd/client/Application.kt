package ltcmd.client

import io.netty.handler.ssl.SslContext
import lighttunnel.client.LTClient
import lighttunnel.client.LTConnDescriptor
import lighttunnel.client.OnLTClientStateListener
import lighttunnel.cmd.CmdLineParser
import lighttunnel.logging.LoggerFactory
import lighttunnel.logging.logger
import lighttunnel.proto.LTManifest
import lighttunnel.proto.LTRequest
import lighttunnel.util.SslContextUtil
import org.apache.log4j.Level
import org.apache.log4j.helpers.OptionConverter
import org.ini4j.Ini
import org.ini4j.Profile
import java.io.File

class Application : OnLTClientStateListener {
    private val logger by logger()

    override fun onConnecting(descriptor: LTConnDescriptor, reconnect: Boolean) {
        logger.debug("onConnecting: {}", descriptor)
    }

    override fun onConnected(descriptor: LTConnDescriptor) {
        logger.info("onConnected: {}", descriptor)
    }

    override fun onDisconnect(descriptor: LTConnDescriptor, err: Boolean, errCause: Throwable?) {
        logger.info("onDisconnect: {}, err: {}", descriptor, err, errCause)
    }

    fun doMain(args: Array<String>) {
        val cmdLine = CmdLine()
        CmdLineParser.parse(cmdLine, args)
        if (cmdLine.help) {
            System.err.printf("%n%nUsage: %n")
            CmdLineParser.printHelp(cmdLine, System.err, "    ")
            return
        }
        val ini = Ini()
        ini.load(File(cmdLine.iniFile))
        setupLog(ini["log"])
        val basic = ini["basic"] ?: return
        val client = newClient(basic)
        val tunnels = ini.entries
            .filter { it.key.startsWith("tunnel.") }
            .mapNotNull { it.value }
        val serverAddr = basic["server_addr"] ?: return
        val serverPort = basic["server_port"].int()
        val sslContext = newSslContext(ini["ssl"])
        val sslServerPort = ini["ssl"]?.get("server_port").int()
        for (tunnel in tunnels) {
            val request = newTunnelRequest(basic, tunnel) ?: continue
            if (tunnel["ssl"]?.toUpperCase() == "TRUE") {
                if (sslContext != null && sslServerPort != null) {
                    client.connect(serverAddr, sslServerPort, request, sslContext)
                }
            } else {
                if (serverPort != null) {
                    client.connect(serverAddr, serverPort, request, null)
                }
            }
        }
    }

    private fun newSslContext(sslSection: Profile.Section?): SslContext? {
        sslSection ?: return null
        val jks = sslSection["jks"] ?: return null
        val storePassword = sslSection["store_password"] ?: return null
        return SslContextUtil.forClient(jks, storePassword)
    }

    private fun newClient(basicSection: Profile.Section): LTClient {
        val workerThreads = basicSection["worker_threads"].int() ?: -1
        val options = LTClient.Options(
            workerThreads = workerThreads,
            listener = this,
            autoReconnect = true
        )
        return LTClient(options)
    }

    private fun newTunnelRequest(basic: Profile.Section, tunnel: Profile.Section): LTRequest? {
        val type = tunnel["type"] ?: "tcp"
        return when (type.toUpperCase()) {
            "TCP" -> newTcpTunnelRequest(basic, tunnel)
            "HTTP" -> newHttpTunnelRequest(basic, tunnel)
            "HTTPS" -> newHttpsTunnelRequest(basic, tunnel)
            else -> null
        }
    }

    private fun newTcpTunnelRequest(basic: Profile.Section, tunnel: Profile.Section): LTRequest? {
        val authToken = basic["auth_token"] ?: return null
        val localAddr = tunnel["local_addr"] ?: "127.0.0.1"
        val localPort = tunnel["local_port"].int() ?: 80
        val remotePort = tunnel["remote_port"].int() ?: return null
        return LTRequest.ofTcp(
            authToken = authToken,
            localAddr = localAddr,
            localPort = localPort,
            remotePort = remotePort
        )
    }

    private fun newHttpTunnelRequest(
        basic: Profile.Section, tunnel: Profile.Section) = newHttpOrHttpsTunnelRequest(basic, tunnel, false)

    private fun newHttpsTunnelRequest(
        basic: Profile.Section, tunnel: Profile.Section) = newHttpOrHttpsTunnelRequest(basic, tunnel, true)

    private fun newHttpOrHttpsTunnelRequest(
        basic: Profile.Section, tunnel: Profile.Section, https: Boolean): LTRequest? {
        val authToken = basic["auth_token"] ?: return null
        val localAddr = tunnel["local_addr"] ?: "127.0.0.1"
        val localPort = tunnel["local_port"].int() ?: 80
        val customDomain = tunnel["custom_domain"] ?: return null
        val proxySetHeaders = mapOf(
            *tunnel.entries
                .filter { it.key.startsWith("header_set_") && it.value.isNotEmpty() }
                .map { Pair(it.key.substring("header_set_".length), it.value.toString()) }.toTypedArray()
        )
        val proxyAddHeaders = mapOf(
            *tunnel.entries
                .filter { it.key.startsWith("header_add_") && it.value.isNotEmpty() }
                .map { Pair(it.key.substring("header_add_".length), it.value.toString()) }.toTypedArray()
        )
        val enableBasicAuth = tunnel["auth_enable"]?.toUpperCase() == "TRUE"
        val basicAuthRealm = tunnel["auth_realm"] ?: "."
        val basicAuthUsername = tunnel["auth_username"] ?: "guest"
        val basicAuthPassword = tunnel["auth_password"] ?: "guest"
        return LTRequest.ofHttp(
            https = https,
            authToken = authToken,
            localAddr = localAddr,
            localPort = localPort,
            host = customDomain,
            proxySetHeaders = proxySetHeaders,
            proxyAddHeaders = proxyAddHeaders,
            enableBasicAuth = enableBasicAuth,
            basicAuthRealm = basicAuthRealm,
            basicAuthUsername = basicAuthUsername,
            basicAuthPassword = basicAuthPassword
        )
    }

    private fun setupLog(log: Profile.Section?) {
        var level = Level.OFF
        var file: String? = null
        var maxBackupIndex: Int? = null
        var maxFileSize: String? = null
        if (log != null) {
            level = Level.toLevel(log["level"])
            file = log["file"] ?: ""
            maxBackupIndex = log["max_backup_index"].int()
            maxFileSize = log["max_file_size"]
        }
        LoggerFactory.configConsole(Level.OFF, names = *LTManifest.thirdLibs)
        LoggerFactory.configConsole(level = level)
        LoggerFactory.configFile(
            level = level,
            file = file ?: "./logs/ltc.log",
            maxBackupIndex = maxBackupIndex ?: 0,
            maxFileSize = OptionConverter.toFileSize(maxFileSize, 1)
        )
    }

    private fun String?.int(): Int? {
        return try {
            this?.toInt()
        } catch (e: NumberFormatException) {
            return null
        }
    }

}