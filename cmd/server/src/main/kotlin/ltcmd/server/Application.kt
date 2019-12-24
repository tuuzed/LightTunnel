package ltcmd.server

import lighttunnel.cmd.AbstractApplication
import lighttunnel.logger.LoggerFactory
import lighttunnel.server.TunnelServer
import lighttunnel.server.interceptor.SimpleRequestInterceptor
import lighttunnel.util.Manifest
import lighttunnel.util.SslContextUtil
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Options
import org.apache.log4j.Level
import org.apache.log4j.helpers.OptionConverter
import org.ini4j.Ini
import org.ini4j.Profile
import java.io.File

class Application : AbstractApplication() {

    override val options: Options
        get() = Options().apply {
            addOption("h", "help", false, "帮助信息")
            addOption("c", "config", true, "配置文件, 默认为lts.ini")
        }


    override fun main(commandLine: CommandLine) {
        if (commandLine.hasOption("h")) {
            printUsage()
            return
        }
        val configFilePath = commandLine.getOptionValue("c") ?: "lts.ini"
        val ini = Ini()
        ini.load(File(configFilePath))
        val basic = ini["basic"] ?: return
        setupLogger(basic)
        newTunnelServer(basic).start()
    }

    private fun newTunnelServer(basic: Profile.Section): TunnelServer {
        val authToken = basic["auth_token"]
        val allowPorts = basic["allow_ports"]
        val interceptor = SimpleRequestInterceptor(authToken, allowPorts)
        return TunnelServer(
            bossThreads = basic["boss_threads"].asInt() ?: -1,
            workerThreads = basic["worker_threads"].asInt() ?: -1,
            // tcp
            bindAddr = basic["bind_addr"] ?: "0.0.0.0",
            bindPort = basic["bind_port"].asInt() ?: 5080,
            tunnelRequestInterceptor = interceptor,
            // ssl
            sslBindPort = basic["ssl_bind_port"].asInt(),
            sslContext = if (basic["ssl_bind_port"] != null) {
                val jks = basic["ssl_jks"] ?: "lts.jks"
                val storePassword = basic["ssl_key_password"] ?: "ltspass"
                val keyPassword = basic["ssl_store_password"] ?: "ltspass"
                SslContextUtil.forServer(jks, storePassword, keyPassword)
            } else null,
            // http
            httpBindPort = basic["vhost_http_port"].asInt(),
            httpRequestInterceptor = interceptor,
            // https
            httpsBindPort = basic["vhost_https_port"].asInt(),
            httpsContext = if (basic["vhost_https_port"] != null) {
                val jks = basic["vhost_https_jks"] ?: "lts.jks"
                val storePassword = basic["vhost_https_key_password"] ?: "ltspass"
                val keyPassword = basic["vhost_https_store_password"] ?: "ltspass"
                SslContextUtil.forServer(jks, storePassword, keyPassword)
            } else null,
            httpsRequestInterceptor = interceptor
        )
    }

    private fun setupLogger(basic: Profile.Section) {
        val logLevel = Level.toLevel(basic["log_level"], Level.INFO)
        val logFile = basic["log_file"]
        val logCount = basic["log_count"].asInt() ?: 3
        val logSize = basic["log_size"] ?: "1MB"
        LoggerFactory.configConsole(Level.OFF, names = *Manifest.thirdPartyLibs)
        LoggerFactory.configConsole(level = logLevel)
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