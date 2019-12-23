package ltcmd.server

import lighttunnel.cmd.CmdLineParser
import lighttunnel.logger.LoggerFactory
import lighttunnel.server.TunnelServer
import lighttunnel.server.interceptor.SimpleRequestInterceptor
import lighttunnel.util.Manifest
import lighttunnel.util.SslContextUtil
import org.apache.log4j.Level
import org.apache.log4j.helpers.OptionConverter
import org.ini4j.Ini
import org.ini4j.Profile
import java.io.File

class Application {

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
        val basic = ini["basic"] ?: return
        setupLogger(basic)
        val authToken = basic["auth_token"]
        val allowPorts = basic["allow_ports"]
        val interceptor = SimpleRequestInterceptor(authToken, allowPorts)
        TunnelServer(
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
        ).start()
    }

    private fun setupLogger(basic: Profile.Section) {
        val logLevel = Level.toLevel(basic["log_level"], Level.OFF)
        val logFile = basic["log_file"] ?: "./logs/lts.log"
        val logCount = basic["log_count"].asInt() ?: 3
        val logSize = basic["log_size"] ?: "1M"
        LoggerFactory.configConsole(Level.OFF, names = *Manifest.thirdPartyLibs)
        LoggerFactory.configConsole(level = logLevel)
        LoggerFactory.configFile(
            level = logLevel,
            file = logFile,
            maxBackupIndex = logCount,
            maxFileSize = OptionConverter.toFileSize(logSize, 1)
        )
    }


    private fun String?.asInt(): Int? {
        return try {
            this?.toInt()
        } catch (e: NumberFormatException) {
            return null
        }
    }

}